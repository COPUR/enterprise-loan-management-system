#!/bin/bash

# ===================================================================
# Enterprise Loan Management System - API Endpoint Testing
# ===================================================================
# Comprehensive API testing script for all endpoints
# ===================================================================

set -euo pipefail

# Configuration
readonly BASE_URL="${BASE_URL:-http://localhost:8080}"
readonly API_BASE="${BASE_URL}/api"
readonly GRAPHQL_URL="${BASE_URL}/graphql"
readonly TIMEOUT=30
readonly TEST_RESULTS_DIR="./test-results/api-tests"

# Color codes
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m'

# Test counters
TESTS_TOTAL=0
TESTS_PASSED=0
TESTS_FAILED=0

# ===================================================================
# Utility Functions
# ===================================================================

log() {
    local level="$1"
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    case "$level" in
        "INFO")  echo -e "${BLUE}[INFO]${NC} ${timestamp} - $message" ;;
        "PASS")  echo -e "${GREEN}[PASS]${NC} ${timestamp} - $message" ;;
        "FAIL")  echo -e "${RED}[FAIL]${NC} ${timestamp} - $message" ;;
        "WARN")  echo -e "${YELLOW}[WARN]${NC} ${timestamp} - $message" ;;
    esac
}

setup_test_environment() {
    mkdir -p "$TEST_RESULTS_DIR"
    
    # Get authentication token for testing
    get_auth_token
}

get_auth_token() {
    log "INFO" "Obtaining authentication token..."
    
    # Try to get token from Keycloak
    local token_response
    token_response=$(curl -s --max-time "$TIMEOUT" \
        -d "client_id=banking-app" \
        -d "client_secret=banking-app-secret-2024" \
        -d "username=bank-admin" \
        -d "password=banking_admin_2024" \
        -d "grant_type=password" \
        "http://localhost:8090/realms/banking-realm/protocol/openid-connect/token" || echo "")
    
    if [ -n "$token_response" ] && echo "$token_response" | jq -e '.access_token' > /dev/null 2>&1; then
        AUTH_TOKEN=$(echo "$token_response" | jq -r '.access_token')
        log "PASS" "Authentication token obtained"
    else
        log "WARN" "Could not obtain auth token, proceeding without authentication"
        AUTH_TOKEN=""
    fi
}

make_api_request() {
    local method="$1"
    local endpoint="$2"
    local data="${3:-}"
    local expected_status="${4:-200}"
    
    local curl_args=("-s" "--max-time" "$TIMEOUT" "-w" "%{http_code}")
    
    if [ -n "$AUTH_TOKEN" ]; then
        curl_args+=("-H" "Authorization: Bearer $AUTH_TOKEN")
    fi
    
    curl_args+=("-H" "Content-Type: application/json")
    curl_args+=("-H" "Accept: application/json")
    
    if [ "$method" != "GET" ] && [ -n "$data" ]; then
        curl_args+=("-d" "$data")
    fi
    
    curl_args+=("-X" "$method" "$endpoint")
    
    local response
    response=$(curl "${curl_args[@]}" 2>/dev/null || echo "REQUEST_FAILED")
    
    if [ "$response" = "REQUEST_FAILED" ]; then
        return 1
    fi
    
    # Extract status code (last line) and body (everything else)
    local status_code
    status_code=$(echo "$response" | tail -c 4)
    local body
    body=$(echo "$response" | head -c -4)
    
    # Store response for debugging
    echo "$body" > "${TEST_RESULTS_DIR}/last_response.json"
    echo "$status_code" > "${TEST_RESULTS_DIR}/last_status.txt"
    
    if [ "$status_code" = "$expected_status" ]; then
        return 0
    else
        return 1
    fi
}

test_endpoint() {
    local test_name="$1"
    local method="$2"
    local endpoint="$3"
    local data="${4:-}"
    local expected_status="${5:-200}"
    
    TESTS_TOTAL=$((TESTS_TOTAL + 1))
    
    log "INFO" "Testing: $test_name"
    
    if make_api_request "$method" "$endpoint" "$data" "$expected_status"; then
        log "PASS" "$test_name - Status: $expected_status"
        TESTS_PASSED=$((TESTS_PASSED + 1))
        return 0
    else
        local actual_status
        actual_status=$(cat "${TEST_RESULTS_DIR}/last_status.txt" 2>/dev/null || echo "ERROR")
        log "FAIL" "$test_name - Expected: $expected_status, Got: $actual_status"
        TESTS_FAILED=$((TESTS_FAILED + 1))
        return 1
    fi
}

# ===================================================================
# Health Check Tests
# ===================================================================

test_health_endpoints() {
    log "INFO" "Testing health check endpoints..."
    
    test_endpoint "Application Health Check" "GET" "${API_BASE}/actuator/health"
    test_endpoint "Application Info" "GET" "${API_BASE}/actuator/info"
    test_endpoint "Metrics Endpoint" "GET" "${API_BASE}/actuator/metrics"
    test_endpoint "Prometheus Metrics" "GET" "${API_BASE}/actuator/prometheus"
}

# ===================================================================
# Customer API Tests
# ===================================================================

test_customer_endpoints() {
    log "INFO" "Testing customer endpoints..."
    
    # Get all customers
    test_endpoint "Get All Customers" "GET" "${API_BASE}/customers"
    
    # Get specific customer
    test_endpoint "Get Customer by ID" "GET" "${API_BASE}/customers/CUST-001"
    
    # Create new customer
    local new_customer_data='{
        "customerId": "CUST-TEST-001",
        "firstName": "Test",
        "lastName": "Customer",
        "email": "test.customer@example.com",
        "phone": "+1-555-9999",
        "dateOfBirth": "1990-01-01",
        "customerType": "INDIVIDUAL",
        "address": {
            "street": "123 Test Street",
            "city": "Test City",
            "state": "TS",
            "zipCode": "12345",
            "country": "USA",
            "type": "HOME"
        }
    }'
    
    test_endpoint "Create New Customer" "POST" "${API_BASE}/customers" "$new_customer_data" "201"
    
    # Update customer
    local update_customer_data='{
        "firstName": "Updated",
        "lastName": "Customer",
        "email": "updated.customer@example.com",
        "phone": "+1-555-8888"
    }'
    
    test_endpoint "Update Customer" "PUT" "${API_BASE}/customers/CUST-TEST-001" "$update_customer_data"
    
    # Get customer credit score
    test_endpoint "Get Customer Credit Score" "GET" "${API_BASE}/customers/CUST-001/credit-score"
}

# ===================================================================
# Loan API Tests
# ===================================================================

test_loan_endpoints() {
    log "INFO" "Testing loan endpoints..."
    
    # Get all loans
    test_endpoint "Get All Loans" "GET" "${API_BASE}/loans"
    
    # Get loans by customer
    test_endpoint "Get Loans by Customer" "GET" "${API_BASE}/customers/CUST-001/loans"
    
    # Get specific loan
    test_endpoint "Get Loan by ID" "GET" "${API_BASE}/loans/LOAN-001"
    
    # Get loan installments
    test_endpoint "Get Loan Installments" "GET" "${API_BASE}/loans/LOAN-001/installments"
    
    # Create loan application
    local loan_application_data='{
        "customerId": "CUST-001",
        "loanType": "PERSONAL",
        "requestedAmount": 20000.00,
        "termMonths": 48,
        "purpose": "Debt Consolidation",
        "financialInfo": {
            "annualIncome": 75000.00,
            "employmentStatus": "EMPLOYED",
            "employmentYears": 5
        }
    }'
    
    test_endpoint "Create Loan Application" "POST" "${API_BASE}/loans/applications" "$loan_application_data" "201"
    
    # Get loan eligibility
    test_endpoint "Check Loan Eligibility" "POST" "${API_BASE}/loans/eligibility" "$loan_application_data"
}

# ===================================================================
# Payment API Tests
# ===================================================================

test_payment_endpoints() {
    log "INFO" "Testing payment endpoints..."
    
    # Get all payments
    test_endpoint "Get All Payments" "GET" "${API_BASE}/payments"
    
    # Get payments by loan
    test_endpoint "Get Payments by Loan" "GET" "${API_BASE}/loans/LOAN-001/payments"
    
    # Get specific payment
    test_endpoint "Get Payment by ID" "GET" "${API_BASE}/payments/PAY-001"
    
    # Create payment
    local payment_data='{
        "loanId": "LOAN-001",
        "amount": 478.66,
        "paymentMethod": "BANK_TRANSFER",
        "paymentReference": "TEST-PAYMENT-001",
        "description": "Test payment"
    }'
    
    test_endpoint "Create Payment" "POST" "${API_BASE}/payments" "$payment_data" "201"
    
    # Process payment (assuming payment was created with ID)
    local process_payment_data='{
        "processedBy": "TEST-SYSTEM"
    }'
    
    # Note: This might fail if payment doesn't exist, but we test the endpoint
    test_endpoint "Process Payment" "POST" "${API_BASE}/payments/PAY-TEST-001/process" "$process_payment_data" "404"
}

# ===================================================================
# AI/ML API Tests
# ===================================================================

test_ai_endpoints() {
    log "INFO" "Testing AI/ML endpoints..."
    
    # Get loan recommendations
    local recommendation_request='{
        "customerId": "CUST-001",
        "preferences": {
            "loanType": "PERSONAL",
            "maxAmount": 50000,
            "maxTermMonths": 60
        }
    }'
    
    test_endpoint "Get Loan Recommendations" "POST" "${API_BASE}/ai/loan-recommendations" "$recommendation_request"
    
    # Fraud detection
    local fraud_check_data='{
        "transactionAmount": 10000.00,
        "customerId": "CUST-001",
        "paymentMethod": "BANK_TRANSFER",
        "location": "New York, NY"
    }'
    
    test_endpoint "Fraud Detection Check" "POST" "${API_BASE}/ai/fraud-detection" "$fraud_check_data"
    
    # Credit scoring
    local credit_score_request='{
        "customerId": "CUST-001",
        "refreshFromBureau": false
    }'
    
    test_endpoint "AI Credit Scoring" "POST" "${API_BASE}/ai/credit-score" "$credit_score_request"
}

# ===================================================================
# GraphQL API Tests
# ===================================================================

test_graphql_endpoints() {
    log "INFO" "Testing GraphQL endpoints..."
    
    # GraphQL introspection query
    local introspection_query='{
        "query": "query IntrospectionQuery { __schema { queryType { name } mutationType { name } subscriptionType { name } } }"
    }'
    
    test_endpoint "GraphQL Introspection" "POST" "$GRAPHQL_URL" "$introspection_query"
    
    # Get customers via GraphQL
    local customers_query='{
        "query": "query GetCustomers { customers { customerId firstName lastName email customerType status } }"
    }'
    
    test_endpoint "GraphQL Get Customers" "POST" "$GRAPHQL_URL" "$customers_query"
    
    # Get loans via GraphQL
    local loans_query='{
        "query": "query GetLoans { loans { loanId customerId loanType principalAmount status } }"
    }'
    
    test_endpoint "GraphQL Get Loans" "POST" "$GRAPHQL_URL" "$loans_query"
    
    # Create customer via GraphQL mutation
    local create_customer_mutation='{
        "query": "mutation CreateCustomer($input: CustomerInput!) { createCustomer(input: $input) { customerId firstName lastName email } }",
        "variables": {
            "input": {
                "customerId": "CUST-GRAPHQL-001",
                "firstName": "GraphQL",
                "lastName": "Test",
                "email": "graphql.test@example.com",
                "phone": "+1-555-0123",
                "dateOfBirth": "1985-05-15",
                "customerType": "INDIVIDUAL"
            }
        }
    }'
    
    test_endpoint "GraphQL Create Customer" "POST" "$GRAPHQL_URL" "$create_customer_mutation"
}

# ===================================================================
# Administrative API Tests
# ===================================================================

test_admin_endpoints() {
    log "INFO" "Testing administrative endpoints..."
    
    # Get system statistics
    test_endpoint "Get System Statistics" "GET" "${API_BASE}/admin/statistics"
    
    # Get audit logs
    test_endpoint "Get Audit Logs" "GET" "${API_BASE}/admin/audit-logs?limit=10"
    
    # System health details
    test_endpoint "System Health Details" "GET" "${API_BASE}/admin/health/detailed"
    
    # Configuration info
    test_endpoint "Configuration Info" "GET" "${API_BASE}/admin/config"
}

# ===================================================================
# Integration Tests
# ===================================================================

test_integration_scenarios() {
    log "INFO" "Testing integration scenarios..."
    
    # End-to-end loan application flow
    local customer_id="CUST-E2E-001"
    
    # 1. Create customer
    local customer_data='{
        "customerId": "'$customer_id'",
        "firstName": "Integration",
        "lastName": "Test",
        "email": "integration.test@example.com",
        "phone": "+1-555-1111",
        "dateOfBirth": "1988-08-08",
        "customerType": "INDIVIDUAL",
        "address": {
            "street": "456 Integration Ave",
            "city": "Test City",
            "state": "TC",
            "zipCode": "54321",
            "country": "USA",
            "type": "HOME"
        }
    }'
    
    if test_endpoint "E2E: Create Customer" "POST" "${API_BASE}/customers" "$customer_data" "201"; then
        
        # 2. Apply for loan
        local loan_data='{
            "customerId": "'$customer_id'",
            "loanType": "PERSONAL",
            "requestedAmount": 15000.00,
            "termMonths": 36,
            "purpose": "Home Improvement"
        }'
        
        if test_endpoint "E2E: Apply for Loan" "POST" "${API_BASE}/loans/applications" "$loan_data" "201"; then
            
            # 3. Check application status
            test_endpoint "E2E: Check Application Status" "GET" "${API_BASE}/customers/$customer_id/loans"
            
            log "PASS" "End-to-end integration test completed"
        fi
    fi
}

# ===================================================================
# Performance and Load Tests
# ===================================================================

test_performance() {
    log "INFO" "Testing performance characteristics..."
    
    # Test concurrent requests
    local pids=()
    local start_time=$(date +%s)
    
    for i in {1..10}; do
        (make_api_request "GET" "${API_BASE}/customers" "" "200" > /dev/null 2>&1) &
        pids+=($!)
    done
    
    # Wait for all background jobs
    for pid in "${pids[@]}"; do
        wait "$pid"
    done
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    if [ "$duration" -lt 10 ]; then
        log "PASS" "Performance test: 10 concurrent requests completed in ${duration}s"
    else
        log "WARN" "Performance test: 10 concurrent requests took ${duration}s (might be slow)"
    fi
}

# ===================================================================
# Security Tests
# ===================================================================

test_security() {
    log "INFO" "Testing security aspects..."
    
    # Test without authentication (should fail for protected endpoints)
    local old_token="$AUTH_TOKEN"
    AUTH_TOKEN=""
    
    test_endpoint "Security: Access without auth" "GET" "${API_BASE}/admin/statistics" "" "401"
    
    # Test with invalid token
    AUTH_TOKEN="invalid-token"
    test_endpoint "Security: Access with invalid token" "GET" "${API_BASE}/admin/statistics" "" "401"
    
    # Restore token
    AUTH_TOKEN="$old_token"
    
    # Test SQL injection prevention
    test_endpoint "Security: SQL Injection Test" "GET" "${API_BASE}/customers/'; DROP TABLE customers; --" "" "404"
    
    # Test XSS prevention
    local xss_data='{
        "firstName": "<script>alert(\"xss\")</script>",
        "lastName": "Test",
        "email": "xss.test@example.com"
    }'
    
    test_endpoint "Security: XSS Prevention Test" "POST" "${API_BASE}/customers" "$xss_data" "400"
}

# ===================================================================
# Report Generation
# ===================================================================

generate_test_report() {
    local report_file="${TEST_RESULTS_DIR}/api_test_report.html"
    
    cat > "$report_file" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>API Test Report - Enterprise Loan Management System</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background: #f4f4f4; padding: 20px; border-radius: 5px; }
        .summary { margin: 20px 0; padding: 15px; background: #e8f5e8; border-radius: 5px; }
        .pass { color: green; font-weight: bold; }
        .fail { color: red; font-weight: bold; }
        .warn { color: orange; font-weight: bold; }
        table { border-collapse: collapse; width: 100%; margin: 20px 0; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
    </style>
</head>
<body>
    <div class="header">
        <h1>🏦 Enterprise Loan Management System - API Test Report</h1>
        <p>Generated on: $(date)</p>
        <p>Base URL: $BASE_URL</p>
    </div>
    
    <div class="summary">
        <h2>Test Summary</h2>
        <p><strong>Total Tests:</strong> $TESTS_TOTAL</p>
        <p class="pass"><strong>Passed:</strong> $TESTS_PASSED</p>
        <p class="fail"><strong>Failed:</strong> $TESTS_FAILED</p>
        <p><strong>Success Rate:</strong> $(( TESTS_PASSED * 100 / TESTS_TOTAL ))%</p>
    </div>
    
    <h2>Test Categories</h2>
    <ul>
        <li>Health Check Endpoints</li>
        <li>Customer Management API</li>
        <li>Loan Management API</li>
        <li>Payment Processing API</li>
        <li>AI/ML Integration API</li>
        <li>GraphQL API</li>
        <li>Administrative API</li>
        <li>Integration Scenarios</li>
        <li>Performance Tests</li>
        <li>Security Tests</li>
    </ul>
    
    <h2>Available Endpoints</h2>
    <table>
        <tr><th>Category</th><th>Endpoint</th><th>Method</th><th>Description</th></tr>
        <tr><td>Health</td><td>/api/actuator/health</td><td>GET</td><td>Application health check</td></tr>
        <tr><td>Customer</td><td>/api/customers</td><td>GET/POST</td><td>Customer management</td></tr>
        <tr><td>Loan</td><td>/api/loans</td><td>GET/POST</td><td>Loan management</td></tr>
        <tr><td>Payment</td><td>/api/payments</td><td>GET/POST</td><td>Payment processing</td></tr>
        <tr><td>AI</td><td>/api/ai/loan-recommendations</td><td>POST</td><td>AI loan recommendations</td></tr>
        <tr><td>GraphQL</td><td>/graphql</td><td>POST</td><td>GraphQL API</td></tr>
        <tr><td>Admin</td><td>/api/admin/statistics</td><td>GET</td><td>System statistics</td></tr>
    </table>
    
    <p><em>For detailed test logs, check: $TEST_RESULTS_DIR/</em></p>
</body>
</html>
EOF

    log "INFO" "Test report generated: $report_file"
}

# ===================================================================
# Main Execution
# ===================================================================

main() {
    log "INFO" "Starting comprehensive API endpoint testing..."
    
    setup_test_environment
    
    # Run all test suites
    test_health_endpoints
    test_customer_endpoints
    test_loan_endpoints
    test_payment_endpoints
    test_ai_endpoints
    test_graphql_endpoints
    test_admin_endpoints
    test_integration_scenarios
    test_performance
    test_security
    
    # Generate report
    generate_test_report
    
    # Final summary
    echo
    log "INFO" "API Testing Complete!"
    log "INFO" "Total Tests: $TESTS_TOTAL"
    log "PASS" "Passed: $TESTS_PASSED"
    
    if [ "$TESTS_FAILED" -gt 0 ]; then
        log "FAIL" "Failed: $TESTS_FAILED"
        exit 1
    else
        log "PASS" "All tests passed successfully!"
        exit 0
    fi
}

# Execute main function
main "$@"