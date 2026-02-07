#!/bin/bash

# =====================================================
# Java 25 Health Check Script
# Enterprise Loan Management System
# =====================================================

set -euo pipefail

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SERVICE_URL="${SERVICE_URL:-http://localhost:8080}"
TIMEOUT="${TIMEOUT:-30}"
RETRIES="${RETRIES:-3}"
RETRY_DELAY="${RETRY_DELAY:-5}"
HEALTH_ENDPOINT="/actuator/health"
METRICS_ENDPOINT="/actuator/metrics"
INFO_ENDPOINT="/actuator/info"

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

# Utility functions
make_request() {
    local url="$1"
    local timeout="${2:-$TIMEOUT}"
    
    curl -sf --max-time "$timeout" "$url" 2>/dev/null
}

retry_request() {
    local url="$1"
    local retries="${2:-$RETRIES}"
    local delay="${3:-$RETRY_DELAY}"
    
    for ((i=1; i<=retries; i++)); do
        if make_request "$url"; then
            return 0
        fi
        
        if [[ $i -lt $retries ]]; then
            log_warning "Request failed, retrying in ${delay}s... (attempt $i/$retries)"
            sleep "$delay"
        fi
    done
    
    return 1
}

get_metric_value() {
    local metric_name="$1"
    local statistic="${2:-VALUE}"
    
    local response=$(make_request "${SERVICE_URL}${METRICS_ENDPOINT}/${metric_name}")
    if [[ -n "$response" ]]; then
        echo "$response" | jq -r ".measurements[] | select(.statistic==\"${statistic}\") | .value // null"
    else
        echo "null"
    fi
}

# Health check functions
check_basic_health() {
    log_info "Checking basic application health..."
    
    if ! retry_request "${SERVICE_URL}${HEALTH_ENDPOINT}"; then
        log_error "Basic health check failed"
        return 1
    fi
    
    local health_response=$(make_request "${SERVICE_URL}${HEALTH_ENDPOINT}")
    local status=$(echo "$health_response" | jq -r '.status // "UNKNOWN"')
    
    if [[ "$status" == "UP" ]]; then
        log_success "Basic health check passed - Status: $status"
        return 0
    else
        log_error "Basic health check failed - Status: $status"
        echo "$health_response" | jq .
        return 1
    fi
}

check_detailed_health() {
    log_info "Checking detailed health components..."
    
    local health_response=$(make_request "${SERVICE_URL}${HEALTH_ENDPOINT}")
    if [[ -z "$health_response" ]]; then
        log_error "Cannot retrieve detailed health information"
        return 1
    fi
    
    local components=$(echo "$health_response" | jq -r '.components // {} | keys[]')
    local failed_components=()
    
    while IFS= read -r component; do
        if [[ -n "$component" ]]; then
            local component_status=$(echo "$health_response" | jq -r ".components.${component}.status // \"UNKNOWN\"")
            
            if [[ "$component_status" == "UP" ]]; then
                log_success "Component $component: $component_status"
            else
                log_error "Component $component: $component_status"
                failed_components+=("$component")
            fi
        fi
    done <<< "$components"
    
    if [[ ${#failed_components[@]} -eq 0 ]]; then
        log_success "All health components are UP"
        return 0
    else
        log_error "Failed components: ${failed_components[*]}"
        return 1
    fi
}

check_java25_features() {
    log_info "Checking Java 25 specific features..."
    
    # Check Java version
    local info_response=$(make_request "${SERVICE_URL}${INFO_ENDPOINT}")
    if [[ -n "$info_response" ]]; then
        local java_version=$(echo "$info_response" | jq -r '.java.version // "unknown"')
        if [[ "$java_version" == *"21"* ]]; then
            log_success "Java version: $java_version"
        else
            log_warning "Unexpected Java version: $java_version"
        fi
    fi
    
    # Check Virtual Threads
    local vt_active=$(get_metric_value "virtual.threads.active")
    if [[ "$vt_active" != "null" ]] && [[ "$vt_active" != "0" ]]; then
        log_success "Virtual Threads active: $vt_active"
    else
        log_warning "Virtual Threads not detected or inactive"
    fi
    
    # Check Pattern Matching operations
    local pm_operations=$(get_metric_value "pattern.matching.operations")
    if [[ "$pm_operations" != "null" ]] && [[ "$pm_operations" != "0" ]]; then
        log_success "Pattern Matching operations: $pm_operations"
    else
        log_warning "Pattern Matching operations not detected"
    fi
    
    # Check Sequenced Collections usage
    local sc_size=$(get_metric_value "sequenced.collections.size")
    if [[ "$sc_size" != "null" ]] && [[ "$sc_size" != "0" ]]; then
        log_success "Sequenced Collections size: $sc_size"
    else
        log_warning "Sequenced Collections not detected or empty"
    fi
}

check_performance_metrics() {
    log_info "Checking performance metrics..."
    
    # CPU Usage
    local cpu_usage=$(get_metric_value "system.cpu.usage")
    if [[ "$cpu_usage" != "null" ]]; then
        local cpu_percent=$(echo "$cpu_usage * 100" | bc -l)
        if (( $(echo "$cpu_percent < 80" | bc -l) )); then
            log_success "CPU usage: $(printf "%.1f" "$cpu_percent")%"
        else
            log_warning "High CPU usage: $(printf "%.1f" "$cpu_percent")%"
        fi
    fi
    
    # Memory Usage
    local memory_used=$(get_metric_value "jvm.memory.used")
    local memory_max=$(get_metric_value "jvm.memory.max")
    if [[ "$memory_used" != "null" ]] && [[ "$memory_max" != "null" ]] && [[ "$memory_max" != "0" ]]; then
        local memory_percent=$(echo "scale=1; ($memory_used / $memory_max) * 100" | bc)
        if (( $(echo "$memory_percent < 85" | bc -l) )); then
            log_success "Memory usage: ${memory_percent}%"
        else
            log_warning "High memory usage: ${memory_percent}%"
        fi
    fi
    
    # GC Metrics
    local gc_pause=$(get_metric_value "jvm.gc.pause" "MAX")
    if [[ "$gc_pause" != "null" ]]; then
        local gc_pause_ms=$(echo "$gc_pause * 1000" | bc -l)
        if (( $(echo "$gc_pause_ms < 100" | bc -l) )); then
            log_success "GC max pause: $(printf "%.0f" "$gc_pause_ms")ms"
        else
            log_warning "High GC pause time: $(printf "%.0f" "$gc_pause_ms")ms"
        fi
    fi
    
    # HTTP Response Time
    local response_time_p95=$(get_metric_value "http.server.requests" "0.95")
    if [[ "$response_time_p95" != "null" ]]; then
        local response_time_ms=$(echo "$response_time_p95 * 1000" | bc -l)
        if (( $(echo "$response_time_ms < 500" | bc -l) )); then
            log_success "HTTP P95 response time: $(printf "%.0f" "$response_time_ms")ms"
        else
            log_warning "High HTTP P95 response time: $(printf "%.0f" "$response_time_ms")ms"
        fi
    fi
    
    # Error Rate
    local error_rate=$(get_metric_value "http.server.requests.error.rate")
    if [[ "$error_rate" != "null" ]]; then
        local error_percent=$(echo "$error_rate * 100" | bc -l)
        if (( $(echo "$error_percent < 1" | bc -l) )); then
            log_success "Error rate: $(printf "%.2f" "$error_percent")%"
        else
            log_warning "High error rate: $(printf "%.2f" "$error_percent")%"
        fi
    fi
}

check_banking_operations() {
    log_info "Checking banking operation metrics..."
    
    # Loan Processing Rate
    local loan_rate=$(get_metric_value "banking.loan.processing.rate")
    if [[ "$loan_rate" != "null" ]] && [[ "$loan_rate" != "0" ]]; then
        log_success "Loan processing rate: $(printf "%.1f" "$loan_rate")/sec"
    else
        log_warning "No loan processing activity detected"
    fi
    
    # Payment Processing Rate
    local payment_rate=$(get_metric_value "banking.payment.processing.rate")
    if [[ "$payment_rate" != "null" ]] && [[ "$payment_rate" != "0" ]]; then
        log_success "Payment processing rate: $(printf "%.1f" "$payment_rate")/sec"
    else
        log_warning "No payment processing activity detected"
    fi
    
    # Risk Assessment Rate
    local risk_rate=$(get_metric_value "banking.risk.assessment.rate")
    if [[ "$risk_rate" != "null" ]] && [[ "$risk_rate" != "0" ]]; then
        log_success "Risk assessment rate: $(printf "%.1f" "$risk_rate")/sec"
    else
        log_warning "No risk assessment activity detected"
    fi
    
    # Fraud Detection Rate
    local fraud_rate=$(get_metric_value "banking.fraud.detection.rate")
    if [[ "$fraud_rate" != "null" ]] && [[ "$fraud_rate" != "0" ]]; then
        log_success "Fraud detection rate: $(printf "%.1f" "$fraud_rate")/sec"
    else
        log_warning "No fraud detection activity detected"
    fi
}

check_database_connectivity() {
    log_info "Checking database connectivity..."
    
    local health_response=$(make_request "${SERVICE_URL}${HEALTH_ENDPOINT}")
    if [[ -n "$health_response" ]]; then
        local db_status=$(echo "$health_response" | jq -r '.components.db.status // "UNKNOWN"')
        
        if [[ "$db_status" == "UP" ]]; then
            log_success "Database connectivity: $db_status"
            
            # Check connection pool metrics
            local pool_active=$(get_metric_value "hikaricp.connections.active")
            local pool_max=$(get_metric_value "hikaricp.connections.max")
            
            if [[ "$pool_active" != "null" ]] && [[ "$pool_max" != "null" ]]; then
                log_success "Connection pool: $pool_active/$pool_max active connections"
            fi
        else
            log_error "Database connectivity: $db_status"
            return 1
        fi
    else
        log_error "Cannot check database connectivity"
        return 1
    fi
}

check_cache_connectivity() {
    log_info "Checking cache connectivity..."
    
    local health_response=$(make_request "${SERVICE_URL}${HEALTH_ENDPOINT}")
    if [[ -n "$health_response" ]]; then
        local redis_status=$(echo "$health_response" | jq -r '.components.redis.status // "UNKNOWN"')
        
        if [[ "$redis_status" == "UP" ]]; then
            log_success "Cache connectivity: $redis_status"
        elif [[ "$redis_status" == "UNKNOWN" ]]; then
            log_warning "Cache status unknown - may not be configured"
        else
            log_error "Cache connectivity: $redis_status"
            return 1
        fi
    else
        log_error "Cannot check cache connectivity"
        return 1
    fi
}

# Comprehensive health check
run_comprehensive_health_check() {
    local overall_status=0
    
    echo "==============================================================================="
    echo "                         Java 25 Health Check Report"
    echo "==============================================================================="
    echo "Service URL: $SERVICE_URL"
    echo "Timestamp: $(date)"
    echo
    
    # Basic Health
    if ! check_basic_health; then
        overall_status=1
    fi
    echo
    
    # Detailed Health Components
    if ! check_detailed_health; then
        overall_status=1
    fi
    echo
    
    # Java 25 Features
    check_java25_features
    echo
    
    # Performance Metrics
    check_performance_metrics
    echo
    
    # Banking Operations
    check_banking_operations
    echo
    
    # Database Connectivity
    if ! check_database_connectivity; then
        overall_status=1
    fi
    echo
    
    # Cache Connectivity
    if ! check_cache_connectivity; then
        overall_status=1
    fi
    echo
    
    echo "==============================================================================="
    if [[ $overall_status -eq 0 ]]; then
        log_success "Overall health check: PASSED"
    else
        log_error "Overall health check: FAILED"
    fi
    echo "==============================================================================="
    
    return $overall_status
}

# Quick health check
run_quick_health_check() {
    log_info "Running quick health check..."
    
    if check_basic_health; then
        log_success "Quick health check: PASSED"
        return 0
    else
        log_error "Quick health check: FAILED"
        return 1
    fi
}

# Readiness check (for Kubernetes)
run_readiness_check() {
    log_info "Running readiness check..."
    
    # Check basic health
    if ! make_request "${SERVICE_URL}${HEALTH_ENDPOINT}/readiness" >/dev/null; then
        log_error "Readiness check failed"
        return 1
    fi
    
    # Check critical metrics are available
    local vt_active=$(get_metric_value "virtual.threads.active")
    if [[ "$vt_active" == "null" ]]; then
        log_error "Virtual Threads metrics not available"
        return 1
    fi
    
    log_success "Readiness check: PASSED"
    return 0
}

# Liveness check (for Kubernetes)
run_liveness_check() {
    log_info "Running liveness check..."
    
    if make_request "${SERVICE_URL}${HEALTH_ENDPOINT}/liveness" >/dev/null; then
        log_success "Liveness check: PASSED"
        return 0
    else
        log_error "Liveness check: FAILED"
        return 1
    fi
}

# Command line interface
show_help() {
    cat <<EOF
Usage: $0 [OPTIONS] [CHECK_TYPE]

Check Types:
  comprehensive   Run comprehensive health check (default)
  quick          Run quick health check
  readiness      Run Kubernetes readiness check
  liveness       Run Kubernetes liveness check

Options:
  --url URL      Service URL (default: http://localhost:8080)
  --timeout SEC  Request timeout in seconds (default: 30)
  --retries NUM  Number of retries for failed requests (default: 3)
  --delay SEC    Delay between retries in seconds (default: 5)
  --color COLOR  Blue-green deployment color (blue|green)
  --quiet        Suppress non-essential output
  --json         Output results in JSON format
  --help         Show this help message

Examples:
  $0 comprehensive
  $0 quick --url http://prod-service:8080
  $0 readiness --timeout 10 --retries 1
  $0 liveness --quiet

EOF
}

# Parse command line arguments
CHECK_TYPE="comprehensive"
QUIET_MODE=false
JSON_OUTPUT=false
COLOR=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --url)
            SERVICE_URL="$2"
            shift 2
            ;;
        --timeout)
            TIMEOUT="$2"
            shift 2
            ;;
        --retries)
            RETRIES="$2"
            shift 2
            ;;
        --delay)
            RETRY_DELAY="$2"
            shift 2
            ;;
        --color)
            COLOR="$2"
            shift 2
            ;;
        --quiet)
            QUIET_MODE=true
            shift
            ;;
        --json)
            JSON_OUTPUT=true
            shift
            ;;
        comprehensive|quick|readiness|liveness)
            CHECK_TYPE="$1"
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

# Adjust service URL for blue-green deployment
if [[ -n "$COLOR" ]]; then
    SERVICE_URL="${SERVICE_URL%-*}-${COLOR}"
fi

# Suppress output if quiet mode
if [[ "$QUIET_MODE" == "true" ]]; then
    exec >/dev/null 2>&1
fi

# Execute health check
case $CHECK_TYPE in
    comprehensive)
        run_comprehensive_health_check
        ;;
    quick)
        run_quick_health_check
        ;;
    readiness)
        run_readiness_check
        ;;
    liveness)
        run_liveness_check
        ;;
    *)
        log_error "Unknown check type: $CHECK_TYPE"
        show_help
        exit 1
        ;;
esac
