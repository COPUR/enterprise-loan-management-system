#!/bin/bash

# Enhanced Enterprise Banking System - Postman API Tests
# Runs comprehensive API testing using Newman CLI

set -e

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
COLLECTION_FILE="postman/Enhanced-Enterprise-Banking-System.postman_collection.json"
ENVIRONMENT_FILE="postman/Enhanced-Enterprise-Environment.postman_environment.json"
BASE_URL="http://localhost:8080"
KEYCLOAK_URL="http://localhost:8090"

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
        npm install -g newman
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
    
    log_success "Prerequisites check completed"
}

# Check service availability
check_services() {
    log_info "Checking service availability..."
    
    # Check main application
    if curl -f "$BASE_URL/actuator/health" &>/dev/null; then
        log_success "Banking application is accessible at $BASE_URL"
    else
        log_warning "Banking application is not accessible at $BASE_URL"
    fi
    
    # Check Keycloak
    if curl -f "$KEYCLOAK_URL/realms/master" &>/dev/null; then
        log_success "Keycloak is accessible at $KEYCLOAK_URL"
    else
        log_warning "Keycloak is not accessible at $KEYCLOAK_URL"
    fi
}

# Run health check tests only (no authentication required)
run_health_tests() {
    log_info "Running health check tests..."
    
    newman run "$COLLECTION_FILE" \
        --environment "$ENVIRONMENT_FILE" \
        --folder "📊 Monitoring & Health" \
        --reporters cli,json \
        --reporter-json-export health-test-results.json \
        --insecure \
        --timeout 10000 \
        --delay-request 500 \
        --bail \
        --color on
}

# Run specific collection folder tests
run_folder_tests() {
    local folder_name="$1"
    log_info "Running tests for folder: $folder_name"
    
    newman run "$COLLECTION_FILE" \
        --environment "$ENVIRONMENT_FILE" \
        --folder "$folder_name" \
        --reporters cli,json \
        --reporter-json-export "${folder_name// /-}-test-results.json" \
        --insecure \
        --timeout 15000 \
        --delay-request 1000 \
        --continue-on-error \
        --color on
}

# Run all tests with authentication
run_all_tests() {
    log_info "Running complete test suite..."
    
    newman run "$COLLECTION_FILE" \
        --environment "$ENVIRONMENT_FILE" \
        --reporters cli,htmlextra,json \
        --reporter-htmlextra-export test-results.html \
        --reporter-json-export complete-test-results.json \
        --insecure \
        --timeout 30000 \
        --delay-request 1000 \
        --continue-on-error \
        --color on \
        --verbose
}

# Create a simple health test collection for current environment
create_simple_health_collection() {
    log_info "Creating simplified health check collection..."
    
    cat > simple-health-tests.json << 'EOF'
{
  "info": {
    "name": "Simple Banking Health Tests",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Application Health Check",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "http://localhost:8080/actuator/health",
          "protocol": "http",
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
              "    pm.response.to.not.have.status(404);",
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
      "name": "Keycloak Health Check",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "http://localhost:8090/realms/master",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8090",
          "path": ["realms", "master"]
        }
      },
      "event": [
        {
          "listen": "test",
          "script": {
            "exec": [
              "pm.test('Keycloak responds', function () {",
              "    pm.response.to.not.have.status(404);",
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
      "name": "Simple Loan API Test",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "http://localhost:8080/api/loans",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "loans"]
        }
      },
      "event": [
        {
          "listen": "test",
          "script": {
            "exec": [
              "pm.test('Loan API endpoint responds', function () {",
              "    // Accept any response that's not 404",
              "    pm.response.to.not.have.status(404);",
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
    
    log_success "Simple health test collection created: simple-health-tests.json"
}

# Run simple health tests
run_simple_tests() {
    log_info "Running simplified health tests..."
    
    create_simple_health_collection
    
    newman run simple-health-tests.json \
        --reporters cli,json \
        --reporter-json-export simple-test-results.json \
        --insecure \
        --timeout 10000 \
        --delay-request 500 \
        --color on
}

# Generate test report
generate_report() {
    log_info "Generating test report..."
    
    if [ -f "simple-test-results.json" ]; then
        echo "========================================="
        echo "           TEST SUMMARY"
        echo "========================================="
        
        # Extract basic stats using jq if available
        if command -v jq &> /dev/null; then
            local total_tests=$(jq '.run.stats.tests.total // 0' simple-test-results.json)
            local passed_tests=$(jq '.run.stats.tests.passed // 0' simple-test-results.json)
            local failed_tests=$(jq '.run.stats.tests.failed // 0' simple-test-results.json)
            
            echo "Total Tests: $total_tests"
            echo "Passed: $passed_tests"
            echo "Failed: $failed_tests"
            
            if [ "$failed_tests" -eq 0 ]; then
                log_success "All tests passed!"
            else
                log_warning "$failed_tests tests failed"
            fi
        else
            log_info "Install jq for detailed test statistics"
        fi
        
        echo "Full results available in: simple-test-results.json"
    fi
}

# Main execution
main() {
    echo "========================================="
    echo "Enhanced Enterprise Banking System"
    echo "Postman API Test Runner"
    echo "========================================="
    echo ""
    
    check_prerequisites
    check_services
    
    case "${1:-simple}" in
        "health")
            if [ -f "$COLLECTION_FILE" ]; then
                run_health_tests
            else
                run_simple_tests
            fi
            ;;
        "simple")
            run_simple_tests
            ;;
        "folder")
            if [ -z "$2" ]; then
                log_error "Please specify folder name for folder tests"
                exit 1
            fi
            run_folder_tests "$2"
            ;;
        "all")
            run_all_tests
            ;;
        *)
            echo "Usage: $0 [simple|health|folder <folder-name>|all]"
            echo ""
            echo "Commands:"
            echo "  simple  - Run basic health and connectivity tests (default)"
            echo "  health  - Run health check tests from main collection"
            echo "  folder  - Run tests from specific folder"
            echo "  all     - Run complete test suite with authentication"
            echo ""
            echo "Examples:"
            echo "  $0 simple"
            echo "  $0 folder '📊 Monitoring & Health'"
            echo "  $0 all"
            exit 1
            ;;
    esac
    
    generate_report
    
    log_success "Test execution completed!"
}

# Execute main function with all arguments
main "$@"