#!/bin/bash
# Enhanced Enterprise Banking System - Pre-start Validation Script
# Validates banking compliance, security, and architectural requirements

set -euo pipefail

echo "🔍 Enhanced Enterprise Banking System - Pre-start Validation"
echo "==========================================================="

# Validation functions
validate_java_version() {
    echo "☕ Validating Java version..."
    local java_version
    java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    
    if [[ "$java_version" =~ ^21\. ]]; then
        echo "✅ Java version: $java_version (Valid for Banking System)"
    else
        echo "❌ Invalid Java version: $java_version (Required: Java 21)"
        exit 1
    fi
}

validate_memory_configuration() {
    echo "💾 Validating memory configuration..."
    local max_memory
    max_memory=$(java -XX:+PrintFlagsFinal -version 2>&1 | grep MaxHeapSize | awk '{print $4}' | tr -d '[:space:]')
    
    if [[ -n "$max_memory" ]] && [[ ${max_memory:-0} -gt 536870912 ]]; then # > 512MB
        echo "✅ Memory configuration: ${max_memory} bytes - Sufficient for banking operations"
    else
        echo "⚠️  Memory configuration: ${max_memory:-unknown} bytes - Proceeding with default"
    fi
}

validate_security_configuration() {
    echo "🔐 Validating security configuration..."
    
    # Check JWT configuration
    if [[ -n "${BANKING_JWT_SECRET:-}" ]]; then
        echo "✅ Banking JWT secret: Configured"
    else
        echo "⚠️ Banking JWT secret: Using default (not recommended for production)"
    fi
    
    # Check FAPI configuration
    if [[ "${FAPI_ENABLED:-false}" == "true" ]]; then
        echo "✅ FAPI security: Enabled"
        
        if [[ -n "${BANKING_JWT_ALGORITHM:-}" ]]; then
            echo "✅ JWT algorithm: ${BANKING_JWT_ALGORITHM}"
        else
            echo "⚠️ JWT algorithm not specified"
        fi
    fi
    
    # Check audit configuration
    if [[ "${AUDIT_ENABLED:-false}" == "true" ]]; then
        echo "✅ Audit logging: Enabled"
    fi
}

validate_banking_compliance() {
    echo "🏛️ Validating banking compliance configuration..."
    
    # BIAN compliance
    if [[ "${BIAN_COMPLIANCE_ENABLED:-false}" == "true" ]]; then
        echo "✅ BIAN compliance: Enabled"
    fi
    
    # Islamic banking
    if [[ "${ISLAMIC_BANKING_ENABLED:-false}" == "true" ]]; then
        echo "✅ Islamic Banking: Enabled"
        
        if [[ "${SHARIA_COMPLIANCE_STRICT:-false}" == "true" ]]; then
            echo "✅ Sharia compliance: Strict mode"
        fi
    fi
    
    # PCI compliance
    if [[ "${PCI_COMPLIANCE_ENABLED:-false}" == "true" ]]; then
        echo "✅ PCI compliance: Enabled"
    fi
}

validate_database_configuration() {
    echo "🗄️ Validating database configuration..."
    
    if [[ -n "${DATABASE_URL:-}" ]]; then
        echo "✅ Database URL: Configured"
        
        # Extract database host and port
        local db_host
        local db_port
        db_host=$(echo "$DATABASE_URL" | sed -n 's|.*://[^@]*@\([^:]*\):.*|\1|p')
        db_port=$(echo "$DATABASE_URL" | sed -n 's|.*://[^@]*@[^:]*:\([0-9]*\)/.*|\1|p')
        
        if [[ -n "$db_host" && -n "$db_port" ]]; then
            echo "✅ Database host: $db_host:$db_port"
        fi
    else
        echo "❌ Database URL not configured"
        exit 1
    fi
    
    # Check pool configuration
    if [[ -n "${DATABASE_POOL_SIZE:-}" ]]; then
        echo "✅ Database pool size: ${DATABASE_POOL_SIZE}"
    fi
}

validate_cache_configuration() {
    echo "📦 Validating cache configuration..."
    
    if [[ -n "${REDIS_HOST:-}" ]]; then
        echo "✅ Redis host: ${REDIS_HOST}:${REDIS_PORT:-6379}"
    else
        echo "⚠️ Redis host not configured - caching may be limited"
    fi
}

validate_messaging_configuration() {
    echo "📨 Validating messaging configuration..."
    
    if [[ -n "${KAFKA_BOOTSTRAP_SERVERS:-}" ]]; then
        echo "✅ Kafka servers: ${KAFKA_BOOTSTRAP_SERVERS}"
    else
        echo "⚠️ Kafka not configured - event streaming may be limited"
    fi
}

validate_ai_configuration() {
    echo "🤖 Validating AI/ML configuration..."
    
    if [[ "${AI_CREDIT_SCORING_ENABLED:-false}" == "true" ]]; then
        echo "✅ AI credit scoring: Enabled"
        
        if [[ -n "${OPENAI_API_KEY:-}" ]]; then
            echo "✅ OpenAI API: Configured"
        else
            echo "⚠️ OpenAI API key not configured"
        fi
    fi
    
    if [[ "${ML_RISK_ASSESSMENT_ENABLED:-false}" == "true" ]]; then
        echo "✅ ML risk assessment: Enabled"
    fi
}

validate_monitoring_configuration() {
    echo "📊 Validating monitoring configuration..."
    
    if [[ "${PROMETHEUS_ENABLED:-false}" == "true" ]]; then
        echo "✅ Prometheus metrics: Enabled"
    fi
    
    if [[ "${JAEGER_ENABLED:-false}" == "true" ]]; then
        echo "✅ Jaeger tracing: Enabled"
    fi
    
    # Check management endpoints
    local endpoints="${MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE:-health,info}"
    echo "✅ Management endpoints: $endpoints"
}

validate_business_rules() {
    echo "📋 Validating business rules configuration..."
    
    if [[ -n "${LOAN_MIN_AMOUNT:-}" && -n "${LOAN_MAX_AMOUNT:-}" ]]; then
        echo "✅ Loan limits: ${LOAN_MIN_AMOUNT} - ${LOAN_MAX_AMOUNT}"
    fi
    
    if [[ -n "${CUSTOMER_MIN_CREDIT_SCORE:-}" ]]; then
        echo "✅ Credit score threshold: ${CUSTOMER_MIN_CREDIT_SCORE}"
    fi
    
    if [[ "${RATE_LIMIT_ENABLED:-false}" == "true" ]]; then
        echo "✅ Rate limiting: Enabled"
    fi
}

create_required_directories() {
    echo "📁 Creating required directories..."
    
    mkdir -p /app/logs
    mkdir -p /app/config
    mkdir -p /app/tmp
    mkdir -p /app/keys
    mkdir -p /app/reports
    
    echo "✅ Required directories created"
}

validate_file_permissions() {
    echo "🔑 Validating file permissions..."
    
    if [[ -w "/app/logs" ]]; then
        echo "✅ Logs directory: Writable"
    else
        echo "❌ Logs directory: Not writable"
        exit 1
    fi
    
    if [[ -w "/app/tmp" ]]; then
        echo "✅ Temp directory: Writable"
    else
        echo "❌ Temp directory: Not writable"
        exit 1
    fi
}

# Main validation execution
main() {
    echo "Starting comprehensive pre-start validation..."
    echo "============================================="
    
    validate_java_version
    validate_memory_configuration
    validate_security_configuration
    validate_banking_compliance
    validate_database_configuration
    validate_cache_configuration
    validate_messaging_configuration
    validate_ai_configuration
    validate_monitoring_configuration
    validate_business_rules
    create_required_directories
    validate_file_permissions
    
    echo "============================================="
    echo "✅ All pre-start validations completed successfully"
    echo "🏦 Enhanced Enterprise Banking System is ready to start"
    echo "============================================="
}

# Execute main validation
main "$@"