#!/bin/bash

# Standalone Postman Tests - No Application Required
# Tests basic connectivity and API structure

set -e

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
COLLECTION_FILE="postman/Standalone-API-Tests.postman_collection.json"
RESULTS_DIR="test-results"

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check if Newman is installed
    if ! command -v newman &> /dev/null; then
        log_info "Newman is not installed. Installing Newman..."
        npm install -g newman newman-reporter-htmlextra
    fi
    
    # Check if collection exists
    if [ ! -f "$COLLECTION_FILE" ]; then
        log_error "Postman collection not found: $COLLECTION_FILE"
        exit 1
    fi
    
    # Create results directory
    mkdir -p "$RESULTS_DIR"
    
    log_success "Prerequisites check completed"
}

# Run connectivity tests
run_connectivity_tests() {
    log_info "Running connectivity tests..."
    
    newman run "$COLLECTION_FILE" \
        --folder "🔗 Connectivity Tests" \
        --reporters cli,json \
        --reporter-json-export "$RESULTS_DIR/connectivity-results.json" \
        --insecure \
        --timeout 10000 \
        --delay-request 1000 \
        --bail false \
        --color on
}

# Run mock API tests
run_mock_tests() {
    log_info "Running mock API tests..."
    
    newman run "$COLLECTION_FILE" \
        --folder "🧪 Mock API Tests" \
        --reporters cli,json \
        --reporter-json-export "$RESULTS_DIR/mock-results.json" \
        --insecure \
        --timeout 10000 \
        --delay-request 1000 \
        --bail false \
        --color on
}

# Run API structure tests
run_structure_tests() {
    log_info "Running API structure tests..."
    
    newman run "$COLLECTION_FILE" \
        --folder "🔍 API Structure Validation" \
        --reporters cli,json \
        --reporter-json-export "$RESULTS_DIR/structure-results.json" \
        --insecure \
        --timeout 10000 \
        --delay-request 1000 \
        --bail false \
        --color on
}

# Run all tests
run_all_tests() {
    log_info "Running all standalone tests..."
    
    newman run "$COLLECTION_FILE" \
        --reporters cli,htmlextra,json \
        --reporter-htmlextra-export "$RESULTS_DIR/standalone-test-report.html" \
        --reporter-json-export "$RESULTS_DIR/standalone-results.json" \
        --insecure \
        --timeout 15000 \
        --delay-request 1000 \
        --bail false \
        --color on \
        --verbose
}

# Generate test report
generate_report() {
    log_info "Generating test report..."
    
    local results_file="$RESULTS_DIR/standalone-results.json"
    if [ -f "$results_file" ]; then
        echo "========================================="
        echo "        STANDALONE TEST SUMMARY"
        echo "========================================="
        
        # Extract basic stats using jq if available
        if command -v jq &> /dev/null; then
            local total_tests=$(jq '.run.stats.tests.total // 0' "$results_file")
            local passed_tests=$(jq '.run.stats.tests.passed // 0' "$results_file")
            local failed_tests=$(jq '.run.stats.tests.failed // 0' "$results_file")
            local total_assertions=$(jq '.run.stats.assertions.total // 0' "$results_file")
            local passed_assertions=$(jq '.run.stats.assertions.passed // 0' "$results_file")
            local failed_assertions=$(jq '.run.stats.assertions.failed // 0' "$results_file")
            
            echo "Tests: $passed_tests/$total_tests passed"
            echo "Assertions: $passed_assertions/$total_assertions passed"
            
            if [ "$failed_tests" -eq 0 ]; then
                log_success "All tests passed!"
            else
                log_warning "$failed_tests tests failed"
            fi
        else
            log_info "Install jq for detailed test statistics"
        fi
        
        echo "Full results available in: $results_file"
        if [ -f "$RESULTS_DIR/standalone-test-report.html" ]; then
            echo "HTML report available in: $RESULTS_DIR/standalone-test-report.html"
        fi
    fi
}

# Main execution
main() {
    echo "========================================="
    echo "Enterprise Banking System"
    echo "Standalone API Tests"
    echo "========================================="
    echo ""
    echo "These tests run without requiring the full application to be running"
    echo "They test basic connectivity, API structure, and mock services"
    echo ""
    
    check_prerequisites
    
    case "${1:-all}" in
        "connectivity")
            run_connectivity_tests
            ;;
        "mock")
            run_mock_tests
            ;;
        "structure")
            run_structure_tests
            ;;
        "all")
            run_all_tests
            ;;
        *)
            echo "Usage: $0 [connectivity|mock|structure|all]"
            echo ""
            echo "Commands:"
            echo "  connectivity  - Test basic connectivity to localhost:8080"
            echo "  mock          - Test mock API services (external)"
            echo "  structure     - Test API structure and error handling"
            echo "  all           - Run all standalone tests (default)"
            echo ""
            echo "Examples:"
            echo "  $0 connectivity"
            echo "  $0 mock"
            echo "  $0 all"
            exit 1
            ;;
    esac
    
    generate_report
    
    log_success "Standalone test execution completed!"
    echo "Results available in: $RESULTS_DIR/"
}

# Execute main function with all arguments
main "$@"