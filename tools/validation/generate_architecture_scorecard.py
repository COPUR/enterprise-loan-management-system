#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


INCLUDE_RE = re.compile(r"^\s*include\s+'([^']+)'\s*$")


@dataclass
class ModuleRecord:
    module_id: str
    path: Path
    origin: str
    exists: bool
    has_build: bool
    main_files: int
    test_files: int
    has_coverage_gate: bool
    has_arch_tests: bool
    has_openapi: bool
    openapi_path: str
    has_jenkins: bool
    has_gitlab: bool
    has_terraform: bool
    classification: str
    score: int
    risks: str


def parse_includes(settings_file: Path) -> list[str]:
    if not settings_file.exists():
        return []
    modules: list[str] = []
    for raw in settings_file.read_text(encoding="utf-8", errors="ignore").splitlines():
        line = raw.strip()
        if not line or line.startswith("//"):
            continue
        match = INCLUDE_RE.match(line)
        if match:
            modules.append(match.group(1))
    return modules


def count_files(path: Path) -> int:
    if not path.exists():
        return 0
    return sum(1 for p in path.rglob("*") if p.is_file())


def read_text(path: Path) -> str:
    if not path.exists():
        return ""
    return path.read_text(encoding="utf-8", errors="ignore")


def service_slug(module_id: str) -> str:
    # services/openfinance-<slug>
    if module_id.startswith("service:openfinance-"):
        return module_id.replace("service:openfinance-", "", 1)
    return ""


def terraform_exists(root: Path, module_id: str) -> bool:
    slug = service_slug(module_id)
    if not slug:
        return False
    candidates = [
        f"infra/terraform/services/{slug}",
        f"infra/terraform/services/{slug.replace('consent-authorization-service', 'consent-and-authorization-service')}",
        f"infra/terraform/services/{slug.replace('confirmation-of-payee-service', 'confirmation-of-payee-service')}",
        f"infra/terraform/services/{slug.replace('open-products-service', 'open-products-service')}",
        f"infra/terraform/services/{slug.replace('personal-financial-data-service', 'personal-financial-data-service')}",
        f"infra/terraform/services/{slug.replace('business-financial-data-service', 'business-financial-data-service')}",
        f"infra/terraform/services/{slug.replace('banking-metadata-service', 'banking-metadata-service')}",
        f"infra/terraform/services/{slug.replace('atm-directory-service', 'atm-directory-service')}",
    ]
    return any((root / c).exists() for c in candidates)


def openapi_for_module(root: Path, module_id: str) -> tuple[bool, str]:
    # root modules
    if ":" in module_id and not module_id.startswith("service:"):
        parent = module_id.split(":")[0]
        candidate = root / "api" / "openapi" / f"{parent}.yaml"
        return candidate.exists(), str(candidate.relative_to(root)) if candidate.exists() else ""

    # standalone openfinance services
    if module_id.startswith("service:openfinance-"):
        slug = module_id.replace("service:openfinance-", "", 1)
        candidate = root / "api" / "openapi" / f"{slug}.yaml"
        return candidate.exists(), str(candidate.relative_to(root)) if candidate.exists() else ""

    # bounded-context extraction stubs
    if module_id.startswith("stub:"):
        ctx = module_id.replace("stub:", "", 1)
        candidate = root / "api" / "openapi" / f"{ctx}.yaml"
        return candidate.exists(), str(candidate.relative_to(root)) if candidate.exists() else ""

    return False, ""


def classify(main_files: int, test_files: int, has_build: bool, has_coverage_gate: bool) -> str:
    if not has_build and main_files == 0 and test_files == 0:
        return "stub"
    if main_files == 0 and test_files == 0:
        return "empty"
    if main_files > 0 and test_files == 0:
        return "needs-tests"
    if main_files > 0 and test_files > 0 and not has_coverage_gate:
        return "needs-gates"
    if main_files > 0 and test_files > 0 and has_coverage_gate:
        return "aligned"
    return "partial"


def compute_score(
    has_build: bool,
    main_files: int,
    test_files: int,
    has_coverage_gate: bool,
    has_arch_tests: bool,
    has_openapi: bool,
    has_jenkins: bool,
    has_gitlab: bool,
    has_terraform: bool,
) -> int:
    score = 0
    score += 15 if has_build else 0
    score += 15 if main_files > 0 else 0
    score += 15 if test_files > 0 else 0
    score += 15 if has_coverage_gate else 0
    score += 10 if has_arch_tests else 0
    score += 10 if has_openapi else 0
    score += 10 if (has_jenkins or has_gitlab) else 0
    score += 10 if has_terraform else 0
    return score


def risk_summary(
    main_files: int,
    test_files: int,
    has_coverage_gate: bool,
    has_openapi: bool,
    has_jenkins: bool,
    has_gitlab: bool,
    has_terraform: bool,
    is_service: bool,
) -> str:
    risks: list[str] = []
    if main_files > 0 and test_files == 0:
        risks.append("missing-tests")
    if main_files > 0 and not has_coverage_gate:
        risks.append("no-coverage-gate")
    if main_files > 0 and not has_openapi:
        risks.append("no-openapi")
    if main_files > 0 and not (has_jenkins or has_gitlab):
        risks.append("no-service-ci")
    if is_service and not has_terraform:
        risks.append("no-terraform-stack")
    return ",".join(risks)


def build_module_records(root: Path) -> list[ModuleRecord]:
    records: list[ModuleRecord] = []

    # 1) Active root modules from settings.gradle
    active_modules = parse_includes(root / "settings.gradle")
    for module_id in sorted(active_modules):
        module_path = root / Path(*module_id.split(":"))
        exists = module_path.exists()
        build_file = module_path / "build.gradle"
        has_build = build_file.exists()
        build_content = read_text(build_file)

        main_files = count_files(module_path / "src" / "main" / "java") + count_files(
            module_path / "src" / "main" / "kotlin"
        )
        test_files = count_files(module_path / "src" / "test" / "java") + count_files(
            module_path / "src" / "test" / "kotlin"
        )
        has_cov = ("jacocoTestCoverageVerification" in build_content) or ("validateBankingTestCoverage" in build_content)
        has_arch = any(token in build_content for token in ("archTest", "architectureTest", "propertyTest"))
        has_openapi, openapi_path = openapi_for_module(root, module_id)
        has_jenkins = (module_path / "Jenkinsfile").exists()
        has_gitlab = (module_path / ".gitlab-ci.yml").exists()
        has_tf = terraform_exists(root, module_id)
        cls = classify(main_files, test_files, has_build, has_cov)
        score = compute_score(has_build, main_files, test_files, has_cov, has_arch, has_openapi, has_jenkins, has_gitlab, has_tf)
        risks = risk_summary(main_files, test_files, has_cov, has_openapi, has_jenkins, has_gitlab, has_tf, False)

        records.append(
            ModuleRecord(
                module_id=module_id,
                path=module_path,
                origin="root-settings",
                exists=exists,
                has_build=has_build,
                main_files=main_files,
                test_files=test_files,
                has_coverage_gate=has_cov,
                has_arch_tests=has_arch,
                has_openapi=has_openapi,
                openapi_path=openapi_path,
                has_jenkins=has_jenkins,
                has_gitlab=has_gitlab,
                has_terraform=has_tf,
                classification=cls,
                score=score,
                risks=risks,
            )
        )

    # 2) Standalone service modules under services/openfinance-*
    services_root = root / "services"
    if services_root.exists():
        for service_dir in sorted(p for p in services_root.iterdir() if p.is_dir() and p.name.startswith("openfinance-")):
            module_id = f"service:{service_dir.name}"
            build_file = service_dir / "build.gradle"
            has_build = build_file.exists()
            build_content = read_text(build_file)
            main_files = count_files(service_dir / "src" / "main" / "java") + count_files(service_dir / "src" / "main" / "kotlin")
            test_files = count_files(service_dir / "src" / "test" / "java") + count_files(service_dir / "src" / "test" / "kotlin")
            has_cov = "jacocoTestCoverageVerification" in build_content
            has_arch = any(token in build_content for token in ("archTest", "architectureTest", "propertyTest"))
            has_openapi, openapi_path = openapi_for_module(root, module_id)
            has_jenkins = (service_dir / "Jenkinsfile").exists()
            has_gitlab = (service_dir / ".gitlab-ci.yml").exists()
            has_tf = terraform_exists(root, module_id)
            cls = classify(main_files, test_files, has_build, has_cov)
            score = compute_score(has_build, main_files, test_files, has_cov, has_arch, has_openapi, has_jenkins, has_gitlab, has_tf)
            risks = risk_summary(main_files, test_files, has_cov, has_openapi, has_jenkins, has_gitlab, has_tf, True)

            records.append(
                ModuleRecord(
                    module_id=module_id,
                    path=service_dir,
                    origin="standalone-service",
                    exists=True,
                    has_build=has_build,
                    main_files=main_files,
                    test_files=test_files,
                    has_coverage_gate=has_cov,
                    has_arch_tests=has_arch,
                    has_openapi=has_openapi,
                    openapi_path=openapi_path,
                    has_jenkins=has_jenkins,
                    has_gitlab=has_gitlab,
                    has_terraform=has_tf,
                    classification=cls,
                    score=score,
                    risks=risks,
                )
            )

    # 3) Extraction stubs under services/bounded-contexts
    stub_root = root / "services" / "bounded-contexts"
    if stub_root.exists():
        for stub_dir in sorted(p for p in stub_root.iterdir() if p.is_dir()):
            module_id = f"stub:{stub_dir.name}"
            has_openapi, openapi_path = openapi_for_module(root, module_id)
            has_jenkins = (stub_dir / "Jenkinsfile").exists()
            has_gitlab = (stub_dir / ".gitlab-ci.yml").exists()
            score = compute_score(False, 0, 0, False, False, has_openapi, has_jenkins, has_gitlab, False)
            records.append(
                ModuleRecord(
                    module_id=module_id,
                    path=stub_dir,
                    origin="extraction-stub",
                    exists=True,
                    has_build=False,
                    main_files=0,
                    test_files=0,
                    has_coverage_gate=False,
                    has_arch_tests=False,
                    has_openapi=has_openapi,
                    openapi_path=openapi_path,
                    has_jenkins=has_jenkins,
                    has_gitlab=has_gitlab,
                    has_terraform=False,
                    classification="stub",
                    score=score,
                    risks="stub-only",
                )
            )

    return records


def write_csv(records: Iterable[ModuleRecord], output_file: Path, root: Path) -> None:
    output_file.parent.mkdir(parents=True, exist_ok=True)
    fieldnames = [
        "module_id",
        "origin",
        "path",
        "exists",
        "has_build",
        "main_files",
        "test_files",
        "has_coverage_gate",
        "has_arch_tests",
        "has_openapi",
        "openapi_path",
        "has_jenkins",
        "has_gitlab",
        "has_terraform",
        "classification",
        "score",
        "risks",
    ]
    with output_file.open("w", encoding="utf-8", newline="") as fh:
        writer = csv.DictWriter(fh, fieldnames=fieldnames)
        writer.writeheader()
        for r in sorted(records, key=lambda x: (-x.score, x.module_id)):
            writer.writerow(
                {
                    "module_id": r.module_id,
                    "origin": r.origin,
                    "path": str(r.path.relative_to(root)),
                    "exists": str(r.exists).lower(),
                    "has_build": str(r.has_build).lower(),
                    "main_files": r.main_files,
                    "test_files": r.test_files,
                    "has_coverage_gate": str(r.has_coverage_gate).lower(),
                    "has_arch_tests": str(r.has_arch_tests).lower(),
                    "has_openapi": str(r.has_openapi).lower(),
                    "openapi_path": r.openapi_path,
                    "has_jenkins": str(r.has_jenkins).lower(),
                    "has_gitlab": str(r.has_gitlab).lower(),
                    "has_terraform": str(r.has_terraform).lower(),
                    "classification": r.classification,
                    "score": r.score,
                    "risks": r.risks,
                }
            )


def write_markdown(records: Iterable[ModuleRecord], output_file: Path, root: Path) -> None:
    output_file.parent.mkdir(parents=True, exist_ok=True)
    records_sorted = sorted(records, key=lambda x: (-x.score, x.module_id))
    total = len(records_sorted)
    aligned = sum(1 for r in records_sorted if r.classification == "aligned")
    needs_tests = sum(1 for r in records_sorted if r.classification == "needs-tests")
    needs_gates = sum(1 for r in records_sorted if r.classification == "needs-gates")
    stubs = sum(1 for r in records_sorted if r.classification == "stub")
    avg_score = round(sum(r.score for r in records_sorted) / total, 1) if total else 0.0

    with output_file.open("w", encoding="utf-8") as fh:
        fh.write("# Architecture Scorecard (Wave 0 Baseline)\n\n")
        fh.write("## Summary\n")
        fh.write(f"- Total modules assessed: {total}\n")
        fh.write(f"- Aligned modules: {aligned}\n")
        fh.write(f"- Modules needing tests: {needs_tests}\n")
        fh.write(f"- Modules needing gates: {needs_gates}\n")
        fh.write(f"- Stub modules: {stubs}\n")
        fh.write(f"- Average score: {avg_score}/100\n\n")

        fh.write("## Top Risk Modules\n")
        risky = [r for r in records_sorted if r.risks and r.risks != "stub-only"]
        for r in sorted(risky, key=lambda x: (x.score, x.module_id))[:10]:
            fh.write(f"- `{r.module_id}` ({r.score}/100): {r.risks}\n")
        fh.write("\n")

        fh.write("## Detailed Scorecard\n")
        fh.write("| Module | Origin | Score | Class | Main | Test | Cov Gate | Arch Tests | OpenAPI | CI | Terraform | Risks |\n")
        fh.write("|---|---|---:|---|---:|---:|---|---|---|---|---|---|\n")
        for r in records_sorted:
            ci = "Y" if (r.has_jenkins or r.has_gitlab) else "N"
            fh.write(
                f"| `{r.module_id}` | {r.origin} | {r.score} | {r.classification} | {r.main_files} | {r.test_files} | "
                f"{'Y' if r.has_coverage_gate else 'N'} | {'Y' if r.has_arch_tests else 'N'} | "
                f"{'Y' if r.has_openapi else 'N'} | {ci} | {'Y' if r.has_terraform else 'N'} | {r.risks or '-'} |\n"
            )

        fh.write("\n## Artifact Paths\n")
        fh.write(f"- CSV: `{output_file.with_suffix('.csv').relative_to(root)}`\n")
        fh.write(f"- Markdown: `{output_file.relative_to(root)}`\n")


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate architecture alignment scorecard for modules/services.")
    parser.add_argument("--root", default=".", help="Repository root path")
    parser.add_argument(
        "--output-dir",
        default="docs/enterprisearchitecture/implementation-development/transformation/outputs",
        help="Directory where scorecard artifacts are written",
    )
    parser.add_argument("--basename", default="architecture-scorecard-latest", help="Base filename (without extension)")
    args = parser.parse_args()

    root = Path(args.root).resolve()
    output_dir = (root / args.output_dir).resolve()
    md_path = output_dir / f"{args.basename}.md"
    csv_path = output_dir / f"{args.basename}.csv"

    records = build_module_records(root)
    write_csv(records, csv_path, root)
    write_markdown(records, md_path, root)

    print(f"Wrote: {md_path}")
    print(f"Wrote: {csv_path}")
    print(f"Modules assessed: {len(records)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

