#!/bin/bash

# Enterprise Banking System - Docker Entrypoint Script
# Handles environment setup, configuration validation, and application startup

set -euo pipefail

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

# Function to validate required environment variables
validate_environment() {
    log_info "Validating environment configuration..."
    
    local required_vars=(
        "SPRING_PROFILES_ACTIVE"
        "DATABASE_URL"
        "DATABASE_USERNAME"
    )
    
    local missing_vars=()
    
    for var in "${required_vars[@]}"; do
        if [[ -z "${!var:-}" ]]; then
            missing_vars+=("$var")
        fi
    done
    
    if [[ ${#missing_vars[@]} -gt 0 ]]; then
        log_error "Missing required environment variables: ${missing_vars[*]}"
        exit 1
    fi
    
    log_success "Environment validation completed"
}

# Function to wait for database connectivity
wait_for_database() {
    log_info "Waiting for database connectivity..."
    
    local max_attempts=30
    local attempt=1
    
    # Extract host and port from DATABASE_URL
    local db_host=$(echo "$DATABASE_URL" | sed -n 's|.*://[^@]*@\([^:]*\):.*|\1|p')
    local db_port=$(echo "$DATABASE_URL" | sed -n 's|.*://[^@]*@[^:]*:\([0-9]*\)/.*|\1|p')
    
    if [[ -z "$db_host" || -z "$db_port" ]]; then
        log_warn "Could not extract database host/port from URL, skipping connectivity check"
        return 0
    fi
    
    while ! nc -z "$db_host" "$db_port"; do
        if [[ $attempt -ge $max_attempts ]]; then
            log_error "Database is not available after $max_attempts attempts"
            exit 1
        fi
        
        log_info "Database not ready, waiting... (attempt $attempt/$max_attempts)"
        sleep 2
        ((attempt++))
    done
    
    log_success "Database is available"
}

# Function to display startup information
display_startup_info() {
    log_info "=========================================="
    log_info "Enterprise Loan Management System"
    log_info "=========================================="
    log_info "Version: ${APP_VERSION:-1.0.0}"
    log_info "Environment: ${SPRING_PROFILES_ACTIVE}"
    log_info "Java Version: $(java -version 2>&1 | head -n 1)"
    log_info "Database: ${DATABASE_URL}"
    log_info "=========================================="
}

# Main function
main() {
    # Execute startup sequence
    display_startup_info
    validate_environment
    wait_for_database
    
    log_info "Starting Enterprise Loan Management System..."
    
    # Start the application
    exec java $JAVA_OPTS -jar /app/app.jar "$@"
}

# Execute main function with all arguments
main "$@"