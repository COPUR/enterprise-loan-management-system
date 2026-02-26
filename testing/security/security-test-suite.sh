#!/bin/bash

# Enterprise Banking Security Test Suite
# Comprehensive security validation for production banking systems

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
NC='\033[0m'

# Test configuration
BASE_URL="https://localhost"
AUTH_URL="http://localhost:8080"
VAULT_URL="https://vault:8200"
POSTGRES_URL="postgres://localhost:5432/banking_db"

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
CRITICAL_FAILURES=0

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

log_critical() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] 🚨 CRITICAL: $1${NC}"
    ((FAILED_TESTS++))
    ((CRITICAL_FAILURES++))
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
    local is_critical="${3:-false}"
    
    log "🧪 Running test: $test_name"
    ((TOTAL_TESTS++))
    
    if $test_function; then
        log_success "Test passed: $test_name"
        return 0
    else
        if [ "$is_critical" = "true" ]; then
            log_critical "Test failed: $test_name"
        else
            log_error "Test failed: $test_name"
        fi
        return 1
    fi
}

# =============================================
# AUTHENTICATION & AUTHORIZATION TESTS
# =============================================

test_oauth_token_validation() {
    log "Testing OAuth 2.1 token validation..."
    
    # Test valid token
    local token_response=$(curl -s -X POST \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=client_credentials&client_id=banking-app&client_secret=banking-secret&scope=banking" \
        "$AUTH_URL/realms/banking/protocol/openid-connect/token" 2>/dev/null)
    
    if echo "$token_response" | jq -e '.access_token' >/dev/null 2>&1; then
        local token=$(echo "$token_response" | jq -r '.access_token')
        
        # Test API access with valid token
        if curl -s -k -H "Authorization: Bearer $token" "$BASE_URL/api/actuator/health" >/dev/null 2>&1; then
            return 0
        fi
    fi
    
    return 1
}

test_invalid_token_rejection() {
    log "Testing invalid token rejection..."
    
    local invalid_token="invalid.jwt.token"
    local response_code=$(curl -s -k -o /dev/null -w "%{http_code}" \
        -H "Authorization: Bearer $invalid_token" \
        "$BASE_URL/customers")
    
    # Should return 401 or 403
    if [ "$response_code" -eq 401 ] || [ "$response_code" -eq 403 ]; then
        return 0
    fi
    
    return 1
}

test_dpop_token_binding() {
    log "Testing DPoP token binding..."
    
    # This would require a proper DPoP implementation
    # For now, test that DPoP header is processed
    local response_code=$(curl -s -k -o /dev/null -w "%{http_code}" \
        -H "Authorization: Bearer test-token" \
        -H "DPoP: eyJ0eXAiOiJkcG9wK2p3dCIsImFsZyI6IkVTMjU2In0.eyJqdGkiOiJ0ZXN0LWp0aSIsImh0bSI6IkdFVCIsImh0dSI6Imh0dHBzOi8vbG9jYWxob3N0L2N1c3RvbWVycyIsImlhdCI6MTY4OTc0NTYwMH0.signature" \
        "$BASE_URL/customers")
    
    # Should process DPoP header (even if it fails authentication)
    if [ "$response_code" -ne 400 ]; then
        return 0
    fi
    
    return 1
}

test_fapi_compliance_headers() {
    log "Testing FAPI compliance headers..."
    
    local token=$(curl -s -X POST \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=client_credentials&client_id=banking-app&client_secret=banking-secret&scope=banking" \
        "$AUTH_URL/realms/banking/protocol/openid-connect/token" 2>/dev/null | jq -r '.access_token')
    
    if [ "$token" != "null" ] && [ -n "$token" ]; then
        local response_code=$(curl -s -k -o /dev/null -w "%{http_code}" \
            -H "Authorization: Bearer $token" \
            -H "x-fapi-auth-date: $(date +%s)" \
            -H "x-fapi-customer-ip-address: 192.168.1.100" \
            -H "x-fapi-interaction-id: $(uuidgen)" \
            "$BASE_URL/open-banking/accounts/test-account")
        
        # Should accept FAPI headers
        if [ "$response_code" -ne 400 ]; then
            return 0
        fi
    fi
    
    return 1
}

# =============================================
# DATA PROTECTION TESTS
# =============================================

test_database_encryption() {
    log "Testing database encryption..."
    
    # Test that sensitive data is encrypted at rest
    if command -v psql &> /dev/null; then
        local encrypted_data=$(psql -h localhost -U banking_user -d banking_db -t -c \
            "SELECT first_name_encrypted FROM banking_customer.customers LIMIT 1;" 2>/dev/null)
        
        if [ -n "$encrypted_data" ] && [[ "$encrypted_data" =~ ^[A-Za-z0-9+/=]+$ ]]; then
            return 0
        fi
    fi
    
    return 1
}

test_pii_masking() {
    log "Testing PII data masking..."
    
    local token=$(curl -s -X POST \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=client_credentials&client_id=banking-app&client_secret=banking-secret&scope=banking" \
        "$AUTH_URL/realms/banking/protocol/openid-connect/token" 2>/dev/null | jq -r '.access_token')
    
    if [ "$token" != "null" ] && [ -n "$token" ]; then
        local customer_data=$(curl -s -k -H "Authorization: Bearer $token" \
            "$BASE_URL/customers/110e8400-e29b-41d4-a716-446655440001" 2>/dev/null)
        
        # Check if sensitive data is masked
        if echo "$customer_data" | jq -e '.email' >/dev/null 2>&1; then
            local email=$(echo "$customer_data" | jq -r '.email')
            if [[ "$email" =~ \*\*\* ]]; then
                return 0
            fi
        fi
    fi
    
    return 1
}

test_sql_injection_protection() {
    log "Testing SQL injection protection..."
    
    local token=$(curl -s -X POST \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=client_credentials&client_id=banking-app&client_secret=banking-secret&scope=banking" \
        "$AUTH_URL/realms/banking/protocol/openid-connect/token" 2>/dev/null | jq -r '.access_token')
    
    if [ "$token" != "null" ] && [ -n "$token" ]; then
        # Test SQL injection attempt
        local response_code=$(curl -s -k -o /dev/null -w "%{http_code}" \
            -H "Authorization: Bearer $token" \
            "$BASE_URL/customers?id=1' OR '1'='1")
        
        # Should not return 500 (internal error) or 200 (success)
        if [ "$response_code" -ne 500 ] && [ "$response_code" -ne 200 ]; then
            return 0
        fi
    fi
    
    return 1
}

# =============================================
# NETWORK SECURITY TESTS
# =============================================

test_ssl_tls_configuration() {
    log "Testing SSL/TLS configuration..."
    
    # Test SSL certificate validity
    if openssl s_client -connect localhost:443 -servername localhost </dev/null 2>/dev/null | \
       openssl x509 -noout -dates >/dev/null 2>&1; then
        
        # Test TLS version
        local tls_version=$(openssl s_client -connect localhost:443 -servername localhost </dev/null 2>/dev/null | \
                          grep "Protocol" | awk '{print $3}')
        
        if [[ "$tls_version" =~ TLSv1\.[23] ]]; then
            return 0
        fi
    fi
    
    return 1
}

test_security_headers() {
    log "Testing security headers..."
    
    local headers=$(curl -s -I -k "$BASE_URL/api/actuator/health" 2>/dev/null)
    
    local required_headers=(
        "X-Frame-Options"
        "X-Content-Type-Options"
        "X-XSS-Protection"
        "Strict-Transport-Security"
        "Content-Security-Policy"
    )
    
    for header in "${required_headers[@]}"; do
        if ! echo "$headers" | grep -i "$header" >/dev/null 2>&1; then
            return 1
        fi
    done
    
    return 0
}

test_rate_limiting() {
    log "Testing rate limiting..."
    
    local failed_requests=0
    local total_requests=15
    
    # Make rapid requests to trigger rate limiting
    for i in $(seq 1 $total_requests); do
        local response_code=$(curl -s -k -o /dev/null -w "%{http_code}" \
            --max-time 5 "$BASE_URL/api/actuator/health")
        
        if [ "$response_code" -eq 429 ]; then
            ((failed_requests++))
        fi
        
        sleep 0.1
    done
    
    # Should have some rate limited responses
    if [ "$failed_requests" -gt 0 ]; then
        return 0
    fi
    
    return 1
}

# =============================================
# VULNERABILITY TESTS
# =============================================

test_exposed_admin_endpoints() {
    log "Testing for exposed admin endpoints..."
    
    local admin_endpoints=(
        "/actuator/env"
        "/actuator/configprops"
        "/actuator/mappings"
        "/actuator/beans"
        "/admin"
        "/management"
        "/debug"
    )
    
    for endpoint in "${admin_endpoints[@]}"; do
        local response_code=$(curl -s -k -o /dev/null -w "%{http_code}" \
            "$BASE_URL$endpoint")
        
        # Should return 401, 403, or 404 (not 200)
        if [ "$response_code" -eq 200 ]; then
            return 1
        fi
    done
    
    return 0
}

test_directory_traversal() {
    log "Testing directory traversal protection..."
    
    local traversal_attempts=(
        "/../../../etc/passwd"
        "/..\\..\\..\\windows\\system32\\config\\sam"
        "/%2e%2e%2f%2e%2e%2f%2e%2e%2fetc%2fpasswd"
    )
    
    for attempt in "${traversal_attempts[@]}"; do
        local response_code=$(curl -s -k -o /dev/null -w "%{http_code}" \
            "$BASE_URL$attempt")
        
        # Should not return 200 (success)
        if [ "$response_code" -eq 200 ]; then
            return 1
        fi
    done
    
    return 0
}

test_cors_policy() {
    log "Testing CORS policy..."
    
    # Test CORS with malicious origin
    local cors_headers=$(curl -s -k -I \
        -H "Origin: https://malicious-site.com" \
        -H "Access-Control-Request-Method: POST" \
        -H "Access-Control-Request-Headers: Content-Type" \
        -X OPTIONS \
        "$BASE_URL/api/actuator/health" 2>/dev/null)
    
    # Should not allow arbitrary origins
    if echo "$cors_headers" | grep -i "access-control-allow-origin: https://malicious-site.com" >/dev/null 2>&1; then
        return 1
    fi
    
    return 0
}

# =============================================
# COMPLIANCE TESTS
# =============================================

test_audit_logging() {
    log "Testing audit logging..."
    
    # Check if audit logs are being generated
    if command -v psql &> /dev/null; then
        local audit_count=$(psql -h localhost -U banking_user -d banking_db -t -c \
            "SELECT COUNT(*) FROM banking_audit.audit_log WHERE changed_at > NOW() - INTERVAL '1 hour';" 2>/dev/null | tr -d ' ')
        
        if [ "$audit_count" -gt 0 ]; then
            return 0
        fi
    fi
    
    return 1
}

test_pci_dss_compliance() {
    log "Testing PCI DSS compliance requirements..."
    
    local compliance_checks=0
    local total_checks=5
    
    # Check 1: Encrypted data transmission
    if curl -s -k -I "$BASE_URL/payments" 2>/dev/null | grep -i "strict-transport-security" >/dev/null 2>&1; then
        ((compliance_checks++))
    fi
    
    # Check 2: Access control
    if curl -s -k -o /dev/null -w "%{http_code}" "$BASE_URL/payments" 2>/dev/null | grep -E "^(401|403)$" >/dev/null 2>&1; then
        ((compliance_checks++))
    fi
    
    # Check 3: Audit logging
    if command -v psql &> /dev/null; then
        local payment_audits=$(psql -h localhost -U banking_user -d banking_db -t -c \
            "SELECT COUNT(*) FROM banking_audit.audit_log WHERE table_name = 'payments' AND changed_at > NOW() - INTERVAL '1 day';" 2>/dev/null | tr -d ' ')
        
        if [ "$payment_audits" -gt 0 ]; then
            ((compliance_checks++))
        fi
    fi
    
    # Check 4: Data encryption
    if command -v psql &> /dev/null; then
        local encrypted_payments=$(psql -h localhost -U banking_user -d banking_db -t -c \
            "SELECT COUNT(*) FROM banking_payment.payments WHERE payment_amount_encrypted IS NOT NULL;" 2>/dev/null | tr -d ' ')
        
        if [ "$encrypted_payments" -gt 0 ]; then
            ((compliance_checks++))
        fi
    fi
    
    # Check 5: Network security
    if netstat -tlnp 2>/dev/null | grep ":5432" | grep -v "127.0.0.1" >/dev/null 2>&1; then
        # Database should not be directly accessible
        return 1
    else
        ((compliance_checks++))
    fi
    
    # Need at least 80% compliance
    if [ "$compliance_checks" -ge 4 ]; then
        return 0
    fi
    
    return 1
}

test_gdpr_compliance() {
    log "Testing GDPR compliance..."
    
    local token=$(curl -s -X POST \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=client_credentials&client_id=banking-app&client_secret=banking-secret&scope=banking" \
        "$AUTH_URL/realms/banking/protocol/openid-connect/token" 2>/dev/null | jq -r '.access_token')
    
    if [ "$token" != "null" ] && [ -n "$token" ]; then
        # Test data minimization - should not expose all customer data
        local customer_data=$(curl -s -k -H "Authorization: Bearer $token" \
            "$BASE_URL/customers/110e8400-e29b-41d4-a716-446655440001" 2>/dev/null)
        
        # Should not contain raw SSN or other sensitive data
        if echo "$customer_data" | jq -e '.ssn' >/dev/null 2>&1; then
            local ssn=$(echo "$customer_data" | jq -r '.ssn')
            if [[ "$ssn" =~ ^[0-9]{3}-[0-9]{2}-[0-9]{4}$ ]]; then
                return 1  # Raw SSN exposed
            fi
        fi
        
        return 0
    fi
    
    return 1
}

# =============================================
# FRAUD DETECTION TESTS
# =============================================

test_fraud_detection_response() {
    log "Testing fraud detection response time..."
    
    local token=$(curl -s -X POST \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=client_credentials&client_id=banking-app&client_secret=banking-secret&scope=banking" \
        "$AUTH_URL/realms/banking/protocol/openid-connect/token" 2>/dev/null | jq -r '.access_token')
    
    if [ "$token" != "null" ] && [ -n "$token" ]; then
        local start_time=$(date +%s%3N)
        
        local response=$(curl -s -k -X POST \
            -H "Authorization: Bearer $token" \
            -H "Content-Type: application/json" \
            -d '{"transactionId":"TEST123","amount":1000,"customerId":"110e8400-e29b-41d4-a716-446655440001"}' \
            "$BASE_URL/ml/fraud-detection" 2>/dev/null)
        
        local end_time=$(date +%s%3N)
        local response_time=$((end_time - start_time))
        
        # Should respond within 200ms
        if [ "$response_time" -lt 200 ]; then
            return 0
        fi
    fi
    
    return 1
}

test_anomaly_detection() {
    log "Testing anomaly detection..."
    
    local token=$(curl -s -X POST \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=client_credentials&client_id=banking-app&client_secret=banking-secret&scope=banking" \
        "$AUTH_URL/realms/banking/protocol/openid-connect/token" 2>/dev/null | jq -r '.access_token')
    
    if [ "$token" != "null" ] && [ -n "$token" ]; then
        # Test with suspicious high-amount transaction
        local response=$(curl -s -k -X POST \
            -H "Authorization: Bearer $token" \
            -H "Content-Type: application/json" \
            -d '{"transactionId":"SUSPICIOUS123","amount":100000,"customerId":"110e8400-e29b-41d4-a716-446655440001"}' \
            "$BASE_URL/ml/fraud-detection" 2>/dev/null)
        
        if echo "$response" | jq -e '.riskScore' >/dev/null 2>&1; then
            local risk_score=$(echo "$response" | jq -r '.riskScore')
            
            # High amount should trigger higher risk score
            if (( $(echo "$risk_score > 0.7" | bc -l) )); then
                return 0
            fi
        fi
    fi
    
    return 1
}

# =============================================
# PERFORMANCE SECURITY TESTS
# =============================================

test_dos_protection() {
    log "Testing DoS protection..."
    
    local concurrent_requests=50
    local pids=()
    
    # Launch concurrent requests
    for i in $(seq 1 $concurrent_requests); do
        {
            curl -s -k --max-time 10 "$BASE_URL/api/actuator/health" >/dev/null 2>&1
        } &
        pids+=($!)
    done
    
    # Wait for all requests
    local failed_count=0
    for pid in "${pids[@]}"; do
        if ! wait $pid; then
            ((failed_count++))
        fi
    done
    
    # Should handle concurrent requests without complete failure
    if [ "$failed_count" -lt $((concurrent_requests / 2)) ]; then
        return 0
    fi
    
    return 1
}

test_memory_leak_security() {
    log "Testing memory leak security..."
    
    local token=$(curl -s -X POST \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=client_credentials&client_id=banking-app&client_secret=banking-secret&scope=banking" \
        "$AUTH_URL/realms/banking/protocol/openid-connect/token" 2>/dev/null | jq -r '.access_token')
    
    if [ "$token" != "null" ] && [ -n "$token" ]; then
        # Get initial memory usage
        local initial_memory=$(curl -s -k -H "Authorization: Bearer $token" \
            "$BASE_URL/api/actuator/metrics/jvm.memory.used" 2>/dev/null | jq -r '.measurements[0].value' 2>/dev/null || echo 0)
        
        # Perform memory-intensive operations
        for i in {1..100}; do
            curl -s -k -H "Authorization: Bearer $token" \
                "$BASE_URL/customers" >/dev/null 2>&1
        done
        
        # Check memory after operations
        local final_memory=$(curl -s -k -H "Authorization: Bearer $token" \
            "$BASE_URL/api/actuator/metrics/jvm.memory.used" 2>/dev/null | jq -r '.measurements[0].value' 2>/dev/null || echo 0)
        
        # Memory growth should be reasonable
        local memory_growth=$(echo "scale=2; ($final_memory - $initial_memory) / 1024 / 1024" | bc -l 2>/dev/null || echo 0)
        
        if (( $(echo "$memory_growth < 50" | bc -l) )); then
            return 0
        fi
    fi
    
    return 1
}

# =============================================
# MAIN TEST EXECUTION
# =============================================

generate_security_report() {
    log "📊 Generating security test report..."
    
    local success_rate=$(( PASSED_TESTS * 100 / TOTAL_TESTS ))
    local timestamp=$(date +"%Y%m%d_%H%M%S")
    local report_file="$PROJECT_ROOT/security-test-report-${timestamp}.md"
    
    cat > "$report_file" << EOF
# Enterprise Banking Security Test Report

**Report Generated:** $(date)
**Total Tests:** $TOTAL_TESTS
**Passed Tests:** $PASSED_TESTS
**Failed Tests:** $FAILED_TESTS
**Critical Failures:** $CRITICAL_FAILURES
**Success Rate:** ${success_rate}%

## Security Test Results

### Authentication & Authorization
- OAuth 2.1 token validation
- Invalid token rejection
- DPoP token binding
- FAPI compliance headers

### Data Protection
- Database encryption at rest
- PII data masking
- SQL injection protection

### Network Security
- SSL/TLS configuration
- Security headers validation
- Rate limiting protection

### Vulnerability Assessment
- Admin endpoint exposure
- Directory traversal protection
- CORS policy validation

### Compliance Testing
- Audit logging functionality
- PCI DSS compliance requirements
- GDPR compliance validation

### Fraud Detection
- Fraud detection response time
- Anomaly detection accuracy

### Performance Security
- DoS protection mechanisms
- Memory leak security validation

## Recommendations

### Critical Issues (Must Fix)
$(if [ $CRITICAL_FAILURES -gt 0 ]; then
    echo "- $CRITICAL_FAILURES critical security failures detected"
    echo "- Immediate remediation required before production"
else
    echo "- No critical security failures detected"
fi)

### Security Improvements
- Implement Web Application Firewall (WAF)
- Add API rate limiting per user
- Enhance monitoring and alerting
- Regular security scanning automation

### Compliance Enhancements
- Implement data loss prevention (DLP)
- Add encryption key rotation
- Enhance audit trail immutability
- Regular compliance assessments

## Next Steps

1. **Address Critical Failures:** Fix all critical security issues
2. **Security Hardening:** Implement additional security measures
3. **Penetration Testing:** Conduct professional security assessment
4. **Compliance Audit:** Perform formal compliance validation
5. **Security Training:** Train development team on secure coding

---
*Generated by Enterprise Banking Security Test Suite*
EOF
    
    log_success "Security report generated: $report_file"
}

show_security_summary() {
    echo -e "${CYAN}"
    cat << 'EOF'
╔══════════════════════════════════════════════════════════════════════════════════════════════════════════╗
║                                    🔒 Security Test Summary 🔒                                           ║
╚══════════════════════════════════════════════════════════════════════════════════════════════════════════╝
EOF
    echo -e "${NC}"
    
    echo -e "${BLUE}Total Tests: $TOTAL_TESTS${NC}"
    echo -e "${GREEN}Passed: $PASSED_TESTS${NC}"
    echo -e "${RED}Failed: $FAILED_TESTS${NC}"
    echo -e "${RED}Critical Failures: $CRITICAL_FAILURES${NC}"
    
    local success_rate=$(( PASSED_TESTS * 100 / TOTAL_TESTS ))
    echo -e "${YELLOW}Success Rate: ${success_rate}%${NC}"
    
    if [ $CRITICAL_FAILURES -gt 0 ]; then
        echo -e "${RED}🚨 CRITICAL SECURITY ISSUES DETECTED${NC}"
        echo -e "${RED}   System NOT ready for production${NC}"
    elif [ $success_rate -ge 90 ]; then
        echo -e "${GREEN}✅ Excellent security posture${NC}"
    elif [ $success_rate -ge 80 ]; then
        echo -e "${YELLOW}⚠️  Good security, minor improvements needed${NC}"
    else
        echo -e "${RED}❌ Security improvements required${NC}"
    fi
}

main() {
    cd "$PROJECT_ROOT"
    
    echo -e "${PURPLE}"
    cat << 'EOF'
╔══════════════════════════════════════════════════════════════════════════════════════════════════════════╗
║                                                                                                          ║
║                            🔒 Enterprise Banking Security Test Suite 🔒                                ║
║                                                                                                          ║
║                                 Comprehensive Security Validation                                       ║
║                                                                                                          ║
╚══════════════════════════════════════════════════════════════════════════════════════════════════════════╝
EOF
    echo -e "${NC}"
    
    # Authentication & Authorization Tests
    log "🔐 Running Authentication & Authorization Tests..."
    run_test "OAuth 2.1 Token Validation" test_oauth_token_validation true
    run_test "Invalid Token Rejection" test_invalid_token_rejection true
    run_test "DPoP Token Binding" test_dpop_token_binding false
    run_test "FAPI Compliance Headers" test_fapi_compliance_headers false
    
    # Data Protection Tests
    log "🛡️  Running Data Protection Tests..."
    run_test "Database Encryption" test_database_encryption true
    run_test "PII Data Masking" test_pii_masking true
    run_test "SQL Injection Protection" test_sql_injection_protection true
    
    # Network Security Tests
    log "🌐 Running Network Security Tests..."
    run_test "SSL/TLS Configuration" test_ssl_tls_configuration true
    run_test "Security Headers" test_security_headers false
    run_test "Rate Limiting" test_rate_limiting false
    
    # Vulnerability Tests
    log "🔍 Running Vulnerability Tests..."
    run_test "Admin Endpoint Exposure" test_exposed_admin_endpoints true
    run_test "Directory Traversal Protection" test_directory_traversal true
    run_test "CORS Policy" test_cors_policy false
    
    # Compliance Tests
    log "📊 Running Compliance Tests..."
    run_test "Audit Logging" test_audit_logging true
    run_test "PCI DSS Compliance" test_pci_dss_compliance true
    run_test "GDPR Compliance" test_gdpr_compliance true
    
    # Fraud Detection Tests
    log "🤖 Running Fraud Detection Tests..."
    run_test "Fraud Detection Response" test_fraud_detection_response false
    run_test "Anomaly Detection" test_anomaly_detection false
    
    # Performance Security Tests
    log "⚡ Running Performance Security Tests..."
    run_test "DoS Protection" test_dos_protection false
    run_test "Memory Leak Security" test_memory_leak_security false
    
    # Generate reports
    generate_security_report
    show_security_summary
    
    # Exit with appropriate code
    if [ $CRITICAL_FAILURES -gt 0 ]; then
        exit 2  # Critical failures
    elif [ $FAILED_TESTS -gt 0 ]; then
        exit 1  # Non-critical failures
    else
        exit 0  # All tests passed
    fi
}

# Execute main function
main "$@"