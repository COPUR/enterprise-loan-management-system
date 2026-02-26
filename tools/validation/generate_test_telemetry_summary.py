#!/usr/bin/env python3

from __future__ import annotations

import argparse
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable
import xml.etree.ElementTree as ET


@dataclass(frozen=True)
class FailureRecord:
    module: str
    testcase: str
    message: str
    source_file: str


@dataclass
class ModuleStats:
    tests: int = 0
    failures: int = 0
    errors: int = 0
    skipped: int = 0
    duration_seconds: float = 0.0


@dataclass
class TelemetryReport:
    result_files: list[Path]
    module_stats: dict[str, ModuleStats]
    failures: list[FailureRecord]

    @property
    def totals(self) -> ModuleStats:
        total = ModuleStats()
        for stats in self.module_stats.values():
            total.tests += stats.tests
            total.failures += stats.failures
            total.errors += stats.errors
            total.skipped += stats.skipped
            total.duration_seconds += stats.duration_seconds
        return total


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Generate CI telemetry summary from JUnit XML reports."
    )
    parser.add_argument(
        "--root",
        default=".",
        help="Repository root containing Gradle module test reports.",
    )
    parser.add_argument(
        "--top-failures",
        type=int,
        default=10,
        help="Number of top failure hotspots to include.",
    )
    return parser.parse_args()


def discover_result_files(root: Path) -> list[Path]:
    return sorted(root.glob("**/build/test-results/test/*.xml"))


def module_from_result_path(path: Path, root: Path) -> str:
    rel = path.relative_to(root)
    parts = rel.parts
    if "build" not in parts:
        return "."
    build_idx = parts.index("build")
    module_parts = parts[:build_idx]
    return "/".join(module_parts) if module_parts else "."


def parse_int(value: str | None) -> int:
    if not value:
        return 0
    try:
        return int(value)
    except ValueError:
        return 0


def parse_float(value: str | None) -> float:
    if not value:
        return 0.0
    try:
        return float(value)
    except ValueError:
        return 0.0


def parse_suite_stats(suite: ET.Element) -> ModuleStats:
    return ModuleStats(
        tests=parse_int(suite.attrib.get("tests")),
        failures=parse_int(suite.attrib.get("failures")),
        errors=parse_int(suite.attrib.get("errors")),
        skipped=parse_int(suite.attrib.get("skipped")),
        duration_seconds=parse_float(suite.attrib.get("time")),
    )


def extract_failures(
    suite: ET.Element,
    module: str,
    source_file: str,
) -> list[FailureRecord]:
    failures: list[FailureRecord] = []
    for testcase in suite.iter("testcase"):
        class_name = testcase.attrib.get("classname", "unknown")
        test_name = testcase.attrib.get("name", "unknown")
        testcase_id = f"{class_name}#{test_name}"

        for tag in ("failure", "error"):
            hit = testcase.find(tag)
            if hit is None:
                continue
            message = (hit.attrib.get("message") or hit.text or "").strip()
            failures.append(
                FailureRecord(
                    module=module,
                    testcase=testcase_id,
                    message=message[:240],
                    source_file=source_file,
                )
            )
            break
    return failures


def collect_report(root: Path) -> TelemetryReport:
    result_files = discover_result_files(root)
    module_stats: dict[str, ModuleStats] = defaultdict(ModuleStats)
    failures: list[FailureRecord] = []

    for xml_path in result_files:
        module = module_from_result_path(xml_path, root)
        try:
            suite = ET.parse(xml_path).getroot()
        except ET.ParseError:
            continue

        stats = parse_suite_stats(suite)
        module_bucket = module_stats[module]
        module_bucket.tests += stats.tests
        module_bucket.failures += stats.failures
        module_bucket.errors += stats.errors
        module_bucket.skipped += stats.skipped
        module_bucket.duration_seconds += stats.duration_seconds
        failures.extend(extract_failures(suite, module, str(xml_path.relative_to(root))))

    return TelemetryReport(
        result_files=result_files,
        module_stats=dict(sorted(module_stats.items())),
        failures=failures,
    )


def format_markdown(report: TelemetryReport, top_failures: int) -> str:
    if not report.result_files:
        return (
            "## CI Test Telemetry\n"
            "- No JUnit XML test result files were found under `**/build/test-results/test/*.xml`.\n"
        )

    totals = report.totals
    lines = [
        "## CI Test Telemetry",
        f"- Result files scanned: `{len(report.result_files)}`",
        f"- Total tests: `{totals.tests}`",
        f"- Failed tests: `{totals.failures + totals.errors}` (failures: `{totals.failures}`, errors: `{totals.errors}`)",
        f"- Skipped tests: `{totals.skipped}`",
        f"- Total test duration: `{totals.duration_seconds:.2f}s`",
        "",
        "### Module Breakdown",
        "| Module | Tests | Failed | Skipped | Duration (s) |",
        "| --- | ---: | ---: | ---: | ---: |",
    ]

    for module, stats in sorted(
        report.module_stats.items(),
        key=lambda item: (item[1].failures + item[1].errors, item[1].tests),
        reverse=True,
    ):
        lines.append(
            f"| `{module}` | {stats.tests} | {stats.failures + stats.errors} | {stats.skipped} | {stats.duration_seconds:.2f} |"
        )

    if report.failures:
        failure_counter = Counter(f.testcase for f in report.failures)
        by_testcase: dict[str, list[FailureRecord]] = defaultdict(list)
        for failure in report.failures:
            by_testcase[failure.testcase].append(failure)

        lines.extend(
            [
                "",
                f"### Top Failure Hotspots (Top {top_failures})",
                "| Test Case | Hits | Module | Message |",
                "| --- | ---: | --- | --- |",
            ]
        )

        for testcase, count in failure_counter.most_common(top_failures):
            first = by_testcase[testcase][0]
            message = first.message.replace("\n", " ").replace("|", "\\|")
            lines.append(f"| `{testcase}` | {count} | `{first.module}` | {message or 'n/a'} |")
    else:
        lines.extend(
            [
                "",
                "### Top Failure Hotspots",
                "- No failing test cases detected.",
            ]
        )

    return "\n".join(lines) + "\n"


def main() -> int:
    args = parse_args()
    root = Path(args.root).resolve()
    report = collect_report(root)
    print(format_markdown(report, max(args.top_failures, 1)))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
