#!/bin/bash

# ===============================================================
# ENTERPRISE BANKING SYSTEM - COMPREHENSIVE REGRESSION TEST SUITE
# ===============================================================
# Document Information:
# - Author: Senior QA Engineer & Test Automation Lead
# - Version: 1.0.0
# - Last Updated: December 2024
# - Classification: Internal - Test Automation
# - Purpose: Execute comprehensive regression testing
# ===============================================================

set -euo pipefail

# ============================================
# CONFIGURATION AND SETUP
# ============================================

# Test configuration
TEST_START_TIME=$(date +%s)
TEST_RESULTS_DIR="/test-results"
TEST_TIMEOUT=${TEST_TIMEOUT:-1800}  # 30 minutes default
PARALLEL_TESTS=${PARALLEL_TESTS:-true}
REGRESSION_REPORT_FORMAT=${REGRESSION_REPORT_FORMAT:-"html,json,junit"}

# Service URLs (from environment or defaults)
API_GATEWAY_URL=${API_GATEWAY_URL:-"http://api-gateway-test:8080"}
CUSTOMER_SERVICE_URL=${CUSTOMER_SERVICE_URL:-"http://customer-service-test:8081"}
LOAN_SERVICE_URL=${LOAN_SERVICE_URL:-"http://loan-service-test:8082"}
PAYMENT_SERVICE_URL=${PAYMENT_SERVICE_URL:-"http://payment-service-test:8083"}

# Database connection
TEST_DATABASE_URL=${TEST_DATABASE_URL:-"jdbc:postgresql://postgres-test:5432/banking_test"}
TEST_DATABASE_USERNAME=${TEST_DATABASE_USERNAME:-"banking_test_user"}
TEST_DATABASE_PASSWORD=${TEST_DATABASE_PASSWORD:-"test_password_2024"}

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ============================================
# UTILITY FUNCTIONS
# ============================================

log_info() {
    echo -e "${BLUE}[REGRESSION-INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_success() {
    echo -e "${GREEN}[REGRESSION-SUCCESS]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_warning() {
    echo -e "${YELLOW}[REGRESSION-WARNING]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[REGRESSION-ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

wait_for_services() {
    log_info "Waiting for all services to be ready..."
    
    local services=(
        "${API_GATEWAY_URL}/actuator/health"
        "${CUSTOMER_SERVICE_URL}/actuator/health" 
        "${LOAN_SERVICE_URL}/actuator/health"
        "${PAYMENT_SERVICE_URL}/actuator/health"
    )
    
    local max_wait=300  # 5 minutes
    local wait_time=0
    local all_ready=false
    
    while [ $wait_time -lt $max_wait ] && [ "$all_ready" = false ]; do
        all_ready=true
        
        for service in "${services[@]}"; do
            if ! curl -s -f "$service" > /dev/null 2>&1; then
                all_ready=false
                break
            fi
        done
        
        if [ "$all_ready" = false ]; then
            log_info "Services not ready yet, waiting... (${wait_time}s/${max_wait}s)"
            sleep 10
            wait_time=$((wait_time + 10))
        fi
    done
    
    if [ "$all_ready" = true ]; then
        log_success "All services are ready"
    else
        log_error "Services failed to become ready within ${max_wait} seconds"
        exit 1
    fi
}

create_test_data() {
    log_info "Creating regression test data..."
    
    # Execute test data creation script
    psql "${TEST_DATABASE_URL}" -U "${TEST_DATABASE_USERNAME}" -f /app/scripts/test-data/create-sample-banking-data.sql || {
        log_error "Failed to create test data"
        exit 1
    }
    
    log_success "Test data created successfully"
}

# ============================================
# REGRESSION TEST CATEGORIES
# ============================================

run_unit_tests() {
    log_info "Running unit test regression suite..."
    
    local test_report="${TEST_RESULTS_DIR}/unit-test-results.xml"
    local test_html="${TEST_RESULTS_DIR}/unit-test-report.html"
    
    ./gradlew test \
        --tests "*UnitTest" \
        --tests "*Test" \
        --exclude-task integrationTest \
        --continue \
        --build-cache \
        --parallel \
        --max-workers=4 \
        -Dspring.profiles.active=test \
        -Djunit.platform.output.capture.stdout=true \
        -Djunit.platform.output.capture.stderr=true \
        || local unit_test_failed=true
    
    # Generate reports
    ./gradlew jacocoTestReport
    
    if [ "${unit_test_failed:-false}" = "true" ]; then
        log_warning "Some unit tests failed, but continuing with regression suite"
        return 1
    else
        log_success "Unit tests completed successfully"
        return 0
    fi
}

run_integration_tests() {
    log_info "Running integration test regression suite..."
    
    local integration_report="${TEST_RESULTS_DIR}/integration-test-results.xml"
    
    ./gradlew integrationTest \
        --tests "*IntegrationTest" \
        --tests "*IT" \
        --continue \
        --stacktrace \
        -Dspring.profiles.active=integration,test \
        -Dtest.database.url="${TEST_DATABASE_URL}" \
        -Dtest.database.username="${TEST_DATABASE_USERNAME}" \
        -Dtest.database.password="${TEST_DATABASE_PASSWORD}" \
        -Dapi.gateway.url="${API_GATEWAY_URL}" \
        || local integration_failed=true
    
    if [ "${integration_failed:-false}" = "true" ]; then
        log_warning "Some integration tests failed"
        return 1
    else
        log_success "Integration tests completed successfully"
        return 0
    fi
}

run_api_regression_tests() {
    log_info "Running API regression test suite..."
    
    local api_test_dir="${TEST_RESULTS_DIR}/api-tests"
    mkdir -p "${api_test_dir}"
    
    # Test API Gateway endpoints
    local api_tests=(
        "customer-management-api-test"
        "loan-origination-api-test"  
        "payment-processing-api-test"
        "saga-workflow-api-test"
    )
    
    local api_failures=0
    
    for test in "${api_tests[@]}"; do
        log_info "Running ${test}..."
        
        if run_api_test_case "${test}" "${api_test_dir}"; then
            log_success "${test} passed"
        else
            log_error "${test} failed"
            ((api_failures++))
        fi
    done
    
    if [ $api_failures -eq 0 ]; then
        log_success "All API regression tests passed"
        return 0
    else
        log_error "${api_failures} API test(s) failed"
        return 1
    fi
}

run_api_test_case() {
    local test_name=$1
    local output_dir=$2
    
    case $test_name in
        "customer-management-api-test")
            run_customer_api_tests "${output_dir}"
            ;;
        "loan-origination-api-test")
            run_loan_api_tests "${output_dir}"
            ;;
        "payment-processing-api-test")
            run_payment_api_tests "${output_dir}"
            ;;
        "saga-workflow-api-test")
            run_saga_api_tests "${output_dir}"
            ;;
        *)
            log_error "Unknown API test case: ${test_name}"
            return 1
            ;;
    esac
}

run_customer_api_tests() {
    local output_dir=$1
    
    # Test customer creation
    local customer_data='{"firstName":"RegressionTest","lastName":"Customer","email":"regression@test.com","phone":"+971501234567","address":"Test Address","city":"Dubai","postalCode":"12345","country":"UAE","creditScore":750,"creditLimit":200000}'
    
    local create_response=$(curl -s -w "\n%{http_code}" -X POST \
        "${API_GATEWAY_URL}/api/v1/customers" \
        -H "Content-Type: application/json" \
        -d "${customer_data}")
    
    local create_status=$(echo "${create_response}" | tail -n1)
    local create_body=$(echo "${create_response}" | head -n -1)
    
    if [ "${create_status}" = "201" ] || [ "${create_status}" = "200" ]; then
        local customer_id=$(echo "${create_body}" | jq -r '.id')
        
        # Test customer retrieval
        local get_response=$(curl -s -w "\n%{http_code}" \
            "${API_GATEWAY_URL}/api/v1/customers/${customer_id}")
        
        local get_status=$(echo "${get_response}" | tail -n1)
        
        if [ "${get_status}" = "200" ]; then
            echo "${get_response}" > "${output_dir}/customer-api-test-success.json"
            return 0
        fi
    fi
    
    echo "${create_response}" > "${output_dir}/customer-api-test-failure.json"
    return 1
}

run_loan_api_tests() {
    local output_dir=$1
    
    # Test loan creation
    local loan_data='{"customerId":1001,"loanAmount":75000,"interestRate":0.15,"installmentCount":12,"loanType":"PERSONAL","purpose":"HOME_RENOVATION"}'
    
    local create_response=$(curl -s -w "\n%{http_code}" -X POST \
        "${API_GATEWAY_URL}/api/v1/loans" \
        -H "Content-Type: application/json" \
        -d "${loan_data}")
    
    local create_status=$(echo "${create_response}" | tail -n1)
    local create_body=$(echo "${create_response}" | head -n -1)
    
    if [ "${create_status}" = "201" ] || [ "${create_status}" = "200" ]; then
        local loan_id=$(echo "${create_body}" | jq -r '.id')
        
        # Test loan retrieval
        local get_response=$(curl -s -w "\n%{http_code}" \
            "${API_GATEWAY_URL}/api/v1/loans/${loan_id}")
        
        local get_status=$(echo "${get_response}" | tail -n1)
        
        if [ "${get_status}" = "200" ]; then
            echo "${get_response}" > "${output_dir}/loan-api-test-success.json"
            return 0
        fi
    fi
    
    echo "${create_response}" > "${output_dir}/loan-api-test-failure.json"
    return 1
}

run_payment_api_tests() {
    local output_dir=$1
    
    # Test payment processing
    local payment_data='{"loanId":2001,"amount":5000,"paymentMethod":"BANK_TRANSFER","paymentDate":"2024-12-01T10:00:00Z"}'
    
    local create_response=$(curl -s -w "\n%{http_code}" -X POST \
        "${API_GATEWAY_URL}/api/v1/payments" \
        -H "Content-Type: application/json" \
        -d "${payment_data}")
    
    local create_status=$(echo "${create_response}" | tail -n1)
    local create_body=$(echo "${create_response}" | head -n -1)
    
    if [ "${create_status}" = "201" ] || [ "${create_status}" = "200" ]; then
        local payment_id=$(echo "${create_body}" | jq -r '.id')
        
        # Test payment retrieval
        local get_response=$(curl -s -w "\n%{http_code}" \
            "${API_GATEWAY_URL}/api/v1/payments/${payment_id}")
        
        local get_status=$(echo "${get_response}" | tail -n1)
        
        if [ "${get_status}" = "200" ]; then
            echo "${get_response}" > "${output_dir}/payment-api-test-success.json"
            return 0
        fi
    fi
    
    echo "${create_response}" > "${output_dir}/payment-api-test-failure.json"
    return 1
}

run_saga_api_tests() {
    local output_dir=$1
    
    # Test complete SAGA workflow
    local saga_data='{"customerId":1002,"loanAmount":100000,"interestRate":0.18,"installmentCount":24,"loanType":"PERSONAL","purpose":"EDUCATION"}'
    
    local saga_response=$(curl -s -w "\n%{http_code}" -X POST \
        "${API_GATEWAY_URL}/api/v1/loans/saga" \
        -H "Content-Type: application/json" \
        -H "X-SAGA-Test: true" \
        -d "${saga_data}")
    
    local saga_status=$(echo "${saga_response}" | tail -n1)
    local saga_body=$(echo "${saga_response}" | head -n -1)
    
    if [ "${saga_status}" = "201" ] || [ "${saga_status}" = "200" ]; then
        local saga_id=$(echo "${saga_body}" | jq -r '.sagaId')
        
        # Wait for SAGA completion
        sleep 5
        
        # Verify SAGA completion
        local saga_status_response=$(curl -s -w "\n%{http_code}" \
            "${API_GATEWAY_URL}/api/v1/sagas/${saga_id}/status")
        
        local status_code=$(echo "${saga_status_response}" | tail -n1)
        
        if [ "${status_code}" = "200" ]; then
            echo "${saga_status_response}" > "${output_dir}/saga-api-test-success.json"
            return 0
        fi
    fi
    
    echo "${saga_response}" > "${output_dir}/saga-api-test-failure.json"
    return 1
}

run_data_integrity_tests() {
    log_info "Running data integrity regression tests..."
    
    local data_test_results="${TEST_RESULTS_DIR}/data-integrity-results.txt"
    
    # Test database constraints
    local constraint_tests=(
        "check_foreign_key_constraints"
        "check_data_consistency"
        "check_audit_trail_integrity"
        "check_transaction_completeness"
    )
    
    local data_failures=0
    
    for test in "${constraint_tests[@]}"; do
        log_info "Running ${test}..."
        
        if run_data_integrity_test "${test}" "${data_test_results}"; then
            log_success "${test} passed"
        else
            log_error "${test} failed"
            ((data_failures++))
        fi
    done
    
    if [ $data_failures -eq 0 ]; then
        log_success "All data integrity tests passed"
        return 0
    else
        log_error "${data_failures} data integrity test(s) failed"
        return 1
    fi
}

run_data_integrity_test() {
    local test_name=$1
    local output_file=$2
    
    case $test_name in
        "check_foreign_key_constraints")
            # Check for orphaned records
            local orphaned_loans=$(psql "${TEST_DATABASE_URL}" -U "${TEST_DATABASE_USERNAME}" -t -c \
                "SELECT COUNT(*) FROM loans l LEFT JOIN customers c ON l.customer_id = c.id WHERE c.id IS NULL;")
            
            if [ "${orphaned_loans}" -eq 0 ]; then
                echo "✓ No orphaned loan records found" >> "${output_file}"
                return 0
            else
                echo "✗ Found ${orphaned_loans} orphaned loan records" >> "${output_file}"
                return 1
            fi
            ;;
        "check_data_consistency")
            # Check loan amount calculations
            local inconsistent_loans=$(psql "${TEST_DATABASE_URL}" -U "${TEST_DATABASE_USERNAME}" -t -c \
                "SELECT COUNT(*) FROM loans WHERE total_amount != (loan_amount * (1 + interest_rate * installment_count / 12));")
            
            if [ "${inconsistent_loans}" -eq 0 ]; then
                echo "✓ All loan calculations are consistent" >> "${output_file}"
                return 0
            else
                echo "✗ Found ${inconsistent_loans} loans with inconsistent calculations" >> "${output_file}"
                return 1
            fi
            ;;
        "check_audit_trail_integrity")
            # Check audit events for critical operations
            local missing_audit_events=$(psql "${TEST_DATABASE_URL}" -U "${TEST_DATABASE_USERNAME}" -t -c \
                "SELECT COUNT(*) FROM loans l LEFT JOIN audit_events a ON l.id::text = a.entity_id AND a.entity_type = 'LOAN' AND a.event_type = 'LOAN_CREATED' WHERE a.id IS NULL;")
            
            if [ "${missing_audit_events}" -eq 0 ]; then
                echo "✓ All loan creations have audit events" >> "${output_file}"
                return 0
            else
                echo "✗ Found ${missing_audit_events} loans without audit events" >> "${output_file}"
                return 1
            fi
            ;;
        "check_transaction_completeness")
            # Check payment allocation completeness
            local incomplete_payments=$(psql "${TEST_DATABASE_URL}" -U "${TEST_DATABASE_USERNAME}" -t -c \
                "SELECT COUNT(*) FROM payments p WHERE p.status = 'COMPLETED' AND NOT EXISTS (SELECT 1 FROM payment_installments pi WHERE pi.payment_id = p.id);")
            
            if [ "${incomplete_payments}" -eq 0 ]; then
                echo "✓ All completed payments have installment allocations" >> "${output_file}"
                return 0
            else
                echo "✗ Found ${incomplete_payments} completed payments without installment allocations" >> "${output_file}"
                return 1
            fi
            ;;
        *)
            echo "✗ Unknown data integrity test: ${test_name}" >> "${output_file}"
            return 1
            ;;
    esac
}

run_business_logic_tests() {
    log_info "Running business logic regression tests..."
    
    ./gradlew test \
        --tests "*BusinessLogicTest" \
        --tests "*BusinessRuleTest" \
        --tests "*ValidationTest" \
        --continue \
        -Dspring.profiles.active=test \
        -Dbusiness.rules.validation.enabled=true \
        || local business_logic_failed=true
    
    if [ "${business_logic_failed:-false}" = "true" ]; then
        log_error "Business logic regression tests failed"
        return 1
    else
        log_success "Business logic regression tests completed successfully"
        return 0
    fi
}

run_security_regression_tests() {
    log_info "Running security regression tests..."
    
    local security_results="${TEST_RESULTS_DIR}/security-regression-results.txt"
    
    # Test authentication endpoints
    local auth_test_response=$(curl -s -w "\n%{http_code}" -X POST \
        "${API_GATEWAY_URL}/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"invalid","password":"invalid"}')
    
    local auth_status=$(echo "${auth_test_response}" | tail -n1)
    
    if [ "${auth_status}" = "401" ]; then
        echo "✓ Authentication properly rejects invalid credentials" >> "${security_results}"
    else
        echo "✗ Authentication security test failed" >> "${security_results}"
        return 1
    fi
    
    # Test unauthorized access
    local unauth_response=$(curl -s -w "\n%{http_code}" \
        "${API_GATEWAY_URL}/api/v1/customers/1")
    
    local unauth_status=$(echo "${unauth_response}" | tail -n1)
    
    if [ "${unauth_status}" = "401" ] || [ "${unauth_status}" = "403" ]; then
        echo "✓ Unauthorized access properly blocked" >> "${security_results}"
    else
        echo "✗ Unauthorized access security test failed" >> "${security_results}"
        return 1
    fi
    
    log_success "Security regression tests completed successfully"
    return 0
}

# ============================================
# REPORT GENERATION
# ============================================

generate_regression_report() {
    log_info "Generating comprehensive regression test report..."
    
    local report_file="${TEST_RESULTS_DIR}/regression-test-report.html"
    local test_end_time=$(date +%s)
    local test_duration=$((test_end_time - TEST_START_TIME))
    
    cat > "${report_file}" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>Enterprise Banking System - Regression Test Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        .header { background: #2c3e50; color: white; padding: 20px; border-radius: 5px; }
        .section { margin: 20px 0; padding: 15px; border: 1px solid #ddd; border-radius: 5px; }
        .success { background: #d4edda; border-color: #c3e6cb; }
        .warning { background: #fff3cd; border-color: #ffeaa7; }
        .error { background: #f8d7da; border-color: #f5c6cb; }
        .metrics { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; }
        .metric { text-align: center; padding: 15px; background: #f8f9fa; border-radius: 5px; }
        .metric h3 { margin: 0; color: #2c3e50; }
        .metric .value { font-size: 2em; font-weight: bold; color: #e74c3c; }
    </style>
</head>
<body>
    <div class="header">
        <h1>Enterprise Banking System - Regression Test Report</h1>
        <p>Generated on: $(date)</p>
        <p>Test Duration: $(date -d@${test_duration} -u +%H:%M:%S)</p>
        <p>Test Environment: Docker Containerized</p>
    </div>
    
    <div class="section">
        <h2>Test Execution Summary</h2>
        <div class="metrics">
            <div class="metric">
                <h3>Total Test Duration</h3>
                <div class="value">$(date -d@${test_duration} -u +%H:%M:%S)</div>
            </div>
            <div class="metric">
                <h3>Test Categories</h3>
                <div class="value">6</div>
            </div>
            <div class="metric">
                <h3>Services Tested</h3>
                <div class="value">4</div>
            </div>
        </div>
    </div>
    
    <div class="section">
        <h2>Test Categories Executed</h2>
        <ul>
            <li><strong>Unit Tests:</strong> Individual component validation</li>
            <li><strong>Integration Tests:</strong> Service interaction validation</li>
            <li><strong>API Regression Tests:</strong> REST endpoint functionality</li>
            <li><strong>Data Integrity Tests:</strong> Database consistency validation</li>
            <li><strong>Business Logic Tests:</strong> Banking rule validation</li>
            <li><strong>Security Regression Tests:</strong> Security control validation</li>
        </ul>
    </div>
    
    <div class="section">
        <h2>Test Results Summary</h2>
        <p>Detailed test results are available in the individual result files.</p>
        <ul>
            <li>Unit Test Results: unit-test-results.xml</li>
            <li>Integration Test Results: integration-test-results.xml</li>
            <li>API Test Results: api-tests/</li>
            <li>Data Integrity Results: data-integrity-results.txt</li>
            <li>Security Test Results: security-regression-results.txt</li>
        </ul>
    </div>
</body>
</html>
EOF
    
    log_success "Regression test report generated: ${report_file}"
}

# ============================================
# MAIN EXECUTION FLOW
# ============================================

main() {
    log_info "Starting Enterprise Banking System Regression Test Suite"
    log_info "=========================================================="
    
    # Verify test environment
    wait_for_services
    create_test_data
    
    # Track test results
    local test_failures=0
    
    # Execute test suites
    log_info "Executing regression test suites..."
    
    run_unit_tests || ((test_failures++))
    run_integration_tests || ((test_failures++))
    run_api_regression_tests || ((test_failures++))
    run_data_integrity_tests || ((test_failures++))
    run_business_logic_tests || ((test_failures++))
    run_security_regression_tests || ((test_failures++))
    
    # Generate comprehensive report
    generate_regression_report
    
    # Final results
    local test_end_time=$(date +%s)
    local total_duration=$((test_end_time - TEST_START_TIME))
    
    log_info "=========================================================="
    log_info "Regression Test Suite Execution Complete"
    log_info "Total Duration: $(date -d@${total_duration} -u +%H:%M:%S)"
    
    if [ $test_failures -eq 0 ]; then
        log_success "All regression test suites passed successfully!"
        exit 0
    else
        log_error "${test_failures} regression test suite(s) failed."
        exit 1
    fi
}

# Execute main function
main "$@"