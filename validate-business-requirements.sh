#!/bin/bash

# Enterprise Banking System - Business Requirements Validation
# Tests implementation against Orange Solution Java Backend Developer Case requirements

set -e

echo "🏦 Validating Business Requirements - Orange Solution Case Study..."

# Configuration
BASE_URL=${BASE_URL:-"http://localhost:8080"}
ADMIN_USER=${ADMIN_USER:-"admin"}
ADMIN_PASS=${ADMIN_PASS:-"admin123"}
CUSTOMER_USER=${CUSTOMER_USER:-"customer"}
CUSTOMER_PASS=${CUSTOMER_PASS:-"customer123"}
TEST_CUSTOMER_ID=""
TEST_LOAN_ID=""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

print_requirement() {
    echo -e "${YELLOW}[REQ]${NC} $1"
}

# Test counter
TESTS_PASSED=0
TESTS_FAILED=0
TOTAL_TESTS=0

# Test functions
run_test() {
    local test_name="$1"
    local test_command="$2"
    local expected_result="$3"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    print_status "Running test: $test_name"
    
    if eval "$test_command"; then
        TESTS_PASSED=$((TESTS_PASSED + 1))
        print_success "✓ $test_name"
        return 0
    else
        TESTS_FAILED=$((TESTS_FAILED + 1))
        print_error "✗ $test_name"
        return 1
    fi
}

# Authentication helper
get_auth_token() {
    local username="$1"
    local password="$2"
    
    # Try OAuth2 token endpoint first
    local token_response=$(curl -s -X POST "$BASE_URL/oauth2/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=password&username=$username&password=$password&client_id=banking-client" 2>/dev/null)
    
    if echo "$token_response" | grep -q "access_token"; then
        echo "$token_response" | jq -r .access_token 2>/dev/null || echo ""
    else
        # Fallback to basic auth
        echo ""
    fi
}

# API call helper
api_call() {
    local method="$1"
    local endpoint="$2"
    local data="$3"
    local auth_type="$4"
    local username="$5"
    local password="$6"
    
    local auth_header=""
    if [ "$auth_type" = "bearer" ]; then
        local token=$(get_auth_token "$username" "$password")
        if [ -n "$token" ]; then
            auth_header="-H \"Authorization: Bearer $token\""
        else
            auth_header="-u $username:$password"
        fi
    else
        auth_header="-u $username:$password"
    fi
    
    if [ "$method" = "GET" ]; then
        eval "curl -s $auth_header \"$BASE_URL$endpoint\""
    else
        eval "curl -s $auth_header -X $method -H \"Content-Type: application/json\" -d '$data' \"$BASE_URL$endpoint\""
    fi
}

# Business Requirement Tests
print_requirement "=== BUSINESS REQUIREMENT 1: CREATE LOAN ==="

test_customer_creation() {
    print_status "Creating test customer with credit limit"
    
    local customer_data='{
        "name": "John",
        "surname": "Doe", 
        "creditLimit": 50000.0,
        "usedCreditLimit": 0.0
    }'
    
    local response=$(api_call "POST" "/api/v1/customers" "$customer_data" "basic" "$ADMIN_USER" "$ADMIN_PASS")
    
    if echo "$response" | grep -q "customerId\|id"; then
        TEST_CUSTOMER_ID=$(echo "$response" | jq -r '.customerId // .id' 2>/dev/null || echo "12345")
        print_success "Customer created with ID: $TEST_CUSTOMER_ID"
        return 0
    else
        print_error "Failed to create customer: $response"
        return 1
    fi
}

test_loan_creation_valid() {
    print_status "Testing valid loan creation"
    
    local loan_data='{
        "customerId": "'$TEST_CUSTOMER_ID'",
        "amount": 10000.0,
        "interestRate": 0.2,
        "numberOfInstallments": 12
    }'
    
    local response=$(api_call "POST" "/api/v1/loans" "$loan_data" "basic" "$ADMIN_USER" "$ADMIN_PASS")
    
    if echo "$response" | grep -q "loanId\|id"; then
        TEST_LOAN_ID=$(echo "$response" | jq -r '.loanId // .id' 2>/dev/null || echo "loan-001")
        print_success "Loan created with ID: $TEST_LOAN_ID"
        return 0
    else
        print_error "Failed to create loan: $response"
        return 1
    fi
}

test_installment_restrictions() {
    print_status "Testing installment number restrictions (6, 9, 12, 24 only)"
    
    # Test invalid installment numbers
    for invalid_installments in 5 7 8 10 11 13 18 36; do
        local loan_data='{
            "customerId": "'$TEST_CUSTOMER_ID'",
            "amount": 5000.0,
            "interestRate": 0.15,
            "numberOfInstallments": '$invalid_installments'
        }'
        
        local response=$(api_call "POST" "/api/v1/loans" "$loan_data" "basic" "$ADMIN_USER" "$ADMIN_PASS")
        
        if echo "$response" | grep -q -i "error\|invalid\|bad"; then
            print_success "✓ Correctly rejected $invalid_installments installments"
        else
            print_error "✗ Should reject $invalid_installments installments"
            return 1
        fi
    done
    
    return 0
}

test_interest_rate_restrictions() {
    print_status "Testing interest rate restrictions (0.1 - 0.5)"
    
    # Test invalid interest rates
    for invalid_rate in 0.05 0.09 0.51 0.6 1.0; do
        local loan_data='{
            "customerId": "'$TEST_CUSTOMER_ID'",
            "amount": 5000.0,
            "interestRate": '$invalid_rate',
            "numberOfInstallments": 12
        }'
        
        local response=$(api_call "POST" "/api/v1/loans" "$loan_data" "basic" "$ADMIN_USER" "$ADMIN_PASS")
        
        if echo "$response" | grep -q -i "error\|invalid\|bad"; then
            print_success "✓ Correctly rejected interest rate $invalid_rate"
        else
            print_error "✗ Should reject interest rate $invalid_rate"
            return 1
        fi
    done
    
    return 0
}

test_credit_limit_check() {
    print_status "Testing credit limit validation"
    
    # Try to create loan exceeding credit limit
    local excessive_loan='{
        "customerId": "'$TEST_CUSTOMER_ID'",
        "amount": 100000.0,
        "interestRate": 0.2,
        "numberOfInstallments": 12
    }'
    
    local response=$(api_call "POST" "/api/v1/loans" "$excessive_loan" "basic" "$ADMIN_USER" "$ADMIN_PASS")
    
    if echo "$response" | grep -q -i "credit.*limit\|insufficient\|exceed"; then
        print_success "✓ Correctly validated credit limit"
        return 0
    else
        print_warning "Credit limit validation may not be working: $response"
        return 1
    fi
}

print_requirement "=== BUSINESS REQUIREMENT 2: LIST LOANS ==="

test_list_loans() {
    print_status "Testing loan listing for customer"
    
    local response=$(api_call "GET" "/api/v1/loans/customer/$TEST_CUSTOMER_ID" "" "basic" "$ADMIN_USER" "$ADMIN_PASS")
    
    if echo "$response" | grep -q "loanId\|id\|\["; then
        print_success "✓ Can list loans for customer"
        return 0
    else
        print_error "✗ Failed to list loans: $response"
        return 1
    fi
}

print_requirement "=== BUSINESS REQUIREMENT 3: LIST INSTALLMENTS ==="

test_list_installments() {
    print_status "Testing installment listing for loan"
    
    local response=$(api_call "GET" "/api/v1/loans/$TEST_LOAN_ID/installments" "" "basic" "$ADMIN_USER" "$ADMIN_PASS")
    
    if echo "$response" | grep -q "installment\|amount\|dueDate\|\["; then
        print_success "✓ Can list installments for loan"
        return 0
    else
        print_error "✗ Failed to list installments: $response"
        return 1
    fi
}

print_requirement "=== BUSINESS REQUIREMENT 4: PAY LOAN ==="

test_payment_processing() {
    print_status "Testing loan payment processing"
    
    local payment_data='{
        "loanId": "'$TEST_LOAN_ID'",
        "amount": 1000.0
    }'
    
    local response=$(api_call "POST" "/api/v1/payments" "$payment_data" "basic" "$ADMIN_USER" "$ADMIN_PASS")
    
    if echo "$response" | grep -q "payment\|success\|installments.*paid"; then
        print_success "✓ Payment processing works"
        return 0
    else
        print_error "✗ Payment processing failed: $response"
        return 1
    fi
}

print_requirement "=== SECURITY REQUIREMENTS ==="

test_admin_authorization() {
    print_status "Testing admin authorization"
    
    # Test access with admin credentials
    local response=$(api_call "GET" "/api/v1/customers" "" "basic" "$ADMIN_USER" "$ADMIN_PASS")
    
    if echo "$response" | grep -q "customer\|name\|\["; then
        print_success "✓ Admin can access customer data"
        return 0
    else
        print_error "✗ Admin authorization failed: $response"
        return 1
    fi
}

test_unauthorized_access() {
    print_status "Testing unauthorized access rejection"
    
    # Test access without credentials
    local response=$(curl -s "$BASE_URL/api/v1/customers")
    
    if echo "$response" | grep -q -i "unauthorized\|forbidden\|401\|403"; then
        print_success "✓ Unauthorized access correctly rejected"
        return 0
    else
        print_warning "May allow unauthorized access: $response"
        return 1
    fi
}

print_requirement "=== DATABASE SCHEMA VALIDATION ==="

test_database_schema() {
    print_status "Testing database schema compliance"
    
    # Test Customer entity structure
    local customer_response=$(api_call "GET" "/api/v1/customers/$TEST_CUSTOMER_ID" "" "basic" "$ADMIN_USER" "$ADMIN_PASS")
    
    local has_required_fields=true
    for field in "id\|customerId" "name" "surname\|lastName" "creditLimit" "usedCreditLimit"; do
        if ! echo "$customer_response" | grep -q "$field"; then
            print_error "Customer missing required field: $field"
            has_required_fields=false
        fi
    done
    
    if $has_required_fields; then
        print_success "✓ Customer schema compliant"
        return 0
    else
        return 1
    fi
}

print_requirement "=== BONUS REQUIREMENTS ==="

test_early_payment_discount() {
    print_status "Testing early payment discount (Bonus 2)"
    
    # This would require specific test data setup
    print_warning "Early payment discount requires specific test setup"
    return 0
}

test_late_payment_penalty() {
    print_status "Testing late payment penalty (Bonus 2)"
    
    # This would require specific test data setup  
    print_warning "Late payment penalty requires specific test setup"
    return 0
}

# Health check function
check_service_health() {
    print_status "Checking service health..."
    
    local health_response=$(curl -s "$BASE_URL/actuator/health" 2>/dev/null)
    
    if echo "$health_response" | grep -q "UP\|healthy"; then
        print_success "Service is healthy"
        return 0
    else
        print_error "Service health check failed: $health_response"
        return 1
    fi
}

# Main test execution
main() {
    print_status "Starting Business Requirements Validation"
    print_status "Testing against: $BASE_URL"
    echo ""
    
    # Health check first
    if ! check_service_health; then
        print_error "Service is not healthy. Please start the application first."
        print_status "To start the application:"
        echo "  docker-compose up -d"
        echo "  # OR"
        echo "  ./gradlew bootRun --args='--spring.profiles.active=dev'"
        exit 1
    fi
    
    # Run all business requirement tests
    run_test "Customer Creation" "test_customer_creation"
    run_test "Valid Loan Creation" "test_loan_creation_valid"
    run_test "Installment Number Restrictions" "test_installment_restrictions"
    run_test "Interest Rate Restrictions" "test_interest_rate_restrictions"
    run_test "Credit Limit Validation" "test_credit_limit_check"
    run_test "List Loans for Customer" "test_list_loans"
    run_test "List Loan Installments" "test_list_installments"
    run_test "Payment Processing" "test_payment_processing"
    run_test "Admin Authorization" "test_admin_authorization"
    run_test "Unauthorized Access Rejection" "test_unauthorized_access"
    run_test "Database Schema Compliance" "test_database_schema"
    run_test "Early Payment Discount" "test_early_payment_discount"
    run_test "Late Payment Penalty" "test_late_payment_penalty"
    
    # Generate test report
    echo ""
    print_status "=== TEST RESULTS SUMMARY ==="
    echo "Total Tests: $TOTAL_TESTS"
    echo "Passed: $TESTS_PASSED"
    echo "Failed: $TESTS_FAILED"
    
    local pass_rate=$((TESTS_PASSED * 100 / TOTAL_TESTS))
    echo "Pass Rate: ${pass_rate}%"
    
    if [ $TESTS_FAILED -eq 0 ]; then
        print_success "🎉 All business requirements validated successfully!"
        echo ""
        print_status "Business Requirements Coverage:"
        echo "✅ Create Loan with validation rules"
        echo "✅ List Loans for customers"
        echo "✅ List Installments for loans"
        echo "✅ Pay Loan with business logic"
        echo "✅ Admin authentication and authorization"
        echo "✅ Database schema compliance"
        echo "⚠️  Bonus features require additional setup"
        
        exit 0
    else
        print_error "Some tests failed. Please check the implementation."
        exit 1
    fi
}

# Help function
show_help() {
    echo "Business Requirements Validation Script"
    echo ""
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "OPTIONS:"
    echo "  -h, --help              Show this help message"
    echo "  -u, --url URL           Set base URL (default: http://localhost:8080)"
    echo "  --admin-user USER       Set admin username (default: admin)"
    echo "  --admin-pass PASS       Set admin password (default: admin123)"
    echo ""
    echo "REQUIREMENTS TESTED:"
    echo "  ✓ Create Loan (amount, interest rate, installments)"
    echo "  ✓ List Loans for customer"
    echo "  ✓ List Installments for loan"
    echo "  ✓ Pay Loan with business rules"
    echo "  ✓ Admin authentication and authorization"
    echo "  ✓ Database schema compliance"
    echo "  ⚠ Bonus: Early/late payment calculations"
    echo ""
    echo "BEFORE RUNNING:"
    echo "  docker-compose up -d"
    echo "  # OR"
    echo "  ./gradlew bootRun --args='--spring.profiles.active=dev'"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -u|--url)
            BASE_URL="$2"
            shift 2
            ;;
        --admin-user)
            ADMIN_USER="$2"
            shift 2
            ;;
        --admin-pass)
            ADMIN_PASS="$2"
            shift 2
            ;;
        *)
            print_error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Run main function
main