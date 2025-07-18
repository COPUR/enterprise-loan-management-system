#!/bin/bash

# Enterprise Banking API Test Runner
# Runs comprehensive API tests using Newman (Postman CLI)

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
POSTMAN_DIR="${PROJECT_ROOT}/postman"
REPORTS_DIR="${PROJECT_ROOT}/test-reports/api"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
ENVIRONMENT="development"
COLLECTION_FILE="Enterprise-Banking-API-v1.postman_collection.json"
ENVIRONMENT_FILE="Enterprise-Banking-Environment.postman_environment.json"
ITERATIONS=1
PARALLEL=false
VERBOSE=false
GENERATE_REPORT=true
REPORTER="htmlextra"

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to show usage
show_usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Enterprise Banking API Test Runner

Options:
    -e, --environment    Test environment (development|staging|production) [default: development]
    -c, --collection     Postman collection file [default: Enterprise-Banking-API-v1.postman_collection.json]
    -n, --iterations     Number of iterations to run [default: 1]
    -p, --parallel       Run tests in parallel
    -v, --verbose        Verbose output
    -r, --reporter       Reporter type (cli|json|html|htmlextra) [default: htmlextra]
    --no-report          Skip generating HTML report
    --load-test          Run load tests (100 iterations)
    --smoke-test         Run smoke tests only
    --health-check       Run health check tests only
    -h, --help           Show this help message

Examples:
    $0                                    # Run basic tests
    $0 -e staging -n 5                   # Run 5 iterations on staging
    $0 --load-test                        # Run load tests
    $0 --smoke-test --no-report          # Run smoke tests without HTML report
    $0 -e production --health-check      # Run health checks on production

EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--environment)
            ENVIRONMENT="$2"
            shift 2
            ;;
        -c|--collection)
            COLLECTION_FILE="$2"
            shift 2
            ;;
        -n|--iterations)
            ITERATIONS="$2"
            shift 2
            ;;
        -p|--parallel)
            PARALLEL=true
            shift
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        -r|--reporter)
            REPORTER="$2"
            shift 2
            ;;
        --no-report)
            GENERATE_REPORT=false
            shift
            ;;
        --load-test)
            ITERATIONS=100
            PARALLEL=true
            print_status "Load test mode enabled (100 iterations in parallel)"
            shift
            ;;
        --smoke-test)
            COLLECTION_FILE="Enterprise-Banking-Smoke-Tests.postman_collection.json"
            print_status "Smoke test mode enabled"
            shift
            ;;
        --health-check)
            COLLECTION_FILE="Enterprise-Banking-Health-Check.postman_collection.json"
            print_status "Health check mode enabled"
            shift
            ;;
        -h|--help)
            show_usage
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Validate environment
case $ENVIRONMENT in
    development|staging|production)
        ;;
    *)
        print_error "Invalid environment: $ENVIRONMENT"
        print_error "Valid environments: development, staging, production"
        exit 1
        ;;
esac

# Check if Newman is installed
if ! command -v newman &> /dev/null; then
    print_error "Newman is not installed. Please install it with: npm install -g newman"
    print_error "Also install the HTML reporter: npm install -g newman-reporter-htmlextra"
    exit 1
fi

# Create reports directory
mkdir -p "${REPORTS_DIR}"

# Set environment-specific variables
case $ENVIRONMENT in
    development)
        BASE_URL="http://localhost:8080"
        AUTH_URL="http://localhost:8081"
        ;;
    staging)
        BASE_URL="https://api-staging.banking.example.com"
        AUTH_URL="https://auth-staging.banking.example.com"
        ;;
    production)
        BASE_URL="https://api.banking.example.com"
        AUTH_URL="https://auth.banking.example.com"
        ;;
esac

print_status "Starting API tests..."
print_status "Environment: $ENVIRONMENT"
print_status "Base URL: $BASE_URL"
print_status "Collection: $COLLECTION_FILE"
print_status "Iterations: $ITERATIONS"
print_status "Parallel: $PARALLEL"
print_status "Reporter: $REPORTER"

# Check if collection file exists
if [[ ! -f "${POSTMAN_DIR}/${COLLECTION_FILE}" ]]; then
    print_error "Collection file not found: ${POSTMAN_DIR}/${COLLECTION_FILE}"
    exit 1
fi

# Check if environment file exists
if [[ ! -f "${POSTMAN_DIR}/${ENVIRONMENT_FILE}" ]]; then
    print_error "Environment file not found: ${POSTMAN_DIR}/${ENVIRONMENT_FILE}"
    exit 1
fi

# Create temporary environment file with updated URLs
TEMP_ENV_FILE="${REPORTS_DIR}/temp_environment_${TIMESTAMP}.json"
jq --arg base_url "$BASE_URL" \
   --arg auth_url "$AUTH_URL" \
   '.values |= map(
     if .key == "base_url" then .value = $base_url
     elif .key == "auth_url" then .value = $auth_url
     else .
     end
   )' "${POSTMAN_DIR}/${ENVIRONMENT_FILE}" > "$TEMP_ENV_FILE"

# Build Newman command
NEWMAN_CMD="newman run"
NEWMAN_CMD+=" \"${POSTMAN_DIR}/${COLLECTION_FILE}\""
NEWMAN_CMD+=" --environment \"${TEMP_ENV_FILE}\""
NEWMAN_CMD+=" --iteration-count ${ITERATIONS}"
NEWMAN_CMD+=" --delay-request 100"
NEWMAN_CMD+=" --timeout-request 30000"
NEWMAN_CMD+=" --timeout-script 10000"

if [[ "$PARALLEL" == true ]]; then
    NEWMAN_CMD+=" --parallel"
fi

if [[ "$VERBOSE" == true ]]; then
    NEWMAN_CMD+=" --verbose"
fi

# Add reporters
if [[ "$REPORTER" == "cli" ]]; then
    NEWMAN_CMD+=" --reporters cli"
elif [[ "$REPORTER" == "json" ]]; then
    NEWMAN_CMD+=" --reporters json"
    NEWMAN_CMD+=" --reporter-json-export \"${REPORTS_DIR}/api-test-results-${TIMESTAMP}.json\""
elif [[ "$REPORTER" == "html" ]]; then
    NEWMAN_CMD+=" --reporters html"
    NEWMAN_CMD+=" --reporter-html-export \"${REPORTS_DIR}/api-test-results-${TIMESTAMP}.html\""
elif [[ "$REPORTER" == "htmlextra" ]]; then
    NEWMAN_CMD+=" --reporters htmlextra"
    NEWMAN_CMD+=" --reporter-htmlextra-export \"${REPORTS_DIR}/api-test-results-${TIMESTAMP}.html\""
    NEWMAN_CMD+=" --reporter-htmlextra-title \"Enterprise Banking API Test Results\""
    NEWMAN_CMD+=" --reporter-htmlextra-titleSize 4"
    NEWMAN_CMD+=" --reporter-htmlextra-logs"
    NEWMAN_CMD+=" --reporter-htmlextra-testPaging"
    NEWMAN_CMD+=" --reporter-htmlextra-browserTitle \"API Test Results - ${ENVIRONMENT}\""
    NEWMAN_CMD+=" --reporter-htmlextra-showOnlyFails false"
fi

# Always include JSON export for CI/CD
NEWMAN_CMD+=" --reporters json"
NEWMAN_CMD+=" --reporter-json-export \"${REPORTS_DIR}/api-test-results-${TIMESTAMP}.json\""

# Run the tests
print_status "Executing Newman command..."
if [[ "$VERBOSE" == true ]]; then
    print_status "Command: $NEWMAN_CMD"
fi

START_TIME=$(date +%s)
set +e
eval "$NEWMAN_CMD"
EXIT_CODE=$?
set -e
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

# Clean up temporary environment file
rm -f "$TEMP_ENV_FILE"

# Process results
if [[ $EXIT_CODE -eq 0 ]]; then
    print_success "All tests passed! ✅"
    print_success "Duration: ${DURATION}s"
    
    # Extract test statistics from JSON report
    if [[ -f "${REPORTS_DIR}/api-test-results-${TIMESTAMP}.json" ]]; then
        STATS=$(jq -r '.run.stats' "${REPORTS_DIR}/api-test-results-${TIMESTAMP}.json")
        TOTAL_TESTS=$(echo "$STATS" | jq -r '.tests.total')
        PASSED_TESTS=$(echo "$STATS" | jq -r '.tests.passed')
        FAILED_TESTS=$(echo "$STATS" | jq -r '.tests.failed')
        
        print_status "Test Statistics:"
        print_status "  Total Tests: $TOTAL_TESTS"
        print_status "  Passed: $PASSED_TESTS"
        print_status "  Failed: $FAILED_TESTS"
        
        if [[ -f "${REPORTS_DIR}/api-test-results-${TIMESTAMP}.html" ]]; then
            print_status "HTML Report: ${REPORTS_DIR}/api-test-results-${TIMESTAMP}.html"
        fi
    fi
else
    print_error "Some tests failed! ❌"
    print_error "Exit code: $EXIT_CODE"
    print_error "Duration: ${DURATION}s"
    
    # Extract failure details from JSON report
    if [[ -f "${REPORTS_DIR}/api-test-results-${TIMESTAMP}.json" ]]; then
        FAILURES=$(jq -r '.run.failures[] | "- \(.error.message) in \(.source.name)"' "${REPORTS_DIR}/api-test-results-${TIMESTAMP}.json" 2>/dev/null || echo "No detailed failure information available")
        if [[ -n "$FAILURES" ]]; then
            print_error "Failure details:"
            echo "$FAILURES"
        fi
    fi
fi

# Generate summary report
SUMMARY_FILE="${REPORTS_DIR}/test-summary-${TIMESTAMP}.txt"
cat > "$SUMMARY_FILE" << EOF
Enterprise Banking API Test Summary
===================================

Environment: $ENVIRONMENT
Collection: $COLLECTION_FILE
Iterations: $ITERATIONS
Parallel: $PARALLEL
Duration: ${DURATION}s
Exit Code: $EXIT_CODE
Timestamp: $(date)

Test Results:
$(if [[ -f "${REPORTS_DIR}/api-test-results-${TIMESTAMP}.json" ]]; then
    jq -r '.run.stats | "Total Tests: \(.tests.total)\nPassed: \(.tests.passed)\nFailed: \(.tests.failed)\nSkipped: \(.tests.skipped)"' "${REPORTS_DIR}/api-test-results-${TIMESTAMP}.json"
else
    echo "No detailed statistics available"
fi)

Files Generated:
- JSON Report: api-test-results-${TIMESTAMP}.json
$(if [[ -f "${REPORTS_DIR}/api-test-results-${TIMESTAMP}.html" ]]; then
    echo "- HTML Report: api-test-results-${TIMESTAMP}.html"
fi)
- Summary: test-summary-${TIMESTAMP}.txt

EOF

print_status "Summary report: $SUMMARY_FILE"

# Performance analysis
if [[ -f "${REPORTS_DIR}/api-test-results-${TIMESTAMP}.json" ]]; then
    print_status "Performance Analysis:"
    
    # Extract response time statistics
    AVG_RESPONSE_TIME=$(jq -r '.run.timings.responseAverage' "${REPORTS_DIR}/api-test-results-${TIMESTAMP}.json" 2>/dev/null || echo "N/A")
    MIN_RESPONSE_TIME=$(jq -r '.run.timings.responseMin' "${REPORTS_DIR}/api-test-results-${TIMESTAMP}.json" 2>/dev/null || echo "N/A")
    MAX_RESPONSE_TIME=$(jq -r '.run.timings.responseMax' "${REPORTS_DIR}/api-test-results-${TIMESTAMP}.json" 2>/dev/null || echo "N/A")
    
    print_status "  Average Response Time: ${AVG_RESPONSE_TIME}ms"
    print_status "  Min Response Time: ${MIN_RESPONSE_TIME}ms"
    print_status "  Max Response Time: ${MAX_RESPONSE_TIME}ms"
    
    # Check for performance issues
    if [[ "$AVG_RESPONSE_TIME" != "N/A" ]] && [[ $(echo "$AVG_RESPONSE_TIME > 1000" | bc -l) -eq 1 ]]; then
        print_warning "Average response time is high (${AVG_RESPONSE_TIME}ms > 1000ms)"
    fi
fi

# CI/CD integration
if [[ -n "$CI" ]]; then
    print_status "CI/CD environment detected"
    
    # Export test results for CI/CD systems
    if [[ -f "${REPORTS_DIR}/api-test-results-${TIMESTAMP}.json" ]]; then
        # GitHub Actions
        if [[ -n "$GITHUB_ACTIONS" ]]; then
            echo "test_results_file=${REPORTS_DIR}/api-test-results-${TIMESTAMP}.json" >> "$GITHUB_OUTPUT"
            echo "test_exit_code=$EXIT_CODE" >> "$GITHUB_OUTPUT"
        fi
        
        # GitLab CI
        if [[ -n "$GITLAB_CI" ]]; then
            echo "TEST_RESULTS_FILE=${REPORTS_DIR}/api-test-results-${TIMESTAMP}.json" >> test.env
            echo "TEST_EXIT_CODE=$EXIT_CODE" >> test.env
        fi
    fi
fi

print_status "API test execution completed!"
exit $EXIT_CODE