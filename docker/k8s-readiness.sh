#!/bin/bash
# Kubernetes readiness probe for Enterprise Banking System
# Validates application is ready to receive traffic

set -e

# Configuration
READINESS_ENDPOINT="http://localhost:8080/actuator/health/readiness"
HEALTH_ENDPOINT="http://localhost:8080/actuator/health"
TIMEOUT=3
MAX_RETRIES=2

# Exit codes
READY=0
NOT_READY=1

# Logging function
log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] READINESS: $1" >&2
}

# Function to check if application is ready
check_readiness() {
    local response
    local http_code
    local status
    
    # Try readiness endpoint first
    response=$(curl -s -w "%{http_code}" --max-time "$TIMEOUT" "$READINESS_ENDPOINT" 2>/dev/null || echo '{"status":"DOWN"}000')
    http_code="${response: -3}"
    response_body="${response%???}"
    
    if [[ "$http_code" == "200" ]]; then
        status=$(echo "$response_body" | jq -r '.status' 2>/dev/null || echo "UNKNOWN")
        if [[ "$status" == "UP" ]]; then
            log "Readiness probe passed"
            return 0
        fi
    fi
    
    # Fallback to general health endpoint
    response=$(curl -s -w "%{http_code}" --max-time "$TIMEOUT" "$HEALTH_ENDPOINT" 2>/dev/null || echo '{"status":"DOWN"}000')
    http_code="${response: -3}"
    response_body="${response%???}"
    
    if [[ "$http_code" == "200" ]]; then
        status=$(echo "$response_body" | jq -r '.status' 2>/dev/null || echo "UNKNOWN")
        if [[ "$status" == "UP" ]]; then
            log "Health check passed (readiness fallback)"
            return 0
        fi
    fi
    
    log "Application is not ready (HTTP: $http_code, Status: $status)"
    return 1
}

# Main readiness check with retries
main() {
    local retry=0
    
    while [[ $retry -lt $MAX_RETRIES ]]; do
        if check_readiness; then
            exit $READY
        fi
        
        ((retry++))
        if [[ $retry -lt $MAX_RETRIES ]]; then
            sleep 1
        fi
    done
    
    log "Readiness check failed after $MAX_RETRIES attempts"
    exit $NOT_READY
}

# Execute main function
main "$@"