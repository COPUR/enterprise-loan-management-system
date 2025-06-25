#!/bin/bash

# Enterprise Banking System - Test Entrypoint Script
# Configures testing environment and runs test suites

set -euo pipefail

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${BLUE}[TEST-INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_success() {
    echo -e "${GREEN}[TEST-SUCCESS]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[TEST-ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

# Wait for test dependencies
wait_for_dependencies() {
    log_info "Waiting for test dependencies to be ready..."
    
    # Wait for PostgreSQL test database
    if [[ -n "${DATABASE_URL:-}" ]]; then
        local db_host=$(echo "$DATABASE_URL" | sed -n 's|.*://[^@]*@\([^:]*\):.*|\1|p')
        local db_port=$(echo "$DATABASE_URL" | sed -n 's|.*://[^@]*@[^:]*:\([0-9]*\)/.*|\1|p')
        
        if [[ -n "$db_host" && -n "$db_port" ]]; then
            local attempt=1
            while ! nc -z "$db_host" "$db_port"; do
                if [[ $attempt -ge 30 ]]; then
                    log_error "Test database not available after 30 attempts"
                    exit 1
                fi
                log_info "Waiting for test database... (attempt $attempt/30)"
                sleep 2
                ((attempt++))
            done
            log_success "Test database is available"
        fi
    fi
    
    # Wait for Redis (if configured)
    if [[ -n "${REDIS_HOST:-}" && -n "${REDIS_PORT:-}" ]]; then
        local attempt=1
        while ! nc -z "$REDIS_HOST" "$REDIS_PORT"; do
            if [[ $attempt -ge 15 ]]; then
                log_error "Redis not available after 15 attempts"
                exit 1
            fi
            log_info "Waiting for Redis... (attempt $attempt/15)"
            sleep 2
            ((attempt++))
        done
        log_success "Redis is available"
    fi
}

# Configure test environment
configure_test_environment() {
    log_info "Configuring test environment..."
    
    # Set test-specific environment variables
    export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-test,testcontainers}"
    export TESTCONTAINERS_REUSE_ENABLE="${TESTCONTAINERS_REUSE_ENABLE:-true}"
    export JAVA_OPTS="${JAVA_OPTS:--XX:+UseG1GC -Xmx4g -XX:+UseStringDeduplication}"
    
    # Create test directories
    mkdir -p /app/test-reports
    mkdir -p /app/test-logs
    
    log_success "Test environment configured"
}

# Run tests with proper error handling
run_tests() {
    log_info "Starting test execution..."
    
    local test_exit_code=0
    
    # Run the tests
    if [[ $# -gt 0 ]]; then
        log_info "Running custom test command: $*"
        "$@" || test_exit_code=$?
    else
        log_info "Running default test suite..."
        ./gradlew test --no-daemon --continue || test_exit_code=$?
    fi
    
    # Generate test report summary
    if [[ -d build/reports/tests/test ]]; then
        log_info "Test reports available in build/reports/tests/test/"
        if [[ -f build/reports/tests/test/index.html ]]; then
            log_success "Test report generated: build/reports/tests/test/index.html"
        fi
    fi
    
    if [[ $test_exit_code -eq 0 ]]; then
        log_success "All tests completed successfully"
    else
        log_error "Tests failed with exit code: $test_exit_code"
        log_info "Check test reports for detailed failure information"
    fi
    
    exit $test_exit_code
}

# Main execution
main() {
    log_info "=========================================="
    log_info "Enterprise Banking System - Test Runner"
    log_info "=========================================="
    
    configure_test_environment
    wait_for_dependencies
    run_tests "$@"
}

# Execute main function with all arguments
main "$@"