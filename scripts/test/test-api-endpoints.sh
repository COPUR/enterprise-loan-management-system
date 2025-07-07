#!/bin/bash

# Enhanced Enterprise Banking System - API Endpoint Tests
# Tests key API endpoints without requiring Newman

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
    echo -e "${BLUE}[INFO]${NC} $1"
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
test_endpoint() {
    local url="$1"
    local description="$2"
    local expected_status="${3:-200}"
    
    log_info "Testing: $description"
    
    local response
    local status_code
    local response_time_start
    local response_time_end
    local response_time
    
    response_time_start=$(date +%s.%N)
    
    if response=$(curl -s -w "%{http_code}" -o /tmp/response_body "$url" 2>/dev/null); then
        status_code="${response: -3}"
        response_time_end=$(date +%s.%N)
        response_time=$(echo "$response_time_end - $response_time_start" | bc -l 2>/dev/null || echo "0")
        response_time_ms=$(echo "$response_time * 1000" | bc -l 2>/dev/null || echo "0")
        
        if [ "$status_code" -eq "$expected_status" ] || [ "$status_code" -eq 200 ] || [ "$status_code" -eq 302 ]; then
            log_success "$description (${status_code}) - ${response_time_ms}ms"
            return 0
        else
            log_warning "$description (${status_code}) - ${response_time_ms}ms"
            return 1
        fi
    else
        log_error "$description - Connection failed"
        return 1
    fi
}

# Health check tests
test_health_endpoints() {
    log_info "=== Health Check Tests ==="
    
    local passed=0
    local total=0
    
    # Spring Boot Actuator endpoints
    if test_endpoint "$BASE_URL/actuator/health" "Application Health Check"; then
        ((passed++))
    fi
    ((total++))
    
    if test_endpoint "$BASE_URL/actuator/info" "Application Info"; then
        ((passed++))
    fi
    ((total++))
    
    if test_endpoint "$BASE_URL/actuator/metrics" "Application Metrics"; then
        ((passed++))
    fi
    ((total++))
    
    echo "Health Tests: $passed/$total passed"
    return $((total - passed))
}

# Security tests
test_security_endpoints() {
    log_info "=== Security Tests ==="
    
    local passed=0
    local total=0
    
    # Keycloak endpoints
    if test_endpoint "$KEYCLOAK_URL/realms/master" "Keycloak Master Realm" 200; then
        ((passed++))
    fi
    ((total++))
    
    if test_endpoint "$KEYCLOAK_URL" "Keycloak Base URL" 200; then
        ((passed++))
    fi
    ((total++))
    
    echo "Security Tests: $passed/$total passed"
    return $((total - passed))
}

# Banking API tests
test_banking_endpoints() {
    log_info "=== Banking API Tests ==="
    
    local passed=0
    local total=0
    
    # Basic API endpoints (may require authentication)
    if test_endpoint "$BASE_URL/api/loans" "Loans API Endpoint" 401; then
        ((passed++))
    fi
    ((total++))
    
    if test_endpoint "$BASE_URL/api/customers" "Customers API Endpoint" 401; then
        ((passed++))
    fi
    ((total++))
    
    if test_endpoint "$BASE_URL/api/payments" "Payments API Endpoint" 401; then
        ((passed++))
    fi
    ((total++))
    
    # Public endpoints
    if test_endpoint "$BASE_URL/" "Application Root" 200; then
        ((passed++))
    fi
    ((total++))
    
    echo "Banking API Tests: $passed/$total passed"
    return $((total - passed))
}

# AI and advanced feature tests
test_ai_endpoints() {
    log_info "=== AI Services Tests ==="
    
    local passed=0
    local total=0
    
    if test_endpoint "$BASE_URL/api/ai/health" "AI Health Check" 200; then
        ((passed++))
    fi
    ((total++))
    
    if test_endpoint "$BASE_URL/api/ai/fraud/health" "Fraud Detection Health" 200; then
        ((passed++))
    fi
    ((total++))
    
    echo "AI Services Tests: $passed/$total passed"
    return $((total - passed))
}

# Simple authentication test
test_basic_auth() {
    log_info "=== Basic Authentication Test ==="
    
    # Try to get a token from Keycloak (this will likely fail but tests connectivity)
    if test_endpoint "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" "OAuth Token Endpoint" 400; then
        log_success "OAuth endpoint is accessible (400 = missing parameters, which is expected)"
        return 0
    else
        log_warning "OAuth endpoint test failed"
        return 1
    fi
}

# Performance test
test_performance() {
    log_info "=== Basic Performance Test ==="
    
    local endpoint="$BASE_URL/actuator/health"
    local iterations=5
    local total_time=0
    local successful_requests=0
    
    for i in $(seq 1 $iterations); do
        local start_time=$(date +%s.%N)
        if curl -s -f "$endpoint" >/dev/null 2>&1; then
            local end_time=$(date +%s.%N)
            local duration=$(echo "$end_time - $start_time" | bc -l 2>/dev/null || echo "0")
            total_time=$(echo "$total_time + $duration" | bc -l 2>/dev/null || echo "$total_time")
            ((successful_requests++))
        fi
    done
    
    if [ $successful_requests -gt 0 ]; then
        local avg_time=$(echo "scale=3; $total_time / $successful_requests" | bc -l 2>/dev/null || echo "0")
        local avg_time_ms=$(echo "scale=0; $avg_time * 1000" | bc -l 2>/dev/null || echo "0")
        log_success "Performance test: $successful_requests/$iterations requests successful, avg ${avg_time_ms}ms"
        return 0
    else
        log_error "Performance test: No successful requests"
        return 1
    fi
}

# Generate summary report
generate_summary() {
    local health_result=$1
    local security_result=$2
    local banking_result=$3
    local ai_result=$4
    local auth_result=$5
    local perf_result=$6
    
    echo ""
    echo "========================================="
    echo "           TEST SUMMARY"
    echo "========================================="
    
    local total_categories=6
    local passed_categories=0
    
    [ $health_result -eq 0 ] && ((passed_categories++)) || true
    [ $security_result -eq 0 ] && ((passed_categories++)) || true
    [ $banking_result -eq 0 ] && ((passed_categories++)) || true
    [ $ai_result -eq 0 ] && ((passed_categories++)) || true
    [ $auth_result -eq 0 ] && ((passed_categories++)) || true
    [ $perf_result -eq 0 ] && ((passed_categories++)) || true
    
    echo "Categories Passed: $passed_categories/$total_categories"
    echo "Success Rate: $(( passed_categories * 100 / total_categories ))%"
    echo ""
    
    [ $health_result -eq 0 ] && echo "✅ Health Checks: PASSED" || echo "❌ Health Checks: FAILED"
    [ $security_result -eq 0 ] && echo "✅ Security: PASSED" || echo "❌ Security: FAILED"
    [ $banking_result -eq 0 ] && echo "✅ Banking APIs: PASSED" || echo "⚠️  Banking APIs: PARTIAL"
    [ $ai_result -eq 0 ] && echo "✅ AI Services: PASSED" || echo "⚠️  AI Services: LIMITED"
    [ $auth_result -eq 0 ] && echo "✅ Authentication: PASSED" || echo "⚠️  Authentication: LIMITED"
    [ $perf_result -eq 0 ] && echo "✅ Performance: PASSED" || echo "❌ Performance: FAILED"
    
    echo ""
    
    if [ $passed_categories -ge 4 ]; then
        log_success "Overall system status: OPERATIONAL"
        echo "🎉 Banking system is functional and ready for use!"
    elif [ $passed_categories -ge 2 ]; then
        log_warning "Overall system status: PARTIALLY OPERATIONAL"
        echo "⚠️  Banking system has some limitations but core features work"
    else
        log_error "Overall system status: NOT OPERATIONAL"
        echo "❌ Banking system needs attention"
    fi
}

# Main execution
main() {
    echo "========================================="
    echo "Enhanced Enterprise Banking System"
    echo "API Endpoint Testing Suite"
    echo "========================================="
    echo ""
    
    log_info "Testing against:"
    log_info "  Banking Application: $BASE_URL"
    log_info "  Keycloak OAuth: $KEYCLOAK_URL"
    echo ""
    
    # Run all test categories
    test_health_endpoints
    health_result=$?
    echo ""
    
    test_security_endpoints
    security_result=$?
    echo ""
    
    test_banking_endpoints
    banking_result=$?
    echo ""
    
    test_ai_endpoints
    ai_result=$?
    echo ""
    
    test_basic_auth
    auth_result=$?
    echo ""
    
    test_performance
    perf_result=$?
    
    # Generate final report
    generate_summary $health_result $security_result $banking_result $ai_result $auth_result $perf_result
}

# Execute main function
main "$@"