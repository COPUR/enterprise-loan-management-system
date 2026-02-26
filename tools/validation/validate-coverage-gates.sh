#!/usr/bin/env bash

set -euo pipefail

SCRIPT_NAME="$(basename "$0")"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
REPORT_DIR="${REPO_ROOT}/build/reports/coverage-gates"
THRESHOLD="${COVERAGE_THRESHOLD:-0.85}"

usage() {
    cat <<EOF
Usage: ${SCRIPT_NAME} [--group <group>] [--threshold <ratio>]

Groups:
  all               Run active runtime coverage target modules (default)
  customer-context  Customer context modules
  loan-context      Loan context modules
  payment-context   Payment context modules
  risk-context      Risk context modules
  compliance-context Compliance context modules
  incubating        Shared foundation and incubating framework modules
  shared-foundation Shared kernel and shared infrastructure
  masrufi           Masrufi framework
EOF
}

GROUP="all"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --group)
            GROUP="${2:-}"
            shift 2
            ;;
        --threshold)
            THRESHOLD="${2:-}"
            shift 2
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown argument: $1" >&2
            usage
            exit 2
            ;;
    esac
done

declare -a MODULES=()

case "${GROUP}" in
    all)
        MODULES=(
            ":customer-context:customer-domain"
            ":customer-context:customer-application"
            ":customer-context:customer-infrastructure"
            ":loan-context:loan-domain"
            ":loan-context:loan-application"
            ":loan-context:loan-infrastructure"
            ":payment-context:payment-domain"
            ":payment-context:payment-application"
            ":payment-context:payment-infrastructure"
            ":risk-context:risk-domain"
            ":risk-context:risk-application"
            ":risk-context:risk-infrastructure"
            ":compliance-context:compliance-domain"
            ":compliance-context:compliance-application"
            ":compliance-context:compliance-infrastructure"
        )
        ;;
    incubating)
        MODULES=(
            ":shared-kernel"
            ":shared-infrastructure"
            ":masrufi-framework"
        )
        ;;
    customer-context)
        MODULES=(
            ":customer-context:customer-domain"
            ":customer-context:customer-application"
            ":customer-context:customer-infrastructure"
        )
        ;;
    loan-context)
        MODULES=(
            ":loan-context:loan-domain"
            ":loan-context:loan-application"
            ":loan-context:loan-infrastructure"
        )
        ;;
    payment-context)
        MODULES=(
            ":payment-context:payment-domain"
            ":payment-context:payment-application"
            ":payment-context:payment-infrastructure"
        )
        ;;
    risk-context)
        MODULES=(
            ":risk-context:risk-domain"
            ":risk-context:risk-application"
            ":risk-context:risk-infrastructure"
        )
        ;;
    compliance-context)
        MODULES=(
            ":compliance-context:compliance-domain"
            ":compliance-context:compliance-application"
            ":compliance-context:compliance-infrastructure"
        )
        ;;
    shared-foundation)
        MODULES=(
            ":shared-kernel"
            ":shared-infrastructure"
        )
        ;;
    masrufi)
        MODULES=(
            ":masrufi-framework"
        )
        ;;
    *)
        echo "Unsupported group: ${GROUP}" >&2
        usage
        exit 2
        ;;
esac

mkdir -p "${REPORT_DIR}"
SUMMARY_FILE="${REPORT_DIR}/${GROUP}.txt"

cd "${REPO_ROOT}"

discover_coverage_task() {
    local module="$1"
    local task_list

    task_list="$(./gradlew -q "${module}:tasks" --all)"

    if grep -qE '(^|[[:space:]])validateBankingTestCoverage[[:space:]]' <<<"${task_list}"; then
        echo "${module}:validateBankingTestCoverage"
        return 0
    fi

    if grep -qE '(^|[[:space:]])jacocoTestCoverageVerification[[:space:]]' <<<"${task_list}"; then
        echo "${module}:jacocoTestCoverageVerification"
        return 0
    fi

    return 1
}

declare -a COVERAGE_TASKS=()
declare -a MISSING_TASK_MODULES=()

for module in "${MODULES[@]}"; do
    if coverage_task="$(discover_coverage_task "${module}")"; then
        COVERAGE_TASKS+=("${coverage_task}")
    else
        MISSING_TASK_MODULES+=("${module}")
    fi
done

if [[ ${#MISSING_TASK_MODULES[@]} -gt 0 ]]; then
    {
        echo "Coverage gate task is missing for the following modules:"
        for module in "${MISSING_TASK_MODULES[@]}"; do
            echo " - ${module}"
        done
    } | tee "${SUMMARY_FILE}" >&2
    exit 1
fi

echo "Running coverage gate tasks for group '${GROUP}'..."
echo "Threshold: ${THRESHOLD}"
printf 'Tasks:\n%s\n' "${COVERAGE_TASKS[@]}"

set +e
./gradlew --no-daemon --continue "${COVERAGE_TASKS[@]}"
GRADLE_EXIT=$?
set -e

python3 - "${THRESHOLD}" "${MODULES[@]}" <<'PY' | tee "${SUMMARY_FILE}"
import pathlib
import sys
import xml.etree.ElementTree as ET

threshold = float(sys.argv[1])
modules = sys.argv[2:]

rows = []
failed = False

for module in modules:
    module_path = module.lstrip(":").replace(":", "/")
    report_path = pathlib.Path(module_path) / "build/reports/jacoco/test/jacocoTestReport.xml"

    if not report_path.exists():
        rows.append((module, "MISSING_REPORT", 0.0, str(report_path)))
        failed = True
        continue

    tree = ET.parse(report_path)
    counters = tree.findall(".//counter[@type='LINE']")
    if not counters:
        rows.append((module, "MISSING_LINE_COUNTER", 0.0, str(report_path)))
        failed = True
        continue

    line_counter = counters[-1]
    covered = int(line_counter.attrib.get("covered", "0"))
    missed = int(line_counter.attrib.get("missed", "0"))
    total = covered + missed
    ratio = (covered / total) if total else 0.0
    status = "PASS" if ratio >= threshold else "FAIL"

    if status != "PASS":
        failed = True

    rows.append((module, status, ratio, str(report_path)))

print("Coverage gate summary")
print(f"Required minimum line coverage: {threshold:.2%}")
for module, status, ratio, report in rows:
    print(f"{module} | {status} | line={ratio:.2%} | report={report}")

sys.exit(1 if failed else 0)
PY
PARSER_EXIT=${PIPESTATUS[0]}

if [[ ${GRADLE_EXIT} -ne 0 || ${PARSER_EXIT} -ne 0 ]]; then
    echo "Coverage validation failed for group '${GROUP}'" >&2
    exit 1
fi

echo "Coverage validation passed for group '${GROUP}'. Summary: ${SUMMARY_FILE}"
