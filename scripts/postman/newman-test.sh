#!/bin/bash

# Enterprise Loan Management System - Newman Test Runner
# Automated API testing using Newman (Postman CLI)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Logging functions
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] ✅ $1${NC}"
}

log_error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ❌ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] ⚠️  $1${NC}"
}

log_info() {
    echo -e "${CYAN}[$(date +'%Y-%m-%d %H:%M:%S')] ℹ️  $1${NC}"
}

# Check if Newman is installed
check_newman() {
    if ! command -v newman &> /dev/null; then
        log_warning "Newman is not installed. Installing Newman..."
        npm install -g newman newman-reporter-htmlextra newman-reporter-junitfull
    fi
}

# Environment configuration
create_environment() {
    log "📋 Creating Postman environment..."
    
    cat > "$PROJECT_ROOT/postman-environment.json" << 'EOF'
{
    "id": "banking-env",
    "name": "Banking Local Environment",
    "values": [
        {
            "key": "base_url",
            "value": "https://localhost",
            "enabled": true
        },
        {
            "key": "auth_url",
            "value": "http://localhost:8080",
            "enabled": true
        },
        {
            "key": "access_token",
            "value": "",
            "enabled": true
        },
        {
            "key": "customer_id",
            "value": "110e8400-e29b-41d4-a716-446655440001",
            "enabled": true
        },
        {
            "key": "loan_id",
            "value": "990e8400-e29b-41d4-a716-446655440001",
            "enabled": true
        },
        {
            "key": "payment_id",
            "value": "770e8400-e29b-41d4-a716-446655440001",
            "enabled": true
        }
    ],
    "_postman_variable_scope": "environment"
}
EOF
    
    log_success "Environment file created"
}

# Run tests
run_tests() {
    local test_type="${1:-all}"
    local collection_file="$PROJECT_ROOT/Enterprise-Banking-API-Collection.postman_collection.json"
    local env_file="$PROJECT_ROOT/postman-environment.json"
    local reports_dir="$PROJECT_ROOT/test-reports"
    
    # Create reports directory
    mkdir -p "$reports_dir"
    
    # Timestamp for report files
    local timestamp=$(date +"%Y%m%d_%H%M%S")
    
    case "$test_type" in
        "smoke")
            log "🔥 Running smoke tests..."
            newman run "$collection_file" \
                --environment "$env_file" \
                --folder "Authentication & Security" \
                --folder "Health & Monitoring" \
                --insecure \
                --reporters cli,htmlextra,junitfull \
                --reporter-htmlextra-export "$reports_dir/smoke-test-report-${timestamp}.html" \
                --reporter-junitfull-export "$reports_dir/smoke-test-report-${timestamp}.xml" \
                --suppress-exit-code
            ;;
            
        "security")
            log "🔒 Running security tests..."
            newman run "$collection_file" \
                --environment "$env_file" \
                --folder "Authentication & Security" \
                --folder "Test Scenarios" \
                --insecure \
                --reporters cli,htmlextra,junitfull \
                --reporter-htmlextra-export "$reports_dir/security-test-report-${timestamp}.html" \
                --reporter-junitfull-export "$reports_dir/security-test-report-${timestamp}.xml" \
                --suppress-exit-code
            ;;
            
        "api")
            log "🔌 Running API tests..."
            newman run "$collection_file" \
                --environment "$env_file" \
                --folder "Customer Management" \
                --folder "Loan Management" \
                --folder "Payment Processing" \
                --insecure \
                --iteration-count 1 \
                --reporters cli,htmlextra,junitfull \
                --reporter-htmlextra-export "$reports_dir/api-test-report-${timestamp}.html" \
                --reporter-junitfull-export "$reports_dir/api-test-report-${timestamp}.xml" \
                --suppress-exit-code
            ;;
            
        "banking")
            log "🏦 Running banking workflow tests..."
            newman run "$collection_file" \
                --environment "$env_file" \
                --folder "Customer Management" \
                --folder "Loan Management" \
                --folder "Payment Processing" \
                --folder "Open Banking Gateway" \
                --insecure \
                --reporters cli,htmlextra,junitfull \
                --reporter-htmlextra-export "$reports_dir/banking-test-report-${timestamp}.html" \
                --reporter-junitfull-export "$reports_dir/banking-test-report-${timestamp}.xml" \
                --suppress-exit-code
            ;;
            
        "ml")
            log "🤖 Running ML/AI tests..."
            newman run "$collection_file" \
                --environment "$env_file" \
                --folder "AI & Machine Learning" \
                --insecure \
                --reporters cli,htmlextra,junitfull \
                --reporter-htmlextra-export "$reports_dir/ml-test-report-${timestamp}.html" \
                --reporter-junitfull-export "$reports_dir/ml-test-report-${timestamp}.xml" \
                --suppress-exit-code
            ;;
            
        "compliance")
            log "📊 Running compliance tests..."
            newman run "$collection_file" \
                --environment "$env_file" \
                --folder "Compliance & Reporting" \
                --insecure \
                --reporters cli,htmlextra,junitfull \
                --reporter-htmlextra-export "$reports_dir/compliance-test-report-${timestamp}.html" \
                --reporter-junitfull-export "$reports_dir/compliance-test-report-${timestamp}.xml" \
                --suppress-exit-code
            ;;
            
        "performance")
            log "⚡ Running performance tests..."
            newman run "$collection_file" \
                --environment "$env_file" \
                --folder "Test Scenarios" \
                --insecure \
                --iteration-count 10 \
                --delay-request 100 \
                --reporters cli,htmlextra,junitfull \
                --reporter-htmlextra-export "$reports_dir/performance-test-report-${timestamp}.html" \
                --reporter-junitfull-export "$reports_dir/performance-test-report-${timestamp}.xml" \
                --suppress-exit-code
            ;;
            
        "all")
            log "🧪 Running all tests..."
            newman run "$collection_file" \
                --environment "$env_file" \
                --insecure \
                --reporters cli,htmlextra,junitfull \
                --reporter-htmlextra-export "$reports_dir/all-tests-report-${timestamp}.html" \
                --reporter-junitfull-export "$reports_dir/all-tests-report-${timestamp}.xml" \
                --suppress-exit-code
            ;;
            
        *)
            log_error "Unknown test type: $test_type"
            echo "Usage: $0 [test-type]"
            echo "Test types:"
            echo "  smoke       - Quick smoke tests"
            echo "  security    - Security and authentication tests"
            echo "  api         - Core API functionality tests"
            echo "  banking     - Banking workflow tests"
            echo "  ml          - ML/AI feature tests"
            echo "  compliance  - Compliance and reporting tests"
            echo "  performance - Performance and load tests"
            echo "  all         - All tests (default)"
            exit 1
            ;;
    esac
    
    # Check if reports were generated
    if ls "$reports_dir"/*-${timestamp}.html 1> /dev/null 2>&1; then
        log_success "Test reports generated in: $reports_dir"
        log_info "HTML Report: $reports_dir/*-${timestamp}.html"
        log_info "JUnit Report: $reports_dir/*-${timestamp}.xml"
    fi
}

# Generate test summary
generate_summary() {
    local reports_dir="$PROJECT_ROOT/test-reports"
    
    log "📊 Generating test summary..."
    
    # Find the latest HTML report
    local latest_report=$(ls -t "$reports_dir"/*.html 2>/dev/null | head -1)
    
    if [ -n "$latest_report" ]; then
        log_info "Latest test report: $latest_report"
        
        # Open report in browser if available
        if command -v open &> /dev/null; then
            open "$latest_report"
        elif command -v xdg-open &> /dev/null; then
            xdg-open "$latest_report"
        fi
    fi
}

# Continuous testing mode
continuous_mode() {
    log "🔄 Starting continuous testing mode..."
    log_info "Running tests every 5 minutes. Press Ctrl+C to stop."
    
    while true; do
        run_tests "smoke"
        log_info "Waiting 5 minutes before next test run..."
        sleep 300
    done
}

# Main execution
main() {
    cd "$PROJECT_ROOT"
    
    case "${1:-all}" in
        "setup")
            check_newman
            create_environment
            log_success "Newman setup complete"
            ;;
            
        "continuous")
            check_newman
            create_environment
            continuous_mode
            ;;
            
        "help"|"-h"|"--help")
            echo "Enterprise Banking System - Newman Test Runner"
            echo ""
            echo "Usage: $0 [command]"
            echo ""
            echo "Commands:"
            echo "  setup       - Install Newman and create environment"
            echo "  smoke       - Run smoke tests"
            echo "  security    - Run security tests"
            echo "  api         - Run API tests"
            echo "  banking     - Run banking workflow tests"
            echo "  ml          - Run ML/AI tests"
            echo "  compliance  - Run compliance tests"
            echo "  performance - Run performance tests"
            echo "  all         - Run all tests (default)"
            echo "  continuous  - Run continuous testing"
            echo "  help        - Show this help message"
            ;;
            
        *)
            check_newman
            create_environment
            run_tests "$1"
            generate_summary
            ;;
    esac
}

# Execute main function
main "$@"