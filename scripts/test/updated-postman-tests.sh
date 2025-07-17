#!/bin/bash

# Updated Enterprise Banking System - Postman API Tests
# Runs API tests against the current working implementation

set -e

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
COLLECTION_FILE="postman/Updated-Enterprise-Banking-API.postman_collection.json"
ENVIRONMENT_FILE="postman/Updated-Enterprise-Environment.postman_environment.json"
BASE_URL="http://localhost:8080"
RESULTS_DIR="test-results"

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check if Newman is installed
    if ! command -v newman &> /dev/null; then
        log_error "Newman is not installed. Installing Newman..."
        npm install -g newman newman-reporter-htmlextra
    fi
    
    # Check if collection exists
    if [ ! -f "$COLLECTION_FILE" ]; then
        log_error "Postman collection not found: $COLLECTION_FILE"
        exit 1
    fi
    
    # Check if environment exists
    if [ ! -f "$ENVIRONMENT_FILE" ]; then
        log_error "Postman environment not found: $ENVIRONMENT_FILE"
        exit 1
    fi
    
    # Create results directory
    mkdir -p "$RESULTS_DIR"
    
    log_success "Prerequisites check completed"
}

# Check if application is running
check_application() {
    log_info "Checking if application is running..."
    
    # Check main application health
    if curl -f -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
        log_success "Application is running at $BASE_URL"
        return 0
    else
        log_warning "Application is not running at $BASE_URL"
        return 1
    fi
}

# Start application if not running
start_application() {
    log_info "Starting application..."
    
    # Check if we're in the project root
    if [ ! -f "build.gradle" ]; then
        log_error "Please run this script from the project root directory"
        exit 1
    fi
    
    # Start application in background
    log_info "Starting Spring Boot application..."
    ./gradlew bootRun > application.log 2>&1 &
    APP_PID=$!
    
    # Wait for application to start
    log_info "Waiting for application to start..."
    for i in {1..30}; do
        if curl -f -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
            log_success "Application started successfully (PID: $APP_PID)"
            return 0
        fi
        sleep 2
    done
    
    log_error "Application failed to start within 60 seconds"
    kill $APP_PID 2>/dev/null || true
    exit 1
}

# Run health checks only
run_health_tests() {
    log_info "Running health check tests..."
    
    newman run "$COLLECTION_FILE" \
        --environment "$ENVIRONMENT_FILE" \
        --folder "🏥 Health & Monitoring" \
        --reporters cli,json \
        --reporter-json-export "$RESULTS_DIR/health-test-results.json" \
        --insecure \
        --timeout 10000 \
        --delay-request 500 \
        --bail \
        --color on \
        --verbose
}

# Run loan management tests
run_loan_tests() {
    log_info "Running loan management tests..."
    
    newman run "$COLLECTION_FILE" \
        --environment "$ENVIRONMENT_FILE" \
        --folder "🏦 Loan Management API" \
        --reporters cli,json \
        --reporter-json-export "$RESULTS_DIR/loan-test-results.json" \
        --insecure \
        --timeout 15000 \
        --delay-request 1000 \
        --continue-on-error \
        --color on \
        --verbose
}

# Run all tests
run_all_tests() {
    log_info "Running complete test suite..."
    
    newman run "$COLLECTION_FILE" \
        --environment "$ENVIRONMENT_FILE" \
        --reporters cli,htmlextra,json \
        --reporter-htmlextra-export "$RESULTS_DIR/complete-test-results.html" \
        --reporter-json-export "$RESULTS_DIR/complete-test-results.json" \
        --insecure \
        --timeout 30000 \
        --delay-request 1000 \
        --continue-on-error \
        --color on \
        --verbose
}

# Run basic connectivity tests without authentication
run_basic_tests() {
    log_info "Running basic connectivity tests..."
    
    # Create a minimal test collection
    cat > "$RESULTS_DIR/basic-tests.json" << 'EOF'
{
  "info": {
    "name": "Basic Connectivity Tests",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Health Check",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "http://localhost:8080/actuator/health",
          "host": ["localhost"],
          "port": "8080",
          "path": ["actuator", "health"]
        }
      },
      "event": [
        {
          "listen": "test",
          "script": {
            "exec": [
              "pm.test('Health check responds', function () {",
              "    pm.expect(pm.response.code).to.be.oneOf([200, 404, 403]);",
              "});",
              "",
              "pm.test('Response time is acceptable', function () {",
              "    pm.expect(pm.response.responseTime).to.be.below(5000);",
              "});"
            ]
          }
        }
      ]
    },
    {
      "name": "Application Info",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "http://localhost:8080/actuator/info",
          "host": ["localhost"],
          "port": "8080",
          "path": ["actuator", "info"]
        }
      },
      "event": [
        {
          "listen": "test",
          "script": {
            "exec": [
              "pm.test('Info endpoint responds', function () {",
              "    pm.expect(pm.response.code).to.be.oneOf([200, 404, 403]);",
              "});",
              "",
              "pm.test('Response time is acceptable', function () {",
              "    pm.expect(pm.response.responseTime).to.be.below(5000);",
              "});"
            ]
          }
        }
      ]
    },
    {
      "name": "Loan API Base",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "http://localhost:8080/api/v1/loans",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "v1", "loans"]
        }
      },
      "event": [
        {
          "listen": "test",
          "script": {
            "exec": [
              "pm.test('Loan API endpoint responds', function () {",
              "    // Accept any response that's not 404 (endpoint exists)",
              "    pm.expect(pm.response.code).to.not.eql(404);",
              "});",
              "",
              "pm.test('Response time is acceptable', function () {",
              "    pm.expect(pm.response.responseTime).to.be.below(5000);",
              "});"
            ]
          }
        }
      ]
    }
  ]
}
EOF
    
    newman run "$RESULTS_DIR/basic-tests.json" \
        --reporters cli,json \
        --reporter-json-export "$RESULTS_DIR/basic-test-results.json" \
        --insecure \
        --timeout 10000 \
        --delay-request 500 \
        --color on
}

# Generate test report
generate_report() {
    log_info "Generating test report..."
    
    local results_file="$RESULTS_DIR/basic-test-results.json"
    if [ -f "$results_file" ]; then
        echo "========================================="
        echo "           TEST SUMMARY"
        echo "========================================="
        
        # Extract basic stats using jq if available
        if command -v jq &> /dev/null; then
            local total_tests=$(jq '.run.stats.tests.total // 0' "$results_file")
            local passed_tests=$(jq '.run.stats.tests.passed // 0' "$results_file")
            local failed_tests=$(jq '.run.stats.tests.failed // 0' "$results_file")
            local total_assertions=$(jq '.run.stats.assertions.total // 0' "$results_file")
            local passed_assertions=$(jq '.run.stats.assertions.passed // 0' "$results_file")
            local failed_assertions=$(jq '.run.stats.assertions.failed // 0' "$results_file")
            
            echo "Tests: $passed_tests/$total_tests passed"
            echo "Assertions: $passed_assertions/$total_assertions passed"
            
            if [ "$failed_tests" -eq 0 ]; then
                log_success "All tests passed!"
            else
                log_warning "$failed_tests tests failed"
            fi
        else
            log_info "Install jq for detailed test statistics"
        fi
        
        echo "Full results available in: $results_file"
        if [ -f "$RESULTS_DIR/complete-test-results.html" ]; then
            echo "HTML report available in: $RESULTS_DIR/complete-test-results.html"
        fi
    fi
}

# Cleanup function
cleanup() {
    log_info "Cleaning up..."
    
    # Kill application if we started it
    if [ ! -z "$APP_PID" ]; then
        log_info "Stopping application (PID: $APP_PID)"
        kill $APP_PID 2>/dev/null || true
        sleep 2
    fi
    
    # Clean up temporary files
    rm -f "$RESULTS_DIR/basic-tests.json"
}

# Set up trap for cleanup
trap cleanup EXIT

# Main execution
main() {
    echo "========================================="
    echo "Updated Enterprise Banking System"
    echo "Postman API Test Runner"
    echo "========================================="
    echo ""
    
    check_prerequisites
    
    # Check if app is running, start if needed
    if ! check_application; then
        case "${1:-basic}" in
            "basic"|"health"|"loan"|"all")
                start_application
                ;;
            *)
                log_error "Application is not running and cannot be started for this test mode"
                exit 1
                ;;
        esac
    fi
    
    case "${1:-basic}" in
        "basic")
            run_basic_tests
            ;;
        "health")
            run_health_tests
            ;;
        "loan")
            run_loan_tests
            ;;
        "all")
            run_all_tests
            ;;
        *)
            echo "Usage: $0 [basic|health|loan|all]"
            echo ""
            echo "Commands:"
            echo "  basic   - Run basic connectivity tests (default)"
            echo "  health  - Run health check tests"
            echo "  loan    - Run loan management API tests"
            echo "  all     - Run complete test suite"
            echo ""
            echo "Examples:"
            echo "  $0 basic"
            echo "  $0 health"
            echo "  $0 loan"
            echo "  $0 all"
            exit 1
            ;;
    esac
    
    generate_report
    
    log_success "Test execution completed!"
    echo "Results available in: $RESULTS_DIR/"
}

# Execute main function with all arguments
main "$@"