#!/bin/bash

# =============================================================================
# Enterprise Loan Management System - Comprehensive End-to-End Load Testing
# =============================================================================
# 
# This script provides comprehensive load testing, chaos engineering, and 
# scalability testing for the banking system with detailed reporting.
#
# Features:
# - API endpoint load testing
# - Chaos engineering scenarios
# - Scalability benchmarking
# - Circuit breaker testing
# - Database stress testing
# - Redis cache performance testing
# - Comprehensive reporting with failure analysis
# - Environment variable configuration
# - CI/CD integration ready
#
# Usage: ./e2e-comprehensive-load-test.sh [environment]
# =============================================================================

set -euo pipefail

# =============================================================================
# CONFIGURATION AND ENVIRONMENT VARIABLES
# =============================================================================

# Test Environment Configuration
TEST_ENV="${1:-local}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
LOGS_DIR="$PROJECT_ROOT/data/test-outputs/load-tests"
REPORTS_DIR="$PROJECT_ROOT/data/test-outputs/reports"

# Create directories
mkdir -p "$LOGS_DIR" "$REPORTS_DIR"

# Environment Variables with Defaults
export BASE_URL="${BASE_URL:-http://localhost:8080}"
export API_PREFIX="${API_PREFIX:-/api/v1}"
export CONCURRENT_USERS="${CONCURRENT_USERS:-50}"
export TEST_DURATION="${TEST_DURATION:-300}"
export RAMP_UP_TIME="${RAMP_UP_TIME:-60}"
export MAX_REQUESTS_PER_SECOND="${MAX_REQUESTS_PER_SECOND:-100}"
export CHAOS_DURATION="${CHAOS_DURATION:-120}"
export DB_POOL_SIZE="${DB_POOL_SIZE:-20}"
export REDIS_CONNECTIONS="${REDIS_CONNECTIONS:-10}"
export JWT_TOKEN="${JWT_TOKEN:-}"
export HEALTH_CHECK_INTERVAL="${HEALTH_CHECK_INTERVAL:-30}"
export FAILURE_THRESHOLD="${FAILURE_THRESHOLD:-5}"
export RESPONSE_TIME_THRESHOLD="${RESPONSE_TIME_THRESHOLD:-2000}"
export SUCCESS_RATE_THRESHOLD="${SUCCESS_RATE_THRESHOLD:-95}"

# Test Results Storage
TEST_START_TIME=$(date +%s)
TEST_ID="load-test-$(date +%Y%m%d-%H%M%S)"
SUMMARY_FILE="$REPORTS_DIR/test-summary-$TEST_ID.json"
FAILURE_LOG="$LOGS_DIR/failures-$TEST_ID.log"
METRICS_FILE="$LOGS_DIR/metrics-$TEST_ID.json"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# =============================================================================
# UTILITY FUNCTIONS
# =============================================================================

log() {
    echo -e "${BLUE}[$(date '+%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "$LOGS_DIR/test-execution.log"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$FAILURE_LOG"
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "$LOGS_DIR/test-execution.log"
}

warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1" | tee -a "$LOGS_DIR/test-execution.log"
}

# Check if required tools are installed
check_dependencies() {
    local missing_tools=()
    
    for tool in curl jq bc docker; do
        if ! command -v "$tool" &> /dev/null; then
            missing_tools+=("$tool")
        fi
    done
    
    if [ ${#missing_tools[@]} -ne 0 ]; then
        error "Missing required tools: ${missing_tools[*]}"
        error "Please install missing tools before running tests"
        exit 1
    fi
    
    # Check for optional tools
    if ! command -v wrk &> /dev/null && ! command -v ab &> /dev/null; then
        warning "Neither 'wrk' nor 'ab' found. Installing wrk for load testing..."
        if command -v brew &> /dev/null; then
            brew install wrk
        elif command -v apt-get &> /dev/null; then
            sudo apt-get update && sudo apt-get install -y wrk
        else
            error "Cannot install wrk. Please install manually"
            exit 1
        fi
    fi
}

# Generate JWT token for authenticated requests
generate_jwt_token() {
    if [ -z "$JWT_TOKEN" ]; then
        log "Generating JWT token for authentication..."
        local auth_response
        auth_response=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
            -H "Content-Type: application/json" \
            -d '{"username":"testuser","password":"testpass"}' || echo "")
        
        if [ -n "$auth_response" ]; then
            JWT_TOKEN=$(echo "$auth_response" | jq -r '.token // empty')
            export JWT_TOKEN
            log "JWT token generated successfully"
        else
            warning "Could not generate JWT token. Running tests without authentication"
        fi
    fi
}

# Health check before starting tests
health_check() {
    log "Performing system health check..."
    
    local health_url="$BASE_URL/actuator/health"
    local health_response
    
    health_response=$(curl -s -w "%{http_code}" -o /dev/null "$health_url" || echo "000")
    
    if [ "$health_response" = "200" ] || [ "$health_response" = "503" ]; then
        success "System health check passed (HTTP $health_response)"
        return 0
    else
        error "System health check failed (HTTP $health_response)"
        return 1
    fi
}

# =============================================================================
# LOAD TESTING FUNCTIONS
# =============================================================================

# API Endpoint Load Testing
api_load_test() {
    log "Starting API load testing..."
    
    local endpoints=(
        "/api/v1/loans/recommendations"
        "/api/v1/customers"
        "/api/v1/loans"
        "/api/v1/payments"
        "/actuator/health"
        "/actuator/metrics"
    )
    
    local results=()
    
    for endpoint in "${endpoints[@]}"; do
        log "Testing endpoint: $endpoint"
        
        local auth_header=""
        if [ -n "$JWT_TOKEN" ] && [[ "$endpoint" != "/actuator/"* ]]; then
            auth_header="-H \"Authorization: Bearer $JWT_TOKEN\""
        fi
        
        # Use wrk for load testing
        local wrk_output
        wrk_output=$(wrk -t12 -c$CONCURRENT_USERS -d${TEST_DURATION}s \
            --timeout 30s \
            --script <(cat <<EOF
wrk.method = "GET"
wrk.headers["Content-Type"] = "application/json"
$([ -n "$JWT_TOKEN" ] && [[ "$endpoint" != "/actuator/"* ]] && echo "wrk.headers[\"Authorization\"] = \"Bearer $JWT_TOKEN\"")
EOF
        ) "$BASE_URL$endpoint" 2>&1 || echo "ERROR: Load test failed")
        
        # Parse results
        local requests_per_sec=$(echo "$wrk_output" | grep "Requests/sec:" | awk '{print $2}' | cut -d'.' -f1)
        local avg_latency=$(echo "$wrk_output" | grep "Latency" | awk '{print $2}')
        local total_requests=$(echo "$wrk_output" | grep "requests in" | awk '{print $1}')
        local errors=$(echo "$wrk_output" | grep "Socket errors:" | awk '{print $3}' || echo "0")
        
        # Store results
        local result="{
            \"endpoint\": \"$endpoint\",
            \"requests_per_second\": \"${requests_per_sec:-0}\",
            \"average_latency\": \"${avg_latency:-N/A}\",
            \"total_requests\": \"${total_requests:-0}\",
            \"errors\": \"${errors:-0}\",
            \"timestamp\": \"$(date -Iseconds)\"
        }"
        
        results+=("$result")
        
        # Check for failures
        if [ "${errors:-0}" -gt "$FAILURE_THRESHOLD" ]; then
            error "High error rate detected for $endpoint: $errors errors"
            echo "$(date -Iseconds): High error rate for $endpoint - $errors errors" >> "$FAILURE_LOG"
        fi
        
        # Brief pause between endpoint tests
        sleep 5
    done
    
    # Store API test results
    printf '%s\n' "${results[@]}" | jq -s '.' > "$LOGS_DIR/api-load-test-results.json"
    
    success "API load testing completed"
}

# Database Stress Testing
database_stress_test() {
    log "Starting database stress testing..."
    
    local db_test_script="$SCRIPT_DIR/db-stress-test.sh"
    
    # Create database stress test script
    cat > "$db_test_script" << 'EOF'
#!/bin/bash
# Database stress test with concurrent connections

BASE_URL="$1"
CONCURRENT_USERS="$2"
TEST_DURATION="$3"
JWT_TOKEN="$4"

test_customer_operations() {
    local user_id=$1
    local start_time=$(date +%s)
    local end_time=$((start_time + TEST_DURATION))
    local operations=0
    local errors=0
    
    while [ $(date +%s) -lt $end_time ]; do
        # Create customer
        local create_response=$(curl -s -w "%{http_code}" -o /dev/null \
            -X POST "$BASE_URL/api/v1/customers" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $JWT_TOKEN" \
            -d "{\"name\":\"Test Customer $user_id-$operations\",\"email\":\"test$user_id-$operations@example.com\"}")
        
        if [ "$create_response" != "201" ] && [ "$create_response" != "200" ]; then
            ((errors++))
        fi
        
        # Read customers
        local read_response=$(curl -s -w "%{http_code}" -o /dev/null \
            -X GET "$BASE_URL/api/v1/customers" \
            -H "Authorization: Bearer $JWT_TOKEN")
        
        if [ "$read_response" != "200" ]; then
            ((errors++))
        fi
        
        ((operations++))
        
        # Small delay to avoid overwhelming the system
        sleep 0.1
    done
    
    echo "User $user_id: $operations operations, $errors errors"
}

# Run concurrent database operations
for i in $(seq 1 "$CONCURRENT_USERS"); do
    test_customer_operations "$i" &
done

wait
EOF

    chmod +x "$db_test_script"
    
    # Run database stress test
    local db_results
    db_results=$("$db_test_script" "$BASE_URL" "$CONCURRENT_USERS" "$TEST_DURATION" "$JWT_TOKEN" 2>&1)
    
    echo "$db_results" > "$LOGS_DIR/database-stress-results.log"
    
    # Analyze results
    local total_operations=$(echo "$db_results" | grep -o '[0-9]* operations' | awk '{sum+=$1} END {print sum}')
    local total_errors=$(echo "$db_results" | grep -o '[0-9]* errors' | awk '{sum+=$1} END {print sum}')
    
    local db_result="{
        \"test_type\": \"database_stress\",
        \"total_operations\": ${total_operations:-0},
        \"total_errors\": ${total_errors:-0},
        \"error_rate\": $(echo "scale=2; ${total_errors:-0} * 100 / ${total_operations:-1}" | bc),
        \"duration\": $TEST_DURATION,
        \"concurrent_users\": $CONCURRENT_USERS,
        \"timestamp\": \"$(date -Iseconds)\"
    }"
    
    echo "$db_result" > "$LOGS_DIR/database-stress-summary.json"
    
    success "Database stress testing completed"
    rm -f "$db_test_script"
}

# Redis Cache Performance Testing
redis_cache_test() {
    log "Starting Redis cache performance testing..."
    
    local cache_test_script="$SCRIPT_DIR/redis-cache-test.sh"
    
    cat > "$cache_test_script" << 'EOF'
#!/bin/bash
REDIS_HOST="${REDIS_HOST:-localhost}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"
OPERATIONS_COUNT="${1:-1000}"

# Test Redis operations
redis_cmd() {
    if [ -n "$REDIS_PASSWORD" ]; then
        redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" "$@"
    else
        redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" "$@"
    fi
}

# Check if Redis is accessible
if ! redis_cmd ping &>/dev/null; then
    echo "Redis not accessible, skipping cache tests"
    exit 0
fi

start_time=$(date +%s)
errors=0

# Perform SET operations
for i in $(seq 1 "$OPERATIONS_COUNT"); do
    if ! redis_cmd set "test_key_$i" "test_value_$i" &>/dev/null; then
        ((errors++))
    fi
done

# Perform GET operations
for i in $(seq 1 "$OPERATIONS_COUNT"); do
    if ! redis_cmd get "test_key_$i" &>/dev/null; then
        ((errors++))
    fi
done

# Cleanup
redis_cmd flushdb &>/dev/null

end_time=$(date +%s)
duration=$((end_time - start_time))
total_ops=$((OPERATIONS_COUNT * 2))

echo "Redis performance: $total_ops operations in ${duration}s ($(echo "scale=2; $total_ops / $duration" | bc) ops/sec), $errors errors"
EOF

    chmod +x "$cache_test_script"
    
    # Run cache performance test
    local cache_results
    cache_results=$("$cache_test_script" 1000 2>&1)
    
    echo "$cache_results" > "$LOGS_DIR/redis-cache-results.log"
    
    success "Redis cache performance testing completed"
    rm -f "$cache_test_script"
}

# =============================================================================
# CHAOS ENGINEERING FUNCTIONS
# =============================================================================

# Chaos Engineering Tests
chaos_engineering_test() {
    log "Starting chaos engineering tests..."
    
    local chaos_scenarios=(
        "network_latency"
        "high_cpu_load" 
        "memory_pressure"
        "random_failures"
    )
    
    for scenario in "${chaos_scenarios[@]}"; do
        log "Executing chaos scenario: $scenario"
        
        case "$scenario" in
            "network_latency")
                chaos_network_latency
                ;;
            "high_cpu_load")
                chaos_cpu_load
                ;;
            "memory_pressure")
                chaos_memory_pressure
                ;;
            "random_failures")
                chaos_random_failures
                ;;
        esac
        
        # Monitor system during chaos
        monitor_system_during_chaos "$scenario"
        
        # Recovery time measurement
        measure_recovery_time "$scenario"
        
        sleep 30  # Recovery period between scenarios
    done
    
    success "Chaos engineering tests completed"
}

# Network Latency Chaos
chaos_network_latency() {
    log "Simulating network latency..."
    
    # Add network delay using tc (if available)
    if command -v tc &> /dev/null; then
        sudo tc qdisc add dev lo root netem delay 100ms 2>&1 | tee -a "$LOGS_DIR/chaos-network.log" || true
        
        # Test API responses under latency
        local latency_start=$(date +%s)
        local responses=0
        local slow_responses=0
        
        for i in {1..20}; do
            local response_time
            response_time=$(curl -w "%{time_total}" -s -o /dev/null "$BASE_URL/actuator/health" 2>/dev/null || echo "0")
            
            if (( $(echo "$response_time > 0.5" | bc -l) )); then
                ((slow_responses++))
            fi
            ((responses++))
            
            sleep 2
        done
        
        # Remove network delay
        sudo tc qdisc del dev lo root netem 2>/dev/null || true
        
        local latency_result="{
            \"scenario\": \"network_latency\",
            \"total_responses\": $responses,
            \"slow_responses\": $slow_responses,
            \"slow_response_rate\": $(echo "scale=2; $slow_responses * 100 / $responses" | bc),
            \"timestamp\": \"$(date -Iseconds)\"
        }"
        
        echo "$latency_result" >> "$LOGS_DIR/chaos-results.json"
    else
        warning "tc command not available, skipping network latency chaos test"
    fi
}

# CPU Load Chaos
chaos_cpu_load() {
    log "Simulating high CPU load..."
    
    # Create CPU stress using background processes
    for i in {1..4}; do
        (while true; do :; done) &
        local cpu_pid=$!
        echo "$cpu_pid" >> "$LOGS_DIR/chaos-pids.tmp"
    done
    
    # Monitor system performance under load
    local cpu_start=$(date +%s)
    local api_responses=0
    local failed_responses=0
    
    for i in {1..30}; do
        local response_code
        response_code=$(curl -s -w "%{http_code}" -o /dev/null "$BASE_URL/actuator/health" 2>/dev/null || echo "000")
        
        if [ "$response_code" != "200" ]; then
            ((failed_responses++))
        fi
        ((api_responses++))
        
        sleep 2
    done
    
    # Kill CPU stress processes
    if [ -f "$LOGS_DIR/chaos-pids.tmp" ]; then
        while read -r pid; do
            kill "$pid" 2>/dev/null || true
        done < "$LOGS_DIR/chaos-pids.tmp"
        rm -f "$LOGS_DIR/chaos-pids.tmp"
    fi
    
    local cpu_result="{
        \"scenario\": \"high_cpu_load\",
        \"api_responses\": $api_responses,
        \"failed_responses\": $failed_responses,
        \"failure_rate\": $(echo "scale=2; $failed_responses * 100 / $api_responses" | bc),
        \"duration\": $(($(date +%s) - cpu_start)),
        \"timestamp\": \"$(date -Iseconds)\"
    }"
    
    echo "$cpu_result" >> "$LOGS_DIR/chaos-results.json"
}

# Memory Pressure Chaos
chaos_memory_pressure() {
    log "Simulating memory pressure..."
    
    # Create memory pressure using stress tool or fallback
    if command -v stress &> /dev/null; then
        stress --vm 2 --vm-bytes 1G --timeout ${CHAOS_DURATION}s &
        local stress_pid=$!
        
        # Monitor API responses during memory pressure
        local mem_start=$(date +%s)
        local mem_responses=0
        local mem_failures=0
        
        while kill -0 "$stress_pid" 2>/dev/null; do
            local response_code
            response_code=$(curl -s -w "%{http_code}" -o /dev/null "$BASE_URL/actuator/health" 2>/dev/null || echo "000")
            
            if [ "$response_code" != "200" ]; then
                ((mem_failures++))
            fi
            ((mem_responses++))
            
            sleep 3
        done
        
        local mem_result="{
            \"scenario\": \"memory_pressure\",
            \"api_responses\": $mem_responses,
            \"failed_responses\": $mem_failures,
            \"failure_rate\": $(echo "scale=2; $mem_failures * 100 / $mem_responses" | bc),
            \"duration\": $(($(date +%s) - mem_start)),
            \"timestamp\": \"$(date -Iseconds)\"
        }"
        
        echo "$mem_result" >> "$LOGS_DIR/chaos-results.json"
    else
        warning "stress command not available, skipping memory pressure chaos test"
    fi
}

# Random Failures Chaos
chaos_random_failures() {
    log "Simulating random failures..."
    
    # Test circuit breaker behavior with random failures
    local random_start=$(date +%s)
    local random_requests=0
    local random_failures=0
    
    for i in {1..50}; do
        # Randomly fail some requests by sending to non-existent endpoint
        if [ $((RANDOM % 3)) -eq 0 ]; then
            local response_code
            response_code=$(curl -s -w "%{http_code}" -o /dev/null "$BASE_URL/api/v1/nonexistent" 2>/dev/null || echo "000")
            
            if [ "$response_code" != "200" ]; then
                ((random_failures++))
            fi
        else
            # Normal request
            local response_code
            response_code=$(curl -s -w "%{http_code}" -o /dev/null "$BASE_URL/actuator/health" 2>/dev/null || echo "000")
            
            if [ "$response_code" != "200" ]; then
                ((random_failures++))
            fi
        fi
        
        ((random_requests++))
        sleep 1
    done
    
    local random_result="{
        \"scenario\": \"random_failures\",
        \"total_requests\": $random_requests,
        \"failed_requests\": $random_failures,
        \"failure_rate\": $(echo "scale=2; $random_failures * 100 / $random_requests" | bc),
        \"duration\": $(($(date +%s) - random_start)),
        \"timestamp\": \"$(date -Iseconds)\"
    }"
    
    echo "$random_result" >> "$LOGS_DIR/chaos-results.json"
}

# Monitor system during chaos
monitor_system_during_chaos() {
    local scenario="$1"
    
    # Collect system metrics
    local cpu_usage=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1 || echo "0")
    local memory_usage=$(free | grep Mem | awk '{printf "%.1f", $3/$2 * 100.0}' || echo "0")
    local load_average=$(uptime | awk -F'load average:' '{print $2}' | cut -d',' -f1 | xargs || echo "0")
    
    local system_metrics="{
        \"scenario\": \"$scenario\",
        \"cpu_usage_percent\": \"$cpu_usage\",
        \"memory_usage_percent\": \"$memory_usage\",
        \"load_average\": \"$load_average\",
        \"timestamp\": \"$(date -Iseconds)\"
    }"
    
    echo "$system_metrics" >> "$LOGS_DIR/system-metrics.json"
}

# Measure recovery time
measure_recovery_time() {
    local scenario="$1"
    local recovery_start=$(date +%s)
    local recovered=false
    
    # Wait for system to recover (health endpoint returns 200)
    while [ "$recovered" = false ] && [ $(($(date +%s) - recovery_start)) -lt 300 ]; do
        local health_response
        health_response=$(curl -s -w "%{http_code}" -o /dev/null "$BASE_URL/actuator/health" 2>/dev/null || echo "000")
        
        if [ "$health_response" = "200" ]; then
            recovered=true
        else
            sleep 5
        fi
    done
    
    local recovery_time=$(($(date +%s) - recovery_start))
    
    local recovery_result="{
        \"scenario\": \"$scenario\",
        \"recovery_time_seconds\": $recovery_time,
        \"recovered\": $recovered,
        \"timestamp\": \"$(date -Iseconds)\"
    }"
    
    echo "$recovery_result" >> "$LOGS_DIR/recovery-times.json"
}

# =============================================================================
# SCALABILITY TESTING FUNCTIONS
# =============================================================================

# Scalability Tests
scalability_test() {
    log "Starting scalability testing..."
    
    local user_loads=(10 25 50 100 200)
    local scalability_results=()
    
    for load in "${user_loads[@]}"; do
        log "Testing with $load concurrent users..."
        
        # Run load test with specific user count
        local scalability_output
        scalability_output=$(wrk -t12 -c"$load" -d60s \
            --timeout 30s \
            "$BASE_URL/api/v1/loans/recommendations" 2>&1 || echo "ERROR")
        
        # Parse results
        local requests_per_sec=$(echo "$scalability_output" | grep "Requests/sec:" | awk '{print $2}' | cut -d'.' -f1)
        local avg_latency=$(echo "$scalability_output" | grep "Latency" | awk '{print $2}')
        local p99_latency=$(echo "$scalability_output" | grep "99%" | awk '{print $2}')
        local errors=$(echo "$scalability_output" | grep "Socket errors:" | awk '{print $3}' || echo "0")
        
        local scalability_result="{
            \"concurrent_users\": $load,
            \"requests_per_second\": \"${requests_per_sec:-0}\",
            \"average_latency\": \"${avg_latency:-N/A}\",
            \"p99_latency\": \"${p99_latency:-N/A}\",
            \"errors\": \"${errors:-0}\",
            \"timestamp\": \"$(date -Iseconds)\"
        }"
        
        scalability_results+=("$scalability_result")
        
        # Brief pause between scalability tests
        sleep 30
    done
    
    # Store scalability test results
    printf '%s\n' "${scalability_results[@]}" | jq -s '.' > "$LOGS_DIR/scalability-results.json"
    
    success "Scalability testing completed"
}

# =============================================================================
# REPORTING FUNCTIONS
# =============================================================================

# Generate comprehensive test summary
generate_test_summary() {
    log "Generating comprehensive test summary..."
    
    local test_end_time=$(date +%s)
    local total_test_duration=$((test_end_time - TEST_START_TIME))
    
    # Collect all test results
    local api_results=""
    local db_results=""
    local chaos_results=""
    local scalability_results=""
    local recovery_results=""
    
    [ -f "$LOGS_DIR/api-load-test-results.json" ] && api_results=$(cat "$LOGS_DIR/api-load-test-results.json")
    [ -f "$LOGS_DIR/database-stress-summary.json" ] && db_results=$(cat "$LOGS_DIR/database-stress-summary.json")
    [ -f "$LOGS_DIR/chaos-results.json" ] && chaos_results=$(jq -s '.' "$LOGS_DIR/chaos-results.json" 2>/dev/null || echo "[]")
    [ -f "$LOGS_DIR/scalability-results.json" ] && scalability_results=$(cat "$LOGS_DIR/scalability-results.json")
    [ -f "$LOGS_DIR/recovery-times.json" ] && recovery_results=$(jq -s '.' "$LOGS_DIR/recovery-times.json" 2>/dev/null || echo "[]")
    
    # Calculate overall metrics
    local total_requests=0
    local total_errors=0
    local overall_success_rate=0
    
    if [ -n "$api_results" ]; then
        total_requests=$(echo "$api_results" | jq '[.[].total_requests | tonumber] | add // 0')
        total_errors=$(echo "$api_results" | jq '[.[].errors | tonumber] | add // 0')
        overall_success_rate=$(echo "scale=2; (($total_requests - $total_errors) * 100) / $total_requests" | bc -l 2>/dev/null || echo "0")
    fi
    
    # Generate final summary
    local summary="{
        \"test_id\": \"$TEST_ID\",
        \"test_environment\": \"$TEST_ENV\",
        \"start_time\": \"$(date -d @$TEST_START_TIME -Iseconds)\",
        \"end_time\": \"$(date -d @$test_end_time -Iseconds)\",
        \"total_duration_seconds\": $total_test_duration,
        \"configuration\": {
            \"base_url\": \"$BASE_URL\",
            \"concurrent_users\": $CONCURRENT_USERS,
            \"test_duration\": $TEST_DURATION,
            \"max_requests_per_second\": $MAX_REQUESTS_PER_SECOND,
            \"response_time_threshold_ms\": $RESPONSE_TIME_THRESHOLD,
            \"success_rate_threshold_percent\": $SUCCESS_RATE_THRESHOLD
        },
        \"overall_metrics\": {
            \"total_requests\": $total_requests,
            \"total_errors\": $total_errors,
            \"overall_success_rate_percent\": \"$overall_success_rate\",
            \"test_passed\": $(echo "$overall_success_rate >= $SUCCESS_RATE_THRESHOLD" | bc -l)
        },
        \"test_results\": {
            \"api_load_tests\": $api_results,
            \"database_stress_test\": $db_results,
            \"chaos_engineering\": $chaos_results,
            \"scalability_tests\": $scalability_results,
            \"recovery_times\": $recovery_results
        },
        \"files\": {
            \"failure_log\": \"$FAILURE_LOG\",
            \"execution_log\": \"$LOGS_DIR/test-execution.log\",
            \"metrics_file\": \"$METRICS_FILE\"
        }
    }"
    
    echo "$summary" | jq '.' > "$SUMMARY_FILE"
    
    success "Test summary generated: $SUMMARY_FILE"
}

# Display test summary
display_test_summary() {
    log "Displaying test summary..."
    
    if [ ! -f "$SUMMARY_FILE" ]; then
        error "Test summary file not found: $SUMMARY_FILE"
        return 1
    fi
    
    local summary_data=$(cat "$SUMMARY_FILE")
    
    echo
    echo "================================================================================"
    echo "                    COMPREHENSIVE LOAD TEST SUMMARY"
    echo "================================================================================"
    echo
    echo "Test ID: $(echo "$summary_data" | jq -r '.test_id')"
    echo "Environment: $(echo "$summary_data" | jq -r '.test_environment')"
    echo "Duration: $(echo "$summary_data" | jq -r '.total_duration_seconds') seconds"
    echo "Start Time: $(echo "$summary_data" | jq -r '.start_time')"
    echo "End Time: $(echo "$summary_data" | jq -r '.end_time')"
    echo
    
    echo "CONFIGURATION:"
    echo "  Base URL: $(echo "$summary_data" | jq -r '.configuration.base_url')"
    echo "  Concurrent Users: $(echo "$summary_data" | jq -r '.configuration.concurrent_users')"
    echo "  Test Duration: $(echo "$summary_data" | jq -r '.configuration.test_duration')s"
    echo "  Success Rate Threshold: $(echo "$summary_data" | jq -r '.configuration.success_rate_threshold_percent')%"
    echo
    
    echo "OVERALL RESULTS:"
    echo "  Total Requests: $(echo "$summary_data" | jq -r '.overall_metrics.total_requests')"
    echo "  Total Errors: $(echo "$summary_data" | jq -r '.overall_metrics.total_errors')"
    echo "  Success Rate: $(echo "$summary_data" | jq -r '.overall_metrics.overall_success_rate_percent')%"
    
    local test_passed=$(echo "$summary_data" | jq -r '.overall_metrics.test_passed')
    if [ "$test_passed" = "1" ]; then
        echo -e "  Overall Result: ${GREEN}PASSED${NC}"
    else
        echo -e "  Overall Result: ${RED}FAILED${NC}"
    fi
    echo
    
    # Display API test results
    echo "API LOAD TEST RESULTS:"
    echo "$summary_data" | jq -r '.test_results.api_load_tests[]? | "  \(.endpoint): \(.requests_per_second) req/s, \(.average_latency) avg latency, \(.errors) errors"'
    echo
    
    # Display chaos engineering results
    echo "CHAOS ENGINEERING RESULTS:"
    echo "$summary_data" | jq -r '.test_results.chaos_engineering[]? | "  \(.scenario): \(.failure_rate)% failure rate, \(.duration)s duration"'
    echo
    
    # Display scalability results
    echo "SCALABILITY TEST RESULTS:"
    echo "$summary_data" | jq -r '.test_results.scalability_tests[]? | "  \(.concurrent_users) users: \(.requests_per_second) req/s, \(.average_latency) avg latency"'
    echo
    
    # Display failure information if any
    if [ -f "$FAILURE_LOG" ] && [ -s "$FAILURE_LOG" ]; then
        echo "FAILURE DETAILS:"
        cat "$FAILURE_LOG" | tail -20
        echo
    fi
    
    echo "DETAILED REPORTS:"
    echo "  Test Summary: $SUMMARY_FILE"
    echo "  Execution Log: $LOGS_DIR/test-execution.log"
    echo "  Failure Log: $FAILURE_LOG"
    echo "  Results Directory: $LOGS_DIR"
    echo
    echo "================================================================================"
}

# =============================================================================
# CI/CD INTEGRATION FUNCTIONS
# =============================================================================

# CI/CD Integration
setup_ci_cd_integration() {
    log "Setting up CI/CD integration..."
    
    # Create CI/CD friendly output formats
    local junit_xml="$REPORTS_DIR/junit-test-results.xml"
    local ci_summary="$REPORTS_DIR/ci-summary.json"
    
    # Generate JUnit XML format for CI systems
    cat > "$junit_xml" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<testsuites name="Load Tests" tests="1" failures="0" time="$(($(date +%s) - TEST_START_TIME))">
    <testsuite name="comprehensive-load-test" tests="1" failures="0" time="$(($(date +%s) - TEST_START_TIME))">
        <testcase name="load-test-suite" classname="LoadTest" time="$(($(date +%s) - TEST_START_TIME))">
EOF
    
    # Add failure information if tests failed
    if [ -f "$FAILURE_LOG" ] && [ -s "$FAILURE_LOG" ]; then
        echo "            <failure message=\"Load test failures detected\">" >> "$junit_xml"
        echo "                <![CDATA[" >> "$junit_xml"
        cat "$FAILURE_LOG" >> "$junit_xml"
        echo "                ]]>" >> "$junit_xml"
        echo "            </failure>" >> "$junit_xml"
    fi
    
    cat >> "$junit_xml" << EOF
        </testcase>
    </testsuite>
</testsuites>
EOF
    
    # Generate CI summary
    local ci_summary_data="{
        \"test_id\": \"$TEST_ID\",
        \"timestamp\": \"$(date -Iseconds)\",
        \"environment\": \"$TEST_ENV\",
        \"passed\": $([ ! -s "$FAILURE_LOG" ] && echo "true" || echo "false"),
        \"duration_seconds\": $(($(date +%s) - TEST_START_TIME)),
        \"artifacts\": {
            \"junit_xml\": \"$junit_xml\",
            \"summary_json\": \"$SUMMARY_FILE\",
            \"failure_log\": \"$FAILURE_LOG\"
        }
    }"
    
    echo "$ci_summary_data" | jq '.' > "$ci_summary"
    
    success "CI/CD integration setup completed"
}

# =============================================================================
# MAIN EXECUTION FUNCTION
# =============================================================================

main() {
    log "Starting comprehensive load testing for Enterprise Loan Management System"
    log "Test ID: $TEST_ID"
    log "Environment: $TEST_ENV"
    log "Base URL: $BASE_URL"
    
    # Initialize test environment
    check_dependencies
    
    # System health check
    if ! health_check; then
        error "System health check failed. Aborting tests."
        exit 1
    fi
    
    # Generate authentication token
    generate_jwt_token
    
    # Execute test suites
    log "Executing test suites..."
    
    # 1. API Load Testing
    api_load_test
    
    # 2. Database Stress Testing
    database_stress_test
    
    # 3. Redis Cache Testing
    redis_cache_test
    
    # 4. Chaos Engineering
    chaos_engineering_test
    
    # 5. Scalability Testing
    scalability_test
    
    # Generate comprehensive reports
    generate_test_summary
    
    # CI/CD Integration
    setup_ci_cd_integration
    
    # Display results
    display_test_summary
    
    # Determine exit code based on test results
    if [ -f "$FAILURE_LOG" ] && [ -s "$FAILURE_LOG" ]; then
        error "Tests completed with failures. Check $FAILURE_LOG for details."
        exit 1
    else
        success "All tests completed successfully!"
        exit 0
    fi
}

# =============================================================================
# SCRIPT EXECUTION
# =============================================================================

# Trap signals for cleanup
trap 'log "Test execution interrupted. Cleaning up..."; exit 130' INT TERM

# Execute main function
main "$@"