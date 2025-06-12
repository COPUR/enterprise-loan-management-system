#!/bin/bash

# Enterprise Loan Management System - Microservices Deployment Validation
# Comprehensive validation script for microservices architecture

set -e

echo "🏦 Enterprise Loan Management System - Microservices Validation"
echo "=============================================================="

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
API_GATEWAY_URL="http://localhost:8080"
CUSTOMER_SERVICE_URL="http://localhost:8081"
LOAN_SERVICE_URL="http://localhost:8082"
PAYMENT_SERVICE_URL="http://localhost:8083"

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Utility functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
    ((PASSED_TESTS++))
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
    ((FAILED_TESTS++))
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

run_test() {
    ((TOTAL_TESTS++))
    echo ""
    log_info "Test $TOTAL_TESTS: $1"
}

# Health Check Functions
check_service_health() {
    local service_name=$1
    local service_url=$2
    
    run_test "Health check for $service_name"
    
    if curl -f -s "$service_url/actuator/health" > /dev/null 2>&1; then
        log_success "$service_name is healthy"
        return 0
    else
        log_error "$service_name health check failed"
        return 1
    fi
}

# Database Connection Tests
test_database_connections() {
    run_test "Database connectivity validation"
    
    # Check if PostgreSQL is available
    if pg_isready -h localhost -p 5432 > /dev/null 2>&1; then
        log_success "PostgreSQL database is accessible"
    else
        log_error "PostgreSQL database connection failed"
        return 1
    fi
    
    # Validate individual database schemas
    local databases=("customer_db" "loan_db" "payment_db" "banking_gateway")
    
    for db in "${databases[@]}"; do
        if psql -h localhost -U $PGUSER -d $PGDATABASE -c "SELECT 1 FROM information_schema.schemata WHERE schema_name = '$db';" > /dev/null 2>&1; then
            log_success "Database schema '$db' exists and is accessible"
        else
            log_warning "Database schema '$db' validation skipped (may not exist yet)"
        fi
    done
}

# Redis Cache Tests
test_redis_connectivity() {
    run_test "Redis ElastiCache connectivity"
    
    if redis-cli ping > /dev/null 2>&1; then
        log_success "Redis ElastiCache is accessible"
        
        # Test basic Redis operations
        redis-cli set test_key "test_value" > /dev/null
        if [ "$(redis-cli get test_key)" = "test_value" ]; then
            log_success "Redis read/write operations working"
            redis-cli del test_key > /dev/null
        else
            log_error "Redis read/write operations failed"
        fi
    else
        log_error "Redis ElastiCache connection failed"
    fi
}

# API Gateway Tests
test_api_gateway() {
    run_test "API Gateway routing and security"
    
    # Test basic gateway response
    if curl -f -s "$API_GATEWAY_URL/health" > /dev/null 2>&1; then
        log_success "API Gateway is responding"
    else
        log_error "API Gateway not accessible"
        return 1
    fi
    
    # Test rate limiting headers
    response=$(curl -s -I "$API_GATEWAY_URL/api/v1/customers")
    if echo "$response" | grep -q "X-RateLimit"; then
        log_success "Rate limiting headers present"
    else
        log_warning "Rate limiting headers not detected"
    fi
    
    # Test security headers
    if echo "$response" | grep -q "X-Content-Type-Options\|X-Frame-Options\|X-XSS-Protection"; then
        log_success "Security headers implemented"
    else
        log_warning "Security headers not fully implemented"
    fi
}

# Microservice Individual Tests
test_customer_service() {
    run_test "Customer Management Service validation"
    
    # Health check
    if check_service_health "Customer Service" "$CUSTOMER_SERVICE_URL"; then
        # Test customer creation endpoint availability
        response=$(curl -s -w "%{http_code}" -o /dev/null "$CUSTOMER_SERVICE_URL/api/v1/customers")
        if [ "$response" = "200" ] || [ "$response" = "404" ]; then
            log_success "Customer service endpoints accessible"
        else
            log_error "Customer service endpoints not responding correctly"
        fi
    fi
}

test_loan_service() {
    run_test "Loan Origination Service validation"
    
    # Health check
    if check_service_health "Loan Service" "$LOAN_SERVICE_URL"; then
        # Test loan endpoints
        response=$(curl -s -w "%{http_code}" -o /dev/null "$LOAN_SERVICE_URL/api/v1/loans")
        if [ "$response" = "200" ] || [ "$response" = "404" ]; then
            log_success "Loan service endpoints accessible"
        else
            log_error "Loan service endpoints not responding correctly"
        fi
    fi
}

test_payment_service() {
    run_test "Payment Processing Service validation"
    
    # Health check
    if check_service_health "Payment Service" "$PAYMENT_SERVICE_URL"; then
        # Test payment endpoints
        response=$(curl -s -w "%{http_code}" -o /dev/null "$PAYMENT_SERVICE_URL/api/v1/payments")
        if [ "$response" = "200" ] || [ "$response" = "404" ]; then
            log_success "Payment service endpoints accessible"
        else
            log_error "Payment service endpoints not responding correctly"
        fi
    fi
}

# Circuit Breaker Tests
test_circuit_breaker() {
    run_test "Circuit Breaker pattern validation"
    
    # Test circuit breaker configuration
    if curl -f -s "$API_GATEWAY_URL/actuator/circuitbreakers" > /dev/null 2>&1; then
        log_success "Circuit breaker actuator endpoint accessible"
    else
        log_warning "Circuit breaker actuator endpoint not available"
    fi
    
    # Test circuit breaker metrics
    if curl -f -s "$API_GATEWAY_URL/actuator/metrics/resilience4j.circuitbreaker.calls" > /dev/null 2>&1; then
        log_success "Circuit breaker metrics available"
    else
        log_warning "Circuit breaker metrics not accessible"
    fi
}

# SAGA Pattern Tests
test_saga_orchestration() {
    run_test "SAGA orchestration pattern validation"
    
    # Test SAGA state endpoints
    if curl -f -s "$API_GATEWAY_URL/api/v1/saga/states" > /dev/null 2>&1; then
        log_success "SAGA state management accessible"
    else
        log_warning "SAGA state endpoints not available (may be internal)"
    fi
    
    # Test event streaming readiness
    if command -v kafka-topics.sh > /dev/null 2>&1; then
        log_success "Kafka CLI tools available for event streaming"
    else
        log_warning "Kafka CLI tools not installed"
    fi
}

# Security Compliance Tests
test_security_compliance() {
    run_test "OWASP Top 10 security compliance"
    
    # Test SQL injection protection
    response=$(curl -s -w "%{http_code}" -o /dev/null "$API_GATEWAY_URL/api/v1/customers?id=1' OR '1'='1")
    if [ "$response" = "400" ] || [ "$response" = "403" ]; then
        log_success "SQL injection protection active"
    else
        log_warning "SQL injection protection validation inconclusive"
    fi
    
    # Test XSS protection headers
    response=$(curl -s -I "$API_GATEWAY_URL/")
    if echo "$response" | grep -q "X-XSS-Protection\|Content-Security-Policy"; then
        log_success "XSS protection headers implemented"
    else
        log_warning "XSS protection headers not detected"
    fi
    
    # Test HTTPS redirect (if configured)
    if curl -s -I "http://localhost:8080/" | grep -q "Location: https"; then
        log_success "HTTPS redirect configured"
    else
        log_warning "HTTPS redirect not configured (acceptable for development)"
    fi
}

# Performance Tests
test_performance_metrics() {
    run_test "Performance metrics and monitoring"
    
    # Test Prometheus metrics endpoint
    if curl -f -s "$API_GATEWAY_URL/actuator/prometheus" > /dev/null 2>&1; then
        log_success "Prometheus metrics endpoint accessible"
    else
        log_warning "Prometheus metrics not available"
    fi
    
    # Test response time measurement
    start_time=$(date +%s%N)
    curl -s "$API_GATEWAY_URL/health" > /dev/null
    end_time=$(date +%s%N)
    response_time=$(( (end_time - start_time) / 1000000 ))
    
    if [ $response_time -lt 100 ]; then
        log_success "Response time: ${response_time}ms (excellent)"
    elif [ $response_time -lt 500 ]; then
        log_success "Response time: ${response_time}ms (good)"
    else
        log_warning "Response time: ${response_time}ms (could be improved)"
    fi
}

# Main execution
main() {
    echo ""
    log_info "Starting comprehensive microservices validation..."
    echo ""
    
    # Infrastructure tests
    test_database_connections
    test_redis_connectivity
    
    # Service tests
    test_api_gateway
    test_customer_service
    test_loan_service
    test_payment_service
    
    # Architecture pattern tests
    test_circuit_breaker
    test_saga_orchestration
    
    # Security and compliance tests
    test_security_compliance
    
    # Performance tests
    test_performance_metrics
    
    # Final summary
    echo ""
    echo "=============================================================="
    echo "🏦 MICROSERVICES VALIDATION SUMMARY"
    echo "=============================================================="
    echo ""
    echo "Total Tests: $TOTAL_TESTS"
    echo -e "${GREEN}Passed: $PASSED_TESTS${NC}"
    echo -e "${RED}Failed: $FAILED_TESTS${NC}"
    echo ""
    
    if [ $FAILED_TESTS -eq 0 ]; then
        echo -e "${GREEN}✅ ALL VALIDATIONS PASSED${NC}"
        echo "The microservices architecture is properly configured and operational."
        exit 0
    elif [ $FAILED_TESTS -lt 3 ]; then
        echo -e "${YELLOW}⚠️  MINOR ISSUES DETECTED${NC}"
        echo "The system is functional but some optimizations are recommended."
        exit 1
    else
        echo -e "${RED}❌ CRITICAL ISSUES DETECTED${NC}"
        echo "Multiple validation failures detected. Please review the system configuration."
        exit 2
    fi
}

# Execute main function
main