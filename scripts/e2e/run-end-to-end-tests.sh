#!/bin/bash

# ===============================================================
# ENTERPRISE BANKING SYSTEM - END-TO-END TEST EXECUTION SCRIPT
# ===============================================================
# Document Information:
# - Author: Senior Test Automation Engineer & DevOps Lead
# - Version: 1.0.0
# - Last Updated: December 2024
# - Classification: Internal - Test Automation Infrastructure
# - Purpose: Comprehensive E2E testing orchestration
# ===============================================================

set -euo pipefail

# ============================================
# CONFIGURATION AND ENVIRONMENT SETUP
# ============================================

# Script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
TEST_RESULTS_DIR="${PROJECT_ROOT}/test-results/e2e-$(date +%Y%m%d-%H%M%S)"

# Docker Compose configuration
DOCKER_COMPOSE_FILE="${PROJECT_ROOT}/docker/testing/docker-compose.e2e-tests.yml"
DOCKER_PROJECT_NAME="banking-e2e-tests"

# Test configuration
TIMEOUT_SERVICES=300  # 5 minutes for services to start
TIMEOUT_TESTS=1800    # 30 minutes for all tests
PARALLEL_TESTS=true
CLEANUP_ON_EXIT=true

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ============================================
# UTILITY FUNCTIONS
# ============================================

log_info() {
    echo -e "${BLUE}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed or not in PATH"
        exit 1
    fi
    
    # Check Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose is not installed or not in PATH"
        exit 1
    fi
    
    # Check available disk space (minimum 10GB)
    AVAILABLE_SPACE=$(df "${PROJECT_ROOT}" | awk 'NR==2 {print $4}')
    if [ "${AVAILABLE_SPACE}" -lt 10485760 ]; then
        log_error "Insufficient disk space. At least 10GB required."
        exit 1
    fi
    
    # Check if ports are available
    REQUIRED_PORTS=(5433 6380 8090 8091 8092 8093 9091 9093 3001 2182)
    for port in "${REQUIRED_PORTS[@]}"; do
        if netstat -tuln | grep -q ":${port} "; then
            log_error "Port ${port} is already in use. Please free the port and try again."
            exit 1
        fi
    done
    
    log_success "Prerequisites check passed"
}

create_test_directories() {
    log_info "Creating test result directories..."
    
    mkdir -p "${TEST_RESULTS_DIR}"
    mkdir -p "${TEST_RESULTS_DIR}/newman"
    mkdir -p "${TEST_RESULTS_DIR}/regression"
    mkdir -p "${TEST_RESULTS_DIR}/performance"
    mkdir -p "${TEST_RESULTS_DIR}/security"
    mkdir -p "${TEST_RESULTS_DIR}/logs"
    mkdir -p "${TEST_RESULTS_DIR}/screenshots"
    
    # Create Docker volume directories
    mkdir -p "${PROJECT_ROOT}/docker/testing/test-results"
    mkdir -p "${PROJECT_ROOT}/docker/testing/aggregated-results"
    
    log_success "Test directories created"
}

cleanup_previous_runs() {
    log_info "Cleaning up previous test runs..."
    
    # Stop and remove containers
    docker-compose -f "${DOCKER_COMPOSE_FILE}" -p "${DOCKER_PROJECT_NAME}" down --volumes --remove-orphans || true
    
    # Remove dangling images
    docker image prune -f || true
    
    # Clean up test networks
    docker network ls --filter name="${DOCKER_PROJECT_NAME}" --format "{{.ID}}" | xargs -r docker network rm || true
    
    log_success "Cleanup completed"
}

# ============================================
# INFRASTRUCTURE SETUP
# ============================================

start_test_infrastructure() {
    log_info "Starting test infrastructure..."
    
    cd "${PROJECT_ROOT}"
    
    # Build test images
    log_info "Building test Docker images..."
    docker-compose -f "${DOCKER_COMPOSE_FILE}" -p "${DOCKER_PROJECT_NAME}" build --parallel
    
    # Start infrastructure services first
    log_info "Starting database and messaging services..."
    docker-compose -f "${DOCKER_COMPOSE_FILE}" -p "${DOCKER_PROJECT_NAME}" up -d postgres-test redis-test zookeeper-test kafka-test
    
    # Wait for infrastructure to be ready
    log_info "Waiting for infrastructure services to be healthy..."
    wait_for_service_health "postgres-test" "${TIMEOUT_SERVICES}"
    wait_for_service_health "redis-test" "${TIMEOUT_SERVICES}"
    wait_for_service_health "kafka-test" "${TIMEOUT_SERVICES}"
    
    log_success "Test infrastructure started successfully"
}

start_microservices() {
    log_info "Starting microservices..."
    
    # Start microservices in dependency order
    docker-compose -f "${DOCKER_COMPOSE_FILE}" -p "${DOCKER_PROJECT_NAME}" up -d customer-service-test
    wait_for_service_health "customer-service-test" "${TIMEOUT_SERVICES}"
    
    docker-compose -f "${DOCKER_COMPOSE_FILE}" -p "${DOCKER_PROJECT_NAME}" up -d loan-service-test
    wait_for_service_health "loan-service-test" "${TIMEOUT_SERVICES}"
    
    docker-compose -f "${DOCKER_COMPOSE_FILE}" -p "${DOCKER_PROJECT_NAME}" up -d payment-service-test
    wait_for_service_health "payment-service-test" "${TIMEOUT_SERVICES}"
    
    docker-compose -f "${DOCKER_COMPOSE_FILE}" -p "${DOCKER_PROJECT_NAME}" up -d api-gateway-test
    wait_for_service_health "api-gateway-test" "${TIMEOUT_SERVICES}"
    
    log_success "All microservices started successfully"
}

wait_for_service_health() {
    local service_name=$1
    local timeout=$2
    local elapsed=0
    local interval=10
    
    log_info "Waiting for ${service_name} to be healthy..."
    
    while [ $elapsed -lt $timeout ]; do
        if docker-compose -f "${DOCKER_COMPOSE_FILE}" -p "${DOCKER_PROJECT_NAME}" ps "${service_name}" | grep -q "healthy"; then
            log_success "${service_name} is healthy"
            return 0
        fi
        
        sleep $interval
        elapsed=$((elapsed + interval))
        log_info "Waiting for ${service_name}... (${elapsed}s/${timeout}s)"
    done
    
    log_error "${service_name} failed to become healthy within ${timeout} seconds"
    collect_service_logs "${service_name}"
    return 1
}

# ============================================
# TEST EXECUTION FUNCTIONS
# ============================================

run_smoke_tests() {
    log_info "Running smoke tests..."
    
    local base_url="http://localhost:8090"
    local max_retries=5
    local retry_delay=10
    
    # Test API Gateway health
    for i in $(seq 1 $max_retries); do
        if curl -s -f "${base_url}/actuator/health" > /dev/null; then
            log_success "API Gateway smoke test passed"
            break
        elif [ $i -eq $max_retries ]; then
            log_error "API Gateway smoke test failed after ${max_retries} attempts"
            return 1
        else
            log_warning "API Gateway not ready, attempt ${i}/${max_retries}"
            sleep $retry_delay
        fi
    done
    
    # Test individual service health
    local services=("customer-service-test:8091" "loan-service-test:8092" "payment-service-test:8093")
    for service in "${services[@]}"; do
        local service_name="${service%:*}"
        local service_port="${service#*:}"
        local service_url="http://localhost:${service_port}"
        
        if curl -s -f "${service_url}/actuator/health" > /dev/null; then
            log_success "${service_name} smoke test passed"
        else
            log_error "${service_name} smoke test failed"
            return 1
        fi
    done
    
    log_success "All smoke tests passed"
}

run_api_tests() {
    log_info "Running API tests with Newman..."
    
    # Start Newman test runner
    docker-compose -f "${DOCKER_COMPOSE_FILE}" -p "${DOCKER_PROJECT_NAME}" up --exit-code-from newman-test-runner newman-test-runner
    
    local newman_exit_code=$?
    
    # Copy Newman results
    docker cp "${DOCKER_PROJECT_NAME}_newman-test-runner_1:/test-results/." "${TEST_RESULTS_DIR}/newman/" 2>/dev/null || true
    
    if [ $newman_exit_code -eq 0 ]; then
        log_success "API tests completed successfully"
    else
        log_error "API tests failed with exit code ${newman_exit_code}"
        return 1
    fi
}

run_regression_tests() {
    log_info "Running regression test suite..."
    
    # Start regression test runner
    docker-compose -f "${DOCKER_COMPOSE_FILE}" -p "${DOCKER_PROJECT_NAME}" up --exit-code-from regression-test-runner regression-test-runner
    
    local regression_exit_code=$?
    
    # Copy regression test results
    docker cp "${DOCKER_PROJECT_NAME}_regression-test-runner_1:/test-results/." "${TEST_RESULTS_DIR}/regression/" 2>/dev/null || true
    
    if [ $regression_exit_code -eq 0 ]; then
        log_success "Regression tests completed successfully"
    else
        log_error "Regression tests failed with exit code ${regression_exit_code}"
        return 1
    fi
}

run_performance_tests() {
    log_info "Running performance tests with K6..."
    
    # Start K6 performance tests
    docker-compose -f "${DOCKER_COMPOSE_FILE}" -p "${DOCKER_PROJECT_NAME}" up --exit-code-from k6-performance-tests k6-performance-tests
    
    local k6_exit_code=$?
    
    # Copy K6 results
    docker cp "${DOCKER_PROJECT_NAME}_k6-performance-tests_1:/results/." "${TEST_RESULTS_DIR}/performance/" 2>/dev/null || true
    
    if [ $k6_exit_code -eq 0 ]; then
        log_success "Performance tests completed successfully"
    else
        log_warning "Performance tests completed with issues (exit code ${k6_exit_code})"
    fi
}

run_security_tests() {
    log_info "Running security tests with OWASP ZAP..."
    
    # Start ZAP security scan
    docker-compose -f "${DOCKER_COMPOSE_FILE}" -p "${DOCKER_PROJECT_NAME}" up --exit-code-from zap-security-scan zap-security-scan
    
    local zap_exit_code=$?
    
    # Copy ZAP results
    docker cp "${DOCKER_PROJECT_NAME}_zap-security-scan_1:/zap/wrk/." "${TEST_RESULTS_DIR}/security/" 2>/dev/null || true
    
    if [ $zap_exit_code -eq 0 ]; then
        log_success "Security tests completed successfully"
    else
        log_warning "Security tests found issues (exit code ${zap_exit_code})"
    fi
}

aggregate_test_results() {
    log_info "Aggregating test results..."
    
    # Start test results aggregator
    docker-compose -f "${DOCKER_COMPOSE_FILE}" -p "${DOCKER_PROJECT_NAME}" up --exit-code-from test-results-aggregator test-results-aggregator
    
    # Copy aggregated results
    docker cp "${DOCKER_PROJECT_NAME}_test-results-aggregator_1:/output-results/." "${TEST_RESULTS_DIR}/" 2>/dev/null || true
    
    log_success "Test results aggregated"
}

# ============================================
# MONITORING AND LOGGING
# ============================================

start_monitoring() {
    log_info "Starting monitoring stack..."
    
    docker-compose -f "${DOCKER_COMPOSE_FILE}" -p "${DOCKER_PROJECT_NAME}" up -d prometheus-test grafana-test
    
    log_info "Monitoring available at:"
    log_info "  - Prometheus: http://localhost:9091"
    log_info "  - Grafana: http://localhost:3001 (admin/test_grafana_2024)"
}

collect_service_logs() {
    local service_name=${1:-"all"}
    
    log_info "Collecting logs for ${service_name}..."
    
    if [ "${service_name}" = "all" ]; then
        docker-compose -f "${DOCKER_COMPOSE_FILE}" -p "${DOCKER_PROJECT_NAME}" logs > "${TEST_RESULTS_DIR}/logs/all-services.log"
    else
        docker-compose -f "${DOCKER_COMPOSE_FILE}" -p "${DOCKER_PROJECT_NAME}" logs "${service_name}" > "${TEST_RESULTS_DIR}/logs/${service_name}.log"
    fi
}

# ============================================
# CLEANUP AND REPORTING
# ============================================

cleanup_test_environment() {
    if [ "${CLEANUP_ON_EXIT}" = "true" ]; then
        log_info "Cleaning up test environment..."
        
        # Collect final logs
        collect_service_logs "all"
        
        # Stop all services
        docker-compose -f "${DOCKER_COMPOSE_FILE}" -p "${DOCKER_PROJECT_NAME}" down --volumes --remove-orphans
        
        # Clean up dangling resources
        docker system prune -f
        
        log_success "Test environment cleaned up"
    else
        log_info "Skipping cleanup (CLEANUP_ON_EXIT=false)"
        log_info "To manually cleanup, run:"
        log_info "  docker-compose -f ${DOCKER_COMPOSE_FILE} -p ${DOCKER_PROJECT_NAME} down --volumes --remove-orphans"
    fi
}

generate_test_report() {
    log_info "Generating comprehensive test report..."
    
    local report_file="${TEST_RESULTS_DIR}/test-execution-report.html"
    
    cat > "${report_file}" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>Enterprise Banking System - E2E Test Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        .header { background: #2c3e50; color: white; padding: 20px; border-radius: 5px; }
        .section { margin: 20px 0; padding: 15px; border: 1px solid #ddd; border-radius: 5px; }
        .success { background: #d4edda; border-color: #c3e6cb; }
        .warning { background: #fff3cd; border-color: #ffeaa7; }
        .error { background: #f8d7da; border-color: #f5c6cb; }
        .metrics { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; }
        .metric { text-align: center; padding: 15px; background: #f8f9fa; border-radius: 5px; }
        .metric h3 { margin: 0; color: #2c3e50; }
        .metric .value { font-size: 2em; font-weight: bold; color: #e74c3c; }
    </style>
</head>
<body>
    <div class="header">
        <h1>Enterprise Banking System - End-to-End Test Execution Report</h1>
        <p>Generated on: $(date)</p>
        <p>Test Suite: Comprehensive E2E Testing with Regression Coverage</p>
    </div>
    
    <div class="section">
        <h2>Test Execution Summary</h2>
        <div class="metrics">
            <div class="metric">
                <h3>Total Test Duration</h3>
                <div class="value">$(date -d@$SECONDS -u +%H:%M:%S)</div>
            </div>
            <div class="metric">
                <h3>Services Tested</h3>
                <div class="value">4</div>
            </div>
            <div class="metric">
                <h3>Test Categories</h3>
                <div class="value">5</div>
            </div>
        </div>
    </div>
    
    <div class="section">
        <h2>Test Results</h2>
        <ul>
            <li><strong>Smoke Tests:</strong> Infrastructure health validation</li>
            <li><strong>API Tests:</strong> Postman collection execution with Newman</li>
            <li><strong>Regression Tests:</strong> Comprehensive business logic validation</li>
            <li><strong>Performance Tests:</strong> Load testing with K6</li>
            <li><strong>Security Tests:</strong> OWASP ZAP baseline security scan</li>
        </ul>
    </div>
    
    <div class="section">
        <h2>Artifacts Generated</h2>
        <ul>
            <li>Newman HTML reports and JSON results</li>
            <li>JUnit XML test results</li>
            <li>K6 performance metrics</li>
            <li>OWASP ZAP security scan reports</li>
            <li>Service logs and monitoring data</li>
        </ul>
    </div>
</body>
</html>
EOF
    
    log_success "Test report generated: ${report_file}"
}

# ============================================
# SIGNAL HANDLERS
# ============================================

trap_cleanup() {
    log_warning "Received interrupt signal, cleaning up..."
    cleanup_test_environment
    exit 1
}

trap trap_cleanup SIGINT SIGTERM

# ============================================
# MAIN EXECUTION FLOW
# ============================================

main() {
    log_info "Starting Enterprise Banking System E2E Test Suite"
    log_info "================================================"
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --no-cleanup)
                CLEANUP_ON_EXIT=false
                shift
                ;;
            --timeout=*)
                TIMEOUT_TESTS="${1#*=}"
                shift
                ;;
            --skip-performance)
                SKIP_PERFORMANCE=true
                shift
                ;;
            --skip-security)
                SKIP_SECURITY=true
                shift
                ;;
            -h|--help)
                echo "Usage: $0 [OPTIONS]"
                echo "Options:"
                echo "  --no-cleanup        Don't cleanup containers after tests"
                echo "  --timeout=SECONDS   Set test timeout (default: 1800)"
                echo "  --skip-performance  Skip performance tests"
                echo "  --skip-security     Skip security tests"
                echo "  -h, --help         Show this help message"
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                exit 1
                ;;
        esac
    done
    
    # Execute test pipeline
    local overall_start_time=$(date +%s)
    local test_failures=0
    
    # Preparation phase
    check_prerequisites
    create_test_directories
    cleanup_previous_runs
    
    # Infrastructure phase
    start_test_infrastructure
    start_microservices
    start_monitoring
    
    # Testing phase
    run_smoke_tests || ((test_failures++))
    
    run_api_tests || ((test_failures++))
    
    run_regression_tests || ((test_failures++))
    
    if [ "${SKIP_PERFORMANCE:-false}" != "true" ]; then
        run_performance_tests || ((test_failures++))
    fi
    
    if [ "${SKIP_SECURITY:-false}" != "true" ]; then
        run_security_tests || ((test_failures++))
    fi
    
    aggregate_test_results
    
    # Cleanup and reporting
    generate_test_report
    cleanup_test_environment
    
    # Final results
    local overall_end_time=$(date +%s)
    local total_duration=$((overall_end_time - overall_start_time))
    
    log_info "================================================"
    log_info "E2E Test Suite Execution Complete"
    log_info "Total Duration: $(date -d@${total_duration} -u +%H:%M:%S)"
    log_info "Test Results: ${TEST_RESULTS_DIR}"
    
    if [ $test_failures -eq 0 ]; then
        log_success "All test suites passed successfully!"
        exit 0
    else
        log_error "${test_failures} test suite(s) failed. Check results for details."
        exit 1
    fi
}

# Execute main function with all arguments
main "$@"