#!/bin/bash
# Kubernetes Liveness Probe for Enhanced Enterprise Banking System
# Validates application is running and responsive

set -euo pipefail

# Configuration
HEALTH_URL="http://localhost:${MANAGEMENT_PORT:-8081}/actuator/health/liveness"
TIMEOUT=5
MAX_RETRIES=2

# Logging function
log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] LIVENESS: $1" >&2
}

# Function to check liveness probe
check_liveness() {
    local response
    local http_code
    
    response=$(curl -s -w "%{http_code}" --max-time "$TIMEOUT" "$HEALTH_URL" 2>/dev/null || echo "000")
    http_code="${response: -3}"
    
    if [[ "$http_code" == "200" ]]; then
        log "Liveness check passed - Application is alive"
        return 0
    else
        log "Liveness check failed - HTTP status: $http_code"
        return 1
    fi
}

# Function to check basic application process
check_application_process() {
    # Check if Java process is running
    if pgrep -f "java.*LoanManagementApplication" > /dev/null; then
        log "Java application process is running"
        return 0
    else
        log "Java application process not found"
        return 1
    fi
}

# Function to check basic connectivity
check_basic_connectivity() {
    local port="${SERVER_PORT:-8080}"
    
    if nc -z localhost "$port" 2>/dev/null; then
        log "Application port $port is responding"
        return 0
    else
        log "Application port $port is not responding"
        return 1
    fi
}

# Main liveness check function
main_liveness_check() {
    local retry=0
    local checks_passed=0
    local total_checks=3
    
    log "Starting Enhanced Banking System liveness check..."
    
    while [[ $retry -lt $MAX_RETRIES ]]; do
        checks_passed=0
        
        # Check liveness endpoint
        if check_liveness; then
            ((checks_passed++))
        fi
        
        # Check application process
        if check_application_process; then
            ((checks_passed++))
        fi
        
        # Check basic connectivity
        if check_basic_connectivity; then
            ((checks_passed++))
        fi
        
        # If at least 2 out of 3 checks pass, consider alive
        if [[ $checks_passed -ge 2 ]]; then
            log "Liveness check successful ($checks_passed/$total_checks checks passed)"
            exit 0
        fi
        
        ((retry++))
        if [[ $retry -lt $MAX_RETRIES ]]; then
            log "Liveness check failed, retrying in 1 second... (attempt $((retry + 1))/$MAX_RETRIES)"
            sleep 1
        fi
    done
    
    log "Liveness check failed after $MAX_RETRIES attempts ($checks_passed/$total_checks checks passed)"
    exit 1
}

# Execute main liveness check
main_liveness_check "$@"