#!/bin/bash
# Kubernetes health check script for Enterprise Banking System
# Provides comprehensive health validation for banking microservice

set -e

# Configuration
HEALTH_ENDPOINT="http://localhost:8080/actuator/health"
READINESS_ENDPOINT="http://localhost:8080/actuator/health/readiness"
LIVENESS_ENDPOINT="http://localhost:8080/actuator/health/liveness"
TIMEOUT=5
MAX_RETRIES=3

# Exit codes
SUCCESS=0
FAILURE=1
WARNING=2

# Logging function
log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] HEALTH: $1" >&2
}

# Function to check HTTP endpoint
check_endpoint() {
    local endpoint="$1"
    local expected_status="${2:-200}"
    local timeout="${3:-$TIMEOUT}"
    
    local response
    local http_code
    
    response=$(curl -s -w "%{http_code}" --max-time "$timeout" "$endpoint" 2>/dev/null || echo "000")
    http_code="${response: -3}"
    
    if [[ "$http_code" == "$expected_status" ]]; then
        return 0
    else
        log "Endpoint $endpoint returned HTTP $http_code (expected $expected_status)"
        return 1
    fi
}

# Function to check application health
check_application_health() {
    local endpoint="$1"
    local response
    local http_code
    local status
    
    response=$(curl -s -w "%{http_code}" --max-time "$TIMEOUT" "$endpoint" 2>/dev/null || echo '{"status":"DOWN"}000')
    http_code="${response: -3}"
    response_body="${response%???}"
    
    if [[ "$http_code" != "200" ]]; then
        log "Health endpoint returned HTTP $http_code"
        return 1
    fi
    
    # Parse JSON response for status
    status=$(echo "$response_body" | jq -r '.status' 2>/dev/null || echo "UNKNOWN")
    
    if [[ "$status" == "UP" ]]; then
        log "Application health check passed"
        return 0
    else
        log "Application health status: $status"
        return 1
    fi
}

# Function to check database connectivity
check_database_health() {
    local response
    local db_status
    
    response=$(curl -s --max-time "$TIMEOUT" "$HEALTH_ENDPOINT" 2>/dev/null || echo '{}')
    db_status=$(echo "$response" | jq -r '.components.db.status' 2>/dev/null || echo "UNKNOWN")
    
    if [[ "$db_status" == "UP" ]]; then
        log "Database connectivity check passed"
        return 0
    else
        log "Database status: $db_status"
        return 1
    fi
}

# Function to check Redis connectivity
check_redis_health() {
    local response
    local redis_status
    
    response=$(curl -s --max-time "$TIMEOUT" "$HEALTH_ENDPOINT" 2>/dev/null || echo '{}')
    redis_status=$(echo "$response" | jq -r '.components.redis.status' 2>/dev/null || echo "UNKNOWN")
    
    if [[ "$redis_status" == "UP" ]]; then
        log "Redis connectivity check passed"
        return 0
    else
        log "Redis status: $redis_status"
        return 1
    fi
}

# Function to check disk space
check_disk_space() {
    local response
    local disk_status
    
    response=$(curl -s --max-time "$TIMEOUT" "$HEALTH_ENDPOINT" 2>/dev/null || echo '{}')
    disk_status=$(echo "$response" | jq -r '.components.diskSpace.status' 2>/dev/null || echo "UNKNOWN")
    
    if [[ "$disk_status" == "UP" ]]; then
        log "Disk space check passed"
        return 0
    else
        log "Disk space status: $disk_status"
        return 1
    fi
}

# Function to perform comprehensive health check
comprehensive_health_check() {
    local failed_checks=0
    
    log "Performing comprehensive health check..."
    
    # Check basic application health
    if ! check_application_health "$HEALTH_ENDPOINT"; then
        ((failed_checks++))
    fi
    
    # Check database connectivity
    if ! check_database_health; then
        ((failed_checks++))
    fi
    
    # Check Redis connectivity (non-critical)
    if ! check_redis_health; then
        log "Redis check failed (non-critical)"
    fi
    
    # Check disk space
    if ! check_disk_space; then
        ((failed_checks++))
    fi
    
    if [[ $failed_checks -eq 0 ]]; then
        log "All health checks passed"
        return 0
    else
        log "$failed_checks critical health checks failed"
        return 1
    fi
}

# Function to check readiness
check_readiness() {
    log "Checking application readiness..."
    
    if check_endpoint "$READINESS_ENDPOINT" 200 "$TIMEOUT"; then
        log "Readiness check passed"
        return 0
    else
        log "Readiness check failed"
        return 1
    fi
}

# Function to check liveness
check_liveness() {
    log "Checking application liveness..."
    
    if check_endpoint "$LIVENESS_ENDPOINT" 200 "$TIMEOUT"; then
        log "Liveness check passed"
        return 0
    else
        log "Liveness check failed"
        return 1
    fi
}

# Function to perform startup health check
startup_health_check() {
    local max_startup_time=300  # 5 minutes
    local check_interval=10
    local elapsed=0
    
    log "Performing startup health check..."
    
    while [[ $elapsed -lt $max_startup_time ]]; do
        if check_endpoint "$HEALTH_ENDPOINT" 200 2; then
            log "Application started successfully"
            return 0
        fi
        
        sleep $check_interval
        elapsed=$((elapsed + check_interval))
        log "Waiting for application startup... ($elapsed/${max_startup_time}s)"
    done
    
    log "Application failed to start within $max_startup_time seconds"
    return 1
}

# Main health check function with retries
main_health_check() {
    local check_type="${1:-health}"
    local retry=0
    
    while [[ $retry -lt $MAX_RETRIES ]]; do
        case "$check_type" in
            "health"|"")
                if comprehensive_health_check; then
                    exit $SUCCESS
                fi
                ;;
            "readiness")
                if check_readiness; then
                    exit $SUCCESS
                fi
                ;;
            "liveness")
                if check_liveness; then
                    exit $SUCCESS
                fi
                ;;
            "startup")
                if startup_health_check; then
                    exit $SUCCESS
                fi
                ;;
            *)
                log "Unknown health check type: $check_type"
                exit $FAILURE
                ;;
        esac
        
        ((retry++))
        if [[ $retry -lt $MAX_RETRIES ]]; then
            log "Health check failed, retrying in 2 seconds... (attempt $((retry + 1))/$MAX_RETRIES)"
            sleep 2
        fi
    done
    
    log "Health check failed after $MAX_RETRIES attempts"
    exit $FAILURE
}

# Handle command line arguments
if [[ $# -eq 0 ]]; then
    # Default comprehensive health check
    main_health_check "health"
else
    # Specific health check type
    main_health_check "$1"
fi