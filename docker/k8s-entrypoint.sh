#!/bin/bash
# Kubernetes-optimized entrypoint for Enterprise Banking System
# Handles graceful startup, configuration management, and health checks

set -e

# Color codes for logging
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging function
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

warn() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARNING: $1${NC}"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR: $1${NC}"
}

success() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

# Function to check if required environment variables are set
check_env_vars() {
    log "Checking required environment variables..."
    
    local required_vars=(
        "DATABASE_URL"
        "DATABASE_USERNAME"
        "REDIS_HOST"
        "KUBERNETES_NAMESPACE"
    )
    
    local missing_vars=()
    
    for var in "${required_vars[@]}"; do
        if [[ -z "${!var}" ]]; then
            missing_vars+=("$var")
        fi
    done
    
    if [[ ${#missing_vars[@]} -gt 0 ]]; then
        error "Missing required environment variables: ${missing_vars[*]}"
        exit 1
    fi
    
    success "All required environment variables are set"
}

# Function to wait for dependencies
wait_for_dependencies() {
    log "Waiting for dependencies to be ready..."
    
    # Extract database host and port from DATABASE_URL
    if [[ $DATABASE_URL =~ jdbc:postgresql://([^:]+):([0-9]+)/ ]]; then
        DB_HOST="${BASH_REMATCH[1]}"
        DB_PORT="${BASH_REMATCH[2]}"
        
        log "Waiting for PostgreSQL at $DB_HOST:$DB_PORT..."
        while ! nc -z "$DB_HOST" "$DB_PORT"; do
            log "PostgreSQL is not ready yet, waiting 2 seconds..."
            sleep 2
        done
        success "PostgreSQL is ready"
    fi
    
    # Wait for Redis
    if [[ -n "$REDIS_HOST" && -n "$REDIS_PORT" ]]; then
        log "Waiting for Redis at $REDIS_HOST:$REDIS_PORT..."
        while ! nc -z "$REDIS_HOST" "$REDIS_PORT"; do
            log "Redis is not ready yet, waiting 2 seconds..."
            sleep 2
        done
        success "Redis is ready"
    fi
}

# Function to perform application health check
app_health_check() {
    log "Performing application health check..."
    
    local max_attempts=30
    local attempt=1
    
    while [[ $attempt -le $max_attempts ]]; do
        if curl -f -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
            success "Application is healthy"
            return 0
        fi
        
        log "Health check attempt $attempt/$max_attempts failed, waiting 10 seconds..."
        sleep 10
        ((attempt++))
    done
    
    error "Application failed health check after $max_attempts attempts"
    return 1
}

# Function to handle graceful shutdown
graceful_shutdown() {
    log "Received shutdown signal, starting graceful shutdown..."
    
    # Send SIGTERM to Java process
    if [[ -n "$JAVA_PID" ]]; then
        log "Sending SIGTERM to Java process (PID: $JAVA_PID)"
        kill -TERM "$JAVA_PID"
        
        # Wait for process to terminate gracefully
        local shutdown_timeout=30
        local count=0
        
        while kill -0 "$JAVA_PID" 2>/dev/null && [[ $count -lt $shutdown_timeout ]]; do
            sleep 1
            ((count++))
        done
        
        if kill -0 "$JAVA_PID" 2>/dev/null; then
            warn "Graceful shutdown timeout, forcing termination"
            kill -KILL "$JAVA_PID"
        else
            success "Application shutdown gracefully"
        fi
    fi
    
    exit 0
}

# Function to setup JVM options for Kubernetes
setup_jvm_options() {
    log "Setting up JVM options for Kubernetes environment..."
    
    # Get container memory limit
    if [[ -f /sys/fs/cgroup/memory/memory.limit_in_bytes ]]; then
        MEMORY_LIMIT=$(cat /sys/fs/cgroup/memory/memory.limit_in_bytes)
    elif [[ -f /sys/fs/cgroup/memory.max ]]; then
        MEMORY_LIMIT=$(cat /sys/fs/cgroup/memory.max)
    else
        MEMORY_LIMIT="4294967296" # Default 4GB
    fi
    
    # Calculate heap size (70% of container memory)
    HEAP_SIZE=$((MEMORY_LIMIT * 70 / 100))
    
    # Update JAVA_OPTS with dynamic memory settings
    export JAVA_OPTS="$JAVA_OPTS -Xmx${HEAP_SIZE} -XX:MaxDirectMemorySize=256m"
    
    # Add Kubernetes-specific system properties
    export JAVA_OPTS="$JAVA_OPTS -Dkubernetes.namespace=${KUBERNETES_NAMESPACE:-default}"
    export JAVA_OPTS="$JAVA_OPTS -Dkubernetes.pod.name=${POD_NAME:-unknown}"
    export JAVA_OPTS="$JAVA_OPTS -Dkubernetes.node.name=${NODE_NAME:-unknown}"
    
    log "JVM options configured: $JAVA_OPTS"
}

# Function to create necessary directories
setup_directories() {
    log "Setting up application directories..."
    
    mkdir -p /app/logs /app/tmp /app/config
    
    # Set proper permissions
    chmod 755 /app/logs /app/tmp /app/config
    
    success "Directories created successfully"
}

# Function to setup signal handlers
setup_signal_handlers() {
    log "Setting up signal handlers for graceful shutdown..."
    
    trap graceful_shutdown SIGTERM SIGINT SIGQUIT
}

# Main execution
main() {
    log "Starting Enterprise Banking System in Kubernetes environment..."
    
    # Setup signal handlers first
    setup_signal_handlers
    
    # Perform pre-startup checks
    check_env_vars
    setup_directories
    setup_jvm_options
    
    # Wait for dependencies if enabled
    if [[ "${WAIT_FOR_DEPENDENCIES:-true}" == "true" ]]; then
        wait_for_dependencies
    fi
    
    # Start the application
    log "Starting Java application with command: $*"
    
    if [[ $# -eq 0 ]]; then
        # Default command
        java $JAVA_OPTS -jar /app/BOOT-INF/lib/*:/app/BOOT-INF/classes com.bank.loanmanagement.LoanManagementApplication &
    else
        # Custom command
        exec "$@" &
    fi
    
    JAVA_PID=$!
    log "Java application started with PID: $JAVA_PID"
    
    # Wait for the application to be ready
    if [[ "${SKIP_HEALTH_CHECK:-false}" != "true" ]]; then
        app_health_check
    fi
    
    success "Enterprise Banking System is ready and running!"
    
    # Wait for the Java process
    wait $JAVA_PID
    
    log "Java process has exited"
}

# Execute main function with all arguments
main "$@"