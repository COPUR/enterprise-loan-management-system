
#!/bin/bash

# =======================================================================
# GraalVM Complete Boot Procedure for Enterprise Loan Management System
# =======================================================================

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BUILD_DIR="$PROJECT_ROOT/build"
NATIVE_BUILD_DIR="$BUILD_DIR/native"
GRAALVM_ENV="$HOME/.graalvm/graalvm-env.sh"

# Default settings
DEFAULT_PORT=5000
DEFAULT_PROFILE="native,production"
RUN_MODE="interactive"
SKIP_BUILD=false
SKIP_SETUP=false

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_header() {
    echo -e "${BOLD}${CYAN}$1${NC}"
}

# Display banner
show_banner() {
    clear
    echo -e "${BOLD}${CYAN}"
    cat << 'EOF'
========================================================================
🏦 Enterprise Loan Management System - GraalVM Native Boot
========================================================================
                          
    ╔══════════════════════════════════════════════════════════════╗
    ║           BANKING SYSTEM NATIVE COMPILATION                  ║
    ║                                                              ║
    ║  • High-Performance Native Execution                        ║
    ║  • Instant Startup (< 3 seconds)                           ║
    ║  • Minimal Memory Footprint (< 200MB)                      ║
    ║  • Production-Ready Banking Platform                        ║
    ╚══════════════════════════════════════════════════════════════╝

EOF
    echo -e "${NC}"
}

# Show help
show_help() {
    cat << EOF
Usage: $0 [OPTIONS]

Enterprise Loan Management System GraalVM Boot Script

OPTIONS:
    --setup-only        Only run GraalVM setup, don't build or run
    --build-only        Only build native image, don't run
    --skip-setup        Skip GraalVM setup (assume already installed)
    --skip-build        Skip build process (use existing native image)
    --port PORT         Server port (default: $DEFAULT_PORT)
    --profile PROFILE   Spring profiles (default: $DEFAULT_PROFILE)
    --daemon            Run in daemon mode (non-interactive)
    --dev               Use development profile
    --test              Run with test profile and exit
    --benchmark         Run performance benchmark
    --help              Show this help

EXAMPLES:
    $0                          # Full setup, build, and run
    $0 --skip-setup --port 8080 # Build and run on port 8080
    $0 --build-only             # Just build native image
    $0 --skip-build --dev       # Run existing native image in dev mode
    $0 --test                   # Run tests with native image
    $0 --benchmark              # Run performance benchmark

ENVIRONMENT VARIABLES:
    DATABASE_URL       PostgreSQL connection URL
    REDIS_URL          Redis connection URL
    OPENAI_API_KEY     OpenAI API key (optional)

EOF
}

# Parse command line arguments
parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --setup-only)
                RUN_MODE="setup"
                shift
                ;;
            --build-only)
                RUN_MODE="build"
                shift
                ;;
            --skip-setup)
                SKIP_SETUP=true
                shift
                ;;
            --skip-build)
                SKIP_BUILD=true
                shift
                ;;
            --port)
                DEFAULT_PORT="$2"
                shift 2
                ;;
            --profile)
                DEFAULT_PROFILE="$2"
                shift 2
                ;;
            --daemon)
                RUN_MODE="daemon"
                shift
                ;;
            --dev)
                DEFAULT_PROFILE="native,development"
                shift
                ;;
            --test)
                RUN_MODE="test"
                shift
                ;;
            --benchmark)
                RUN_MODE="benchmark"
                shift
                ;;
            --help)
                show_help
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                show_help
                exit 1
                ;;
        esac
    done
}

# Check system requirements
check_system_requirements() {
    log_header "🔍 Checking System Requirements"
    
    # Check available memory
    local available_memory
    if command -v free >/dev/null 2>&1; then
        available_memory=$(free -m | awk 'NR==2{printf "%.0f", $2}')
    elif command -v vm_stat >/dev/null 2>&1; then
        # macOS
        local pages=$(vm_stat | grep "Pages free" | awk '{print $3}' | tr -d '.')
        local page_size=$(vm_stat | grep "page size" | awk '{print $8}')
        available_memory=$((pages * page_size / 1024 / 1024))
    else
        available_memory=8192  # Assume 8GB
    fi
    
    log_info "Available memory: ${available_memory}MB"
    
    if [ "$available_memory" -lt 4096 ]; then
        log_warning "Low memory detected. Native image compilation may be slow or fail."
        log_warning "Recommended: 8GB+ RAM for optimal performance"
    fi
    
    # Check disk space
    local available_space=$(df -m "$PROJECT_ROOT" | awk 'NR==2 {print $4}')
    log_info "Available disk space: ${available_space}MB"
    
    if [ "$available_space" -lt 2048 ]; then
        log_warning "Low disk space. At least 2GB recommended for compilation."
    fi
    
    # Check required tools
    local missing_tools=()
    
    for tool in curl tar java javac; do
        if ! command -v "$tool" >/dev/null 2>&1; then
            missing_tools+=("$tool")
        fi
    done
    
    if [ ${#missing_tools[@]} -ne 0 ]; then
        log_error "Missing required tools: ${missing_tools[*]}"
        exit 1
    fi
    
    log_success "System requirements check completed"
}

# Setup GraalVM
setup_graalvm() {
    if [ "$SKIP_SETUP" = true ]; then
        log_info "Skipping GraalVM setup"
        return 0
    fi
    
    log_header "🔧 Setting Up GraalVM"
    
    if [ -f "$GRAALVM_ENV" ]; then
        source "$GRAALVM_ENV"
        if command -v native-image >/dev/null 2>&1; then
            log_info "GraalVM already installed and configured"
            return 0
        fi
    fi
    
    log_info "Running GraalVM setup script..."
    "$SCRIPT_DIR/graalvm-setup.sh"
    
    log_success "GraalVM setup completed"
}

# Build native image
build_native_image() {
    if [ "$SKIP_BUILD" = true ]; then
        log_info "Skipping native image build"
        return 0
    fi
    
    log_header "🔨 Building Native Image"
    
    log_info "Running native image build script..."
    "$SCRIPT_DIR/graalvm-build.sh"
    
    log_success "Native image build completed"
}

# Start services
start_services() {
    log_header "🚀 Starting Required Services"
    
    # Check if PostgreSQL is running
    if command -v pg_isready >/dev/null 2>&1; then
        if ! pg_isready -q; then
            log_info "Starting PostgreSQL..."
            if command -v systemctl >/dev/null 2>&1; then
                sudo systemctl start postgresql || true
            elif command -v service >/dev/null 2>&1; then
                sudo service postgresql start || true
            elif command -v brew >/dev/null 2>&1; then
                brew services start postgresql@16 || true
            fi
        fi
        log_success "PostgreSQL is running"
    else
        log_warning "PostgreSQL not found or not configured"
    fi
    
    # Check if Redis is running
    if command -v redis-cli >/dev/null 2>&1; then
        if ! redis-cli ping >/dev/null 2>&1; then
            log_info "Starting Redis..."
            if command -v systemctl >/dev/null 2>&1; then
                sudo systemctl start redis || true
            elif command -v service >/dev/null 2>&1; then
                sudo service redis-server start || true
            elif command -v brew >/dev/null 2>&1; then
                brew services start redis || true
            else
                redis-server --daemonize yes || true
            fi
        fi
        log_success "Redis is running"
    else
        log_warning "Redis not found or not configured"
    fi
}

# Run application
run_application() {
    log_header "🏦 Starting Enterprise Loan Management System"
    
    local native_executable="$NATIVE_BUILD_DIR/enterprise-loan-management-native"
    
    if [ ! -f "$native_executable" ]; then
        log_error "Native executable not found: $native_executable"
        log_error "Please run build first or use --build-only option"
        exit 1
    fi
    
    # Set environment variables
    export DATABASE_URL="${DATABASE_URL:-jdbc:postgresql://localhost:5432/banking_system}"
    export REDIS_URL="${REDIS_URL:-redis://localhost:6379}"
    export SERVER_PORT="$DEFAULT_PORT"
    export SPRING_PROFILES_ACTIVE="$DEFAULT_PROFILE"
    
    # Display startup information
    echo "========================================================================="
    echo "🏦 Enterprise Loan Management System - Native Mode"
    echo "========================================================================="
    echo "Version: 1.0.0"
    echo "Mode: Native Compilation (GraalVM)"
    echo "Port: $DEFAULT_PORT"
    echo "Profiles: $DEFAULT_PROFILE"
    echo "Database: $DATABASE_URL"
    echo "Redis: $REDIS_URL"
    echo "Executable: $native_executable"
    echo "========================================================================="
    echo
    
    # Start the application
    case "$RUN_MODE" in
        "test")
            log_info "Running in test mode..."
            "$native_executable" \
                --spring.profiles.active=native,test \
                --server.port=0 \
                --spring.main.web-application-type=none \
                --spring.application.name="Test Mode" &
            local app_pid=$!
            sleep 10
            if kill -0 $app_pid 2>/dev/null; then
                log_success "Test run successful"
                kill $app_pid
            else
                log_error "Test run failed"
                exit 1
            fi
            ;;
        "benchmark")
            log_info "Running performance benchmark..."
            local start_time=$(date +%s%N)
            "$native_executable" \
                --spring.profiles.active=native,test \
                --server.port=0 \
                --spring.main.web-application-type=none &
            local app_pid=$!
            sleep 5
            kill $app_pid 2>/dev/null || true
            local end_time=$(date +%s%N)
            local startup_time=$((($end_time - $start_time) / 1000000))
            echo "Startup Time: ${startup_time}ms"
            echo "Memory Footprint: $(du -h "$native_executable" | cut -f1)"
            log_success "Benchmark completed"
            ;;
        "daemon")
            log_info "Starting in daemon mode..."
            nohup "$native_executable" \
                --server.port="$DEFAULT_PORT" \
                --server.address=0.0.0.0 \
                --spring.profiles.active="$DEFAULT_PROFILE" > "$BUILD_DIR/application.log" 2>&1 &
            echo $! > "$BUILD_DIR/application.pid"
            log_success "Application started in daemon mode (PID: $(cat "$BUILD_DIR/application.pid"))"
            echo "Log file: $BUILD_DIR/application.log"
            ;;
        *)
            log_info "Starting in interactive mode..."
            echo "Access URLs:"
            echo "  • Main Application: http://0.0.0.0:$DEFAULT_PORT"
            echo "  • API Documentation: http://0.0.0.0:$DEFAULT_PORT/swagger-ui.html"
            echo "  • Health Check: http://0.0.0.0:$DEFAULT_PORT/actuator/health"
            echo "  • Risk Dashboard: http://0.0.0.0:$DEFAULT_PORT/risk-dashboard.html"
            echo
            echo "Press Ctrl+C to stop the application"
            echo "========================================================================="
            
            # Start with proper signal handling
            trap 'echo "Shutting down..."; exit 0' INT TERM
            
            "$native_executable" \
                --server.port="$DEFAULT_PORT" \
                --server.address=0.0.0.0 \
                --spring.profiles.active="$DEFAULT_PROFILE" \
                --management.endpoints.web.exposure.include=health,info,metrics,prometheus \
                --management.endpoint.health.show-details=always \
                --logging.level.com.bank.loanmanagement=INFO
            ;;
    esac
}

# Main boot procedure
main() {
    show_banner
    parse_arguments "$@"
    
    # Execute based on run mode
    case "$RUN_MODE" in
        "setup")
            check_system_requirements
            setup_graalvm
            log_success "GraalVM setup completed"
            ;;
        "build")
            check_system_requirements
            setup_graalvm
            build_native_image
            log_success "Native image build completed"
            ;;
        *)
            check_system_requirements
            setup_graalvm
            build_native_image
            start_services
            run_application
            ;;
    esac
}

# Run main function with all arguments
main "$@"
