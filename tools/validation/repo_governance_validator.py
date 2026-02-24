#!/usr/bin/env python3

from __future__ import annotations

import os
import re
import subprocess
import sys
from dataclasses import dataclass, field
from pathlib import Path


RESTRICTED_PATTERNS = (
    "archive/",
    "temp-src/",
    "simple-test/",
)

RESTRICTED_ALLOWED_FILES = (
    "archive/README.md",
    "temp-src/README.md",
    "simple-test/README.md",
)

DEPRECATED_ROOT_PATTERNS = (
    "bankwide/",
    "bank-wide-services/",
    "loan-service/",
    "payment-service/",
)

DEPRECATED_ALLOWED_FILES = (
    "bankwide/README.md",
    "bank-wide-services/README.md",
    "loan-service/README.md",
    "payment-service/README.md",
)

SHARED_FOUNDATION_PATTERNS = (
    "shared-kernel/",
    "shared-infrastructure/",
)

ADR_PATTERNS = (
    "docs/architecture/adr/",
    "docs/architecture/decisions/",
)

REQUIRED_DOCS = (
    "docs/architecture/REPOSITORY_STRUCTURE_POLICY.md",
    "docs/architecture/MODULE_OWNERSHIP_MAP.md",
    "docs/GENERAL_BACKLOG.md",
)

DEPRECATED_SETTINGS_INCLUDES = (
    "include 'bankwide",
    "include 'bank-wide-services",
    "include 'loan-service",
    "include 'payment-service",
)


@dataclass
class ValidationContext:
    changed_files: list[str]
    deleted_files: list[str]
    tracked_files: list[str]
    settings_content: str
    allow_legacy_path_edits: bool
    allow_deprecated_root_changes: bool
    allow_shared_foundation_change: bool
    strict_deprecated_roots: bool


@dataclass
class ValidationResult:
    errors: list[str] = field(default_factory=list)
    warnings: list[str] = field(default_factory=list)

    @property
    def exit_code(self) -> int:
        return 1 if self.errors else 0


def flag_from_env(env: dict[str, str], key: str) -> bool:
    return env.get(key, "").strip().lower() == "true"


def find_missing_required_docs(existing_paths: list[str]) -> list[str]:
    existing = set(existing_paths)
    return [doc for doc in REQUIRED_DOCS if doc not in existing]


def matches_any_prefix(path: str, patterns: tuple[str, ...]) -> bool:
    return any(path.startswith(pattern) for pattern in patterns)


def is_allowed_or_deleted(path: str, allowed_files: tuple[str, ...], deleted_files: set[str]) -> bool:
    if path in allowed_files:
        return True
    if path in deleted_files:
        return True
    return False


def collect_blocked_changes(
    changed_files: list[str],
    patterns: tuple[str, ...],
    allowed_files: tuple[str, ...],
    deleted_files: set[str],
) -> list[str]:
    hits = []
    for file_path in changed_files:
        if matches_any_prefix(file_path, patterns) and not is_allowed_or_deleted(
            file_path, allowed_files, deleted_files
        ):
            hits.append(file_path)
    return hits


def collect_shared_hits(changed_files: list[str]) -> list[str]:
    return [f for f in changed_files if matches_any_prefix(f, SHARED_FOUNDATION_PATTERNS)]


def collect_adr_hits(changed_files: list[str]) -> list[str]:
    return [f for f in changed_files if matches_any_prefix(f, ADR_PATTERNS)]


def has_deprecated_includes(settings_content: str) -> bool:
    compact = re.sub(r"\s+", " ", settings_content)
    return any(token in compact for token in DEPRECATED_SETTINGS_INCLUDES)


def find_residual_tracked_files(tracked_files: list[str], deleted_files: set[str]) -> list[str]:
    residual = []
    for file_path in tracked_files:
        if not matches_any_prefix(file_path, DEPRECATED_ROOT_PATTERNS):
            continue
        if file_path.endswith("/README.md"):
            continue
        if file_path in deleted_files:
            continue
        residual.append(file_path)
    return residual


def evaluate_context(ctx: ValidationContext) -> ValidationResult:
    result = ValidationResult()
    deleted = set(ctx.deleted_files)

    restricted_hits = collect_blocked_changes(
        ctx.changed_files, RESTRICTED_PATTERNS, RESTRICTED_ALLOWED_FILES, deleted
    )
    if restricted_hits and not ctx.allow_legacy_path_edits:
        details = "\n".join(f" - {hit}" for hit in restricted_hits)
        result.errors.append(
            "ERROR: Changes detected in frozen legacy paths.\n"
            f"{details}\n"
            "Set ALLOW_LEGACY_PATH_EDITS=true only for approved migration work."
        )

    deprecated_hits = collect_blocked_changes(
        ctx.changed_files, DEPRECATED_ROOT_PATTERNS, DEPRECATED_ALLOWED_FILES, deleted
    )
    if deprecated_hits and not ctx.allow_deprecated_root_changes:
        details = "\n".join(f" - {hit}" for hit in deprecated_hits)
        result.errors.append(
            "ERROR: Changes detected in deprecated roots.\n"
            f"{details}\n"
            "Move changes to canonical modules or set ALLOW_DEPRECATED_ROOT_CHANGES=true for approved migration work."
        )

    if has_deprecated_includes(ctx.settings_content):
        result.errors.append(
            "ERROR: Deprecated roots are referenced in settings.gradle includes.\n"
            "Use canonical modules under *-context/ and services/ instead."
        )

    residual = find_residual_tracked_files(ctx.tracked_files, deleted)
    if residual:
        details = "\n".join(residual)
        message = (
            "WARN: Residual tracked files exist in deprecated roots:\n"
            f"{details}"
        )
        result.warnings.append(message)
        if ctx.strict_deprecated_roots:
            result.errors.append(
                "ERROR: STRICT_DEPRECATED_ROOTS=true and residual files were detected."
            )

    shared_hits = collect_shared_hits(ctx.changed_files)
    adr_hits = collect_adr_hits(ctx.changed_files)
    if shared_hits and not adr_hits and not ctx.allow_shared_foundation_change:
        details = "\n".join(f" - {hit}" for hit in shared_hits)
        result.errors.append(
            "ERROR: Shared foundation changed without ADR/decision update.\n"
            f"{details}\n"
            "Expected at least one accompanying change in docs/architecture/adr/ or docs/architecture/decisions/.\n"
            "Set ALLOW_SHARED_FOUNDATION_CHANGE=true only for pre-approved emergency work."
        )

    return result


def run_command(args: list[str], cwd: Path) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        args,
        cwd=str(cwd),
        check=False,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )


def command_lines(args: list[str], cwd: Path) -> list[str]:
    completed = run_command(args, cwd)
    if completed.returncode != 0:
        return []
    return [line.strip() for line in completed.stdout.splitlines() if line.strip()]


def detect_diff_mode(cwd: Path, env: dict[str, str]) -> tuple[str, str, str]:
    base_sha = env.get("GITHUB_BASE_SHA", "")
    head_sha = env.get("GITHUB_SHA", "")
    if base_sha and head_sha:
        return ("commit-range", base_sha, head_sha)

    if not env.get("CI", ""):
        return ("working-tree", "", "")

    has_prev = run_command(["git", "rev-parse", "--verify", "HEAD~1"], cwd).returncode == 0
    if has_prev:
        return ("commit-range", "HEAD~1", "HEAD")

    empty_tree = run_command(["git", "hash-object", "-t", "tree", "/dev/null"], cwd)
    base_ref = empty_tree.stdout.strip() if empty_tree.returncode == 0 else ""
    return ("commit-range", base_ref, "HEAD")


def collect_changed_and_deleted(cwd: Path, mode: str, base: str, head: str) -> tuple[list[str], list[str]]:
    if mode == "working-tree":
        changed = command_lines(["git", "diff", "--name-only", "HEAD"], cwd)
        name_status = command_lines(["git", "diff", "--name-status", "HEAD"], cwd)
    else:
        changed = command_lines(["git", "diff", "--name-only", base, head], cwd)
        name_status = command_lines(["git", "diff", "--name-status", base, head], cwd)

    deleted = []
    for line in name_status:
        parts = line.split(maxsplit=1)
        if len(parts) == 2 and parts[0] == "D":
            deleted.append(parts[1])
    return changed, deleted


def cli() -> int:
    root = Path(__file__).resolve().parents[2]
    os.chdir(root)

    print("==> Running repository governance checks")

    mode, base_ref, head_ref = detect_diff_mode(root, os.environ)
    if mode == "working-tree":
        print("Diff mode: working-tree (HEAD vs local changes)")
    else:
        print(f"Diff range: {base_ref}..{head_ref}")

    changed_files, deleted_files = collect_changed_and_deleted(root, mode, base_ref, head_ref)
    if not changed_files:
        print("No changed files detected. Governance checks passed.")
        return 0

    missing_docs = find_missing_required_docs(
        [doc for doc in REQUIRED_DOCS if (root / doc).exists()]
    )
    if missing_docs:
        for doc in missing_docs:
            print(f"ERROR: Required governance document missing: {doc}")
        return 1

    settings_path = root / "settings.gradle"
    settings_content = settings_path.read_text(encoding="utf-8") if settings_path.exists() else ""
    tracked_files = command_lines(
        ["git", "ls-files", "bankwide", "bank-wide-services", "loan-service", "payment-service"],
        root,
    )

    context = ValidationContext(
        changed_files=changed_files,
        deleted_files=deleted_files,
        tracked_files=tracked_files,
        settings_content=settings_content,
        allow_legacy_path_edits=flag_from_env(os.environ, "ALLOW_LEGACY_PATH_EDITS"),
        allow_deprecated_root_changes=flag_from_env(os.environ, "ALLOW_DEPRECATED_ROOT_CHANGES"),
        allow_shared_foundation_change=flag_from_env(os.environ, "ALLOW_SHARED_FOUNDATION_CHANGE"),
        strict_deprecated_roots=flag_from_env(os.environ, "STRICT_DEPRECATED_ROOTS"),
    )

    result = evaluate_context(context)

    for warning in result.warnings:
        print(warning)

    for error in result.errors:
        print(error)

    if result.exit_code == 0:
        print("Repository governance checks passed.")

    return result.exit_code


if __name__ == "__main__":
    sys.exit(cli())
