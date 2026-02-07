#!/bin/bash

# MongoDB analytics design validation for BCNF/DKNF guardrails.

set -euo pipefail

readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m'

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_ok() {
    echo -e "${GREEN}[PASS]${NC} $1"
}

print_fail() {
    echo -e "${RED}[FAIL]${NC} $1"
}

require_file() {
    local file="$1"
    if [[ -f "$file" ]]; then
        print_ok "File exists: $file"
    else
        print_fail "Missing file: $file"
        exit 1
    fi
}

require_pattern() {
    local file="$1"
    local pattern="$2"
    local description="$3"

    if rg -n -q "$pattern" "$file"; then
        print_ok "$description"
    else
        print_fail "$description"
        print_fail "Expected pattern '$pattern' in $file"
        exit 1
    fi
}

main() {
    print_info "Validating MongoDB analytics BCNF/DKNF guardrails..."

    if ! command -v rg >/dev/null 2>&1; then
        print_fail "ripgrep (rg) is required but not found."
        exit 1
    fi

    local consent_metrics="${PROJECT_ROOT}/open-finance-context/open-finance-infrastructure/src/main/java/com/enterprise/openfinance/infrastructure/analytics/model/ConsentMetricsSummary.java"
    local customer_pattern="${PROJECT_ROOT}/open-finance-context/open-finance-infrastructure/src/main/java/com/enterprise/openfinance/infrastructure/analytics/model/CustomerConsentPattern.java"
    local compliance_report="${PROJECT_ROOT}/open-finance-context/open-finance-infrastructure/src/main/java/com/enterprise/openfinance/infrastructure/analytics/model/ComplianceReport.java"
    local security_incident="${PROJECT_ROOT}/open-finance-context/open-finance-infrastructure/src/main/java/com/enterprise/openfinance/infrastructure/analytics/model/SecurityIncident.java"
    local analytics_service="${PROJECT_ROOT}/open-finance-context/open-finance-infrastructure/src/main/java/com/enterprise/openfinance/infrastructure/analytics/MongoConsentAnalyticsService.java"

    require_file "$consent_metrics"
    require_file "$customer_pattern"
    require_file "$compliance_report"
    require_file "$security_incident"
    require_file "$analytics_service"

    print_info "Checking BCNF key constraints..."
    require_pattern "$consent_metrics" "@Indexed\\(unique = true\\)" "Unique participant business key in consent metrics summary"
    require_pattern "$customer_pattern" "@Indexed\\(unique = true\\)" "Unique customer business key in customer patterns"
    require_pattern "$compliance_report" "@CompoundIndex\\(name = \"report_date_type_idx\".*unique = true\\)" "Unique (reportDate, reportType) business key in compliance reports"

    print_info "Checking DKNF domain typing..."
    require_pattern "$security_incident" "private Severity severity;" "Security incident severity uses enum"
    require_pattern "$security_incident" "private IncidentStatus status;" "Security incident status uses enum"
    require_pattern "$security_incident" "private Map<String, String> details;" "Security incident details map is strongly typed"
    require_pattern "$compliance_report" "private ReportType reportType;" "Compliance report type uses enum"
    require_pattern "$compliance_report" "private ReportStatus status;" "Compliance report status uses enum"
    require_pattern "$compliance_report" "private Map<String, Boolean> regulatoryChecks;" "Regulatory checks map is strongly typed"

    print_info "Checking controlled denormalization..."
    require_pattern "$customer_pattern" "@Transient" "Derived fields are marked transient in customer patterns"
    require_pattern "$customer_pattern" "public String getPreferredParticipant\\(\\)" "Preferred participant is derived at read time"
    require_pattern "$customer_pattern" "public List<String> getFrequentScopes\\(\\)" "Frequent scopes are derived at read time"

    print_info "Checking write/read path invariants..."
    require_pattern "$analytics_service" "maskCustomerId\\(customerId\\)" "Customer pattern lookup uses masked customer ID"
    require_pattern "$analytics_service" "Criteria\\.where\\(\"customerId\"\\)\\.is\\(maskedCustomerId\\)" "Masked key is used in Mongo query"
    require_pattern "$analytics_service" "SecurityIncident\\.Severity\\.valueOf" "Severity filters are validated against enum domain"
    require_pattern "$analytics_service" "Map<String, Boolean> performRegulatoryChecks" "Regulatory checks return typed boolean map"
    require_pattern "$analytics_service" "event\\.getRevocationReason\\(\\)" "Revocation reason uses correct event field"

    print_ok "MongoDB analytics BCNF/DKNF validation passed."
}

main "$@"
