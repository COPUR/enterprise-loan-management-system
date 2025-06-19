#!/bin/bash

# Enterprise Banking System - Health Check Script
# Comprehensive health verification for container orchestration

set -euo pipefail

# Configuration
HEALTH_ENDPOINT="${HEALTH_ENDPOINT:-http://localhost:8080/actuator/health}"
TIMEOUT="${HEALTH_CHECK_TIMEOUT:-10}"
MAX_RETRIES="${HEALTH_CHECK_RETRIES:-3}"

# Health check function
check_health() {
    local attempt=1
    
    while [[ $attempt -le $MAX_RETRIES ]]; do
        # Check if the health endpoint responds
        if curl -f -s --max-time "$TIMEOUT" "$HEALTH_ENDPOINT" > /dev/null 2>&1; then
            # Get detailed health status
            local health_response
            health_response=$(curl -s --max-time "$TIMEOUT" "$HEALTH_ENDPOINT" 2>/dev/null || echo '{"status":"UNKNOWN"}')
            
            # Parse status from JSON response
            local status
            status=$(echo "$health_response" | grep -o '"status":"[^"]*"' | cut -d'"' -f4 2>/dev/null || echo "UNKNOWN")
            
            if [[ "$status" == "UP" ]]; then
                echo "Health check PASSED (status: $status)"
                exit 0
            else
                echo "Health check FAILED (status: $status, attempt: $attempt/$MAX_RETRIES)"
            fi
        else
            echo "Health endpoint unreachable (attempt: $attempt/$MAX_RETRIES)"
        fi
        
        if [[ $attempt -lt $MAX_RETRIES ]]; then
            sleep 2
        fi
        ((attempt++))
    done
    
    echo "Health check FAILED after $MAX_RETRIES attempts"
    exit 1
}

# Execute health check
check_health