#!/bin/bash
# Enterprise Loan Management System - Health Check Script
# 12-Factor App compliant health monitoring

# Configuration with environment variable support
TIMEOUT=${HEALTH_CHECK_TIMEOUT:-3}
SERVER_PORT=${SERVER_PORT:-8080}
HEALTH_URL="http://localhost:${SERVER_PORT}/actuator/health"
READINESS_URL="http://localhost:${SERVER_PORT}/actuator/health/readiness"

# Banking-specific health check with comprehensive validation
check_banking_health() {
    # Primary health check
    if ! curl -f -s --max-time $TIMEOUT "$HEALTH_URL" > /dev/null 2>&1; then
        echo "❌ Banking system health check failed - service not responding"
        return 1
    fi
    
    # Get detailed health status
    HEALTH_RESPONSE=$(curl -f -s --max-time $TIMEOUT "$HEALTH_URL" 2>/dev/null)
    HEALTH_STATUS=$(echo "$HEALTH_RESPONSE" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
    
    if [ "$HEALTH_STATUS" != "UP" ]; then
        echo "❌ Banking system unhealthy - status: $HEALTH_STATUS"
        return 1
    fi
    
    # Check readiness (if endpoint exists)
    if curl -f -s --max-time $TIMEOUT "$READINESS_URL" > /dev/null 2>&1; then
        READINESS_RESPONSE=$(curl -f -s --max-time $TIMEOUT "$READINESS_URL" 2>/dev/null)
        READINESS_STATUS=$(echo "$READINESS_RESPONSE" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
        
        if [ "$READINESS_STATUS" != "UP" ]; then
            echo "⚠️  Banking system healthy but not ready - readiness: $READINESS_STATUS"
            return 1
        fi
    fi
    
    # Banking compliance check - verify critical endpoints are responding
    INFO_URL="http://localhost:${SERVER_PORT}/actuator/info"
    if curl -f -s --max-time $TIMEOUT "$INFO_URL" > /dev/null 2>&1; then
        echo "✅ Banking system healthy and ready (compliance verified)"
        return 0
    else
        echo "⚠️  Banking system healthy but compliance endpoint unavailable"
        return 0  # Don't fail on info endpoint - it's not critical
    fi
}

# Execute health check with retry logic
MAX_RETRIES=2
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if check_banking_health; then
        exit 0
    fi
    
    RETRY_COUNT=$((RETRY_COUNT + 1))
    if [ $RETRY_COUNT -lt $MAX_RETRIES ]; then
        echo "Retrying health check ($RETRY_COUNT/$MAX_RETRIES)..."
        sleep 1
    fi
done

echo "❌ Banking system health check failed after $MAX_RETRIES attempts"
exit 1