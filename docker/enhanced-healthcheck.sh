#!/bin/bash
# Enhanced Enterprise Banking System - Health Check Script
# Comprehensive health validation for banking compliance

set -euo pipefail

# Configuration
HEALTH_URL="http://localhost:${SERVER_PORT:-8080}/actuator/health"
TIMEOUT=10
MAX_RETRIES=3

# Banking-specific health checks
check_basic_health() {
    local response
    response=$(curl -s -w "%{http_code}" -m $TIMEOUT "$HEALTH_URL" --output /dev/null)
    
    if [[ "$response" == "200" ]]; then
        return 0
    else
        echo "❌ Basic health check failed - HTTP status: $response"
        return 1
    fi
}

check_database_health() {
    local db_health_url="http://localhost:${SERVER_PORT:-8080}/actuator/health/db"
    local response
    response=$(curl -s -m $TIMEOUT "$db_health_url" 2>/dev/null || echo "error")
    
    if echo "$response" | grep -q '"status":"UP"'; then
        echo "✅ Database health: OK"
        return 0
    else
        echo "❌ Database health check failed"
        return 1
    fi
}

check_redis_health() {
    local redis_health_url="http://localhost:${SERVER_PORT:-8080}/actuator/health/redis"
    local response
    response=$(curl -s -m $TIMEOUT "$redis_health_url" 2>/dev/null || echo "error")
    
    if echo "$response" | grep -q '"status":"UP"'; then
        echo "✅ Redis health: OK"
        return 0
    else
        echo "❌ Redis health check failed"
        return 1
    fi
}

check_banking_services() {
    local loan_service_url="http://localhost:${SERVER_PORT:-8080}/api/v1/loans/health"
    local response
    response=$(curl -s -w "%{http_code}" -m $TIMEOUT "$loan_service_url" --output /dev/null 2>/dev/null || echo "000")
    
    if [[ "$response" =~ ^(200|404)$ ]]; then
        echo "✅ Banking services: OK"
        return 0
    else
        echo "❌ Banking services health check failed - HTTP status: $response"
        return 1
    fi
}

check_security_endpoints() {
    local security_url="http://localhost:${SERVER_PORT:-8080}/actuator/info"
    local response
    response=$(curl -s -w "%{http_code}" -m $TIMEOUT "$security_url" --output /dev/null 2>/dev/null || echo "000")
    
    if [[ "$response" == "200" ]]; then
        echo "✅ Security endpoints: OK"
        return 0
    else
        echo "❌ Security endpoints health check failed"
        return 1
    fi
}

check_compliance_features() {
    # Check if compliance features are properly loaded
    local info_url="http://localhost:${SERVER_PORT:-8080}/actuator/info"
    local response
    response=$(curl -s -m $TIMEOUT "$info_url" 2>/dev/null || echo "error")
    
    local compliance_ok=true
    
    if [[ "${FAPI_ENABLED:-false}" == "true" ]]; then
        if echo "$response" | grep -q -i "fapi\|financial-grade"; then
            echo "✅ FAPI compliance: OK"
        else
            echo "❌ FAPI compliance feature not detected"
            compliance_ok=false
        fi
    fi
    
    if [[ "${BIAN_COMPLIANCE_ENABLED:-false}" == "true" ]]; then
        echo "✅ BIAN compliance: Configured"
    fi
    
    if [[ "${ISLAMIC_BANKING_ENABLED:-false}" == "true" ]]; then
        echo "✅ Islamic Banking: Configured"
    fi
    
    if [[ "$compliance_ok" == "true" ]]; then
        return 0
    else
        return 1
    fi
}

# Main health check execution
main() {
    echo "🏥 Enhanced Enterprise Banking System Health Check"
    echo "================================================"
    echo "Timestamp: $(date)"
    echo "Health URL: $HEALTH_URL"
    
    local checks_passed=0
    local total_checks=5
    
    # Execute all health checks
    if check_basic_health; then
        ((checks_passed++))
    fi
    
    if check_database_health; then
        ((checks_passed++))
    fi
    
    if check_redis_health; then
        ((checks_passed++))
    fi
    
    if check_banking_services; then
        ((checks_passed++))
    fi
    
    if check_security_endpoints; then
        ((checks_passed++))
    fi
    
    # Additional compliance checks (non-critical)
    check_compliance_features || true
    
    echo "================================================"
    echo "Health Check Results: $checks_passed/$total_checks passed"
    
    # Determine overall health status
    if [[ $checks_passed -ge 4 ]]; then
        echo "✅ Overall Status: HEALTHY"
        echo "🏦 Enhanced Enterprise Banking System is ready to serve"
        exit 0
    elif [[ $checks_passed -ge 2 ]]; then
        echo "⚠️ Overall Status: DEGRADED"
        echo "🏦 Banking system is partially functional"
        exit 1
    else
        echo "❌ Overall Status: UNHEALTHY"
        echo "🏦 Banking system requires immediate attention"
        exit 1
    fi
}

# Execute main function
main "$@"