import os
import sys
import unittest
from pathlib import Path
from unittest.mock import MagicMock, patch

ROOT = Path(__file__).resolve().parents[3]
VALIDATION_DIR = ROOT / "tools" / "validation"
if str(VALIDATION_DIR) not in sys.path:
    sys.path.insert(0, str(VALIDATION_DIR))

import repo_governance_validator as validator  # noqa: E402


class RepoGovernanceValidatorTests(unittest.TestCase):
    def base_ctx(self):
        return validator.ValidationContext(
            changed_files=[],
            deleted_files=[],
            tracked_files=[],
            settings_content="",
            allow_legacy_path_edits=False,
            allow_deprecated_root_changes=False,
            allow_shared_foundation_change=False,
            strict_deprecated_roots=False,
        )

    def test_blocks_frozen_path_changes(self):
        ctx = self.base_ctx()
        ctx.changed_files = ["archive/old.txt"]
        result = validator.evaluate_context(ctx)
        self.assertEqual(result.exit_code, 1)
        self.assertTrue(any("frozen legacy paths" in e for e in result.errors))

    def test_allows_frozen_path_readme_updates(self):
        ctx = self.base_ctx()
        ctx.changed_files = ["archive/README.md", "temp-src/README.md", "simple-test/README.md"]
        result = validator.evaluate_context(ctx)
        self.assertEqual(result.exit_code, 0)

    def test_blocks_deprecated_root_code_changes(self):
        ctx = self.base_ctx()
        ctx.changed_files = ["loan-service/src/main/java/App.java"]
        result = validator.evaluate_context(ctx)
        self.assertEqual(result.exit_code, 1)
        self.assertTrue(any("deprecated roots" in e for e in result.errors))

    def test_allows_deleted_deprecated_root_files(self):
        ctx = self.base_ctx()
        ctx.changed_files = ["bankwide/build.gradle"]
        ctx.deleted_files = ["bankwide/build.gradle"]
        result = validator.evaluate_context(ctx)
        self.assertEqual(result.exit_code, 0)

    def test_blocks_shared_foundation_without_adr(self):
        ctx = self.base_ctx()
        ctx.changed_files = ["shared-kernel/src/main/java/A.java"]
        result = validator.evaluate_context(ctx)
        self.assertEqual(result.exit_code, 1)
        self.assertTrue(any("Shared foundation changed" in e for e in result.errors))

    def test_allows_shared_foundation_with_adr(self):
        ctx = self.base_ctx()
        ctx.changed_files = [
            "shared-infrastructure/src/main/java/B.java",
            "docs/architecture/adr/ADR-999-sample.md",
        ]
        result = validator.evaluate_context(ctx)
        self.assertEqual(result.exit_code, 0)

    def test_blocks_settings_with_deprecated_includes(self):
        ctx = self.base_ctx()
        ctx.settings_content = "include 'loan-service'\ninclude 'customer-context:customer-domain'\n"
        result = validator.evaluate_context(ctx)
        self.assertEqual(result.exit_code, 1)
        self.assertTrue(any("settings.gradle" in e for e in result.errors))

    def test_warns_on_residual_tracked_files(self):
        ctx = self.base_ctx()
        ctx.tracked_files = ["bankwide/build.gradle", "bankwide/README.md"]
        result = validator.evaluate_context(ctx)
        self.assertEqual(result.exit_code, 0)
        self.assertTrue(any("Residual tracked files" in w for w in result.warnings))

    def test_ignores_residual_when_marked_deleted(self):
        ctx = self.base_ctx()
        ctx.tracked_files = ["bankwide/build.gradle", "bankwide/README.md"]
        ctx.deleted_files = ["bankwide/build.gradle"]
        result = validator.evaluate_context(ctx)
        self.assertEqual(result.exit_code, 0)
        self.assertEqual(result.warnings, [])

    def test_strict_deprecated_roots_escalates_warning_to_error(self):
        ctx = self.base_ctx()
        ctx.tracked_files = ["bankwide/build.gradle"]
        ctx.strict_deprecated_roots = True
        result = validator.evaluate_context(ctx)
        self.assertEqual(result.exit_code, 1)
        self.assertTrue(any("STRICT_DEPRECATED_ROOTS" in e for e in result.errors))

    def test_honors_override_flags(self):
        ctx = self.base_ctx()
        ctx.changed_files = ["archive/a.txt", "loan-service/code.java", "shared-kernel/x.java"]
        ctx.allow_legacy_path_edits = True
        ctx.allow_deprecated_root_changes = True
        ctx.allow_shared_foundation_change = True
        result = validator.evaluate_context(ctx)
        self.assertEqual(result.exit_code, 0)

    def test_required_docs_validation(self):
        missing = validator.find_missing_required_docs(
            existing_paths=[
                "docs/architecture/REPOSITORY_STRUCTURE_POLICY.md",
                "docs/GENERAL_BACKLOG.md",
            ]
        )
        self.assertEqual(missing, ["docs/architecture/MODULE_OWNERSHIP_MAP.md"])

    def test_env_flag_parser(self):
        env = {"A": "true", "B": "TRUE", "C": "false", "D": ""}
        self.assertTrue(validator.flag_from_env(env, "A"))
        self.assertTrue(validator.flag_from_env(env, "B"))
        self.assertFalse(validator.flag_from_env(env, "C"))
        self.assertFalse(validator.flag_from_env(env, "D"))
        self.assertFalse(validator.flag_from_env(env, "MISSING"))

    def test_find_residual_skips_non_deprecated_paths(self):
        residual = validator.find_residual_tracked_files(
            tracked_files=["docs/README.md", "bankwide/README.md", "bankwide/build.gradle"],
            deleted_files=set(),
        )
        self.assertEqual(residual, ["bankwide/build.gradle"])

    def test_run_command_and_command_lines(self):
        completed = validator.run_command(["python3", "-c", "print('ok')"], Path("."))
        self.assertEqual(completed.returncode, 0)
        self.assertIn("ok", completed.stdout)

        lines = validator.command_lines(["python3", "-c", "print('a')\nprint('b')"], Path("."))
        self.assertEqual(lines, ["a", "b"])

        fail_lines = validator.command_lines(["python3", "-c", "import sys; sys.exit(2)"], Path("."))
        self.assertEqual(fail_lines, [])

    def test_detect_diff_mode_variants(self):
        mode = validator.detect_diff_mode(Path("."), {"GITHUB_BASE_SHA": "abc", "GITHUB_SHA": "def"})
        self.assertEqual(mode, ("commit-range", "abc", "def"))

        mode = validator.detect_diff_mode(Path("."), {})
        self.assertEqual(mode, ("working-tree", "", ""))

        with patch.object(validator, "run_command") as run_cmd:
            run_cmd.return_value = MagicMock(returncode=0, stdout="")
            mode = validator.detect_diff_mode(Path("."), {"CI": "true"})
            self.assertEqual(mode, ("commit-range", "HEAD~1", "HEAD"))

        with patch.object(validator, "run_command") as run_cmd:
            run_cmd.side_effect = [
                MagicMock(returncode=1, stdout=""),
                MagicMock(returncode=0, stdout="emptytreehash\n"),
            ]
            mode = validator.detect_diff_mode(Path("."), {"CI": "true"})
            self.assertEqual(mode, ("commit-range", "emptytreehash", "HEAD"))

    def test_collect_changed_and_deleted_variants(self):
        with patch.object(validator, "command_lines") as cmd_lines:
            cmd_lines.side_effect = [
                ["a.txt", "b.txt"],
                ["M a.txt", "D b.txt"],
            ]
            changed, deleted = validator.collect_changed_and_deleted(Path("."), "working-tree", "", "")
            self.assertEqual(changed, ["a.txt", "b.txt"])
            self.assertEqual(deleted, ["b.txt"])

        with patch.object(validator, "command_lines") as cmd_lines:
            cmd_lines.side_effect = [
                ["c.txt"],
                ["D c.txt"],
            ]
            changed, deleted = validator.collect_changed_and_deleted(Path("."), "commit-range", "x", "y")
            self.assertEqual(changed, ["c.txt"])
            self.assertEqual(deleted, ["c.txt"])

    @patch.object(validator, "detect_diff_mode")
    @patch.object(validator, "collect_changed_and_deleted")
    def test_cli_no_changes(self, collect_changed_and_deleted_mock, detect_diff_mode_mock):
        detect_diff_mode_mock.return_value = ("working-tree", "", "")
        collect_changed_and_deleted_mock.return_value = ([], [])
        self.assertEqual(validator.cli(), 0)

    @patch.object(validator, "detect_diff_mode")
    @patch.object(validator, "collect_changed_and_deleted")
    def test_cli_missing_required_docs(self, collect_changed_and_deleted_mock, detect_diff_mode_mock):
        detect_diff_mode_mock.return_value = ("working-tree", "", "")
        collect_changed_and_deleted_mock.return_value = (["a.txt"], [])
        with patch.object(validator, "REQUIRED_DOCS", ("docs/architecture/DOES_NOT_EXIST.md",)):
            self.assertEqual(validator.cli(), 1)

    @patch.object(validator, "detect_diff_mode")
    @patch.object(validator, "collect_changed_and_deleted")
    @patch.object(validator, "command_lines")
    @patch.object(validator, "evaluate_context")
    def test_cli_success_and_error_paths(
        self,
        evaluate_context_mock,
        command_lines_mock,
        collect_changed_and_deleted_mock,
        detect_diff_mode_mock,
    ):
        detect_diff_mode_mock.return_value = ("working-tree", "", "")
        collect_changed_and_deleted_mock.return_value = (["a.txt"], [])
        command_lines_mock.return_value = []

        evaluate_context_mock.return_value = validator.ValidationResult(errors=[], warnings=["warn"])
        self.assertEqual(validator.cli(), 0)

        evaluate_context_mock.return_value = validator.ValidationResult(errors=["err"], warnings=[])
        self.assertEqual(validator.cli(), 1)


if __name__ == "__main__":
    unittest.main()
