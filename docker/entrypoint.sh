#!/bin/bash
set -e

# Enterprise Loan Management System - Container Entry Point
# 12-Factor App compliant startup script

echo "=== Enterprise Banking System Startup ==="
echo "Build Date: $(date)"
echo "Container User: $(whoami)"
echo "Working Directory: $(pwd)"

# Parse DATABASE_URL if provided (12-Factor: Backing Services)
if [ -n "$DATABASE_URL" ]; then
    # Extract host and port from DATABASE_URL for connection testing
    DB_HOST=$(echo $DATABASE_URL | sed -n 's/.*\/\/.*@\([^:]*\):.*/\1/p' || echo "postgres")
    DB_PORT=$(echo $DATABASE_URL | sed -n 's/.*:\([0-9]*\)\/.*/\1/p' || echo "5432")
else
    DB_HOST=${DATABASE_HOST:-postgres}
    DB_PORT=${DATABASE_PORT:-5432}
fi

# Wait for database to be ready with timeout
echo "Waiting for database connection at ${DB_HOST}:${DB_PORT}..."
DB_READY=0
DB_TIMEOUT=60
DB_COUNTER=0

while [ $DB_READY -eq 0 ] && [ $DB_COUNTER -lt $DB_TIMEOUT ]; do
    if timeout 3 bash -c "</dev/tcp/${DB_HOST}/${DB_PORT}" 2>/dev/null; then
        DB_READY=1
        echo "✓ Database connection established"
    else
        echo "Database not ready yet, waiting... (${DB_COUNTER}/${DB_TIMEOUT})"
        sleep 2
        DB_COUNTER=$((DB_COUNTER + 1))
    fi
done

if [ $DB_READY -eq 0 ]; then
    echo "⚠ Database connection timeout - continuing startup (may fail if DB required)"
fi

# Wait for Redis to be ready with timeout (if Redis is configured)
if [ "$REDIS_HOST" != "" ]; then
    echo "Waiting for Redis connection at ${REDIS_HOST}:${REDIS_PORT}..."
    REDIS_READY=0
    REDIS_TIMEOUT=30
    REDIS_COUNTER=0
    
    while [ $REDIS_READY -eq 0 ] && [ $REDIS_COUNTER -lt $REDIS_TIMEOUT ]; do
        if timeout 3 bash -c "</dev/tcp/${REDIS_HOST}/${REDIS_PORT}" 2>/dev/null; then
            REDIS_READY=1
            echo "✓ Redis connection established"
        else
            echo "Redis not ready yet, waiting... (${REDIS_COUNTER}/${REDIS_TIMEOUT})"
            sleep 2
            REDIS_COUNTER=$((REDIS_COUNTER + 1))
        fi
    done
    
    if [ $REDIS_READY -eq 0 ]; then
        echo "⚠ Redis connection timeout - continuing startup (caching may be disabled)"
    fi
fi

# Initialize application directories
mkdir -p /app/logs /app/tmp
touch /app/logs/application.log

# Display build information if available
if [ -f "/app/build-info.properties" ]; then
    echo "=== Build Information ==="
    cat /app/build-info.properties
    echo "========================="
fi

# Set 12-Factor App configuration defaults
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-production}"
export SERVER_PORT="${SERVER_PORT:-8080}"

# Banking domain configuration (12-Factor: Config)
export BANKING_COMPLIANCE_STRICT="${BANKING_COMPLIANCE_STRICT:-true}"
export FAPI_ENABLED="${FAPI_ENABLED:-true}"
export PCI_ENABLED="${PCI_ENABLED:-true}"
export AUDIT_ENABLED="${AUDIT_ENABLED:-true}"
export KYC_REQUIRED="${KYC_REQUIRED:-true}"

# Performance and limits
export LOAN_MAX_AMOUNT="${LOAN_MAX_AMOUNT:-5000000}"
export LOAN_MIN_AMOUNT="${LOAN_MIN_AMOUNT:-1000}"
export TRANSACTION_DAILY_LIMIT="${TRANSACTION_DAILY_LIMIT:-50000}"
export MAX_CONCURRENT_REQUESTS="${MAX_CONCURRENT_REQUESTS:-100}"

# Cache configuration
export CACHE_TTL="${CACHE_TTL:-3600}"
export REDIS_TIMEOUT="${REDIS_TIMEOUT:-2000ms}"

# Monitoring and observability (12-Factor: Admin Processes)
export ACTUATOR_ENDPOINTS="${ACTUATOR_ENDPOINTS:-health,info,metrics,prometheus}"
export HEALTH_SHOW_DETAILS="${HEALTH_SHOW_DETAILS:-always}"
export PROMETHEUS_ENABLED="${PROMETHEUS_ENABLED:-true}"

# Security configuration
export JWT_EXPIRATION="${JWT_EXPIRATION:-86400}"
export SESSION_TIMEOUT="${SESSION_TIMEOUT:-1800}"

# Circuit breaker configuration
export CB_FAILURE_RATE="${CB_FAILURE_RATE:-50}"
export CB_WAIT_DURATION="${CB_WAIT_DURATION:-30s}"

echo "=== Runtime Configuration ==="
echo "Application Name: ${APP_NAME}"
echo "Version: ${APP_VERSION}"
echo "Java Version: $(java -version 2>&1 | head -n 1)"
echo "JVM Options: ${JAVA_OPTS}"
echo "Spring Profiles: ${SPRING_PROFILES_ACTIVE}"
echo "Server Port: ${SERVER_PORT}"
echo "Database URL: ${DATABASE_URL}"
echo "Redis: ${REDIS_HOST}:${REDIS_PORT}"
echo "Compliance Mode: ${BANKING_COMPLIANCE_STRICT}"
echo "FAPI Enabled: ${FAPI_ENABLED}"
echo "Audit Enabled: ${AUDIT_ENABLED}"
echo "Max Loan Amount: ${LOAN_MAX_AMOUNT}"
echo "============================="

# Start the banking application
echo "🏦 Starting Enterprise Loan Management System..."
exec java ${JAVA_OPTS} -jar app.jar "$@"