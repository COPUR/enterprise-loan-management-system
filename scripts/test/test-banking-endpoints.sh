#!/bin/bash

# Banking API Functional Test Suite
# Tests all core banking endpoints without Newman pre-request script issues

echo "🏦 Enhanced Enterprise Banking System - Functional Test Report"
echo "=============================================================="
echo "Date: $(date)"
echo "Target: http://localhost:8080"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

passed=0
failed=0

test_endpoint() {
    local name="$1"
    local method="$2"
    local url="$3"
    local data="$4"
    local expected_status="$5"
    
    echo -n "Testing: $name... "
    
    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "%{http_code}" "$url")
        status_code="${response: -3}"
        body="${response%???}"
    else
        response=$(curl -s -w "%{http_code}" -X "$method" -H "Content-Type: application/json" -d "$data" "$url")
        status_code="${response: -3}"
        body="${response%???}"
    fi
    
    if [ "$status_code" = "$expected_status" ]; then
        echo -e "${GREEN}✅ PASS${NC} ($status_code)"
        ((passed++))
        if [ -n "$body" ] && [ "$body" != "null" ] && [ ${#body} -gt 10 ]; then
            echo "   Response: $(echo "$body" | jq -c . 2>/dev/null || echo "$body" | head -c 100)..."
        fi
    else
        echo -e "${RED}❌ FAIL${NC} (Expected: $expected_status, Got: $status_code)"
        ((failed++))
        if [ -n "$body" ]; then
            echo "   Response: $body"
        fi
    fi
    echo ""
}

echo "📊 HEALTH CHECKS"
echo "=================="
test_endpoint "Application Health" "GET" "http://localhost:8080/actuator/health" "" "200"
test_endpoint "AI Health Check" "GET" "http://localhost:8080/api/ai/health" "" "200"

echo "👤 CUSTOMER MANAGEMENT"
echo "======================"
customer_data='{"name":"Ahmed Al-Rashid","email":"ahmed@example.com","phone":"+971501234567","preferredLanguage":"ar","islamicBankingPreference":true}'
test_endpoint "Create Customer" "POST" "http://localhost:8080/api/v1/customers" "$customer_data" "201"

test_endpoint "Get Customer Details" "GET" "http://localhost:8080/api/v1/customers/CUST-1234567890" "" "404"

echo "💰 LOAN MANAGEMENT"
echo "==================="
murabaha_loan='{"customerId":"CUST-1234567890","amount":50000,"termInMonths":36,"loanType":"MURABAHA","purpose":"HOME_PURCHASE","islamicCompliant":true}'
test_endpoint "Submit Murabaha Loan" "POST" "http://localhost:8080/api/v1/loans" "$murabaha_loan" "201"

personal_loan='{"customerId":"CUST-TEST","amount":15000,"termInMonths":24,"loanType":"PERSONAL","purpose":"EDUCATION"}'
test_endpoint "Submit Personal Loan" "POST" "http://localhost:8080/api/v1/loans" "$personal_loan" "201"

test_endpoint "Get All Loans" "GET" "http://localhost:8080/api/v1/loans" "" "200"
test_endpoint "Get Customer Loans" "GET" "http://localhost:8080/api/v1/loans?customerId=CUST-TEST" "" "200"

# Get a real loan ID from the system first
real_loan_id=$(curl -s "http://localhost:8080/api/v1/loans" | jq -r '.[0].loanId' 2>/dev/null || echo "LOAN-123")
test_endpoint "Get Loan Installments" "GET" "http://localhost:8080/api/v1/loans/$real_loan_id/installments" "" "200"

echo "💳 PAYMENT PROCESSING"
echo "======================"
payment_data='{"amount":916.67,"paymentMethod":"BANK_TRANSFER","notes":"Monthly installment payment"}'
test_endpoint "Process Payment" "POST" "http://localhost:8080/api/v1/loans/$real_loan_id/pay" "$payment_data" "200"

echo "🤖 AI SERVICES"
echo "==============="
fraud_data='{"transactionAmount":5000,"customerId":"CUST-TEST","merchantCategory":"RETAIL","location":"Dubai","deviceFingerprint":"web-browser-chrome"}'
test_endpoint "AI Fraud Analysis" "POST" "http://localhost:8080/api/ai/fraud/analyze" "$fraud_data" "200"

test_endpoint "AI Loan Recommendations" "GET" "http://localhost:8080/api/ai/recommendations/loans?customerId=CUST-TEST" "" "200"

echo ""
echo "📈 TEST SUMMARY"
echo "==============="
total=$((passed + failed))
echo "Total Tests: $total"
echo -e "Passed: ${GREEN}$passed${NC}"
echo -e "Failed: ${RED}$failed${NC}"

if [ $failed -eq 0 ]; then
    echo -e "\n🎉 ${GREEN}ALL TESTS PASSED!${NC}"
    echo "✅ Banking API is fully functional"
else
    echo -e "\n⚠️  ${YELLOW}Some tests failed${NC}"
    echo "Check the endpoints that returned unexpected status codes"
fi

success_rate=$((passed * 100 / total))
echo "Success Rate: $success_rate%"

echo ""
echo "🔗 TESTED ENDPOINTS:"
echo "===================="
echo "✓ GET  /actuator/health"
echo "✓ GET  /ai/health"
echo "✓ POST /api/v1/customers"
echo "✓ GET  /api/v1/customers/{id}"
echo "✓ POST /api/v1/loans"
echo "✓ GET  /api/v1/loans"
echo "✓ GET  /api/v1/loans/{id}/installments"
echo "✓ POST /api/v1/loans/{id}/pay"
echo "✓ POST /api/ai/fraud/analyze"
echo "✓ GET  /api/ai/recommendations/loans"

echo ""
echo "🏁 Test completed at $(date)"