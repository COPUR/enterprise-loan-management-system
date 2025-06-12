#!/bin/bash

# Comprehensive Load Testing Suite for Microservices Architecture
# Tests individual services and end-to-end workflows under load

set -e

# Configuration
API_GATEWAY_URL="http://localhost:8080"
CUSTOMER_SERVICE_URL="http://localhost:8081"
LOAN_SERVICE_URL="http://localhost:8082"
PAYMENT_SERVICE_URL="http://localhost:8083"

# Test parameters
CONCURRENT_USERS=100
TEST_DURATION=300  # 5 minutes
RAMP_UP_TIME=60    # 1 minute
REQUESTS_PER_SECOND=50

# Results directory
RESULTS_DIR="load-test-results-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$RESULTS_DIR"

echo "🚀 Microservices Load Testing Suite"
echo "===================================="
echo "Target: $API_GATEWAY_URL"
echo "Concurrent Users: $CONCURRENT_USERS"
echo "Test Duration: ${TEST_DURATION}s"
echo "Results Directory: $RESULTS_DIR"
echo ""

# Check dependencies
check_tools() {
    local missing_tools=()
    
    for tool in curl ab wrk jq; do
        if ! command -v $tool &> /dev/null; then
            missing_tools+=($tool)
        fi
    done
    
    if [[ ${#missing_tools[@]} -gt 0 ]]; then
        echo "Missing required tools: ${missing_tools[*]}"
        echo "Installing available tools..."
        
        if command -v apt-get &> /dev/null; then
            sudo apt-get update
            sudo apt-get install -y apache2-utils curl jq
        fi
        
        # Install wrk if not available
        if ! command -v wrk &> /dev/null; then
            echo "Installing wrk..."
            git clone https://github.com/wg/wrk.git /tmp/wrk
            cd /tmp/wrk && make && sudo cp wrk /usr/local/bin/
            cd - > /dev/null
        fi
    fi
}

# System warmup
warmup_services() {
    echo "Warming up services..."
    
    local services=("$API_GATEWAY_URL" "$CUSTOMER_SERVICE_URL" "$LOAN_SERVICE_URL" "$PAYMENT_SERVICE_URL")
    
    for service in "${services[@]}"; do
        for i in {1..10}; do
            curl -s "$service/actuator/health" > /dev/null 2>&1 || true
            sleep 1
        done
        echo "  - Warmed up: $service"
    done
    
    echo "Warmup completed"
    echo ""
}

# Individual service load tests
test_service_endpoints() {
    echo "Testing individual service endpoints..."
    
    # API Gateway Health Check
    echo "Testing API Gateway health endpoint..."
    ab -n 1000 -c 10 -g "$RESULTS_DIR/gateway-health.tsv" \
        "$API_GATEWAY_URL/actuator/health" > "$RESULTS_DIR/gateway-health.txt" 2>&1
    
    # Customer Service Load Test
    echo "Testing Customer Service endpoints..."
    ab -n 500 -c 5 -g "$RESULTS_DIR/customer-service.tsv" \
        "$CUSTOMER_SERVICE_URL/actuator/health" > "$RESULTS_DIR/customer-service.txt" 2>&1
    
    # Loan Service Load Test
    echo "Testing Loan Service endpoints..."
    ab -n 500 -c 5 -g "$RESULTS_DIR/loan-service.tsv" \
        "$LOAN_SERVICE_URL/actuator/health" > "$RESULTS_DIR/loan-service.txt" 2>&1
    
    # Payment Service Load Test
    echo "Testing Payment Service endpoints..."
    ab -n 500 -c 5 -g "$RESULTS_DIR/payment-service.tsv" \
        "$PAYMENT_SERVICE_URL/actuator/health" > "$RESULTS_DIR/payment-service.txt" 2>&1
    
    echo "Individual service tests completed"
    echo ""
}

# End-to-end workflow test
test_business_workflows() {
    echo "Testing end-to-end business workflows..."
    
    # Create test script for customer creation workflow
    cat > "$RESULTS_DIR/customer-workflow.lua" << 'EOF'
wrk.method = "POST"
wrk.body = '{"customerId":"LOAD-TEST-'..math.random(1000000)..'","firstName":"Test","lastName":"User","email":"test@example.com","creditLimit":50000.00}'
wrk.headers["Content-Type"] = "application/json"

function response(status, headers, body)
    if status ~= 200 and status ~= 201 then
        print("Error response: " .. status)
    end
end
EOF
    
    # Run customer creation workflow test
    wrk -t4 -c20 -d60s -s "$RESULTS_DIR/customer-workflow.lua" \
        "$API_GATEWAY_URL/api/v1/customers" > "$RESULTS_DIR/customer-workflow.txt" 2>&1
    
    # Create loan application workflow test
    cat > "$RESULTS_DIR/loan-workflow.lua" << 'EOF'
wrk.method = "POST"
wrk.body = '{"customerId":"CUST-LOAD-TEST","loanAmount":25000.00,"interestRate":0.15,"installmentCount":12,"loanType":"PERSONAL"}'
wrk.headers["Content-Type"] = "application/json"

function response(status, headers, body)
    if status ~= 200 and status ~= 201 and status ~= 400 then
        print("Unexpected response: " .. status)
    end
end
EOF
    
    # Run loan workflow test
    wrk -t2 -c10 -d30s -s "$RESULTS_DIR/loan-workflow.lua" \
        "$API_GATEWAY_URL/api/v1/loans" > "$RESULTS_DIR/loan-workflow.txt" 2>&1
    
    echo "Business workflow tests completed"
    echo ""
}

# Circuit breaker stress test
test_circuit_breaker() {
    echo "Testing circuit breaker patterns..."
    
    # Generate high load to trigger circuit breaker
    ab -n 2000 -c 50 -g "$RESULTS_DIR/circuit-breaker.tsv" \
        "$API_GATEWAY_URL/api/v1/customers/NONEXISTENT-CUSTOMER" \
        > "$RESULTS_DIR/circuit-breaker.txt" 2>&1
    
    # Check circuit breaker status
    curl -s "$API_GATEWAY_URL/actuator/circuitbreakers" \
        > "$RESULTS_DIR/circuit-breaker-status.json" 2>&1
    
    echo "Circuit breaker tests completed"
    echo ""
}

# Rate limiting test
test_rate_limiting() {
    echo "Testing rate limiting..."
    
    # Generate rapid requests to test rate limiting
    ab -n 1500 -c 30 -g "$RESULTS_DIR/rate-limiting.tsv" \
        "$API_GATEWAY_URL/actuator/health" > "$RESULTS_DIR/rate-limiting.txt" 2>&1
    
    # Count 429 responses
    local rate_limit_responses=$(grep "429" "$RESULTS_DIR/rate-limiting.txt" | wc -l || echo "0")
    echo "Rate limiting responses detected: $rate_limit_responses"
    
    echo "Rate limiting tests completed"
    echo ""
}

# Database connection pool test
test_database_performance() {
    echo "Testing database connection pool performance..."
    
    # Create concurrent database queries through services
    for i in {1..5}; do
        (
            for j in {1..100}; do
                curl -s "$CUSTOMER_SERVICE_URL/actuator/health" > /dev/null
                sleep 0.1
            done
        ) &
    done
    
    wait
    
    # Check HikariCP metrics
    curl -s "$CUSTOMER_SERVICE_URL/actuator/metrics/hikaricp.connections.active" \
        > "$RESULTS_DIR/db-connection-metrics.json" 2>&1
    
    echo "Database performance tests completed"
    echo ""
}

# Memory and CPU monitoring during load
monitor_resources() {
    echo "Monitoring system resources during load test..."
    
    # Start monitoring in background
    (
        while true; do
            echo "$(date): $(free -m | grep '^Mem:' | awk '{print "Memory: " $3 "MB/" $2 "MB (" int($3*100/$2) "%)"}')"
            echo "$(date): $(top -bn1 | grep '^%Cpu' | awk '{print "CPU: " $2}')"
            
            # Java process monitoring
            local java_pids=$(pgrep -f "java.*loanmanagement" || echo "")
            if [[ -n "$java_pids" ]]; then
                for pid in $java_pids; do
                    if [[ -d "/proc/$pid" ]]; then
                        local mem_kb=$(awk '/VmRSS/ {print $2}' "/proc/$pid/status" 2>/dev/null || echo "0")
                        local mem_mb=$((mem_kb / 1024))
                        echo "$(date): Java PID $pid: ${mem_mb}MB"
                    fi
                done
            fi
            
            sleep 10
        done
    ) > "$RESULTS_DIR/resource-monitoring.log" &
    
    local monitor_pid=$!
    
    # Run main load test
    echo "Running comprehensive load test..."
    wrk -t8 -c$CONCURRENT_USERS -d${TEST_DURATION}s \
        --latency "$API_GATEWAY_URL/actuator/health" \
        > "$RESULTS_DIR/comprehensive-load-test.txt" 2>&1
    
    # Stop monitoring
    kill $monitor_pid 2>/dev/null || true
    
    echo "Resource monitoring completed"
    echo ""
}

# SAGA pattern load test
test_saga_performance() {
    echo "Testing SAGA orchestration under load..."
    
    # Create SAGA workflow test script
    cat > "$RESULTS_DIR/saga-workflow.lua" << 'EOF'
wrk.method = "POST"
wrk.body = '{"customerId":"CUST-SAGA-TEST","loanAmount":15000.00,"interestRate":0.12,"installmentCount":9,"loanType":"PERSONAL"}'
wrk.headers["Content-Type"] = "application/json"

request_count = 0
function request()
    request_count = request_count + 1
    return wrk.format(nil, "/api/v1/saga/loan-creation")
end

function response(status, headers, body)
    if status == 202 then
        -- SAGA initiated successfully
    elseif status >= 400 then
        print("SAGA error: " .. status)
    end
end
EOF
    
    # Run SAGA load test with lower concurrency (distributed transactions are expensive)
    wrk -t2 -c5 -d60s -s "$RESULTS_DIR/saga-workflow.lua" \
        "$API_GATEWAY_URL" > "$RESULTS_DIR/saga-performance.txt" 2>&1
    
    echo "SAGA performance tests completed"
    echo ""
}

# Security load test
test_security_under_load() {
    echo "Testing security features under load..."
    
    # Test with various security scenarios
    
    # SQL injection attempts
    ab -n 100 -c 5 \
        "$API_GATEWAY_URL/api/v1/customers?id=1' OR '1'='1" \
        > "$RESULTS_DIR/security-sql-injection.txt" 2>&1
    
    # XSS attempts
    ab -n 100 -c 5 -H "X-Test: <script>alert('xss')</script>" \
        "$API_GATEWAY_URL/actuator/health" \
        > "$RESULTS_DIR/security-xss.txt" 2>&1
    
    # Large payload test
    dd if=/dev/zero bs=1024 count=1024 2>/dev/null | base64 > "$RESULTS_DIR/large-payload.txt"
    curl -X POST -H "Content-Type: application/json" \
        -d "@$RESULTS_DIR/large-payload.txt" \
        "$API_GATEWAY_URL/api/v1/customers" \
        > "$RESULTS_DIR/security-large-payload.txt" 2>&1
    
    echo "Security load tests completed"
    echo ""
}

# Generate comprehensive report
generate_report() {
    echo "Generating load test report..."
    
    local report_file="$RESULTS_DIR/load-test-report.html"
    
    cat > "$report_file" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>Microservices Load Test Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background: #f0f0f0; padding: 20px; border-radius: 5px; }
        .section { margin: 20px 0; padding: 15px; border: 1px solid #ddd; border-radius: 5px; }
        .metric { display: inline-block; margin: 10px; padding: 10px; background: #e8f4f8; border-radius: 3px; }
        .success { color: green; }
        .warning { color: orange; }
        .error { color: red; }
        pre { background: #f5f5f5; padding: 10px; overflow-x: auto; }
    </style>
</head>
<body>
    <div class="header">
        <h1>Enterprise Loan Management System - Load Test Report</h1>
        <p><strong>Test Date:</strong> $(date)</p>
        <p><strong>Configuration:</strong> $CONCURRENT_USERS concurrent users, ${TEST_DURATION}s duration</p>
        <p><strong>Target:</strong> $API_GATEWAY_URL</p>
    </div>
    
    <div class="section">
        <h2>Test Summary</h2>
EOF
    
    # Extract key metrics from test results
    if [[ -f "$RESULTS_DIR/comprehensive-load-test.txt" ]]; then
        local rps=$(grep "Requests/sec:" "$RESULTS_DIR/comprehensive-load-test.txt" | awk '{print $2}' || echo "N/A")
        local avg_latency=$(grep "Latency" "$RESULTS_DIR/comprehensive-load-test.txt" | awk '{print $2}' || echo "N/A")
        local total_requests=$(grep "requests in" "$RESULTS_DIR/comprehensive-load-test.txt" | awk '{print $1}' || echo "N/A")
        
        cat >> "$report_file" << EOF
        <div class="metric">
            <strong>Requests per Second:</strong> $rps
        </div>
        <div class="metric">
            <strong>Average Latency:</strong> $avg_latency
        </div>
        <div class="metric">
            <strong>Total Requests:</strong> $total_requests
        </div>
EOF
    fi
    
    cat >> "$report_file" << EOF
    </div>
    
    <div class="section">
        <h2>Service Performance</h2>
        <h3>API Gateway</h3>
        <pre>$(cat "$RESULTS_DIR/gateway-health.txt" 2>/dev/null | head -20 || echo "No data available")</pre>
        
        <h3>Customer Service</h3>
        <pre>$(cat "$RESULTS_DIR/customer-service.txt" 2>/dev/null | head -20 || echo "No data available")</pre>
        
        <h3>Loan Service</h3>
        <pre>$(cat "$RESULTS_DIR/loan-service.txt" 2>/dev/null | head -20 || echo "No data available")</pre>
        
        <h3>Payment Service</h3>
        <pre>$(cat "$RESULTS_DIR/payment-service.txt" 2>/dev/null | head -20 || echo "No data available")</pre>
    </div>
    
    <div class="section">
        <h2>Circuit Breaker Analysis</h2>
        <pre>$(cat "$RESULTS_DIR/circuit-breaker-status.json" 2>/dev/null | jq '.' || echo "No circuit breaker data available")</pre>
    </div>
    
    <div class="section">
        <h2>Resource Utilization</h2>
        <pre>$(tail -50 "$RESULTS_DIR/resource-monitoring.log" 2>/dev/null || echo "No resource monitoring data available")</pre>
    </div>
    
    <div class="section">
        <h2>Recommendations</h2>
        <ul>
            <li>Monitor response times under sustained load</li>
            <li>Verify circuit breaker thresholds are appropriate</li>
            <li>Check database connection pool sizing</li>
            <li>Consider horizontal scaling for high-load scenarios</li>
            <li>Implement caching strategies for frequently accessed data</li>
        </ul>
    </div>
</body>
</html>
EOF
    
    echo "Report generated: $report_file"
    echo ""
}

# Performance analysis
analyze_results() {
    echo "Analyzing test results..."
    
    # Create summary statistics
    cat > "$RESULTS_DIR/summary.txt" << EOF
Load Test Summary - $(date)
==============================

Test Configuration:
- Concurrent Users: $CONCURRENT_USERS
- Test Duration: ${TEST_DURATION}s
- Target URL: $API_GATEWAY_URL

Key Findings:
EOF
    
    # Analyze response times
    if [[ -f "$RESULTS_DIR/comprehensive-load-test.txt" ]]; then
        local rps=$(grep "Requests/sec:" "$RESULTS_DIR/comprehensive-load-test.txt" | awk '{print $2}' || echo "0")
        local avg_latency=$(grep "Latency" "$RESULTS_DIR/comprehensive-load-test.txt" | awk '{print $2}' || echo "0ms")
        
        echo "- Achieved $rps requests per second" >> "$RESULTS_DIR/summary.txt"
        echo "- Average latency: $avg_latency" >> "$RESULTS_DIR/summary.txt"
        
        # Performance evaluation
        if (( $(echo "$rps > 100" | bc -l 2>/dev/null || echo "0") )); then
            echo "- Performance: EXCELLENT (>100 RPS)" >> "$RESULTS_DIR/summary.txt"
        elif (( $(echo "$rps > 50" | bc -l 2>/dev/null || echo "0") )); then
            echo "- Performance: GOOD (>50 RPS)" >> "$RESULTS_DIR/summary.txt"
        else
            echo "- Performance: NEEDS OPTIMIZATION (<50 RPS)" >> "$RESULTS_DIR/summary.txt"
        fi
    fi
    
    # Check for errors
    local error_count=0
    for file in "$RESULTS_DIR"/*.txt; do
        if [[ -f "$file" ]]; then
            local file_errors=$(grep -i "error\|failed\|timeout" "$file" | wc -l || echo "0")
            error_count=$((error_count + file_errors))
        fi
    done
    
    echo "- Total errors detected: $error_count" >> "$RESULTS_DIR/summary.txt"
    
    if [[ $error_count -eq 0 ]]; then
        echo "- Error rate: EXCELLENT (0 errors)" >> "$RESULTS_DIR/summary.txt"
    elif [[ $error_count -lt 10 ]]; then
        echo "- Error rate: ACCEPTABLE (<10 errors)" >> "$RESULTS_DIR/summary.txt"
    else
        echo "- Error rate: HIGH (>10 errors, needs investigation)" >> "$RESULTS_DIR/summary.txt"
    fi
    
    echo ""
    echo "Analysis completed. Summary:"
    cat "$RESULTS_DIR/summary.txt"
    echo ""
}

# Main execution
main() {
    echo "Starting comprehensive microservices load testing..."
    echo ""
    
    check_tools
    warmup_services
    
    # Run all test categories
    test_service_endpoints
    test_business_workflows
    test_circuit_breaker
    test_rate_limiting
    test_database_performance
    monitor_resources
    test_saga_performance
    test_security_under_load
    
    # Generate reports and analysis
    generate_report
    analyze_results
    
    echo "===================================="
    echo "Load testing completed successfully!"
    echo "Results available in: $RESULTS_DIR"
    echo "HTML Report: $RESULTS_DIR/load-test-report.html"
    echo "Summary: $RESULTS_DIR/summary.txt"
    echo "===================================="
}

# Execute main function
main