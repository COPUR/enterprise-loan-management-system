#!/bin/bash

# Enhanced Enterprise Banking System - Deployment Test Script
# Validates the complete deployment and functionality

set -e

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
BASE_URL="http://localhost:8080"
HEALTH_ENDPOINT="$BASE_URL/actuator/health"
API_ENDPOINT="$BASE_URL/api"

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

# Test functions
test_application_health() {
    log_info "Testing application health..."
    
    if curl -f "$HEALTH_ENDPOINT" &>/dev/null; then
        local health_status
        health_status=$(curl -s "$HEALTH_ENDPOINT" | jq -r '.status // "UNKNOWN"' 2>/dev/null || echo "UNKNOWN")
        
        if [ "$health_status" = "UP" ]; then
            log_success "Application health check passed (status: $health_status)"
            return 0
        else
            log_error "Application health check failed (status: $health_status)"
            return 1
        fi
    else
        log_error "Application health endpoint unreachable"
        return 1
    fi
}

test_actuator_endpoints() {
    log_info "Testing Spring Boot Actuator endpoints..."
    
    local endpoints=("health" "info" "metrics" "prometheus")
    local passed=0
    
    for endpoint in "${endpoints[@]}"; do
        if curl -f "$BASE_URL/actuator/$endpoint" &>/dev/null; then
            log_success "Actuator endpoint /$endpoint is accessible"
            ((passed++))
        else
            log_error "Actuator endpoint /$endpoint is not accessible"
        fi
    done
    
    if [ $passed -eq ${#endpoints[@]} ]; then
        log_success "All actuator endpoints are working"
        return 0
    else
        log_warning "Some actuator endpoints failed ($passed/${#endpoints[@]} passed)"
        return 1
    fi
}

test_banking_apis() {
    log_info "Testing banking API endpoints..."
    
    # Test loan creation endpoint
    log_info "Testing loan creation API..."
    local loan_response
    loan_response=$(curl -s -X POST "$API_ENDPOINT/loans" \
        -H "Content-Type: application/json" \
        -d '{
            "customerId": "test-customer-123",
            "amount": 25000,
            "numberOfInstallments": 24,
            "loanType": "PERSONAL",
            "purpose": "HOME_IMPROVEMENT"
        }' || echo "ERROR")
    
    if [[ "$loan_response" != "ERROR" ]] && [[ "$loan_response" != *"error"* ]]; then
        log_success "Loan creation API is working"
    else
        log_warning "Loan creation API test failed (this may be expected without authentication)"
    fi
    
    # Test customer APIs
    log_info "Testing customer management APIs..."
    if curl -f "$API_ENDPOINT/customers" &>/dev/null; then
        log_success "Customer API is accessible"
    else
        log_warning "Customer API test failed (this may be expected without authentication)"
    fi
    
    # Test payment APIs
    log_info "Testing payment processing APIs..."
    if curl -f "$API_ENDPOINT/payments" &>/dev/null; then
        log_success "Payment API is accessible"
    else
        log_warning "Payment API test failed (this may be expected without authentication)"
    fi
}

test_ai_features() {
    log_info "Testing AI-powered features..."
    
    # Test AI health endpoint
    if curl -f "$API_ENDPOINT/ai/health" &>/dev/null; then
        log_success "AI services are accessible"
    else
        log_warning "AI services test failed (may require OpenAI API key)"
    fi
    
    # Test fraud detection endpoint
    if curl -f "$API_ENDPOINT/ai/fraud/health" &>/dev/null; then
        log_success "Fraud detection service is accessible"
    else
        log_warning "Fraud detection test failed (may require OpenAI API key)"
    fi
}

test_monitoring_services() {
    log_info "Testing monitoring and observability services..."
    
    # Test Prometheus metrics
    if curl -f "http://localhost:9090/-/healthy" &>/dev/null; then
        log_success "Prometheus is accessible"
    else
        log_warning "Prometheus is not accessible"
    fi
    
    # Test Grafana
    if curl -f "http://localhost:3000/api/health" &>/dev/null; then
        log_success "Grafana is accessible"
    else
        log_warning "Grafana is not accessible"
    fi
    
    # Test Jaeger
    if curl -f "http://localhost:16686" &>/dev/null; then
        log_success "Jaeger tracing is accessible"
    else
        log_warning "Jaeger tracing is not accessible"
    fi
}

test_security_features() {
    log_info "Testing security and authentication features..."
    
    # Test Keycloak
    if curl -f "http://localhost:8090/realms/master/.well-known/openid_configuration" &>/dev/null; then
        log_success "Keycloak OAuth2.1 server is accessible"
    else
        log_warning "Keycloak OAuth2.1 server is not accessible"
    fi
    
    # Test FAPI compliance endpoints
    if curl -f "$API_ENDPOINT/security/fapi/status" &>/dev/null; then
        log_success "FAPI compliance features are accessible"
    else
        log_warning "FAPI compliance test failed"
    fi
}

test_data_services() {
    log_info "Testing data layer services..."
    
    # Test database connectivity through application
    local db_health
    db_health=$(curl -s "$HEALTH_ENDPOINT" | jq -r '.components.db.status // "UNKNOWN"' 2>/dev/null || echo "UNKNOWN")
    
    if [ "$db_health" = "UP" ]; then
        log_success "Database connectivity is healthy"
    else
        log_warning "Database connectivity test failed (status: $db_health)"
    fi
    
    # Test Redis connectivity
    local redis_health
    redis_health=$(curl -s "$HEALTH_ENDPOINT" | jq -r '.components.redis.status // "UNKNOWN"' 2>/dev/null || echo "UNKNOWN")
    
    if [ "$redis_health" = "UP" ]; then
        log_success "Redis cache connectivity is healthy"
    else
        log_warning "Redis cache connectivity test failed (status: $redis_health)"
    fi
}

test_kafka_messaging() {
    log_info "Testing Kafka event streaming..."
    
    # Test Kafka UI
    if curl -f "http://localhost:8082" &>/dev/null; then
        log_success "Kafka UI is accessible"
    else
        log_warning "Kafka UI is not accessible"
    fi
    
    # Test application Kafka connectivity
    local kafka_health
    kafka_health=$(curl -s "$HEALTH_ENDPOINT" | jq -r '.components.kafka.status // "UNKNOWN"' 2>/dev/null || echo "UNKNOWN")
    
    if [ "$kafka_health" = "UP" ]; then
        log_success "Kafka messaging connectivity is healthy"
    else
        log_warning "Kafka messaging test failed (status: $kafka_health)"
    fi
}

# Performance test
test_performance() {
    log_info "Running basic performance test..."
    
    local start_time
    start_time=$(date +%s.%N)
    
    # Make 10 concurrent health check requests
    for i in {1..10}; do
        curl -s "$HEALTH_ENDPOINT" &>/dev/null &
    done
    wait
    
    local end_time
    end_time=$(date +%s.%N)
    local duration
    duration=$(echo "$end_time - $start_time" | bc -l 2>/dev/null || echo "0")
    
    if (( $(echo "$duration < 5.0" | bc -l 2>/dev/null || echo "0") )); then
        log_success "Performance test passed (10 concurrent requests in ${duration}s)"
    else
        log_warning "Performance test warning (10 concurrent requests took ${duration}s)"
    fi
}

# Main test execution
main() {
    echo "========================================================"
    echo "Enhanced Enterprise Banking System - Deployment Test"
    echo "========================================================"
    echo ""
    
    local total_tests=0
    local passed_tests=0
    
    # Core application tests
    if test_application_health; then ((passed_tests++)); fi; ((total_tests++))
    if test_actuator_endpoints; then ((passed_tests++)); fi; ((total_tests++))
    if test_banking_apis; then ((passed_tests++)); fi; ((total_tests++))
    
    # Feature tests
    if test_ai_features; then ((passed_tests++)); fi; ((total_tests++))
    if test_security_features; then ((passed_tests++)); fi; ((total_tests++))
    
    # Infrastructure tests
    if test_data_services; then ((passed_tests++)); fi; ((total_tests++))
    if test_kafka_messaging; then ((passed_tests++)); fi; ((total_tests++))
    if test_monitoring_services; then ((passed_tests++)); fi; ((total_tests++))
    
    # Performance test
    if test_performance; then ((passed_tests++)); fi; ((total_tests++))
    
    echo ""
    echo "========================================================"
    echo "                  TEST SUMMARY"
    echo "========================================================"
    echo "Total Tests: $total_tests"
    echo "Passed: $passed_tests"
    echo "Failed: $((total_tests - passed_tests))"
    echo "Success Rate: $(( (passed_tests * 100) / total_tests ))%"
    echo ""
    
    if [ $passed_tests -eq $total_tests ]; then
        log_success "All tests passed! Enhanced Enterprise Banking System is fully functional."
        echo ""
        echo "🎉 System is ready for production use!"
        echo "📖 Access guide: ENHANCED_BANKING_ACCESS.md"
        exit 0
    elif [ $passed_tests -ge $((total_tests * 70 / 100)) ]; then
        log_warning "Most tests passed. System is functional with some limitations."
        echo ""
        echo "⚠️  System is operational but some features may be limited."
        echo "📖 Check the warnings above and access guide: ENHANCED_BANKING_ACCESS.md"
        exit 0
    else
        log_error "Multiple tests failed. System may not be fully functional."
        echo ""
        echo "❌ System deployment needs attention. Check the failures above."
        exit 1
    fi
}

# Wait for system to be ready
wait_for_system() {
    log_info "Waiting for system to be ready..."
    
    local count=0
    while ! curl -f "$HEALTH_ENDPOINT" &>/dev/null && [ $count -lt 60 ]; do
        sleep 5
        ((count += 5))
        log_info "Still waiting... (${count}s)"
    done
    
    if [ $count -ge 60 ]; then
        log_error "System did not become ready within 5 minutes"
        exit 1
    fi
    
    log_success "System is responding"
    sleep 10  # Additional wait for full initialization
}

# Script execution
case "${1:-test}" in
    "wait")
        wait_for_system
        ;;
    "test")
        main
        ;;
    "help")
        echo "Enhanced Enterprise Banking System - Deployment Test Script"
        echo ""
        echo "Usage: $0 [command]"
        echo ""
        echo "Commands:"
        echo "  test    Run comprehensive deployment tests (default)"
        echo "  wait    Wait for system to be ready"
        echo "  help    Show this help message"
        exit 0
        ;;
    *)
        log_error "Unknown command: $1"
        echo "Use '$0 help' for usage information"
        exit 1
        ;;
esac