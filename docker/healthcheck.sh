#!/bin/bash

# Enhanced Enterprise Banking System - Health Check Script
# Comprehensive health verification for container orchestration

set -e

# Configuration
HEALTH_ENDPOINT="${HEALTH_ENDPOINT:-http://localhost:8080/actuator/health}"
TIMEOUT="${HEALTH_CHECK_TIMEOUT:-5}"
MAX_RETRIES="${HEALTH_CHECK_RETRIES:-3}"

# Logging
log_info() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [HEALTH] $1"
}

# Comprehensive health check
check_application_health() {
    local attempt=1
    
    while [ $attempt -le $MAX_RETRIES ]; do
        # Check if the health endpoint responds
        if curl -f -s --max-time "$TIMEOUT" "$HEALTH_ENDPOINT" >/dev/null 2>&1; then
            # Get detailed health status
            local health_response
            health_response=$(curl -s --max-time "$TIMEOUT" "$HEALTH_ENDPOINT" 2>/dev/null || echo '{"status":"UNKNOWN"}')
            
            # Parse status from JSON response
            local status
            status=$(echo "$health_response" | jq -r '.status // "UNKNOWN"' 2>/dev/null || echo "UNKNOWN")
            
            if [ "$status" = "UP" ]; then
                log_info "Application health check PASSED (status: $status)"
                return 0
            else
                log_info "Application health check FAILED (status: $status, attempt: $attempt/$MAX_RETRIES)"
            fi
        else
            log_info "Health endpoint unreachable (attempt: $attempt/$MAX_RETRIES)"
        fi
        
        if [ $attempt -lt $MAX_RETRIES ]; then
            sleep 2
        fi
        attempt=$((attempt + 1))
    done
    
    log_info "Application health check FAILED after $MAX_RETRIES attempts"
    return 1
}

# Check critical dependencies
check_dependencies() {
    local deps_healthy=true
    
    # Check database connectivity if configured
    if [ -n "${DATABASE_HOST:-}" ]; then
        if ! nc -z "${DATABASE_HOST}" "${DATABASE_PORT:-5432}" 2>/dev/null; then
            log_info "Database connectivity check FAILED"
            deps_healthy=false
        else
            log_info "Database connectivity check PASSED"
        fi
    fi
    
    # Check Redis connectivity if configured
    if [ -n "${REDIS_HOST:-}" ]; then
        if ! nc -z "${REDIS_HOST}" "${REDIS_PORT:-6379}" 2>/dev/null; then
            log_info "Redis connectivity check FAILED"
            deps_healthy=false
        else
            log_info "Redis connectivity check PASSED"
        fi
    fi
    
    if [ "$deps_healthy" = "true" ]; then
        return 0
    else
        return 1
    fi
}

# Main health check execution
main() {
    log_info "Starting comprehensive health check..."
    
    # Check application health
    if ! check_application_health; then
        log_info "Overall health check FAILED - Application unhealthy"
        exit 1
    fi
    
    # Check dependencies
    if ! check_dependencies; then
        log_info "Overall health check FAILED - Dependencies unhealthy"
        exit 1
    fi
    
    log_info "Overall health check PASSED - System healthy"
    exit 0
}

# Execute main health check
main