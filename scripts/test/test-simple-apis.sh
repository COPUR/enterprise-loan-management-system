#!/bin/bash

# Test simple APIs using the existing JAR if available
# This script tests the working simple loan controller endpoints

set -e

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
BASE_URL="http://localhost:8080"
KEYCLOAK_URL="http://localhost:8090"

# Logging functions
log_info() {
    echo -e "${BLUE}[TEST]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[PASS]${NC} $1"
}

log_error() {
    echo -e "${RED}[FAIL]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

# Start a simple Spring Boot app from JAR if available
start_simple_app() {
    log_info "Checking for existing application JAR..."
    
    if [ -f "build/libs/enterprise-loan-management-system.jar" ]; then
        log_info "Found JAR file, attempting to start..."
        
        # Kill any existing Java processes
        pkill -f "enterprise-loan-management-system.jar" 2>/dev/null || true
        
        # Start the application with simple profile
        nohup java -jar build/libs/enterprise-loan-management-system.jar \
            --spring.profiles.active=simple \
            --server.port=8080 \
            --logging.level.com.bank=DEBUG \
            --management.endpoints.web.exposure.include=health,info,metrics > app.log 2>&1 &
        
        local app_pid=$!
        log_info "Started application with PID: $app_pid"
        
        # Wait for startup
        log_info "Waiting for application startup..."
        sleep 15
        
        # Check if it's responding
        if curl -f "$BASE_URL/actuator/health" &>/dev/null; then
            log_success "Application started successfully!"
            return 0
        else
            log_warning "Application may not be fully started yet"
            return 1
        fi
    else
        log_error "No JAR file found. Build the application first."
        return 1
    fi
}

# Test Keycloak endpoints
test_keycloak() {
    log_info "Testing Keycloak endpoints..."
    
    local passed=0
    local total=2
    
    # Test Keycloak main page
    if curl -s -I "$KEYCLOAK_URL" | grep -q "HTTP.*200\|HTTP.*302"; then
        log_success "Keycloak main page accessible"
        ((passed++))
    else
        log_error "Keycloak main page not accessible"
    fi
    
    # Test Keycloak admin console
    if curl -s -I "$KEYCLOAK_URL/admin/" | grep -q "HTTP.*200\|HTTP.*302"; then
        log_success "Keycloak admin console accessible"
        ((passed++))
    else
        log_error "Keycloak admin console not accessible"
    fi
    
    echo "Keycloak Tests: $passed/$total passed"
    return $((total - passed))
}

# Test simple loan API endpoints using curl
test_simple_loan_api() {
    log_info "Testing Simple Loan API endpoints..."
    
    local passed=0
    local total=5
    
    # 1. Test create loan endpoint
    log_info "Testing loan creation..."
    local create_response=$(curl -s -X POST "$BASE_URL/api/v1/loans" \
        -H "Content-Type: application/json" \
        -d '{
            "customerId": "test-customer-123",
            "amount": 10000,
            "interestRate": 5.5,
            "numberOfInstallments": 12
        }')
    
    if echo "$create_response" | jq -e '.loanId' >/dev/null 2>&1; then
        local loan_id=$(echo "$create_response" | jq -r '.loanId')
        log_success "Loan creation successful (ID: $loan_id)"
        ((passed++))
        
        # Store loan ID for subsequent tests
        echo "$loan_id" > /tmp/test_loan_id
    else
        log_error "Loan creation failed"
        echo "Response: $create_response"
    fi
    
    # 2. Test get loans by customer
    log_info "Testing get loans by customer..."
    local customer_loans=$(curl -s "$BASE_URL/api/v1/loans?customerId=test-customer-123")
    
    if echo "$customer_loans" | jq -e '.[0].loanId' >/dev/null 2>&1; then
        log_success "Get customer loans successful"
        ((passed++))
    else
        log_error "Get customer loans failed"
        echo "Response: $customer_loans"
    fi
    
    # 3. Test get loan installments (if loan was created)
    if [ -f /tmp/test_loan_id ]; then
        local loan_id=$(cat /tmp/test_loan_id)
        log_info "Testing get loan installments for loan: $loan_id"
        
        local installments=$(curl -s "$BASE_URL/api/v1/loans/$loan_id/installments")
        
        if echo "$installments" | jq -e '.[0].installmentNumber' >/dev/null 2>&1; then
            log_success "Get loan installments successful"
            ((passed++))
        else
            log_error "Get loan installments failed"
            echo "Response: $installments"
        fi
        
        # 4. Test pay installment
        log_info "Testing pay loan installment..."
        local payment_response=$(curl -s -X POST "$BASE_URL/api/v1/loans/$loan_id/pay" \
            -H "Content-Type: application/json" \
            -d '{
                "amount": 916.67
            }')
        
        if echo "$payment_response" | jq -e '.success' >/dev/null 2>&1; then
            log_success "Pay loan installment successful"
            ((passed++))
        else
            log_error "Pay loan installment failed"
            echo "Response: $payment_response"
        fi
        
        # 5. Test get updated installments
        log_info "Testing get updated installments after payment..."
        local updated_installments=$(curl -s "$BASE_URL/api/v1/loans/$loan_id/installments")
        
        if echo "$updated_installments" | jq -e '.[0].paidDate' >/dev/null 2>&1; then
            log_success "Get updated installments successful - payment recorded"
            ((passed++))
        else
            log_warning "Get updated installments - payment may not be recorded yet"
            echo "Response: $updated_installments"
        fi
    else
        log_warning "Skipping installment tests - no loan created"
        total=2  # Adjust total since we couldn't test installment endpoints
    fi
    
    echo "Simple Loan API Tests: $passed/$total passed"
    return $((total - passed))
}

# Test system health endpoints
test_system_health() {
    log_info "Testing system health endpoints..."
    
    local passed=0
    local total=3
    
    # Health check
    if curl -s -f "$BASE_URL/actuator/health" >/dev/null; then
        local health_status=$(curl -s "$BASE_URL/actuator/health" | jq -r '.status // "UNKNOWN"')
        if [ "$health_status" = "UP" ]; then
            log_success "Health check: $health_status"
            ((passed++))
        else
            log_warning "Health check: $health_status"
        fi
    else
        log_error "Health check endpoint not accessible"
    fi
    
    # Info endpoint
    if curl -s -f "$BASE_URL/actuator/info" >/dev/null; then
        log_success "Info endpoint accessible"
        ((passed++))
    else
        log_error "Info endpoint not accessible"
    fi
    
    # Metrics endpoint
    if curl -s -f "$BASE_URL/actuator/metrics" >/dev/null; then
        log_success "Metrics endpoint accessible"
        ((passed++))
    else
        log_error "Metrics endpoint not accessible"
    fi
    
    echo "System Health Tests: $passed/$total passed"
    return $((total - passed))
}

# Generate comprehensive test report
generate_final_report() {
    local keycloak_result=$1
    local health_result=$2
    local api_result=$3
    
    echo ""
    echo "========================================="
    echo "    SIMPLE API TEST RESULTS"
    echo "========================================="
    
    local total_categories=3
    local passed_categories=0
    
    [ $keycloak_result -eq 0 ] && ((passed_categories++))
    [ $health_result -eq 0 ] && ((passed_categories++))
    [ $api_result -eq 0 ] && ((passed_categories++))
    
    echo "Test Categories: $passed_categories/$total_categories passed"
    echo "Overall Success Rate: $(( passed_categories * 100 / total_categories ))%"
    echo ""
    
    # Detailed results
    [ $keycloak_result -eq 0 ] && echo "✅ Keycloak OAuth: OPERATIONAL" || echo "❌ Keycloak OAuth: FAILED"
    [ $health_result -eq 0 ] && echo "✅ System Health: HEALTHY" || echo "❌ System Health: UNHEALTHY"
    [ $api_result -eq 0 ] && echo "✅ Banking APIs: FUNCTIONAL" || echo "❌ Banking APIs: NON-FUNCTIONAL"
    
    echo ""
    echo "Business Requirements Validation:"
    
    if [ $api_result -eq 0 ]; then
        echo "✅ Requirement 1: Create loan - IMPLEMENTED"
        echo "✅ Requirement 2: List loans by customer - IMPLEMENTED" 
        echo "✅ Requirement 3: List installments by loan - IMPLEMENTED"
        echo "✅ Requirement 4: Pay loan installment - IMPLEMENTED"
        echo ""
        log_success "All Orange Solution business requirements are satisfied!"
    else
        echo "❌ Some business requirements may not be working properly"
    fi
    
    echo ""
    if [ $passed_categories -eq 3 ]; then
        log_success "🎉 System is fully operational and ready for Postman testing!"
        echo "You can now run the full Postman collection against the working APIs."
    elif [ $passed_categories -ge 2 ]; then
        log_warning "⚠️  System is partially operational"
        echo "Some features work but there may be issues with infrastructure components."
    else
        log_error "❌ System needs significant troubleshooting"
    fi
}

# Main execution
main() {
    echo "========================================="
    echo "Enhanced Enterprise Banking System"
    echo "Simple API Testing Suite"
    echo "========================================="
    echo ""
    
    # Clean up any existing temp files
    rm -f /tmp/test_loan_id
    
    # Start the application if possible
    if ! curl -f "$BASE_URL/actuator/health" &>/dev/null; then
        log_info "Application not running, attempting to start..."
        if ! start_simple_app; then
            log_warning "Could not start application automatically"
            log_info "You may need to start it manually with:"
            log_info "  java -jar build/libs/enterprise-loan-management-system.jar --spring.profiles.active=simple"
        fi
    else
        log_success "Application is already running"
    fi
    
    echo ""
    
    # Run tests
    test_keycloak
    keycloak_result=$?
    echo ""
    
    test_system_health  
    health_result=$?
    echo ""
    
    test_simple_loan_api
    api_result=$?
    
    # Generate final report
    generate_final_report $keycloak_result $health_result $api_result
    
    # Clean up
    rm -f /tmp/test_loan_id
}

# Execute main function
main "$@"