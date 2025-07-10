#!/bin/bash

# Enterprise Loan Management System - Test Runner
# Comprehensive end-to-end testing for the banking system

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Test results
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Logging functions
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] ✅ $1${NC}"
    ((PASSED_TESTS++))
}

log_error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ❌ $1${NC}"
    ((FAILED_TESTS++))
}

log_warning() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] ⚠️  $1${NC}"
}

log_info() {
    echo -e "${CYAN}[$(date +'%Y-%m-%d %H:%M:%S')] ℹ️  $1${NC}"
}

# Test function wrapper
run_test() {
    local test_name="$1"
    local test_function="$2"
    
    log "🧪 Running test: $test_name"
    ((TOTAL_TESTS++))
    
    if $test_function; then
        log_success "Test passed: $test_name"
    else
        log_error "Test failed: $test_name"
    fi
}

# Service health tests
test_postgres_health() {
    docker-compose exec -T postgres pg_isready -U banking_user -d banking_db >/dev/null 2>&1
}

test_redis_health() {
    docker-compose exec -T redis redis-cli -a banking_password ping >/dev/null 2>&1
}

test_kafka_health() {
    docker-compose exec -T kafka kafka-broker-api-versions --bootstrap-server localhost:9092 >/dev/null 2>&1
}

test_keycloak_health() {
    curl -s --max-time 10 http://localhost:8080/health/ready >/dev/null 2>&1
}

test_elasticsearch_health() {
    curl -s --max-time 10 http://localhost:9200/_cluster/health >/dev/null 2>&1
}

# Banking service tests
test_party_data_server_health() {
    curl -s --max-time 10 http://localhost:8081/actuator/health >/dev/null 2>&1
}

test_api_gateway_health() {
    curl -s --max-time 10 http://localhost:8082/actuator/health >/dev/null 2>&1
}

test_customer_service_health() {
    curl -s --max-time 10 http://localhost:8083/actuator/health >/dev/null 2>&1
}

test_loan_service_health() {
    curl -s --max-time 10 http://localhost:8084/actuator/health >/dev/null 2>&1
}

test_payment_service_health() {
    curl -s --max-time 10 http://localhost:8085/actuator/health >/dev/null 2>&1
}

test_open_banking_gateway_health() {
    curl -s --max-time 10 http://localhost:8086/actuator/health >/dev/null 2>&1
}

test_ml_anomaly_service_health() {
    curl -s --max-time 10 http://localhost:8087/actuator/health >/dev/null 2>&1
}

test_federation_monitoring_health() {
    curl -s --max-time 10 http://localhost:8088/actuator/health >/dev/null 2>&1
}

# Monitoring service tests
test_prometheus_health() {
    curl -s --max-time 10 http://localhost:9090/-/healthy >/dev/null 2>&1
}

test_grafana_health() {
    curl -s --max-time 10 http://localhost:3000/api/health >/dev/null 2>&1
}

test_kibana_health() {
    curl -s --max-time 10 http://localhost:5601/api/status >/dev/null 2>&1
}

test_jaeger_health() {
    curl -s --max-time 10 http://localhost:16686/ >/dev/null 2>&1
}

test_nginx_health() {
    curl -s --max-time 10 http://localhost/health >/dev/null 2>&1
}

# Database tests
test_database_schema() {
    local result=$(docker-compose exec -T postgres psql -U banking_user -d banking_db -c "
        SELECT COUNT(*) FROM information_schema.schemata 
        WHERE schema_name LIKE 'banking_%';" -t)
    [ "$(echo "$result" | tr -d ' \n')" -eq 7 ]
}

test_sample_data() {
    local customer_count=$(docker-compose exec -T postgres psql -U banking_user -d banking_db -c "
        SELECT COUNT(*) FROM banking_customer.customers;" -t)
    [ "$(echo "$customer_count" | tr -d ' \n')" -gt 0 ]
}

test_redis_data() {
    local config_keys=$(docker-compose exec -T redis redis-cli -a banking_password --no-auth-warning KEYS "config:*" | wc -l)
    [ "$config_keys" -gt 0 ]
}

test_kafka_topics() {
    local topic_count=$(docker-compose exec -T kafka kafka-topics --bootstrap-server localhost:9092 --list | wc -l)
    [ "$topic_count" -gt 50 ]
}

# API endpoint tests
test_oauth_token_endpoint() {
    local response=$(curl -s --max-time 10 -X POST \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=client_credentials&client_id=banking-app&client_secret=banking-secret&scope=banking" \
        http://localhost:8080/realms/banking/protocol/openid-connect/token)
    echo "$response" | grep -q "access_token"
}

test_customer_api_endpoint() {
    # First get token
    local token=$(curl -s --max-time 10 -X POST \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=client_credentials&client_id=banking-app&client_secret=banking-secret&scope=banking" \
        http://localhost:8080/realms/banking/protocol/openid-connect/token | jq -r '.access_token')
    
    if [ "$token" != "null" ] && [ -n "$token" ]; then
        curl -s --max-time 10 -k -H "Authorization: Bearer $token" \
            https://localhost/customers >/dev/null 2>&1
    else
        return 1
    fi
}

test_loan_api_endpoint() {
    # First get token
    local token=$(curl -s --max-time 10 -X POST \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=client_credentials&client_id=banking-app&client_secret=banking-secret&scope=banking" \
        http://localhost:8080/realms/banking/protocol/openid-connect/token | jq -r '.access_token')
    
    if [ "$token" != "null" ] && [ -n "$token" ]; then
        curl -s --max-time 10 -k -H "Authorization: Bearer $token" \
            https://localhost/loans >/dev/null 2>&1
    else
        return 1
    fi
}

test_payment_api_endpoint() {
    # First get token
    local token=$(curl -s --max-time 10 -X POST \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=client_credentials&client_id=banking-app&client_secret=banking-secret&scope=banking" \
        http://localhost:8080/realms/banking/protocol/openid-connect/token | jq -r '.access_token')
    
    if [ "$token" != "null" ] && [ -n "$token" ]; then
        curl -s --max-time 10 -k -H "Authorization: Bearer $token" \
            https://localhost/payments/history >/dev/null 2>&1
    else
        return 1
    fi
}

# Security tests
test_ssl_certificate() {
    openssl s_client -connect localhost:443 -servername localhost </dev/null 2>/dev/null | \
        openssl x509 -noout -dates >/dev/null 2>&1
}

test_oauth_security() {
    # Test that endpoints require authentication
    local response_code=$(curl -s --max-time 10 -k -o /dev/null -w "%{http_code}" https://localhost/customers)
    [ "$response_code" -eq 401 ] || [ "$response_code" -eq 403 ]
}

test_rate_limiting() {
    # Test rate limiting by making multiple requests
    local failed_requests=0
    for i in {1..15}; do
        local response_code=$(curl -s --max-time 5 -o /dev/null -w "%{http_code}" http://localhost/health)
        if [ "$response_code" -eq 429 ]; then
            ((failed_requests++))
        fi
    done
    [ "$failed_requests" -gt 0 ]
}

# Performance tests
test_response_time() {
    local response_time=$(curl -s --max-time 10 -o /dev/null -w "%{time_total}" http://localhost/health)
    # Check if response time is less than 1 second
    [ "$(echo "$response_time < 1.0" | bc -l)" -eq 1 ]
}

test_concurrent_requests() {
    local success_count=0
    local pids=()
    
    # Start 10 concurrent requests
    for i in {1..10}; do
        {
            if curl -s --max-time 10 http://localhost/health >/dev/null 2>&1; then
                echo "success"
            fi
        } &
        pids+=($!)
    done
    
    # Wait for all requests to complete
    for pid in "${pids[@]}"; do
        if wait "$pid"; then
            ((success_count++))
        fi
    done
    
    # At least 8 out of 10 requests should succeed
    [ "$success_count" -ge 8 ]
}

# ML and Analytics tests
test_fraud_detection_api() {
    local token=$(curl -s --max-time 10 -X POST \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=client_credentials&client_id=banking-app&client_secret=banking-secret&scope=banking" \
        http://localhost:8080/realms/banking/protocol/openid-connect/token | jq -r '.access_token')
    
    if [ "$token" != "null" ] && [ -n "$token" ]; then
        curl -s --max-time 10 -k -H "Authorization: Bearer $token" \
            -H "Content-Type: application/json" \
            -d '{"transactionId":"TEST001","amount":100.00,"customerId":"110e8400-e29b-41d4-a716-446655440001"}' \
            https://localhost/ml/fraud-detection >/dev/null 2>&1
    else
        return 1
    fi
}

test_federation_monitoring() {
    local token=$(curl -s --max-time 10 -X POST \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=client_credentials&client_id=banking-app&client_secret=banking-secret&scope=banking" \
        http://localhost:8080/realms/banking/protocol/openid-connect/token | jq -r '.access_token')
    
    if [ "$token" != "null" ] && [ -n "$token" ]; then
        curl -s --max-time 10 -k -H "Authorization: Bearer $token" \
            https://localhost/federation/status >/dev/null 2>&1
    else
        return 1
    fi
}

# Monitoring tests
test_prometheus_metrics() {
    curl -s --max-time 10 http://localhost:9090/api/v1/query?query=up | grep -q "success"
}

test_grafana_datasource() {
    local response=$(curl -s --max-time 10 -u admin:admin http://localhost:3000/api/datasources)
    echo "$response" | grep -q "prometheus"
}

test_elasticsearch_indices() {
    curl -s --max-time 10 http://localhost:9200/_cat/indices | grep -q "banking"
}

test_jaeger_services() {
    curl -s --max-time 10 http://localhost:16686/api/services | grep -q "banking"
}

# Integration tests
test_end_to_end_loan_flow() {
    local token=$(curl -s --max-time 10 -X POST \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=client_credentials&client_id=banking-app&client_secret=banking-secret&scope=banking" \
        http://localhost:8080/realms/banking/protocol/openid-connect/token | jq -r '.access_token')
    
    if [ "$token" != "null" ] && [ -n "$token" ]; then
        # Test customer lookup
        local customer_response=$(curl -s --max-time 10 -k -H "Authorization: Bearer $token" \
            https://localhost/customers/110e8400-e29b-41d4-a716-446655440001)
        
        if echo "$customer_response" | grep -q "customer"; then
            # Test loan application
            local loan_response=$(curl -s --max-time 10 -k -H "Authorization: Bearer $token" \
                -H "Content-Type: application/json" \
                -d '{"customerId":"110e8400-e29b-41d4-a716-446655440001","loanType":"PERSONAL","requestedAmount":25000.00,"termMonths":60}' \
                https://localhost/loans/applications)
            
            echo "$loan_response" | grep -q "application"
        else
            return 1
        fi
    else
        return 1
    fi
}

# Main test runner
run_all_tests() {
    log "🧪 Starting comprehensive test suite for Enterprise Loan Management System..."
    
    echo -e "${PURPLE}"
    cat << 'EOF'
╔══════════════════════════════════════════════════════════════════════════════════════════════════════════╗
║                                                                                                          ║
║                                      🧪 Test Suite Execution 🧪                                         ║
║                                                                                                          ║
║                               Enterprise Loan Management System                                         ║
║                                                                                                          ║
╚══════════════════════════════════════════════════════════════════════════════════════════════════════════╝
EOF
    echo -e "${NC}"
    
    # Infrastructure health tests
    log "🏗️  Testing infrastructure health..."
    run_test "PostgreSQL Health" test_postgres_health
    run_test "Redis Health" test_redis_health
    run_test "Kafka Health" test_kafka_health
    run_test "Keycloak Health" test_keycloak_health
    run_test "Elasticsearch Health" test_elasticsearch_health
    
    # Banking service health tests
    log "🏦 Testing banking service health..."
    run_test "Party Data Server Health" test_party_data_server_health
    run_test "API Gateway Health" test_api_gateway_health
    run_test "Customer Service Health" test_customer_service_health
    run_test "Loan Service Health" test_loan_service_health
    run_test "Payment Service Health" test_payment_service_health
    run_test "Open Banking Gateway Health" test_open_banking_gateway_health
    run_test "ML Anomaly Service Health" test_ml_anomaly_service_health
    run_test "Federation Monitoring Health" test_federation_monitoring_health
    
    # Monitoring service tests
    log "📊 Testing monitoring services..."
    run_test "Prometheus Health" test_prometheus_health
    run_test "Grafana Health" test_grafana_health
    run_test "Kibana Health" test_kibana_health
    run_test "Jaeger Health" test_jaeger_health
    run_test "Nginx Health" test_nginx_health
    
    # Data tests
    log "💾 Testing data integrity..."
    run_test "Database Schema" test_database_schema
    run_test "Sample Data" test_sample_data
    run_test "Redis Data" test_redis_data
    run_test "Kafka Topics" test_kafka_topics
    
    # API endpoint tests
    log "🔌 Testing API endpoints..."
    run_test "OAuth Token Endpoint" test_oauth_token_endpoint
    run_test "Customer API Endpoint" test_customer_api_endpoint
    run_test "Loan API Endpoint" test_loan_api_endpoint
    run_test "Payment API Endpoint" test_payment_api_endpoint
    
    # Security tests
    log "🔒 Testing security..."
    run_test "SSL Certificate" test_ssl_certificate
    run_test "OAuth Security" test_oauth_security
    run_test "Rate Limiting" test_rate_limiting
    
    # Performance tests
    log "⚡ Testing performance..."
    run_test "Response Time" test_response_time
    run_test "Concurrent Requests" test_concurrent_requests
    
    # ML and Analytics tests
    log "🤖 Testing ML and Analytics..."
    run_test "Fraud Detection API" test_fraud_detection_api
    run_test "Federation Monitoring" test_federation_monitoring
    
    # Monitoring tests
    log "📈 Testing monitoring..."
    run_test "Prometheus Metrics" test_prometheus_metrics
    run_test "Grafana Datasource" test_grafana_datasource
    run_test "Elasticsearch Indices" test_elasticsearch_indices
    run_test "Jaeger Services" test_jaeger_services
    
    # Integration tests
    log "🔄 Testing integration..."
    run_test "End-to-End Loan Flow" test_end_to_end_loan_flow
}

# Generate test report
generate_test_report() {
    local success_rate=$((PASSED_TESTS * 100 / TOTAL_TESTS))
    
    echo -e "${CYAN}"
    cat << 'EOF'
╔══════════════════════════════════════════════════════════════════════════════════════════════════════════╗
║                                                                                                          ║
║                                      📊 Test Results Summary 📊                                         ║
║                                                                                                          ║
╚══════════════════════════════════════════════════════════════════════════════════════════════════════════╝
EOF
    echo -e "${NC}"
    
    echo -e "${BLUE}Total Tests Run: ${TOTAL_TESTS}${NC}"
    echo -e "${GREEN}Tests Passed: ${PASSED_TESTS}${NC}"
    echo -e "${RED}Tests Failed: ${FAILED_TESTS}${NC}"
    echo -e "${YELLOW}Success Rate: ${success_rate}%${NC}"
    
    if [ "$success_rate" -ge 90 ]; then
        echo -e "${GREEN}🎉 Excellent! System is ready for production use.${NC}"
    elif [ "$success_rate" -ge 80 ]; then
        echo -e "${YELLOW}⚠️  Good! Some minor issues need attention.${NC}"
    elif [ "$success_rate" -ge 70 ]; then
        echo -e "${YELLOW}🔧 Several issues need to be resolved.${NC}"
    else
        echo -e "${RED}❌ System requires significant fixes before deployment.${NC}"
    fi
    
    # Save detailed report
    {
        echo "Enterprise Loan Management System - Test Report"
        echo "Generated: $(date)"
        echo "Total Tests: $TOTAL_TESTS"
        echo "Passed: $PASSED_TESTS"
        echo "Failed: $FAILED_TESTS"
        echo "Success Rate: $success_rate%"
        echo ""
        echo "Test execution completed at $(date)"
    } > "$PROJECT_ROOT/test-report.txt"
    
    log_info "Detailed test report saved to: test-report.txt"
}

# Quick health check
quick_health_check() {
    log "🏥 Performing quick health check..."
    
    local services=(
        "http://localhost:5432|PostgreSQL"
        "http://localhost:6379|Redis"
        "http://localhost:9092|Kafka"
        "http://localhost:8080|Keycloak"
        "http://localhost:8081/actuator/health|Party Data Server"
        "http://localhost:8082/actuator/health|API Gateway"
        "http://localhost:8083/actuator/health|Customer Service"
        "http://localhost:8084/actuator/health|Loan Service"
        "http://localhost:8085/actuator/health|Payment Service"
        "http://localhost:9090|Prometheus"
        "http://localhost:3000|Grafana"
        "http://localhost:80|Nginx"
    )
    
    local healthy=0
    local total=${#services[@]}
    
    for service in "${services[@]}"; do
        IFS='|' read -r url name <<< "$service"
        
        if curl -s --max-time 5 "$url" >/dev/null 2>&1; then
            log_success "$name is healthy"
            ((healthy++))
        else
            log_error "$name is not responding"
        fi
    done
    
    local health_rate=$((healthy * 100 / total))
    
    if [ "$health_rate" -ge 90 ]; then
        log_success "System health: $health_rate% - Excellent!"
    elif [ "$health_rate" -ge 80 ]; then
        log_warning "System health: $health_rate% - Good"
    else
        log_error "System health: $health_rate% - Needs attention"
    fi
}

# Performance benchmark
performance_benchmark() {
    log "⚡ Running performance benchmark..."
    
    # Test API Gateway performance
    log "Testing API Gateway performance..."
    local api_response_time=$(curl -s --max-time 10 -o /dev/null -w "%{time_total}" http://localhost:8082/actuator/health)
    log_info "API Gateway response time: ${api_response_time}s"
    
    # Test database performance
    log "Testing database performance..."
    local db_query_time=$(docker-compose exec -T postgres psql -U banking_user -d banking_db -c "
        \timing on
        SELECT COUNT(*) FROM banking_customer.customers;
    " 2>&1 | grep "Time:" | awk '{print $2}')
    log_info "Database query time: ${db_query_time:-unknown}"
    
    # Test Redis performance
    log "Testing Redis performance..."
    local redis_response_time=$(docker-compose exec -T redis redis-cli -a banking_password --no-auth-warning --latency-history -i 1 2>/dev/null | head -1)
    log_info "Redis latency: ${redis_response_time:-unknown}"
}

# Main execution
main() {
    cd "$PROJECT_ROOT"
    
    case "${1:-all}" in
        "all")
            run_all_tests
            generate_test_report
            ;;
        
        "health")
            quick_health_check
            ;;
        
        "performance")
            performance_benchmark
            ;;
        
        "security")
            log "🔒 Running security tests..."
            run_test "SSL Certificate" test_ssl_certificate
            run_test "OAuth Security" test_oauth_security
            run_test "Rate Limiting" test_rate_limiting
            ;;
        
        "api")
            log "🔌 Running API tests..."
            run_test "OAuth Token Endpoint" test_oauth_token_endpoint
            run_test "Customer API Endpoint" test_customer_api_endpoint
            run_test "Loan API Endpoint" test_loan_api_endpoint
            run_test "Payment API Endpoint" test_payment_api_endpoint
            ;;
        
        "integration")
            log "🔄 Running integration tests..."
            run_test "End-to-End Loan Flow" test_end_to_end_loan_flow
            ;;
        
        "help"|"-h"|"--help")
            echo "Enterprise Loan Management System - Test Runner"
            echo ""
            echo "Usage: $0 [command]"
            echo ""
            echo "Commands:"
            echo "  all           - Run all tests (default)"
            echo "  health        - Quick health check"
            echo "  performance   - Performance benchmark"
            echo "  security      - Security tests only"
            echo "  api           - API endpoint tests only"
            echo "  integration   - Integration tests only"
            echo "  help          - Show this help message"
            ;;
        
        *)
            log_error "Unknown command: $1"
            echo "Use '$0 help' for available commands"
            exit 1
            ;;
    esac
}

# Execute main function
main "$@"