#!/bin/bash
set -e

# Enterprise Loan Management System - Container Entry Point

# Wait for database to be ready
echo "Waiting for database connection..."
until nc -z ${DATABASE_HOST:-postgres} ${DATABASE_PORT:-5432}; do
  echo "Database not ready yet, waiting..."
  sleep 5
done
echo "Database connection established"

# Wait for Redis to be ready
echo "Waiting for Redis connection..."
until nc -z ${REDIS_HOST:-redis} ${REDIS_PORT:-6379}; do
  echo "Redis not ready yet, waiting..."
  sleep 5
done
echo "Redis connection established"

# Initialize application logs
mkdir -p /app/logs
touch /app/logs/application.log

# Set default Java options if not provided
export JAVA_OPTS="${JAVA_OPTS:--Xmx2g -Xms1g -XX:+UseContainerSupport -XX:+UseG1GC}"

# Banking system specific environment variables
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-kubernetes,production}"
export BANKING_INSTALLMENTS_ALLOWED="${BANKING_INSTALLMENTS_ALLOWED:-6,9,12,24}"
export BANKING_INTEREST_RATE_MIN="${BANKING_INTEREST_RATE_MIN:-0.1}"
export BANKING_INTEREST_RATE_MAX="${BANKING_INTEREST_RATE_MAX:-0.5}"
export BANKING_FAPI_ENABLED="${BANKING_FAPI_ENABLED:-true}"

# Cache configuration
export REDIS_TIMEOUT="${REDIS_TIMEOUT:-2000}"
export REDIS_POOL_MAX_ACTIVE="${REDIS_POOL_MAX_ACTIVE:-8}"

# Monitoring configuration
export MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE="health,metrics,prometheus,info"
export MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS="always"

echo "Starting Enterprise Loan Management System..."
echo "Java Version: $(java -version 2>&1 | head -n 1)"
echo "Memory: ${JAVA_OPTS}"
echo "Environment: ${SPRING_PROFILES_ACTIVE}"
echo "Database: ${DATABASE_URL}"
echo "Redis: ${REDIS_HOST}:${REDIS_PORT}"

# Start the banking application
exec java ${JAVA_OPTS} -jar app.jar "$@"