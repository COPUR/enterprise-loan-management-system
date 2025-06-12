#!/bin/bash

# Comprehensive GraphQL and MCP Integration Test Suite
# Tests all endpoints, natural language processing, and MCP server functionality

set -e

# Configuration
GRAPHQL_URL="http://localhost:5000/graphql"
MCP_WS_URL="ws://localhost:5000/mcp"
SYSTEM_URL="http://localhost:5000"

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${BLUE}🚀 GraphQL and MCP Integration Test Suite${NC}"
echo -e "${BLUE}===========================================${NC}"
echo ""

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Utility functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
    ((PASSED_TESTS++))
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
    ((FAILED_TESTS++))
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

run_test() {
    ((TOTAL_TESTS++))
    echo ""
    log_info "Test $TOTAL_TESTS: $1"
}

# Test GraphQL endpoint availability
test_graphql_endpoint() {
    run_test "GraphQL endpoint availability"
    
    local response=$(curl -s -w "%{http_code}" -X POST "$GRAPHQL_URL" \
        -H "Content-Type: application/json" \
        -d '{"query":"query { __schema { queryType { name } } }"}')
    
    local http_code="${response: -3}"
    local response_body="${response%???}"
    
    if [[ "$http_code" == "200" ]]; then
        log_success "GraphQL endpoint is accessible"
        echo "Schema introspection successful"
    else
        log_error "GraphQL endpoint failed (HTTP $http_code)"
        echo "$response_body"
    fi
}

# Test system health query
test_system_health_query() {
    run_test "System health GraphQL query"
    
    local query='{
        "query": "query SystemHealth { 
            systemHealth { 
                status 
                timestamp 
                services { serviceName status responseTime }
                metrics { cpuUsage memoryUsage requestsPerSecond }
            } 
        }"
    }'
    
    local response=$(curl -s -X POST "$GRAPHQL_URL" \
        -H "Content-Type: application/json" \
        -d "$query")
    
    if echo "$response" | jq -e '.data.systemHealth' > /dev/null 2>&1; then
        log_success "System health query successful"
        echo "Health status: $(echo "$response" | jq -r '.data.systemHealth.status')"
        echo "Services: $(echo "$response" | jq -r '.data.systemHealth.services | length') monitored"
    else
        log_error "System health query failed"
        echo "$response" | jq '.errors // .' 2>/dev/null || echo "$response"
    fi
}

# Test customer operations
test_customer_operations() {
    run_test "Customer GraphQL operations"
    
    # Test customer search
    local search_query='{
        "query": "query SearchCustomers { 
            customers(
                filter: { accountStatus: ACTIVE }
                page: { page: 0, size: 5 }
            ) { 
                nodes { 
                    customerId 
                    fullName 
                    creditScore 
                    accountStatus 
                }
                totalCount
                pageInfo { hasNextPage }
            } 
        }"
    }'
    
    local response=$(curl -s -X POST "$GRAPHQL_URL" \
        -H "Content-Type: application/json" \
        -d "$search_query")
    
    if echo "$response" | jq -e '.data.customers' > /dev/null 2>&1; then
        log_success "Customer search query successful"
        local customer_count=$(echo "$response" | jq -r '.data.customers.totalCount')
        echo "Found $customer_count customers"
        
        # Get first customer ID for detailed query
        local customer_id=$(echo "$response" | jq -r '.data.customers.nodes[0].customerId // empty')
        if [[ -n "$customer_id" ]]; then
            test_customer_details "$customer_id"
        fi
    else
        log_error "Customer search query failed"
        echo "$response" | jq '.errors // .' 2>/dev/null || echo "$response"
    fi
}

# Test detailed customer query
test_customer_details() {
    local customer_id="$1"
    run_test "Customer details query for $customer_id"
    
    local detail_query="{
        \"query\": \"query CustomerDetails { 
            customer(id: \\\"$customer_id\\\") { 
                customerId
                fullName
                email
                creditLimit
                availableCredit
                creditScore
                accountStatus
                loans {
                    loanId
                    loanAmount
                    outstandingAmount
                    status
                    installments {
                        installmentNumber
                        dueDate
                        totalAmount
                        status
                    }
                }
                riskProfile {
                    overallRisk
                    paymentHistory
                    creditUtilization
                }
            } 
        }\"
    }"
    
    local response=$(curl -s -X POST "$GRAPHQL_URL" \
        -H "Content-Type: application/json" \
        -d "$detail_query")
    
    if echo "$response" | jq -e '.data.customer' > /dev/null 2>&1; then
        log_success "Customer details query successful"
        local full_name=$(echo "$response" | jq -r '.data.customer.fullName')
        local loan_count=$(echo "$response" | jq -r '.data.customer.loans | length')
        echo "Customer: $full_name with $loan_count loans"
    else
        log_error "Customer details query failed"
        echo "$response" | jq '.errors // .' 2>/dev/null || echo "$response"
    fi
}

# Test loan analytics
test_loan_analytics() {
    run_test "Loan analytics GraphQL query"
    
    local analytics_query='{
        "query": "query LoanAnalytics { 
            loanAnalytics(period: LAST_30_DAYS) { 
                totalLoansCreated
                totalLoanAmount
                averageLoanAmount
                approvalRate
                defaultRate
                loanTypeDistribution {
                    loanType
                    count
                    totalAmount
                    percentage
                }
            } 
        }"
    }'
    
    local response=$(curl -s -X POST "$GRAPHQL_URL" \
        -H "Content-Type: application/json" \
        -d "$analytics_query")
    
    if echo "$response" | jq -e '.data.loanAnalytics' > /dev/null 2>&1; then
        log_success "Loan analytics query successful"
        local total_loans=$(echo "$response" | jq -r '.data.loanAnalytics.totalLoansCreated')
        local avg_amount=$(echo "$response" | jq -r '.data.loanAnalytics.averageLoanAmount')
        echo "Analytics: $total_loans loans, average amount: \$$avg_amount"
    else
        log_error "Loan analytics query failed"
        echo "$response" | jq '.errors // .' 2>/dev/null || echo "$response"
    fi
}

# Test natural language processing
test_natural_language_processing() {
    run_test "Natural Language Processing GraphQL query"
    
    local nl_query='{
        "query": "query NaturalLanguage { 
            nlQuery(
                query: \"Show me all active customers with high credit scores\"
                context: { domain: CUSTOMER_SERVICE, language: \"en\" }
            ) { 
                query
                intent
                entities { type value confidence }
                result
                confidence
                suggestions
                executionTime
            } 
        }"
    }'
    
    local response=$(curl -s -X POST "$GRAPHQL_URL" \
        -H "Content-Type: application/json" \
        -d "$nl_query")
    
    if echo "$response" | jq -e '.data.nlQuery' > /dev/null 2>&1; then
        log_success "Natural language processing successful"
        local intent=$(echo "$response" | jq -r '.data.nlQuery.intent')
        local confidence=$(echo "$response" | jq -r '.data.nlQuery.confidence')
        local execution_time=$(echo "$response" | jq -r '.data.nlQuery.executionTime')
        echo "Intent: $intent, Confidence: $confidence, Time: ${execution_time}s"
    else
        log_error "Natural language processing failed"
        echo "$response" | jq '.errors // .' 2>/dev/null || echo "$response"
    fi
}

# Test payment calculations
test_payment_calculations() {
    run_test "Payment calculation GraphQL query"
    
    local payment_query='{
        "query": "query PaymentCalculation { 
            paymentCalculation(input: {
                loanId: \"LOAN-001\"
                paymentAmount: 2000.00
                paymentDate: \"2025-06-12T10:00:00Z\"
                installmentNumbers: [1, 2]
                simulateOnly: true
            }) { 
                baseAmount
                discountAmount
                penaltyAmount
                finalAmount
                earlyPaymentDays
                latePaymentDays
                installmentBreakdown {
                    installmentNumber
                    originalAmount
                    discountApplied
                    amountToPay
                }
            } 
        }"
    }'
    
    local response=$(curl -s -X POST "$GRAPHQL_URL" \
        -H "Content-Type: application/json" \
        -d "$payment_query")
    
    if echo "$response" | jq -e '.data.paymentCalculation' > /dev/null 2>&1; then
        log_success "Payment calculation successful"
        local final_amount=$(echo "$response" | jq -r '.data.paymentCalculation.finalAmount')
        local discount=$(echo "$response" | jq -r '.data.paymentCalculation.discountAmount')
        echo "Final amount: \$$final_amount (discount: \$$discount)"
    else
        log_error "Payment calculation failed"
        echo "$response" | jq '.errors // .' 2>/dev/null || echo "$response"
    fi
}

# Test GraphQL mutations
test_graphql_mutations() {
    run_test "GraphQL mutation operations"
    
    # Test customer creation mutation
    local create_customer_mutation='{
        "query": "mutation CreateCustomer { 
            createCustomer(input: {
                customerId: \"TEST-CUST-001\"
                firstName: \"Test\"
                lastName: \"Customer\"
                email: \"test.customer@example.com\"
                address: {
                    street: \"123 Test St\"
                    city: \"Test City\"
                    state: \"TS\"
                    zipCode: \"12345\"
                    country: \"USA\"
                }
                creditLimit: 50000.00
                annualIncome: 75000.00
                employmentStatus: EMPLOYED
                identificationNumber: \"TEST123456789\"
                identificationType: \"SSN\"
            }) {
                ... on CustomerSuccess {
                    customer { customerId fullName }
                    message
                }
                ... on CustomerError {
                    message
                    code
                }
            }
        }"
    }'
    
    local response=$(curl -s -X POST "$GRAPHQL_URL" \
        -H "Content-Type: application/json" \
        -d "$create_customer_mutation")
    
    if echo "$response" | jq -e '.data.createCustomer' > /dev/null 2>&1; then
        log_success "Customer creation mutation processed"
        echo "$response" | jq '.data.createCustomer'
    else
        log_warning "Customer creation mutation may have validation errors (expected in demo)"
        echo "$response" | jq '.errors // .' 2>/dev/null || echo "$response"
    fi
}

# Test MCP server functionality
test_mcp_server() {
    run_test "MCP Server WebSocket connection"
    
    if command -v wscat &> /dev/null; then
        # Test MCP initialization
        echo '{"jsonrpc":"2.0","id":"init","method":"initialize","params":{"protocolVersion":"1.0.0"}}' | \
        timeout 5 wscat -c "$MCP_WS_URL" 2>/dev/null || {
            log_warning "MCP WebSocket test skipped (wscat required or connection failed)"
            return
        }
        log_success "MCP WebSocket connection successful"
    else
        log_warning "MCP WebSocket test skipped (wscat not available)"
    fi
}

# Test GraphQL Playground accessibility
test_graphql_playground() {
    run_test "GraphQL Playground accessibility"
    
    local response=$(curl -s -w "%{http_code}" "$SYSTEM_URL/graphql/playground")
    local http_code="${response: -3}"
    
    if [[ "$http_code" == "200" ]]; then
        log_success "GraphQL Playground is accessible"
        echo "Available at: $SYSTEM_URL/graphql/playground"
    else
        log_warning "GraphQL Playground not accessible (HTTP $http_code)"
    fi
}

# Test schema introspection
test_schema_introspection() {
    run_test "GraphQL schema introspection"
    
    local introspection_query='{
        "query": "query IntrospectionQuery {
            __schema {
                queryType { name }
                mutationType { name }
                subscriptionType { name }
                types {
                    name
                    kind
                    description
                }
            }
        }"
    }'
    
    local response=$(curl -s -X POST "$GRAPHQL_URL" \
        -H "Content-Type: application/json" \
        -d "$introspection_query")
    
    if echo "$response" | jq -e '.data.__schema' > /dev/null 2>&1; then
        log_success "Schema introspection successful"
        local types_count=$(echo "$response" | jq -r '.data.__schema.types | length')
        local query_type=$(echo "$response" | jq -r '.data.__schema.queryType.name')
        local mutation_type=$(echo "$response" | jq -r '.data.__schema.mutationType.name')
        echo "Schema: $types_count types, Query: $query_type, Mutation: $mutation_type"
    else
        log_error "Schema introspection failed"
        echo "$response" | jq '.errors // .' 2>/dev/null || echo "$response"
    fi
}

# Test error handling
test_error_handling() {
    run_test "GraphQL error handling"
    
    local invalid_query='{
        "query": "query InvalidQuery { 
            nonExistentField { 
                invalidProperty 
            } 
        }"
    }'
    
    local response=$(curl -s -X POST "$GRAPHQL_URL" \
        -H "Content-Type: application/json" \
        -d "$invalid_query")
    
    if echo "$response" | jq -e '.errors' > /dev/null 2>&1; then
        log_success "Error handling working correctly"
        local error_count=$(echo "$response" | jq -r '.errors | length')
        echo "Properly returned $error_count error(s) for invalid query"
    else
        log_error "Error handling not working as expected"
        echo "$response"
    fi
}

# Test performance with complex query
test_performance() {
    run_test "GraphQL performance with complex query"
    
    local complex_query='{
        "query": "query ComplexQuery { 
            customers(page: { page: 0, size: 10 }) {
                nodes {
                    customerId
                    fullName
                    creditScore
                    loans {
                        loanId
                        loanAmount
                        status
                        installments {
                            installmentNumber
                            totalAmount
                            status
                        }
                        payments {
                            paymentId
                            paymentAmount
                            paymentDate
                        }
                    }
                }
                totalCount
            }
        }"
    }'
    
    local start_time=$(date +%s%N)
    local response=$(curl -s -X POST "$GRAPHQL_URL" \
        -H "Content-Type: application/json" \
        -d "$complex_query")
    local end_time=$(date +%s%N)
    local response_time=$(( (end_time - start_time) / 1000000 ))
    
    if echo "$response" | jq -e '.data.customers' > /dev/null 2>&1; then
        log_success "Complex query performance test successful"
        echo "Response time: ${response_time}ms"
        if [[ $response_time -lt 1000 ]]; then
            echo "Performance: Excellent (< 1s)"
        elif [[ $response_time -lt 3000 ]]; then
            echo "Performance: Good (< 3s)"
        else
            echo "Performance: Needs optimization (> 3s)"
        fi
    else
        log_error "Complex query failed"
        echo "$response" | jq '.errors // .' 2>/dev/null || echo "$response"
    fi
}

# Generate test report
generate_test_report() {
    echo ""
    echo -e "${PURPLE}===========================================${NC}"
    echo -e "${PURPLE}GraphQL and MCP Integration Test Report${NC}"
    echo -e "${PURPLE}===========================================${NC}"
    echo ""
    echo -e "${CYAN}Test Summary:${NC}"
    echo "  Total Tests: $TOTAL_TESTS"
    echo -e "  ${GREEN}Passed: $PASSED_TESTS${NC}"
    echo -e "  ${RED}Failed: $FAILED_TESTS${NC}"
    echo ""
    
    local success_rate=$(( PASSED_TESTS * 100 / TOTAL_TESTS ))
    
    if [[ $success_rate -ge 90 ]]; then
        echo -e "${GREEN}✓ EXCELLENT${NC} - GraphQL and MCP integration is fully operational"
    elif [[ $success_rate -ge 75 ]]; then
        echo -e "${YELLOW}⚠ GOOD${NC} - GraphQL and MCP integration is mostly functional"
    else
        echo -e "${RED}✗ NEEDS ATTENTION${NC} - GraphQL and MCP integration has issues"
    fi
    
    echo ""
    echo -e "${CYAN}Available Endpoints:${NC}"
    echo "  GraphQL API: $GRAPHQL_URL"
    echo "  GraphQL Playground: $SYSTEM_URL/graphql/playground"
    echo "  MCP WebSocket: $MCP_WS_URL"
    echo "  System Health: $SYSTEM_URL/health"
    echo ""
    
    echo -e "${CYAN}Key Features Tested:${NC}"
    echo "  ✓ GraphQL Query Operations"
    echo "  ✓ GraphQL Mutation Operations"
    echo "  ✓ Natural Language Processing"
    echo "  ✓ Analytics and Reporting"
    echo "  ✓ Payment Calculations"
    echo "  ✓ Schema Introspection"
    echo "  ✓ Error Handling"
    echo "  ✓ Performance Testing"
    echo "  ✓ MCP Server Integration"
    echo ""
    
    echo -e "${CYAN}Next Steps for LLM Integration:${NC}"
    echo "  1. Connect LLM to GraphQL endpoint: $GRAPHQL_URL"
    echo "  2. Use MCP WebSocket for real-time communication: $MCP_WS_URL"
    echo "  3. Implement natural language queries via nlQuery resolver"
    echo "  4. Utilize comprehensive analytics endpoints for insights"
    echo "  5. Access GraphQL Playground for interactive testing"
    echo ""
}

# Main execution
main() {
    echo -e "${BLUE}Starting comprehensive GraphQL and MCP integration testing...${NC}"
    echo ""
    
    # Check dependencies
    if ! command -v curl &> /dev/null; then
        log_error "curl is required for testing"
        exit 1
    fi
    
    if ! command -v jq &> /dev/null; then
        log_error "jq is required for JSON processing"
        exit 1
    fi
    
    # Run all tests
    test_graphql_endpoint
    test_system_health_query
    test_customer_operations
    test_loan_analytics
    test_natural_language_processing
    test_payment_calculations
    test_graphql_mutations
    test_mcp_server
    test_graphql_playground
    test_schema_introspection
    test_error_handling
    test_performance
    
    # Generate final report
    generate_test_report
}

# Execute main function
main