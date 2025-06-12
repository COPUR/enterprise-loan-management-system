#!/bin/bash

# Interactive Microservices Architecture Demo
# Comprehensive demonstration of all system capabilities

set -e

# Configuration
API_GATEWAY_URL="http://localhost:8080"
CUSTOMER_SERVICE_URL="http://localhost:8081"
LOAN_SERVICE_URL="http://localhost:8082"
PAYMENT_SERVICE_URL="http://localhost:8083"

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# Demo data
DEMO_CUSTOMER_ID="CUST-2024-001"
DEMO_LOAN_ID=""
DEMO_PAYMENT_ID=""

echo -e "${BLUE}🏦 Enterprise Loan Management System${NC}"
echo -e "${BLUE}=====================================${NC}"
echo -e "${CYAN}Interactive Microservices Architecture Demo${NC}"
echo ""

# Utility functions
print_header() {
    echo ""
    echo -e "${PURPLE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${PURPLE} $1${NC}"
    echo -e "${PURPLE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

print_step() {
    echo -e "${BLUE}▶ $1${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_info() {
    echo -e "${CYAN}ℹ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

wait_for_input() {
    echo ""
    echo -e "${YELLOW}Press Enter to continue...${NC}"
    read -r
}

# System status check
check_system_status() {
    print_header "SYSTEM STATUS VERIFICATION"
    
    print_step "Checking microservices health..."
    
    local services=(
        "API Gateway:$API_GATEWAY_URL/actuator/health"
        "Customer Service:$CUSTOMER_SERVICE_URL/actuator/health"
        "Loan Service:$LOAN_SERVICE_URL/actuator/health"
        "Payment Service:$PAYMENT_SERVICE_URL/actuator/health"
    )
    
    for service_info in "${services[@]}"; do
        IFS=':' read -ra ADDR <<< "$service_info"
        local service_name="${ADDR[0]}"
        local service_url="${ADDR[1]}"
        
        if curl -f -s "$service_url" > /dev/null 2>&1; then
            print_success "$service_name is healthy"
        else
            print_warning "$service_name health check failed (service may not be running)"
        fi
    done
    
    print_step "Checking database connectivity..."
    if pg_isready -h localhost -p 5432 > /dev/null 2>&1; then
        print_success "PostgreSQL database is accessible"
    else
        print_warning "PostgreSQL database connection failed"
    fi
    
    print_step "Checking Redis cache..."
    if redis-cli ping > /dev/null 2>&1; then
        print_success "Redis ElastiCache is accessible"
    else
        print_warning "Redis cache connection failed"
    fi
    
    wait_for_input
}

# Demo 1: Customer Management
demo_customer_management() {
    print_header "DEMO 1: CUSTOMER MANAGEMENT MICROSERVICE"
    
    print_info "Demonstrating customer lifecycle management with credit operations"
    
    print_step "Creating new customer..."
    
    local customer_data='{
        "customerId": "'$DEMO_CUSTOMER_ID'",
        "firstName": "John",
        "lastName": "Doe",
        "email": "john.doe@email.com",
        "phone": "+1234567890",
        "address": {
            "street": "123 Main St",
            "city": "New York",
            "state": "NY",
            "zipCode": "10001"
        },
        "creditLimit": 50000.00,
        "annualIncome": 75000.00,
        "employmentStatus": "EMPLOYED",
        "creditScore": 750
    }'
    
    echo -e "${CYAN}Customer Data:${NC}"
    echo "$customer_data" | jq '.'
    
    local response=$(curl -s -X POST "$CUSTOMER_SERVICE_URL/api/v1/customers" \
        -H "Content-Type: application/json" \
        -d "$customer_data" \
        -w "%{http_code}")
    
    local http_code="${response: -3}"
    local response_body="${response%???}"
    
    if [[ "$http_code" == "200" || "$http_code" == "201" ]]; then
        print_success "Customer created successfully"
        echo -e "${CYAN}Response:${NC}"
        echo "$response_body" | jq '.'
    else
        print_warning "Customer creation response: $http_code"
        echo "$response_body"
    fi
    
    print_step "Retrieving customer details..."
    local customer_response=$(curl -s "$CUSTOMER_SERVICE_URL/api/v1/customers/$DEMO_CUSTOMER_ID")
    
    if echo "$customer_response" | jq -e . > /dev/null 2>&1; then
        print_success "Customer retrieved successfully"
        echo -e "${CYAN}Customer Details:${NC}"
        echo "$customer_response" | jq '.'
    else
        print_warning "Customer retrieval failed or returned non-JSON response"
    fi
    
    print_step "Testing credit reservation..."
    local credit_reservation='{
        "amount": 10000.00,
        "purpose": "LOAN_APPLICATION",
        "reservationTimeout": 300
    }'
    
    local reservation_response=$(curl -s -X POST \
        "$CUSTOMER_SERVICE_URL/api/v1/customers/$DEMO_CUSTOMER_ID/credit/reserve" \
        -H "Content-Type: application/json" \
        -d "$credit_reservation")
    
    if echo "$reservation_response" | jq -e . > /dev/null 2>&1; then
        print_success "Credit reservation completed"
        echo -e "${CYAN}Reservation Details:${NC}"
        echo "$reservation_response" | jq '.'
    else
        print_warning "Credit reservation failed"
    fi
    
    wait_for_input
}

# Demo 2: Loan Origination
demo_loan_origination() {
    print_header "DEMO 2: LOAN ORIGINATION MICROSERVICE"
    
    print_info "Demonstrating loan creation with business rule validation"
    
    print_step "Creating loan application..."
    
    local loan_data='{
        "customerId": "'$DEMO_CUSTOMER_ID'",
        "loanAmount": 25000.00,
        "interestRate": 0.15,
        "installmentCount": 12,
        "loanType": "PERSONAL",
        "purpose": "HOME_IMPROVEMENT",
        "applicationDate": "'$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)'"
    }'
    
    echo -e "${CYAN}Loan Application:${NC}"
    echo "$loan_data" | jq '.'
    
    local loan_response=$(curl -s -X POST "$LOAN_SERVICE_URL/api/v1/loans" \
        -H "Content-Type: application/json" \
        -d "$loan_data" \
        -w "%{http_code}")
    
    local http_code="${loan_response: -3}"
    local response_body="${loan_response%???}"
    
    if [[ "$http_code" == "200" || "$http_code" == "201" ]]; then
        print_success "Loan created successfully"
        echo -e "${CYAN}Response:${NC}"
        echo "$response_body" | jq '.'
        
        # Extract loan ID for later use
        DEMO_LOAN_ID=$(echo "$response_body" | jq -r '.loanId // .id // empty')
    else
        print_warning "Loan creation response: $http_code"
        echo "$response_body"
    fi
    
    if [[ -n "$DEMO_LOAN_ID" ]]; then
        print_step "Retrieving loan details..."
        local loan_details=$(curl -s "$LOAN_SERVICE_URL/api/v1/loans/$DEMO_LOAN_ID")
        
        if echo "$loan_details" | jq -e . > /dev/null 2>&1; then
            print_success "Loan details retrieved"
            echo -e "${CYAN}Loan Details:${NC}"
            echo "$loan_details" | jq '.'
        fi
        
        print_step "Retrieving installment schedule..."
        local installments=$(curl -s "$LOAN_SERVICE_URL/api/v1/loans/$DEMO_LOAN_ID/installments")
        
        if echo "$installments" | jq -e . > /dev/null 2>&1; then
            print_success "Installment schedule generated"
            echo -e "${CYAN}Installment Schedule:${NC}"
            echo "$installments" | jq '.'
        fi
    fi
    
    print_step "Testing business rule validation...")
    local invalid_loan='{
        "customerId": "'$DEMO_CUSTOMER_ID'",
        "loanAmount": 25000.00,
        "interestRate": 0.75,
        "installmentCount": 15,
        "loanType": "PERSONAL"
    }'
    
    print_info "Testing invalid interest rate (0.75% > 0.5% max) and installment count (15, not in [6,9,12,24])"
    
    local validation_response=$(curl -s -X POST "$LOAN_SERVICE_URL/api/v1/loans" \
        -H "Content-Type: application/json" \
        -d "$invalid_loan" \
        -w "%{http_code}")
    
    local validation_code="${validation_response: -3}"
    local validation_body="${validation_response%???}"
    
    if [[ "$validation_code" == "400" ]]; then
        print_success "Business rule validation working correctly"
        echo -e "${CYAN}Validation Response:${NC}"
        echo "$validation_body" | jq '.'
    else
        print_warning "Business rule validation response: $validation_code"
    fi
    
    wait_for_input
}

# Demo 3: Payment Processing
demo_payment_processing() {
    print_header "DEMO 3: PAYMENT PROCESSING MICROSERVICE"
    
    print_info "Demonstrating payment processing with discount/penalty calculations"
    
    if [[ -z "$DEMO_LOAN_ID" ]]; then
        print_warning "No loan ID available, using sample loan ID"
        DEMO_LOAN_ID="LOAN-SAMPLE-001"
    fi
    
    print_step "Processing payment..."
    
    local payment_data='{
        "loanId": "'$DEMO_LOAN_ID'",
        "paymentAmount": 2000.00,
        "paymentDate": "'$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)'",
        "paymentMethod": "BANK_TRANSFER",
        "installmentNumbers": [1, 2],
        "isEarlyPayment": true
    }'
    
    echo -e "${CYAN}Payment Request:${NC}"
    echo "$payment_data" | jq '.'
    
    local payment_response=$(curl -s -X POST "$PAYMENT_SERVICE_URL/api/v1/payments" \
        -H "Content-Type: application/json" \
        -d "$payment_data" \
        -w "%{http_code}")
    
    local http_code="${payment_response: -3}"
    local response_body="${payment_response%???}"
    
    if [[ "$http_code" == "200" || "$http_code" == "201" ]]; then
        print_success "Payment processed successfully"
        echo -e "${CYAN}Response:${NC}"
        echo "$response_body" | jq '.'
        
        DEMO_PAYMENT_ID=$(echo "$response_body" | jq -r '.paymentId // .id // empty')
    else
        print_warning "Payment processing response: $http_code"
        echo "$response_body"
    fi
    
    print_step "Testing payment calculation logic...")
    
    # Test early payment discount
    local early_payment='{
        "loanId": "'$DEMO_LOAN_ID'",
        "paymentAmount": 1500.00,
        "paymentDate": "'$(date -u -d '+5 days' +%Y-%m-%dT%H:%M:%S.%3NZ)'",
        "installmentNumbers": [3],
        "isEarlyPayment": true,
        "daysBefore": 10
    }'
    
    print_info "Testing early payment discount (10 days before due date)"
    echo -e "${CYAN}Early Payment Request:${NC}"
    echo "$early_payment" | jq '.'
    
    local early_response=$(curl -s -X POST "$PAYMENT_SERVICE_URL/api/v1/payments/calculate" \
        -H "Content-Type: application/json" \
        -d "$early_payment")
    
    if echo "$early_response" | jq -e . > /dev/null 2>&1; then
        print_success "Early payment calculation completed"
        echo -e "${CYAN}Calculation Result:${NC}"
        echo "$early_response" | jq '.'
    fi
    
    # Test late payment penalty
    local late_payment='{
        "loanId": "'$DEMO_LOAN_ID'",
        "paymentAmount": 1500.00,
        "paymentDate": "'$(date -u -d '+5 days' +%Y-%m-%dT%H:%M:%S.%3NZ)'",
        "installmentNumbers": [4],
        "isLatePayment": true,
        "daysLate": 15
    }'
    
    print_info "Testing late payment penalty (15 days after due date)"
    echo -e "${CYAN}Late Payment Request:${NC}"
    echo "$late_payment" | jq '.'
    
    local late_response=$(curl -s -X POST "$PAYMENT_SERVICE_URL/api/v1/payments/calculate" \
        -H "Content-Type: application/json" \
        -d "$late_payment")
    
    if echo "$late_response" | jq -e . > /dev/null 2>&1; then
        print_success "Late payment calculation completed"
        echo -e "${CYAN}Calculation Result:${NC}"
        echo "$late_response" | jq '.'
    fi
    
    wait_for_input
}

# Demo 4: SAGA Pattern
demo_saga_orchestration() {
    print_header "DEMO 4: SAGA ORCHESTRATION PATTERN"
    
    print_info "Demonstrating distributed transaction management"
    
    print_step "Initiating SAGA workflow for loan creation..."
    
    local saga_request='{
        "customerId": "'$DEMO_CUSTOMER_ID'",
        "loanAmount": 15000.00,
        "interestRate": 0.12,
        "installmentCount": 9,
        "loanType": "PERSONAL",
        "sagaTimeout": 300
    }'
    
    echo -e "${CYAN}SAGA Request:${NC}"
    echo "$saga_request" | jq '.'
    
    local saga_response=$(curl -s -X POST "$API_GATEWAY_URL/api/v1/saga/loan-creation" \
        -H "Content-Type: application/json" \
        -d "$saga_request" \
        -w "%{http_code}")
    
    local http_code="${saga_response: -3}"
    local response_body="${saga_response%???}"
    
    if [[ "$http_code" == "200" || "$http_code" == "202" ]]; then
        print_success "SAGA initiated successfully"
        echo -e "${CYAN}Response:${NC}"
        echo "$response_body" | jq '.'
        
        local saga_id=$(echo "$response_body" | jq -r '.sagaId // .id // empty')
        
        if [[ -n "$saga_id" ]]; then
            print_step "Monitoring SAGA progress..."
            
            for i in {1..10}; do
                sleep 2
                local saga_status=$(curl -s "$API_GATEWAY_URL/api/v1/saga/states/$saga_id")
                
                if echo "$saga_status" | jq -e . > /dev/null 2>&1; then
                    local status=$(echo "$saga_status" | jq -r '.status // "UNKNOWN"')
                    local current_step=$(echo "$saga_status" | jq -r '.currentStep // "N/A"')
                    
                    print_info "SAGA Status: $status, Current Step: $current_step"
                    
                    if [[ "$status" == "COMPLETED" || "$status" == "FAILED" || "$status" == "COMPENSATED" ]]; then
                        break
                    fi
                else
                    print_warning "Could not retrieve SAGA status"
                    break
                fi
            done
            
            print_step "Final SAGA state..."
            local final_status=$(curl -s "$API_GATEWAY_URL/api/v1/saga/states/$saga_id")
            if echo "$final_status" | jq -e . > /dev/null 2>&1; then
                echo -e "${CYAN}Final SAGA State:${NC}"
                echo "$final_status" | jq '.'
            fi
        fi
    else
        print_warning "SAGA initiation response: $http_code"
        echo "$response_body"
    fi
    
    print_step "Testing SAGA compensation workflow...")
    
    local failing_saga='{
        "customerId": "INVALID-CUSTOMER",
        "loanAmount": 999999.00,
        "interestRate": 0.99,
        "installmentCount": 99,
        "loanType": "INVALID"
    }'
    
    print_info "Testing SAGA with invalid data to trigger compensation"
    
    local compensation_response=$(curl -s -X POST "$API_GATEWAY_URL/api/v1/saga/loan-creation" \
        -H "Content-Type: application/json" \
        -d "$failing_saga" \
        -w "%{http_code}")
    
    local comp_code="${compensation_response: -3}"
    local comp_body="${compensation_response%???}"
    
    print_info "Compensation test response: $comp_code"
    if echo "$comp_body" | jq -e . > /dev/null 2>&1; then
        echo "$comp_body" | jq '.'
    fi
    
    wait_for_input
}

# Demo 5: Circuit Breaker
demo_circuit_breaker() {
    print_header "DEMO 5: CIRCUIT BREAKER PATTERNS"
    
    print_info "Demonstrating resilience patterns and fault tolerance"
    
    print_step "Checking circuit breaker status..."
    
    local cb_status=$(curl -s "$API_GATEWAY_URL/actuator/circuitbreakers")
    if echo "$cb_status" | jq -e . > /dev/null 2>&1; then
        print_success "Circuit breaker status retrieved"
        echo -e "${CYAN}Circuit Breaker States:${NC}"
        echo "$cb_status" | jq '.'
    else
        print_warning "Circuit breaker status not available"
    fi
    
    print_step "Testing circuit breaker under load..."
    
    print_info "Generating rapid requests to trigger circuit breaker..."
    
    local success_count=0
    local failure_count=0
    local circuit_breaker_count=0
    
    for i in {1..50}; do
        local response_code=$(curl -s -w "%{http_code}" -o /dev/null \
            "$API_GATEWAY_URL/api/v1/customers/LOAD-TEST-$i" 2>/dev/null)
        
        case "$response_code" in
            200) ((success_count++)) ;;
            404) ((success_count++)) ;;  # Expected for non-existent customers
            503) ((circuit_breaker_count++)) ;;
            *) ((failure_count++)) ;;
        esac
        
        # Add small delay
        sleep 0.1
    done
    
    print_success "Load test completed"
    echo -e "${CYAN}Results:${NC}"
    echo "  - Successful requests: $success_count"
    echo "  - Failed requests: $failure_count"
    echo "  - Circuit breaker responses: $circuit_breaker_count"
    
    if [[ $circuit_breaker_count -gt 0 ]]; then
        print_success "Circuit breaker activated correctly"
    else
        print_info "Circuit breaker threshold not reached"
    fi
    
    print_step "Checking updated circuit breaker status..."
    local updated_cb_status=$(curl -s "$API_GATEWAY_URL/actuator/circuitbreakers")
    if echo "$updated_cb_status" | jq -e . > /dev/null 2>&1; then
        echo -e "${CYAN}Updated Circuit Breaker States:${NC}"
        echo "$updated_cb_status" | jq '.'
    fi
    
    wait_for_input
}

# Demo 6: Security Features
demo_security_features() {
    print_header "DEMO 6: OWASP SECURITY COMPLIANCE"
    
    print_info "Demonstrating security features and protections"
    
    print_step "Testing security headers..."
    
    local headers_response=$(curl -s -I "$API_GATEWAY_URL/api/v1/customers")
    echo -e "${CYAN}Security Headers:${NC}"
    echo "$headers_response" | grep -E "(X-|Content-Security|Strict-Transport)"
    
    print_step "Testing SQL injection protection..."
    
    local sql_injection_attempt=$(curl -s -w "%{http_code}" -o /dev/null \
        "$API_GATEWAY_URL/api/v1/customers?id=1' OR '1'='1")
    
    if [[ "$sql_injection_attempt" == "400" || "$sql_injection_attempt" == "403" ]]; then
        print_success "SQL injection protection active (Response: $sql_injection_attempt)"
    else
        print_warning "SQL injection test response: $sql_injection_attempt"
    fi
    
    print_step "Testing XSS protection..."
    
    local xss_attempt=$(curl -s -w "%{http_code}" -o /dev/null \
        "$API_GATEWAY_URL/api/v1/customers" \
        -H "X-Test-Header: <script>alert('xss')</script>")
    
    print_info "XSS protection test response: $xss_attempt"
    
    print_step "Testing rate limiting..."
    
    print_info "Sending rapid requests to test rate limiting..."
    
    local rate_limit_count=0
    for i in {1..20}; do
        local rate_response=$(curl -s -w "%{http_code}" -o /dev/null "$API_GATEWAY_URL/actuator/health")
        if [[ "$rate_response" == "429" ]]; then
            ((rate_limit_count++))
        fi
        sleep 0.1
    done
    
    if [[ $rate_limit_count -gt 0 ]]; then
        print_success "Rate limiting activated ($rate_limit_count responses)"
    else
        print_info "Rate limiting threshold not reached"
    fi
    
    wait_for_input
}

# Demo 7: Performance Monitoring
demo_performance_monitoring() {
    print_header "DEMO 7: PERFORMANCE MONITORING"
    
    print_info "Demonstrating metrics collection and monitoring"
    
    print_step "Retrieving Prometheus metrics..."
    
    local metrics=$(curl -s "$API_GATEWAY_URL/actuator/prometheus")
    if [[ -n "$metrics" ]]; then
        print_success "Metrics endpoint accessible"
        
        echo -e "${CYAN}Sample Metrics:${NC}"
        echo "$metrics" | grep -E "(http_requests|circuit_breaker|jvm_memory)" | head -10
        
        local request_count=$(echo "$metrics" | grep "http_requests_total" | wc -l)
        print_info "Total HTTP request metrics: $request_count"
    else
        print_warning "Metrics endpoint not available"
    fi
    
    print_step "Testing response times..."
    
    local endpoints=(
        "Health Check:$API_GATEWAY_URL/actuator/health"
        "Customer Service:$CUSTOMER_SERVICE_URL/actuator/health"
        "Loan Service:$LOAN_SERVICE_URL/actuator/health"
        "Payment Service:$PAYMENT_SERVICE_URL/actuator/health"
    )
    
    for endpoint_info in "${endpoints[@]}"; do
        IFS=':' read -ra ADDR <<< "$endpoint_info"
        local endpoint_name="${ADDR[0]}"
        local endpoint_url="${ADDR[1]}"
        
        local start_time=$(date +%s%N)
        curl -s "$endpoint_url" > /dev/null 2>&1
        local end_time=$(date +%s%N)
        local response_time=$(( (end_time - start_time) / 1000000 ))
        
        if [[ $response_time -lt 50 ]]; then
            print_success "$endpoint_name: ${response_time}ms (excellent)"
        elif [[ $response_time -lt 100 ]]; then
            print_info "$endpoint_name: ${response_time}ms (good)"
        else
            print_warning "$endpoint_name: ${response_time}ms (needs optimization)"
        fi
    done
    
    wait_for_input
}

# Main menu
show_main_menu() {
    print_header "INTERACTIVE DEMO MENU"
    
    echo "Select a demonstration:"
    echo ""
    echo "1. System Status Check"
    echo "2. Customer Management Microservice"
    echo "3. Loan Origination Microservice"
    echo "4. Payment Processing Microservice"
    echo "5. SAGA Orchestration Pattern"
    echo "6. Circuit Breaker Patterns"
    echo "7. Security Features (OWASP Compliance)"
    echo "8. Performance Monitoring"
    echo "9. Run All Demos"
    echo "0. Exit"
    echo ""
    echo -n "Enter your choice (0-9): "
}

# Main execution
main() {
    while true; do
        clear
        show_main_menu
        read -r choice
        
        case $choice in
            1) check_system_status ;;
            2) demo_customer_management ;;
            3) demo_loan_origination ;;
            4) demo_payment_processing ;;
            5) demo_saga_orchestration ;;
            6) demo_circuit_breaker ;;
            7) demo_security_features ;;
            8) demo_performance_monitoring ;;
            9) 
                check_system_status
                demo_customer_management
                demo_loan_origination
                demo_payment_processing
                demo_saga_orchestration
                demo_circuit_breaker
                demo_security_features
                demo_performance_monitoring
                print_header "ALL DEMOS COMPLETED"
                print_success "Enterprise Loan Management System demonstration finished"
                wait_for_input
                ;;
            *)
                print_error "Invalid choice. Please select 0-9."
                sleep 2
                ;;
            0)
                print_header "DEMO SESSION COMPLETED"
                print_success "Thank you for exploring the Enterprise Loan Management System"
                echo ""
                exit 0
                ;;
            *)
                print_error "Invalid choice. Please select 0-9."
                sleep 2
                ;;
        esac
    done
}

# Check dependencies
check_dependencies() {
    local missing_deps=()
    
    if ! command -v curl &> /dev/null; then
        missing_deps+=("curl")
    fi
    
    if ! command -v jq &> /dev/null; then
        missing_deps+=("jq")
    fi
    
    if ! command -v pg_isready &> /dev/null; then
        missing_deps+=("postgresql-client")
    fi
    
    if ! command -v redis-cli &> /dev/null; then
        missing_deps+=("redis-tools")
    fi
    
    if [[ ${#missing_deps[@]} -gt 0 ]]; then
        print_error "Missing dependencies: ${missing_deps[*]}"
        echo "Please install the missing tools and try again."
        exit 1
    fi
}

# Initialize
check_dependencies
main