#!/bin/bash

# =====================================================
# Java 25 Performance Monitoring Script
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
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICE_URL="${SERVICE_URL:-http://localhost:8080}"
MONITORING_DURATION="${MONITORING_DURATION:-60}"
METRICS_INTERVAL="${METRICS_INTERVAL:-5}"
ALERT_THRESHOLD_CPU="${ALERT_THRESHOLD_CPU:-80}"
ALERT_THRESHOLD_MEMORY="${ALERT_THRESHOLD_MEMORY:-85}"
ALERT_THRESHOLD_ERROR_RATE="${ALERT_THRESHOLD_ERROR_RATE:-3}"

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
get_metric_value() {
    local metric_name="$1"
    local statistic="${2:-VALUE}"
    
    curl -s "${SERVICE_URL}/actuator/metrics/${metric_name}" | \
        jq -r ".measurements[] | select(.statistic=\"${statistic}\") | .value // 0"
}

get_simple_metric() {
    local metric_name="$1"
    
    curl -s "${SERVICE_URL}/actuator/metrics/${metric_name}" | \
        jq -r '.measurements[0].value // 0'
}

format_number() {
    local number="$1"
    local decimals="${2:-2}"
    
    printf "%.${decimals}f" "$number"
}

format_bytes() {
    local bytes="$1"
    
    if (( $(echo "$bytes >= 1073741824" | bc -l) )); then
        echo "$(echo "scale=2; $bytes / 1073741824" | bc)GB"
    elif (( $(echo "$bytes >= 1048576" | bc -l) )); then
        echo "$(echo "scale=2; $bytes / 1048576" | bc)MB"
    elif (( $(echo "$bytes >= 1024" | bc -l) )); then
        echo "$(echo "scale=2; $bytes / 1024" | bc)KB"
    else
        echo "${bytes}B"
    fi
}

# Performance monitoring functions
get_jvm_metrics() {
    local cpu_usage=$(get_metric_value "system.cpu.usage" "VALUE")
    local memory_used=$(get_metric_value "jvm.memory.used" "VALUE")
    local memory_max=$(get_metric_value "jvm.memory.max" "VALUE")
    local gc_pause=$(get_metric_value "jvm.gc.pause" "MAX")
    local gc_collections=$(get_metric_value "jvm.gc.pause" "COUNT")
    
    local memory_usage_percent=$(echo "scale=2; ($memory_used / $memory_max) * 100" | bc)
    
    echo "JVM_CPU:$(format_number "$(echo "$cpu_usage * 100" | bc)" 1)"
    echo "JVM_MEMORY_USED:$(format_bytes "$memory_used")"
    echo "JVM_MEMORY_MAX:$(format_bytes "$memory_max")"
    echo "JVM_MEMORY_PERCENT:$(format_number "$memory_usage_percent" 1)"
    echo "JVM_GC_PAUSE_MAX:$(format_number "$(echo "$gc_pause * 1000" | bc)" 0)ms"
    echo "JVM_GC_COLLECTIONS:$(format_number "$gc_collections" 0)"
}

get_virtual_threads_metrics() {
    local active_threads=$(get_simple_metric "virtual.threads.active")
    local created_threads=$(get_simple_metric "virtual.threads.created")
    local completed_threads=$(get_simple_metric "virtual.threads.completed")
    local creation_rate=$(get_simple_metric "virtual.threads.creation.rate")
    
    echo "VT_ACTIVE:$(format_number "$active_threads" 0)"
    echo "VT_CREATED:$(format_number "$created_threads" 0)"
    echo "VT_COMPLETED:$(format_number "$completed_threads" 0)"
    echo "VT_CREATION_RATE:$(format_number "$creation_rate" 1)/sec"
}

get_pattern_matching_metrics() {
    local operations=$(get_simple_metric "pattern.matching.operations")
    local latency_p95=$(get_metric_value "pattern.matching.latency" "0.95")
    local latency_p99=$(get_metric_value "pattern.matching.latency" "0.99")
    local success_rate=$(get_simple_metric "pattern.matching.success.rate")
    
    echo "PM_OPERATIONS:$(format_number "$operations" 0)"
    echo "PM_LATENCY_P95:$(format_number "$(echo "$latency_p95 * 1000" | bc)" 0)ms"
    echo "PM_LATENCY_P99:$(format_number "$(echo "$latency_p99 * 1000" | bc)" 0)ms"
    echo "PM_SUCCESS_RATE:$(format_number "$(echo "$success_rate * 100" | bc)" 1)%"
}

get_banking_metrics() {
    local loan_processing_rate=$(get_simple_metric "banking.loan.processing.rate")
    local payment_processing_rate=$(get_simple_metric "banking.payment.processing.rate")
    local risk_assessment_rate=$(get_simple_metric "banking.risk.assessment.rate")
    local fraud_detection_rate=$(get_simple_metric "banking.fraud.detection.rate")
    
    local loan_latency=$(get_metric_value "banking.loan.processing.time" "MEAN")
    local payment_latency=$(get_metric_value "banking.payment.processing.time" "MEAN")
    
    echo "LOAN_RATE:$(format_number "$loan_processing_rate" 1)/sec"
    echo "PAYMENT_RATE:$(format_number "$payment_processing_rate" 1)/sec"
    echo "RISK_RATE:$(format_number "$risk_assessment_rate" 1)/sec"
    echo "FRAUD_RATE:$(format_number "$fraud_detection_rate" 1)/sec"
    echo "LOAN_LATENCY:$(format_number "$(echo "$loan_latency * 1000" | bc)" 0)ms"
    echo "PAYMENT_LATENCY:$(format_number "$(echo "$payment_latency * 1000" | bc)" 0)ms"
}

get_sequenced_collections_metrics() {
    local collection_size=$(get_simple_metric "sequenced.collections.size")
    local insertion_rate=$(get_simple_metric "sequenced.collections.insertion.rate")
    local first_access_time=$(get_metric_value "sequenced.collections.first.access.time" "MEAN")
    local last_access_time=$(get_metric_value "sequenced.collections.last.access.time" "MEAN")
    
    echo "SC_SIZE:$(format_number "$collection_size" 0)"
    echo "SC_INSERTION_RATE:$(format_number "$insertion_rate" 1)/sec"
    echo "SC_FIRST_ACCESS:$(format_number "$(echo "$first_access_time * 1000000" | bc)" 0)μs"
    echo "SC_LAST_ACCESS:$(format_number "$(echo "$last_access_time * 1000000" | bc)" 0)μs"
}

get_application_metrics() {
    local requests_total=$(get_metric_value "http.server.requests" "COUNT")
    local requests_rate=$(get_simple_metric "http.server.requests.rate")
    local response_time_p95=$(get_metric_value "http.server.requests" "0.95")
    local response_time_p99=$(get_metric_value "http.server.requests" "0.99")
    local error_rate=$(get_simple_metric "http.server.requests.error.rate")
    
    echo "HTTP_REQUESTS:$(format_number "$requests_total" 0)"
    echo "HTTP_RATE:$(format_number "$requests_rate" 1)/sec"
    echo "HTTP_P95:$(format_number "$(echo "$response_time_p95 * 1000" | bc)" 0)ms"
    echo "HTTP_P99:$(format_number "$(echo "$response_time_p99 * 1000" | bc)" 0)ms"
    echo "HTTP_ERROR_RATE:$(format_number "$(echo "$error_rate * 100" | bc)" 2)%"
}

# Alert functions
check_alerts() {
    local jvm_cpu=$(echo "$1" | grep "JVM_CPU:" | cut -d: -f2)
    local jvm_memory_percent=$(echo "$1" | grep "JVM_MEMORY_PERCENT:" | cut -d: -f2)
    local error_rate=$(echo "$1" | grep "HTTP_ERROR_RATE:" | cut -d: -f2 | tr -d '%')
    
    local alerts_triggered=0
    
    if (( $(echo "$jvm_cpu > $ALERT_THRESHOLD_CPU" | bc -l) )); then
        log_warning "HIGH CPU USAGE: ${jvm_cpu}% (threshold: ${ALERT_THRESHOLD_CPU}%)"
        alerts_triggered=$((alerts_triggered + 1))
    fi
    
    if (( $(echo "$jvm_memory_percent > $ALERT_THRESHOLD_MEMORY" | bc -l) )); then
        log_warning "HIGH MEMORY USAGE: ${jvm_memory_percent}% (threshold: ${ALERT_THRESHOLD_MEMORY}%)"
        alerts_triggered=$((alerts_triggered + 1))
    fi
    
    if (( $(echo "$error_rate > $ALERT_THRESHOLD_ERROR_RATE" | bc -l) )); then
        log_error "HIGH ERROR RATE: ${error_rate}% (threshold: ${ALERT_THRESHOLD_ERROR_RATE}%)"
        alerts_triggered=$((alerts_triggered + 1))
    fi
    
    return $alerts_triggered
}

# Display functions
display_metrics() {
    clear
    echo "==============================================================================="
    echo "                    Java 25 Performance Monitoring Dashboard"
    echo "==============================================================================="
    echo "Service URL: $SERVICE_URL"
    echo "Monitoring Duration: ${MONITORING_DURATION}s | Interval: ${METRICS_INTERVAL}s"
    echo "Last Updated: $(date)"
    echo
    
    # JVM Metrics
    echo "${BLUE}JVM Metrics:${NC}"
    local jvm_metrics=$(get_jvm_metrics)
    echo "$jvm_metrics" | while IFS=':' read -r key value; do
        printf "  %-20s: %s\n" "$key" "$value"
    done
    echo
    
    # Virtual Threads Metrics
    echo "${GREEN}Virtual Threads:${NC}"
    local vt_metrics=$(get_virtual_threads_metrics)
    echo "$vt_metrics" | while IFS=':' read -r key value; do
        printf "  %-20s: %s\n" "$key" "$value"
    done
    echo
    
    # Pattern Matching Metrics
    echo "${YELLOW}Pattern Matching:${NC}"
    local pm_metrics=$(get_pattern_matching_metrics)
    echo "$pm_metrics" | while IFS=':' read -r key value; do
        printf "  %-20s: %s\n" "$key" "$value"
    done
    echo
    
    # Banking Operations
    echo "${RED}Banking Operations:${NC}"
    local banking_metrics=$(get_banking_metrics)
    echo "$banking_metrics" | while IFS=':' read -r key value; do
        printf "  %-20s: %s\n" "$key" "$value"
    done
    echo
    
    # Sequenced Collections
    echo "${BLUE}Sequenced Collections:${NC}"
    local sc_metrics=$(get_sequenced_collections_metrics)
    echo "$sc_metrics" | while IFS=':' read -r key value; do
        printf "  %-20s: %s\n" "$key" "$value"
    done
    echo
    
    # Application Metrics
    echo "${GREEN}Application Metrics:${NC}"
    local app_metrics=$(get_application_metrics)
    echo "$app_metrics" | while IFS=':' read -r key value; do
        printf "  %-20s: %s\n" "$key" "$value"
    done
    echo
    
    # Check for alerts
    local all_metrics="$jvm_metrics\n$app_metrics"
    if ! check_alerts "$all_metrics"; then
        log_success "All metrics within normal thresholds"
    fi
    
    echo "==============================================================================="
    echo "Press Ctrl+C to stop monitoring"
}

# Main monitoring loop
start_monitoring() {
    log_info "Starting Java 25 performance monitoring..."
    log_info "Service URL: $SERVICE_URL"
    log_info "Duration: ${MONITORING_DURATION}s, Interval: ${METRICS_INTERVAL}s"
    
    # Check if service is accessible
    if ! curl -sf "$SERVICE_URL/actuator/health" >/dev/null 2>&1; then
        log_error "Service is not accessible at $SERVICE_URL"
        exit 1
    fi
    
    local start_time=$(date +%s)
    local end_time=$((start_time + MONITORING_DURATION))
    
    # Create log file
    local log_file="java25-performance-$(date +%Y%m%d-%H%M%S).log"
    
    while [[ $(date +%s) -lt $end_time ]]; do
        display_metrics | tee -a "$log_file"
        sleep "$METRICS_INTERVAL"
    done
    
    log_success "Monitoring completed. Log saved to: $log_file"
}

# Generate performance report
generate_report() {
    local output_file="${1:-java25-performance-report-$(date +%Y%m%d-%H%M%S).json}"
    
    log_info "Generating performance report..."
    
    local report=$(cat <<EOF
{
  "timestamp": "$(date -Iseconds)",
  "service_url": "$SERVICE_URL",
  "jvm_metrics": {
$(get_jvm_metrics | sed 's/:/": "/' | sed 's/^/    "/' | sed 's/$/",/' | sed '$s/,$//')
  },
  "virtual_threads": {
$(get_virtual_threads_metrics | sed 's/:/": "/' | sed 's/^/    "/' | sed 's/$/",/' | sed '$s/,$//')
  },
  "pattern_matching": {
$(get_pattern_matching_metrics | sed 's/:/": "/' | sed 's/^/    "/' | sed 's/$/",/' | sed '$s/,$//')
  },
  "banking_operations": {
$(get_banking_metrics | sed 's/:/": "/' | sed 's/^/    "/' | sed 's/$/",/' | sed '$s/,$//')
  },
  "sequenced_collections": {
$(get_sequenced_collections_metrics | sed 's/:/": "/' | sed 's/^/    "/' | sed 's/$/",/' | sed '$s/,$//')
  },
  "application_metrics": {
$(get_application_metrics | sed 's/:/": "/' | sed 's/^/    "/' | sed 's/$/",/' | sed '$s/,$//')
  }
}
EOF
    )
    
    echo "$report" | jq . > "$output_file"
    log_success "Performance report saved to: $output_file"
}

# Command line interface
show_help() {
    cat <<EOF
Usage: $0 [OPTIONS] [COMMAND]

Commands:
  monitor     Start interactive monitoring dashboard (default)
  report      Generate one-time performance report
  help        Show this help message

Options:
  --url URL               Service URL (default: http://localhost:8080)
  --duration SECONDS      Monitoring duration in seconds (default: 60)
  --interval SECONDS      Metrics collection interval (default: 5)
  --cpu-threshold PERCENT CPU usage alert threshold (default: 80)
  --memory-threshold PERCENT Memory usage alert threshold (default: 85)
  --error-threshold PERCENT Error rate alert threshold (default: 3)
  --output FILE           Output file for report command

Examples:
  $0 monitor --duration 300 --interval 10
  $0 report --output my-report.json
  $0 --url http://prod-service:8080 monitor

EOF
}

# Parse command line arguments
COMMAND="monitor"
OUTPUT_FILE=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --url)
            SERVICE_URL="$2"
            shift 2
            ;;
        --duration)
            MONITORING_DURATION="$2"
            shift 2
            ;;
        --interval)
            METRICS_INTERVAL="$2"
            shift 2
            ;;
        --cpu-threshold)
            ALERT_THRESHOLD_CPU="$2"
            shift 2
            ;;
        --memory-threshold)
            ALERT_THRESHOLD_MEMORY="$2"
            shift 2
            ;;
        --error-threshold)
            ALERT_THRESHOLD_ERROR_RATE="$2"
            shift 2
            ;;
        --output)
            OUTPUT_FILE="$2"
            shift 2
            ;;
        monitor|report|help)
            COMMAND="$1"
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

# Execute command
case $COMMAND in
    monitor)
        start_monitoring
        ;;
    report)
        generate_report "$OUTPUT_FILE"
        ;;
    help)
        show_help
        ;;
    *)
        log_error "Unknown command: $COMMAND"
        show_help
        exit 1
        ;;
esac
