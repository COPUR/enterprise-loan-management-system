#!/bin/bash

# Enhanced Enterprise Banking System - Application Entrypoint
# Production-ready startup script with comprehensive checks

set -e

# Configuration
APP_JAR="${APP_JAR:-app.jar}"
SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-production}"
LOG_LEVEL="${LOG_LEVEL:-INFO}"
JAVA_OPTS="${JAVA_OPTS:-}"

# Logging functions
log_info() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [INFO] $1"
}

log_error() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [ERROR] $1" >&2
}

log_warning() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [WARNING] $1"
}

# Wait for dependent services
wait_for_service() {
    local host="$1"
    local port="$2"
    local service_name="$3"
    local timeout="${4:-60}"
    
    log_info "Waiting for $service_name at $host:$port..."
    
    local count=0
    while ! nc -z "$host" "$port" &>/dev/null; do
        if [ $count -ge $timeout ]; then
            log_error "Timeout waiting for $service_name at $host:$port"
            return 1
        fi
        sleep 1
        ((count++))
    done
    
    log_info "$service_name is ready at $host:$port"
}

# Health check for external dependencies
check_dependencies() {
    log_info "Checking external dependencies..."
    
    # PostgreSQL
    if [ -n "${DATABASE_HOST:-}" ]; then
        wait_for_service "${DATABASE_HOST}" "${DATABASE_PORT:-5432}" "PostgreSQL Database" 120
    fi
    
    # Redis
    if [ -n "${REDIS_HOST:-}" ]; then
        wait_for_service "${REDIS_HOST}" "${REDIS_PORT:-6379}" "Redis Cache" 60
    fi
    
    # Kafka
    if [ -n "${KAFKA_HOST:-}" ]; then
        wait_for_service "${KAFKA_HOST}" "${KAFKA_PORT:-9092}" "Kafka Message Broker" 120
    fi
    
    # Keycloak
    if [ -n "${KEYCLOAK_HOST:-}" ]; then
        wait_for_service "${KEYCLOAK_HOST}" "${KEYCLOAK_PORT:-8080}" "Keycloak OAuth Server" 180
    fi
    
    log_info "All dependencies are ready"
}

# Validate environment
validate_environment() {
    log_info "Validating environment configuration..."
    
    # Check required environment variables
    local required_vars=(
        "SPRING_PROFILES_ACTIVE"
    )
    
    for var in "${required_vars[@]}"; do
        if [ -z "${!var:-}" ]; then
            log_error "Required environment variable $var is not set"
            exit 1
        fi
    done
    
    # Validate database configuration
    if [[ "$SPRING_PROFILES_ACTIVE" != *"test"* ]]; then
        if [ -z "${DATABASE_URL:-}" ] && [ -z "${DATABASE_HOST:-}" ]; then
            log_warning "No database configuration found. Using embedded H2 database."
        fi
    fi
    
    # Validate AI configuration
    if [[ "$SPRING_PROFILES_ACTIVE" == *"ai-enabled"* ]]; then
        if [ -z "${OPENAI_API_KEY:-}" ]; then
            log_warning "AI features enabled but OPENAI_API_KEY not set. AI features will be limited."
        fi
    fi
    
    log_info "Environment validation completed"
}

# Setup application directories
setup_directories() {
    log_info "Setting up application directories..."
    
    # Create necessary directories
    mkdir -p /app/logs /app/tmp /app/config
    
    # Set permissions (if running as root, change ownership)
    if [ "$(id -u)" = "0" ]; then
        chown -R banking:banking /app/logs /app/tmp /app/config
    fi
    
    log_info "Directories setup completed"
}

# Configure JVM options
configure_jvm() {
    log_info "Configuring JVM options..."
    
    # Base JVM options for production
    local base_opts=(
        "-server"
        "-XX:+UseG1GC"
        "-XX:+UseContainerSupport"
        "-XX:MaxRAMPercentage=75"
        "-XX:+ExitOnOutOfMemoryError"
        "-XX:+HeapDumpOnOutOfMemoryError"
        "-XX:HeapDumpPath=/app/logs/heapdump.hprof"
        "-Djava.security.egd=file:/dev/./urandom"
        "-Dfile.encoding=UTF-8"
        "-Duser.timezone=UTC"
        "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}"
    )
    
    # Performance optimizations for banking workloads
    local performance_opts=(
        "-XX:+OptimizeStringConcat"
        "-XX:+UseStringDeduplication"
        "-XX:MaxGCPauseMillis=200"
        "-XX:G1HeapRegionSize=16m"
    )
    
    # Security options for banking compliance
    local security_opts=(
        "-Djava.awt.headless=true"
        "-Dcom.sun.management.jmxremote=false"
        "-Djdk.tls.ephemeralDHKeySize=2048"
    )
    
    # Combine all options
    JAVA_OPTS="${JAVA_OPTS} ${base_opts[*]} ${performance_opts[*]} ${security_opts[*]}"
    
    log_info "JVM configuration completed"
}

# Signal handlers for graceful shutdown
shutdown_handler() {
    log_info "Received shutdown signal. Initiating graceful shutdown..."
    
    if [ -n "${APP_PID:-}" ] && kill -0 "$APP_PID" 2>/dev/null; then
        log_info "Sending SIGTERM to application (PID: $APP_PID)"
        kill -TERM "$APP_PID"
        
        # Wait for graceful shutdown
        local count=0
        while kill -0 "$APP_PID" 2>/dev/null && [ $count -lt 30 ]; do
            sleep 1
            ((count++))
        done
        
        # Force kill if still running
        if kill -0 "$APP_PID" 2>/dev/null; then
            log_warning "Application did not shut down gracefully. Forcing termination."
            kill -KILL "$APP_PID"
        fi
    fi
    
    log_info "Application shutdown completed"
    exit 0
}

# Setup signal handlers
trap shutdown_handler SIGTERM SIGINT

# Main startup function
start_application() {
    log_info "Starting Enhanced Enterprise Banking Application..."
    log_info "Profile: $SPRING_PROFILES_ACTIVE"
    log_info "JAR: $APP_JAR"
    log_info "JVM Options: $JAVA_OPTS"
    
    # Start the application in background
    java $JAVA_OPTS -jar "/app/$APP_JAR" &
    APP_PID=$!
    
    log_info "Application started with PID: $APP_PID"
    
    # Wait for application to start
    local count=0
    while ! curl -f http://localhost:8080/actuator/health &>/dev/null && [ $count -lt 120 ]; do
        if ! kill -0 "$APP_PID" 2>/dev/null; then
            log_error "Application process died during startup"
            exit 1
        fi
        sleep 1
        ((count++))
    done
    
    if [ $count -ge 120 ]; then
        log_error "Application failed to start within 2 minutes"
        exit 1
    fi
    
    log_info "Application is ready and healthy"
    
    # Wait for the application process
    wait "$APP_PID"
}

# Main execution flow
main() {
    log_info "Enhanced Enterprise Banking System - Starting Up"
    log_info "======================================================="
    
    setup_directories
    validate_environment
    configure_jvm
    check_dependencies
    start_application
}

# Execute main function
main "$@"