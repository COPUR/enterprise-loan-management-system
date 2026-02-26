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

LEGACY_USE_CASE_PATTERN = re.compile(r"(?:UC|Uc|uc)\d{2,3}")
HTTP_METHOD_PATTERN = re.compile(r"^\s{4}(get|post|put|patch|delete|head|options|trace):\s*$")
PATH_PATTERN = re.compile(r"^\s{2}(/[^:]+):\s*$")
NON_EMPTY_OPENAPI_SPECS = (
    "api/openapi/customer-context.yaml",
    "api/openapi/loan-context.yaml",
    "api/openapi/payment-context.yaml",
    "api/openapi/risk-context.yaml",
    "api/openapi/compliance-context.yaml",
)
SCANNABLE_SOURCE_SUFFIXES = (
    ".java",
    ".kt",
    ".groovy",
    ".kts",
    ".yml",
    ".yaml",
    ".properties",
    ".xml",
    ".json",
    ".md",
    ".txt",
    ".adoc",
    ".sql",
    ".js",
    ".ts",
    ".tsx",
    ".jsx",
    ".py",
    ".sh",
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
    legacy_use_case_hits: list[str] = field(default_factory=list)
    openapi_dpop_issues: list[str] = field(default_factory=list)
    openapi_structure_issues: list[str] = field(default_factory=list)


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


def is_scannable_source_file(path: str) -> bool:
    normalized = path.replace("\\", "/")
    if "/src/" not in f"/{normalized}":
        return False
    if normalized.startswith(".git/") or normalized.startswith(".gradle/"):
        return False
    if "/build/" in f"/{normalized}" or "/target/" in f"/{normalized}" or "/out/" in f"/{normalized}":
        return False
    lowered = normalized.lower()
    return lowered.endswith(SCANNABLE_SOURCE_SUFFIXES)


def scan_source_files_for_legacy_use_case_numbering(root: Path, tracked_files: list[str]) -> list[str]:
    hits: list[str] = []
    for rel_path in sorted(set(tracked_files)):
        if not is_scannable_source_file(rel_path):
            continue
        file_path = root / rel_path
        if not file_path.exists() or not file_path.is_file():
            continue
        try:
            content = file_path.read_text(encoding="utf-8", errors="ignore")
        except OSError:
            continue
        for index, line in enumerate(content.splitlines(), start=1):
            match = LEGACY_USE_CASE_PATTERN.search(line)
            if match:
                hits.append(f"{rel_path}:{index}: {match.group(0)}")
    return hits


def extract_operation_blocks(lines: list[str]) -> list[tuple[str, str, str]]:
    operations: list[tuple[str, str, str]] = []
    current_path = ""
    index = 0
    while index < len(lines):
        line = lines[index]
        path_match = PATH_PATTERN.match(line)
        if path_match:
            current_path = path_match.group(1)
            index += 1
            continue
        method_match = HTTP_METHOD_PATTERN.match(line)
        if method_match and current_path:
            method = method_match.group(1)
            start = index + 1
            end = start
            while end < len(lines):
                if PATH_PATTERN.match(lines[end]) or HTTP_METHOD_PATTERN.match(lines[end]):
                    break
                end += 1
            block = "\n".join(lines[start:end])
            operations.append((current_path, method, block))
            index = end
            continue
        index += 1
    return operations


def operation_requires_dpop(block: str) -> bool:
    has_security = bool(re.search(r"(?m)^\s{6}security:\s*$", block))
    if not has_security:
        return False

    has_dpop_ref = bool(
        re.search(r"\$ref:\s*['\"]#\/components\/parameters\/DPoP['\"]", block)
    )
    if has_dpop_ref:
        return True

    # Accept explicit header declaration as an alternative to component ref.
    return (
        "name: DPoP" in block
        and "in: header" in block
        and re.search(r"(?m)^\s*required:\s*true\s*$", block) is not None
    )


def spec_has_required_dpop_parameter(lines: list[str]) -> bool:
    start = None
    for idx, line in enumerate(lines):
        if re.match(r"^\s{4}DPoP:\s*$", line):
            start = idx + 1
            break
    if start is None:
        return False

    block_lines: list[str] = []
    for idx in range(start, len(lines)):
        line = lines[idx]
        if re.match(r"^\s{4}[A-Za-z0-9_\-]+:\s*$", line):
            break
        block_lines.append(line)

    block = "\n".join(block_lines)
    return (
        re.search(r"(?m)^\s{6}name:\s*DPoP\s*$", block) is not None
        and re.search(r"(?m)^\s{6}in:\s*header\s*$", block) is not None
        and re.search(r"(?m)^\s{6}required:\s*true\s*$", block) is not None
    )


def collect_openapi_dpop_issues(root: Path, tracked_files: list[str]) -> list[str]:
    specs = sorted(
        path
        for path in set(tracked_files)
        if path.startswith("api/openapi/") and path.endswith((".yaml", ".yml"))
    )
    issues: list[str] = []
    for spec in specs:
        file_path = root / spec
        if not file_path.exists() or not file_path.is_file():
            continue
        try:
            lines = file_path.read_text(encoding="utf-8", errors="ignore").splitlines()
        except OSError:
            continue

        protected_operations = []
        for path, method, block in extract_operation_blocks(lines):
            has_security = bool(re.search(r"(?m)^\s{6}security:\s*$", block))
            if not has_security:
                continue
            protected_operations.append((path, method))
            if not operation_requires_dpop(block):
                issues.append(
                    f"{spec}:{path} {method.upper()} missing required DPoP header parameter"
                )

        if protected_operations and not spec_has_required_dpop_parameter(lines):
            issues.append(f"{spec}: components.parameters.DPoP missing or not required=true header")

    return issues


def collect_openapi_structure_issues(root: Path, tracked_files: list[str]) -> list[str]:
    tracked = set(tracked_files)
    issues: list[str] = []

    for spec in NON_EMPTY_OPENAPI_SPECS:
        if spec not in tracked:
            issues.append(f"{spec}: required OpenAPI spec is not tracked")
            continue

        file_path = root / spec
        if not file_path.exists() or not file_path.is_file():
            issues.append(f"{spec}: required OpenAPI spec file is missing")
            continue

        try:
            content = file_path.read_text(encoding="utf-8", errors="ignore")
        except OSError:
            issues.append(f"{spec}: unable to read OpenAPI spec")
            continue

        lines = content.splitlines()
        if re.search(r"(?m)^paths:\s*\{\}\s*$", content):
            issues.append(f"{spec}: paths are empty (paths: {{}})")
            continue

        if not any(PATH_PATTERN.match(line) for line in lines):
            issues.append(f"{spec}: no concrete API paths found under paths")

    return issues


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

    if ctx.legacy_use_case_hits:
        details = "\n".join(f" - {hit}" for hit in ctx.legacy_use_case_hits)
        result.errors.append(
            "ERROR: Source files contain legacy use-case numbering (UCxx/ucxx).\n"
            f"{details}\n"
            "Use capability-based names in code/config/test identifiers."
        )

    if ctx.openapi_dpop_issues:
        details = "\n".join(f" - {hit}" for hit in ctx.openapi_dpop_issues)
        result.errors.append(
            "ERROR: OpenAPI protected operations are missing mandatory DPoP header requirements.\n"
            f"{details}\n"
            "Ensure protected operations define security and required DPoP header parity."
        )

    if ctx.openapi_structure_issues:
        details = "\n".join(f" - {hit}" for hit in ctx.openapi_structure_issues)
        result.errors.append(
            "ERROR: OpenAPI structure validation failed for required bounded-context specs.\n"
            f"{details}\n"
            "Provide concrete paths and keep required context contracts tracked in repository."
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
    override = env.get("GOVERNANCE_DIFF_MODE", "").strip().lower()
    if override == "staged":
        return ("staged", "", "")
    if override == "working-tree":
        return ("working-tree", "", "")

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
    if mode == "staged":
        changed = command_lines(["git", "diff", "--cached", "--name-only"], cwd)
        name_status = command_lines(["git", "diff", "--cached", "--name-status"], cwd)
    elif mode == "working-tree":
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
    if mode == "staged":
        print("Diff mode: staged index")
    elif mode == "working-tree":
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
    deprecated_tracked_files = command_lines(
        ["git", "ls-files", "bankwide", "bank-wide-services", "loan-service", "payment-service"],
        root,
    )
    all_tracked_files = command_lines(["git", "ls-files"], root)
    legacy_use_case_hits = scan_source_files_for_legacy_use_case_numbering(root, all_tracked_files)
    openapi_dpop_issues = collect_openapi_dpop_issues(root, all_tracked_files)
    openapi_structure_issues = collect_openapi_structure_issues(root, all_tracked_files)

    context = ValidationContext(
        changed_files=changed_files,
        deleted_files=deleted_files,
        tracked_files=deprecated_tracked_files,
        settings_content=settings_content,
        allow_legacy_path_edits=flag_from_env(os.environ, "ALLOW_LEGACY_PATH_EDITS"),
        allow_deprecated_root_changes=flag_from_env(os.environ, "ALLOW_DEPRECATED_ROOT_CHANGES"),
        allow_shared_foundation_change=flag_from_env(os.environ, "ALLOW_SHARED_FOUNDATION_CHANGE"),
        strict_deprecated_roots=flag_from_env(os.environ, "STRICT_DEPRECATED_ROOTS"),
        legacy_use_case_hits=legacy_use_case_hits,
        openapi_dpop_issues=openapi_dpop_issues,
        openapi_structure_issues=openapi_structure_issues,
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
