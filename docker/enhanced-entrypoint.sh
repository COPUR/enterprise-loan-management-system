#!/bin/bash
# Enhanced Enterprise Banking System - Entry Point Script
# Supports DDD/Hexagonal Architecture, BIAN Compliance, FAPI Security, and Islamic Banking

set -euo pipefail

# Banking system configuration
export APP_NAME="Enhanced Enterprise Banking System"
export APP_VERSION="2.0.0"

# Logging setup
exec > >(tee -a /app/logs/startup.log)
exec 2>&1

echo "=========================================="
echo "🏦 Enhanced Enterprise Banking System 🏦"
echo "=========================================="
echo "Version: ${APP_VERSION}"
echo "Architecture: DDD + Hexagonal"
echo "Compliance: BIAN + FAPI + Islamic Banking"
echo "Features: AI/ML + Functional Tests"
echo "Startup Time: $(date)"
echo "=========================================="

# Pre-start validation script
if [[ -x "/app/prestart.sh" ]]; then
    echo "🔍 Running pre-start validation..."
    /app/prestart.sh
fi

# Wait for database connection
echo "🔗 Waiting for database connection..."
while ! nc -z ${DATABASE_URL##*://} 2>/dev/null; do
    echo "⏳ Waiting for database to be ready..."
    sleep 2
done
echo "✅ Database connection established"

# Wait for Redis connection
echo "🔗 Waiting for Redis connection..."
while ! nc -z ${REDIS_HOST:-redis} ${REDIS_PORT:-6379} 2>/dev/null; do
    echo "⏳ Waiting for Redis to be ready..."
    sleep 2
done
echo "✅ Redis connection established"

# Validate banking compliance settings
echo "🏛️ Validating banking compliance settings..."
if [[ "${FAPI_ENABLED:-false}" == "true" ]]; then
    echo "✅ FAPI (Financial-grade API) security enabled"
fi

if [[ "${BIAN_COMPLIANCE_ENABLED:-false}" == "true" ]]; then
    echo "✅ BIAN (Banking Industry Architecture Network) compliance enabled"
fi

if [[ "${ISLAMIC_BANKING_ENABLED:-false}" == "true" ]]; then
    echo "✅ Islamic Banking (Sharia-compliant) features enabled"
fi

if [[ "${AI_CREDIT_SCORING_ENABLED:-false}" == "true" ]]; then
    echo "✅ AI-powered credit scoring enabled"
fi

# Security validation
echo "🔐 Validating security configuration..."
if [[ -z "${BANKING_JWT_SECRET:-}" ]]; then
    echo "⚠️ WARNING: Banking JWT secret not configured - using default (not for production!)"
fi

if [[ "${AUDIT_ENABLED:-false}" == "true" ]]; then
    echo "✅ Audit logging enabled for compliance"
fi

# Create required directories
mkdir -p /app/logs /app/reports /app/tmp

# Set up logging configuration
export LOGGING_CONFIG="${LOGGING_CONFIG:-/app/config/logback-spring.xml}"

# Memory and performance tuning
echo "🚀 System performance configuration:"
echo "   Max Memory: $(echo ${JAVA_OPTS} | grep -o 'MaxRAMPercentage=[0-9.]*' || echo 'Default')"
echo "   GC Algorithm: $(echo ${JAVA_OPTS} | grep -o 'UseG1GC' || echo 'Default')"

# Banking-specific environment setup
export SPRING_APPLICATION_NAME="${APP_NAME}"
export SPRING_APPLICATION_VERSION="${APP_VERSION}"

# Start the Enhanced Enterprise Banking System
echo "🏦 Starting Enhanced Enterprise Banking System..."
echo "=========================================="

exec "$@"