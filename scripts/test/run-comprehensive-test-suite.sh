#!/bin/bash

# ===============================================================
# ENTERPRISE BANKING SYSTEM - COMPREHENSIVE TEST ORCHESTRATOR
# ===============================================================
# Document Information:
# - Author: Senior DevOps Engineer & Test Automation Architect
# - Version: 1.0.0
# - Last Updated: December 2024
# - Classification: Internal - Test Infrastructure Orchestration
# - Purpose: Master test execution orchestrator for all test types
# ===============================================================

set -euo pipefail

# ============================================
# CONFIGURATION
# ============================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
TEST_EXECUTION_ID="test-$(date +%Y%m%d-%H%M%S)"
MASTER_RESULTS_DIR="${PROJECT_ROOT}/test-results/${TEST_EXECUTION_ID}"

# Test execution configuration
PARALLEL_EXECUTION=${PARALLEL_EXECUTION:-true}
CONTINUE_ON_FAILURE=${CONTINUE_ON_FAILURE:-true}
GENERATE_CONSOLIDATED_REPORT=${GENERATE_CONSOLIDATED_REPORT:-true}
CLEANUP_AFTER_TESTS=${CLEANUP_AFTER_TESTS:-true}

# Test suite selection
RUN_UNIT_TESTS=${RUN_UNIT_TESTS:-true}
RUN_INTEGRATION_TESTS=${RUN_INTEGRATION_TESTS:-true}
RUN_E2E_TESTS=${RUN_E2E_TESTS:-true}
RUN_PERFORMANCE_TESTS=${RUN_PERFORMANCE_TESTS:-true}
RUN_SECURITY_TESTS=${RUN_SECURITY_TESTS:-true}
RUN_REGRESSION_TESTS=${RUN_REGRESSION_TESTS:-true}

# Islamic Banking test suite selection
RUN_ISLAMIC_BANKING_TESTS=${RUN_ISLAMIC_BANKING_TESTS:-true}
RUN_SHARIA_COMPLIANCE_TESTS=${RUN_SHARIA_COMPLIANCE_TESTS:-true}
RUN_UAE_CBDC_TESTS=${RUN_UAE_CBDC_TESTS:-true}
RUN_MFA_TESTS=${RUN_MFA_TESTS:-true}
RUN_SECURITY_AUDIT_TESTS=${RUN_SECURITY_AUDIT_TESTS:-true}

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# Islamic Banking emojis
ISLAMIC_EMOJI="🕌"
UAE_EMOJI="🇦🇪"
SECURITY_EMOJI="🔒"
COMPLIANCE_EMOJI="✅"

# ============================================
# UTILITY FUNCTIONS
# ============================================

log_master() {
    echo -e "${PURPLE}[MASTER-TEST]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_info() {
    echo -e "${BLUE}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

setup_test_environment() {
    log_master "Setting up comprehensive test environment..."
    
    # Create master results directory
    mkdir -p "${MASTER_RESULTS_DIR}"
    mkdir -p "${MASTER_RESULTS_DIR}/unit-tests"
    mkdir -p "${MASTER_RESULTS_DIR}/integration-tests"
    mkdir -p "${MASTER_RESULTS_DIR}/e2e-tests"
    mkdir -p "${MASTER_RESULTS_DIR}/performance-tests"
    mkdir -p "${MASTER_RESULTS_DIR}/security-tests"
    mkdir -p "${MASTER_RESULTS_DIR}/regression-tests"
    mkdir -p "${MASTER_RESULTS_DIR}/islamic-banking-tests"
    mkdir -p "${MASTER_RESULTS_DIR}/sharia-compliance-tests"
    mkdir -p "${MASTER_RESULTS_DIR}/uae-cbdc-tests"
    mkdir -p "${MASTER_RESULTS_DIR}/mfa-tests"
    mkdir -p "${MASTER_RESULTS_DIR}/security-audit-tests"
    mkdir -p "${MASTER_RESULTS_DIR}/consolidated-reports"
    
    # Initialize test execution log
    cat > "${MASTER_RESULTS_DIR}/test-execution.log" << EOF
===============================================================
ENTERPRISE BANKING SYSTEM - COMPREHENSIVE TEST EXECUTION LOG
===============================================================
Execution ID: ${TEST_EXECUTION_ID}
Started at: $(date)
Project Root: ${PROJECT_ROOT}
Test Configuration:
  - Parallel Execution: ${PARALLEL_EXECUTION}
  - Continue on Failure: ${CONTINUE_ON_FAILURE}
  - Unit Tests: ${RUN_UNIT_TESTS}
  - Integration Tests: ${RUN_INTEGRATION_TESTS}
  - E2E Tests: ${RUN_E2E_TESTS}
  - Performance Tests: ${RUN_PERFORMANCE_TESTS}
  - Security Tests: ${RUN_SECURITY_TESTS}
  - Regression Tests: ${RUN_REGRESSION_TESTS}
===============================================================

EOF
    
    log_success "Test environment setup complete"
}

# ============================================
# INDIVIDUAL TEST SUITE RUNNERS
# ============================================

run_unit_test_suite() {
    if [ "${RUN_UNIT_TESTS}" != "true" ]; then
        log_info "Skipping unit tests (disabled)"
        return 0
    fi
    
    log_master "Executing unit test suite..."
    local start_time=$(date +%s)
    
    cd "${PROJECT_ROOT}"
    
    # Run unit tests with comprehensive reporting
    ./gradlew test \
        --continue \
        --build-cache \
        --parallel \
        --max-workers=4 \
        -Dspring.profiles.active=test \
        -Djunit.platform.output.capture.stdout=true \
        -Djunit.platform.output.capture.stderr=true \
        > "${MASTER_RESULTS_DIR}/unit-tests/unit-test-execution.log" 2>&1 \
        || local unit_failed=true
    
    # Generate coverage report
    ./gradlew jacocoTestReport \
        >> "${MASTER_RESULTS_DIR}/unit-tests/unit-test-execution.log" 2>&1 \
        || true
    
    # Copy test results
    cp -r build/reports/tests/test/* "${MASTER_RESULTS_DIR}/unit-tests/" 2>/dev/null || true
    cp -r build/reports/jacoco/test/html/* "${MASTER_RESULTS_DIR}/unit-tests/coverage/" 2>/dev/null || true
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    if [ "${unit_failed:-false}" = "true" ]; then
        log_error "Unit tests failed (Duration: ${duration}s)"
        echo "UNIT_TESTS=FAILED (${duration}s)" >> "${MASTER_RESULTS_DIR}/test-execution.log"
        return 1
    else
        log_success "Unit tests completed successfully (Duration: ${duration}s)"
        echo "UNIT_TESTS=PASSED (${duration}s)" >> "${MASTER_RESULTS_DIR}/test-execution.log"
        return 0
    fi
}

run_integration_test_suite() {
    if [ "${RUN_INTEGRATION_TESTS}" != "true" ]; then
        log_info "Skipping integration tests (disabled)"
        return 0
    fi
    
    log_master "Executing integration test suite..."
    local start_time=$(date +%s)
    
    cd "${PROJECT_ROOT}"
    
    # Run integration tests
    ./gradlew integrationTest \
        --continue \
        --stacktrace \
        -Dspring.profiles.active=integration,test \
        > "${MASTER_RESULTS_DIR}/integration-tests/integration-test-execution.log" 2>&1 \
        || local integration_failed=true
    
    # Copy integration test results
    cp -r build/reports/tests/integrationTest/* "${MASTER_RESULTS_DIR}/integration-tests/" 2>/dev/null || true
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    if [ "${integration_failed:-false}" = "true" ]; then
        log_error "Integration tests failed (Duration: ${duration}s)"
        echo "INTEGRATION_TESTS=FAILED (${duration}s)" >> "${MASTER_RESULTS_DIR}/test-execution.log"
        return 1
    else
        log_success "Integration tests completed successfully (Duration: ${duration}s)"
        echo "INTEGRATION_TESTS=PASSED (${duration}s)" >> "${MASTER_RESULTS_DIR}/test-execution.log"
        return 0
    fi
}

run_e2e_test_suite() {
    if [ "${RUN_E2E_TESTS}" != "true" ]; then
        log_info "Skipping E2E tests (disabled)"
        return 0
    fi
    
    log_master "Executing end-to-end test suite..."
    local start_time=$(date +%s)
    
    # Run E2E tests using the dedicated script
    "${SCRIPT_DIR}/e2e/run-end-to-end-tests.sh" \
        --no-cleanup \
        > "${MASTER_RESULTS_DIR}/e2e-tests/e2e-test-execution.log" 2>&1 \
        || local e2e_failed=true
    
    # Copy E2E test results
    find "${PROJECT_ROOT}/test-results" -name "*e2e*" -type d -exec cp -r {} "${MASTER_RESULTS_DIR}/e2e-tests/" \; 2>/dev/null || true
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    if [ "${e2e_failed:-false}" = "true" ]; then
        log_error "E2E tests failed (Duration: ${duration}s)"
        echo "E2E_TESTS=FAILED (${duration}s)" >> "${MASTER_RESULTS_DIR}/test-execution.log"
        return 1
    else
        log_success "E2E tests completed successfully (Duration: ${duration}s)"
        echo "E2E_TESTS=PASSED (${duration}s)" >> "${MASTER_RESULTS_DIR}/test-execution.log"
        return 0
    fi
}

run_performance_test_suite() {
    if [ "${RUN_PERFORMANCE_TESTS}" != "true" ]; then
        log_info "Skipping performance tests (disabled)"
        return 0
    fi
    
    log_master "Executing performance test suite..."
    local start_time=$(date +%s)
    
    # Check if K6 is available
    if ! command -v k6 &> /dev/null; then
        log_warning "K6 not found, skipping performance tests"
        echo "PERFORMANCE_TESTS=SKIPPED (K6 not available)" >> "${MASTER_RESULTS_DIR}/test-execution.log"
        return 0
    fi
    
    # Run K6 performance tests
    k6 run "${PROJECT_ROOT}/scripts/performance/banking-load-test.js" \
        --out json="${MASTER_RESULTS_DIR}/performance-tests/k6-results.json" \
        --vus 10 \
        --duration 5m \
        > "${MASTER_RESULTS_DIR}/performance-tests/performance-test-execution.log" 2>&1 \
        || local perf_failed=true
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    if [ "${perf_failed:-false}" = "true" ]; then
        log_warning "Performance tests completed with issues (Duration: ${duration}s)"
        echo "PERFORMANCE_TESTS=WARNING (${duration}s)" >> "${MASTER_RESULTS_DIR}/test-execution.log"
        return 0  # Don't fail build for performance issues
    else
        log_success "Performance tests completed successfully (Duration: ${duration}s)"
        echo "PERFORMANCE_TESTS=PASSED (${duration}s)" >> "${MASTER_RESULTS_DIR}/test-execution.log"
        return 0
    fi
}

run_security_test_suite() {
    if [ "${RUN_SECURITY_TESTS}" != "true" ]; then
        log_info "Skipping security tests (disabled)"
        return 0
    fi
    
    log_master "Executing security test suite..."
    local start_time=$(date +%s)
    
    # Run security-specific tests
    ./gradlew test \
        --tests "*SecurityTest" \
        --tests "*AuthenticationTest" \
        --tests "*AuthorizationTest" \
        --continue \
        -Dspring.profiles.active=test,security \
        > "${MASTER_RESULTS_DIR}/security-tests/security-test-execution.log" 2>&1 \
        || local security_failed=true
    
    # Run OWASP dependency check if available
    if ./gradlew tasks | grep -q "dependencyCheckAnalyze"; then
        ./gradlew dependencyCheckAnalyze \
            >> "${MASTER_RESULTS_DIR}/security-tests/security-test-execution.log" 2>&1 \
            || true
        
        # Copy dependency check report
        cp -r build/reports/dependency-check-report.html "${MASTER_RESULTS_DIR}/security-tests/" 2>/dev/null || true
    fi
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    if [ "${security_failed:-false}" = "true" ]; then
        log_error "Security tests failed (Duration: ${duration}s)"
        echo "SECURITY_TESTS=FAILED (${duration}s)" >> "${MASTER_RESULTS_DIR}/test-execution.log"
        return 1
    else
        log_success "Security tests completed successfully (Duration: ${duration}s)"
        echo "SECURITY_TESTS=PASSED (${duration}s)" >> "${MASTER_RESULTS_DIR}/test-execution.log"
        return 0
    fi
}

run_regression_test_suite() {
    if [ "${RUN_REGRESSION_TESTS}" != "true" ]; then
        log_info "Skipping regression tests (disabled)"
        return 0
    fi
    
    log_master "Executing regression test suite..."
    local start_time=$(date +%s)
    
    # Run comprehensive regression tests
    "${SCRIPT_DIR}/test/regression-test-suite.sh" \
        > "${MASTER_RESULTS_DIR}/regression-tests/regression-test-execution.log" 2>&1 \
        || local regression_failed=true
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    if [ "${regression_failed:-false}" = "true" ]; then
        log_error "Regression tests failed (Duration: ${duration}s)"
        echo "REGRESSION_TESTS=FAILED (${duration}s)" >> "${MASTER_RESULTS_DIR}/test-execution.log"
        return 1
    else
        log_success "Regression tests completed successfully (Duration: ${duration}s)"
        echo "REGRESSION_TESTS=PASSED (${duration}s)" >> "${MASTER_RESULTS_DIR}/test-execution.log"
        return 0
    fi
}

# ============================================
# PARALLEL EXECUTION ORCHESTRATOR
# ============================================

run_tests_parallel() {
    log_master "Executing test suites in parallel..."
    
    local pids=()
    local test_results=()
    
    # Start all test suites in background
    if [ "${RUN_UNIT_TESTS}" = "true" ]; then
        run_unit_test_suite &
        pids+=($!)
        test_results+=("unit")
    fi
    
    if [ "${RUN_INTEGRATION_TESTS}" = "true" ]; then
        run_integration_test_suite &
        pids+=($!)
        test_results+=("integration")
    fi
    
    if [ "${RUN_SECURITY_TESTS}" = "true" ]; then
        run_security_test_suite &
        pids+=($!)
        test_results+=("security")
    fi
    
    # Wait for fast tests to complete first
    local failed_suites=()
    for i in "${!pids[@]}"; do
        local pid=${pids[$i]}
        local suite=${test_results[$i]}
        
        if wait $pid; then
            log_success "${suite} test suite completed successfully"
        else
            log_error "${suite} test suite failed"
            failed_suites+=("${suite}")
            
            if [ "${CONTINUE_ON_FAILURE}" != "true" ]; then
                log_error "Stopping execution due to ${suite} test failure"
                return 1
            fi
        fi
    done
    
    # Run longer tests sequentially
    if [ "${RUN_E2E_TESTS}" = "true" ]; then
        if run_e2e_test_suite; then
            log_success "E2E test suite completed successfully"
        else
            failed_suites+=("e2e")
        fi
    fi
    
    if [ "${RUN_PERFORMANCE_TESTS}" = "true" ]; then
        if run_performance_test_suite; then
            log_success "Performance test suite completed successfully"
        else
            failed_suites+=("performance")
        fi
    fi
    
    if [ "${RUN_REGRESSION_TESTS}" = "true" ]; then
        if run_regression_test_suite; then
            log_success "Regression test suite completed successfully"
        else
            failed_suites+=("regression")
        fi
    fi
    
    # Return overall result
    if [ ${#failed_suites[@]} -eq 0 ]; then
        return 0
    else
        log_error "Failed test suites: ${failed_suites[*]}"
        return 1
    fi
}

run_tests_sequential() {
    log_master "Executing test suites sequentially..."
    
    local failed_suites=()
    
    # Run tests in optimal order
    local test_order=("unit" "integration" "security" "e2e" "performance" "regression")
    
    for suite in "${test_order[@]}"; do
        local suite_var="RUN_${suite^^}_TESTS"
        
        if [ "${!suite_var}" = "true" ]; then
            if "run_${suite}_test_suite"; then
                log_success "${suite} test suite completed successfully"
            else
                log_error "${suite} test suite failed"
                failed_suites+=("${suite}")
                
                if [ "${CONTINUE_ON_FAILURE}" != "true" ]; then
                    log_error "Stopping execution due to ${suite} test failure"
                    return 1
                fi
            fi
        fi
    done
    
    # Return overall result
    if [ ${#failed_suites[@]} -eq 0 ]; then
        return 0
    else
        log_error "Failed test suites: ${failed_suites[*]}"
        return 1
    fi
}

# ============================================
# REPORTING AND CONSOLIDATION
# ============================================

generate_consolidated_report() {
    if [ "${GENERATE_CONSOLIDATED_REPORT}" != "true" ]; then
        return 0
    fi
    
    log_master "Generating consolidated test report..."
    
    local report_file="${MASTER_RESULTS_DIR}/consolidated-reports/comprehensive-test-report.html"
    local execution_end_time=$(date)
    
    cat > "${report_file}" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>Enterprise Banking System - Comprehensive Test Report</title>
    <style>
        body { font-family: 'Segoe UI', Arial, sans-serif; margin: 40px; background: #f8f9fa; }
        .header { background: linear-gradient(135deg, #2c3e50, #3498db); color: white; padding: 30px; border-radius: 10px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }
        .section { margin: 20px 0; padding: 20px; background: white; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .metrics { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; margin: 20px 0; }
        .metric { text-align: center; padding: 20px; background: #f8f9fa; border-radius: 8px; border-left: 4px solid #3498db; }
        .metric h3 { margin: 0 0 10px 0; color: #2c3e50; font-size: 14px; text-transform: uppercase; }
        .metric .value { font-size: 2.5em; font-weight: bold; color: #e74c3c; margin: 10px 0; }
        .metric .label { color: #7f8c8d; font-size: 12px; }
        .success { background: #d4edda; border-color: #c3e6cb; color: #155724; }
        .warning { background: #fff3cd; border-color: #ffeaa7; color: #856404; }
        .error { background: #f8d7da; border-color: #f5c6cb; color: #721c24; }
        .test-suite { display: flex; justify-content: space-between; align-items: center; padding: 15px; margin: 10px 0; border-radius: 5px; }
        .test-suite.passed { background: #d4edda; border-left: 4px solid #28a745; }
        .test-suite.failed { background: #f8d7da; border-left: 4px solid #dc3545; }
        .test-suite.warning { background: #fff3cd; border-left: 4px solid #ffc107; }
        .test-suite.skipped { background: #e2e3e5; border-left: 4px solid #6c757d; }
        .badge { padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: bold; text-transform: uppercase; }
        .badge.passed { background: #28a745; color: white; }
        .badge.failed { background: #dc3545; color: white; }
        .badge.warning { background: #ffc107; color: #212529; }
        .badge.skipped { background: #6c757d; color: white; }
        .artifacts { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; }
        .artifact { padding: 15px; background: #f8f9fa; border-radius: 5px; text-align: center; }
        .artifact a { text-decoration: none; color: #3498db; font-weight: bold; }
        .artifact a:hover { color: #2980b9; }
    </style>
</head>
<body>
    <div class="header">
        <h1>🏦 Enterprise Banking System</h1>
        <h2>Comprehensive Test Execution Report</h2>
        <p><strong>Execution ID:</strong> ${TEST_EXECUTION_ID}</p>
        <p><strong>Started:</strong> $(head -n 10 "${MASTER_RESULTS_DIR}/test-execution.log" | grep "Started at:" | cut -d: -f2-)</p>
        <p><strong>Completed:</strong> ${execution_end_time}</p>
        <p><strong>Environment:</strong> Docker Containerized Testing</p>
    </div>
    
    <div class="section">
        <h2>📊 Test Execution Overview</h2>
        <div class="metrics">
            <div class="metric">
                <h3>Total Test Suites</h3>
                <div class="value">$(grep -c "TESTS=" "${MASTER_RESULTS_DIR}/test-execution.log" || echo "0")</div>
                <div class="label">Executed Suites</div>
            </div>
            <div class="metric">
                <h3>Passed Suites</h3>
                <div class="value" style="color: #28a745;">$(grep -c "=PASSED" "${MASTER_RESULTS_DIR}/test-execution.log" || echo "0")</div>
                <div class="label">Successful</div>
            </div>
            <div class="metric">
                <h3>Failed Suites</h3>
                <div class="value" style="color: #dc3545;">$(grep -c "=FAILED" "${MASTER_RESULTS_DIR}/test-execution.log" || echo "0")</div>
                <div class="label">Failed</div>
            </div>
            <div class="metric">
                <h3>Warning Suites</h3>
                <div class="value" style="color: #ffc107;">$(grep -c "=WARNING" "${MASTER_RESULTS_DIR}/test-execution.log" || echo "0")</div>
                <div class="label">With Issues</div>
            </div>
        </div>
    </div>
    
    <div class="section">
        <h2>🧪 Test Suite Results</h2>
EOF

    # Add test suite results
    local test_suites=("UNIT_TESTS" "INTEGRATION_TESTS" "E2E_TESTS" "PERFORMANCE_TESTS" "SECURITY_TESTS" "REGRESSION_TESTS")
    
    for suite in "${test_suites[@]}"; do
        if grep -q "${suite}=" "${MASTER_RESULTS_DIR}/test-execution.log"; then
            local result=$(grep "${suite}=" "${MASTER_RESULTS_DIR}/test-execution.log" | cut -d= -f2 | cut -d' ' -f1)
            local duration=$(grep "${suite}=" "${MASTER_RESULTS_DIR}/test-execution.log" | grep -o '([0-9]*s)' || echo "")
            local suite_name=$(echo "${suite}" | sed 's/_/ /g' | sed 's/\b\w/\U&/g')
            
            local css_class=""
            local badge_class=""
            case "${result}" in
                "PASSED") css_class="passed"; badge_class="passed" ;;
                "FAILED") css_class="failed"; badge_class="failed" ;;
                "WARNING") css_class="warning"; badge_class="warning" ;;
                "SKIPPED") css_class="skipped"; badge_class="skipped" ;;
            esac
            
            cat >> "${report_file}" << EOF
        <div class="test-suite ${css_class}">
            <div>
                <strong>${suite_name}</strong>
                <div style="font-size: 12px; color: #666;">${duration}</div>
            </div>
            <span class="badge ${badge_class}">${result}</span>
        </div>
EOF
        fi
    done

    cat >> "${report_file}" << EOF
    </div>
    
    <div class="section">
        <h2>📁 Test Artifacts</h2>
        <div class="artifacts">
            <div class="artifact">
                <h4>Unit Test Reports</h4>
                <a href="../unit-tests/index.html">View Report</a>
            </div>
            <div class="artifact">
                <h4>Integration Test Reports</h4>
                <a href="../integration-tests/index.html">View Report</a>
            </div>
            <div class="artifact">
                <h4>Coverage Report</h4>
                <a href="../unit-tests/coverage/index.html">View Coverage</a>
            </div>
            <div class="artifact">
                <h4>E2E Test Results</h4>
                <a href="../e2e-tests/">View Results</a>
            </div>
            <div class="artifact">
                <h4>Performance Results</h4>
                <a href="../performance-tests/k6-results.json">View Metrics</a>
            </div>
            <div class="artifact">
                <h4>Security Test Results</h4>
                <a href="../security-tests/">View Results</a>
            </div>
        </div>
    </div>
    
    <div class="section">
        <h2>📋 Test Execution Summary</h2>
        <p>This comprehensive test execution validates all critical aspects of the Enterprise Banking System:</p>
        <ul>
            <li><strong>Unit Tests:</strong> Individual component validation and business logic verification</li>
            <li><strong>Integration Tests:</strong> Service-to-service communication and data flow validation</li>
            <li><strong>End-to-End Tests:</strong> Complete user journey and SAGA pattern validation</li>
            <li><strong>Performance Tests:</strong> Load testing and performance characteristic validation</li>
            <li><strong>Security Tests:</strong> Authentication, authorization, and vulnerability scanning</li>
            <li><strong>Regression Tests:</strong> Comprehensive validation of existing functionality</li>
        </ul>
    </div>
    
    <div class="section">
        <h2>💾 Execution Log</h2>
        <pre style="background: #f8f9fa; padding: 15px; border-radius: 5px; overflow-x: auto; font-size: 12px;">
$(cat "${MASTER_RESULTS_DIR}/test-execution.log")
        </pre>
    </div>
    
    <footer style="text-align: center; margin-top: 40px; color: #666; font-size: 12px;">
        <p>Generated by Enterprise Banking System Test Automation Framework</p>
        <p>Report generated on: $(date)</p>
    </footer>
</body>
</html>
EOF
    
    log_success "Consolidated test report generated: ${report_file}"
}

cleanup_test_environment() {
    if [ "${CLEANUP_AFTER_TESTS}" = "true" ]; then
        log_master "Cleaning up test environment..."
        
        # Clean up Docker resources from E2E tests
        docker system prune -f >/dev/null 2>&1 || true
        
        # Clean up build artifacts
        cd "${PROJECT_ROOT}"
        ./gradlew clean >/dev/null 2>&1 || true
        
        log_success "Test environment cleanup completed"
    fi
}

# ============================================
# MAIN EXECUTION FLOW
# ============================================

show_usage() {
    cat << EOF
Enterprise Banking System - Comprehensive Test Suite

Usage: $0 [OPTIONS]

Test Suite Options:
    --unit-only           Run only unit tests
    --integration-only    Run only integration tests
    --e2e-only           Run only end-to-end tests
    --performance-only   Run only performance tests
    --security-only      Run only security tests
    --regression-only    Run only regression tests
    --skip-unit          Skip unit tests
    --skip-integration   Skip integration tests
    --skip-e2e           Skip end-to-end tests
    --skip-performance   Skip performance tests
    --skip-security      Skip security tests
    --skip-regression    Skip regression tests

Execution Options:
    --sequential         Run tests sequentially (default: parallel)
    --fail-fast          Stop on first failure (default: continue)
    --no-report          Skip consolidated report generation
    --no-cleanup         Skip cleanup after tests

Other Options:
    -h, --help           Show this help message

Examples:
    $0                           # Run all test suites
    $0 --unit-only              # Run only unit tests
    $0 --skip-performance --skip-e2e  # Run all except performance and E2E tests
    $0 --sequential --fail-fast  # Run sequentially and stop on first failure

EOF
}

main() {
    local start_time=$(date +%s)
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --unit-only)
                RUN_UNIT_TESTS=true
                RUN_INTEGRATION_TESTS=false
                RUN_E2E_TESTS=false
                RUN_PERFORMANCE_TESTS=false
                RUN_SECURITY_TESTS=false
                RUN_REGRESSION_TESTS=false
                shift
                ;;
            --integration-only)
                RUN_UNIT_TESTS=false
                RUN_INTEGRATION_TESTS=true
                RUN_E2E_TESTS=false
                RUN_PERFORMANCE_TESTS=false
                RUN_SECURITY_TESTS=false
                RUN_REGRESSION_TESTS=false
                shift
                ;;
            --e2e-only)
                RUN_UNIT_TESTS=false
                RUN_INTEGRATION_TESTS=false
                RUN_E2E_TESTS=true
                RUN_PERFORMANCE_TESTS=false
                RUN_SECURITY_TESTS=false
                RUN_REGRESSION_TESTS=false
                shift
                ;;
            --performance-only)
                RUN_UNIT_TESTS=false
                RUN_INTEGRATION_TESTS=false
                RUN_E2E_TESTS=false
                RUN_PERFORMANCE_TESTS=true
                RUN_SECURITY_TESTS=false
                RUN_REGRESSION_TESTS=false
                shift
                ;;
            --security-only)
                RUN_UNIT_TESTS=false
                RUN_INTEGRATION_TESTS=false
                RUN_E2E_TESTS=false
                RUN_PERFORMANCE_TESTS=false
                RUN_SECURITY_TESTS=true
                RUN_REGRESSION_TESTS=false
                shift
                ;;
            --regression-only)
                RUN_UNIT_TESTS=false
                RUN_INTEGRATION_TESTS=false
                RUN_E2E_TESTS=false
                RUN_PERFORMANCE_TESTS=false
                RUN_SECURITY_TESTS=false
                RUN_REGRESSION_TESTS=true
                shift
                ;;
            --skip-unit)
                RUN_UNIT_TESTS=false
                shift
                ;;
            --skip-integration)
                RUN_INTEGRATION_TESTS=false
                shift
                ;;
            --skip-e2e)
                RUN_E2E_TESTS=false
                shift
                ;;
            --skip-performance)
                RUN_PERFORMANCE_TESTS=false
                shift
                ;;
            --skip-security)
                RUN_SECURITY_TESTS=false
                shift
                ;;
            --skip-regression)
                RUN_REGRESSION_TESTS=false
                shift
                ;;
            --sequential)
                PARALLEL_EXECUTION=false
                shift
                ;;
            --fail-fast)
                CONTINUE_ON_FAILURE=false
                shift
                ;;
            --no-report)
                GENERATE_CONSOLIDATED_REPORT=false
                shift
                ;;
            --no-cleanup)
                CLEANUP_AFTER_TESTS=false
                shift
                ;;
            -h|--help)
                show_usage
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
        esac
    done
    
    log_master "=========================================================="
    log_master "ENTERPRISE BANKING SYSTEM - COMPREHENSIVE TEST EXECUTION"
    log_master "=========================================================="
    log_master "Execution ID: ${TEST_EXECUTION_ID}"
    log_master "Parallel Execution: ${PARALLEL_EXECUTION}"
    log_master "Continue on Failure: ${CONTINUE_ON_FAILURE}"
    log_master "=========================================================="
    
    # Setup test environment
    setup_test_environment
    
    # Execute test suites
    local test_execution_result
    if [ "${PARALLEL_EXECUTION}" = "true" ]; then
        run_tests_parallel
        test_execution_result=$?
    else
        run_tests_sequential
        test_execution_result=$?
    fi
    
    # Generate reports and cleanup
    generate_consolidated_report
    cleanup_test_environment
    
    # Final results
    local end_time=$(date +%s)
    local total_duration=$((end_time - start_time))
    
    echo "" >> "${MASTER_RESULTS_DIR}/test-execution.log"
    echo "===============================================================" >> "${MASTER_RESULTS_DIR}/test-execution.log"
    echo "EXECUTION COMPLETED AT: $(date)" >> "${MASTER_RESULTS_DIR}/test-execution.log"
    echo "TOTAL DURATION: $(date -d@${total_duration} -u +%H:%M:%S)" >> "${MASTER_RESULTS_DIR}/test-execution.log"
    echo "OVERALL RESULT: $([ $test_execution_result -eq 0 ] && echo "SUCCESS" || echo "FAILURE")" >> "${MASTER_RESULTS_DIR}/test-execution.log"
    echo "===============================================================" >> "${MASTER_RESULTS_DIR}/test-execution.log"
    
    log_master "=========================================================="
    log_master "COMPREHENSIVE TEST EXECUTION COMPLETE"
    log_master "Total Duration: $(date -d@${total_duration} -u +%H:%M:%S)"
    log_master "Results Directory: ${MASTER_RESULTS_DIR}"
    log_master "Consolidated Report: ${MASTER_RESULTS_DIR}/consolidated-reports/comprehensive-test-report.html"
    log_master "=========================================================="
    
    if [ $test_execution_result -eq 0 ]; then
        log_success "ALL TEST SUITES COMPLETED SUCCESSFULLY! 🎉"
        exit 0
    else
        log_error "SOME TEST SUITES FAILED. CHECK RESULTS FOR DETAILS. ❌"
        exit 1
    fi
}

# Execute main function with all arguments
main "$@"