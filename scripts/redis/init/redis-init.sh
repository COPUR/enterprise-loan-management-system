#!/bin/bash

# Enterprise Loan Management System - Redis Initialization Script
# Populates Redis with sample data for testing and demonstration

set -e

REDIS_HOST=${REDIS_HOST:-"redis"}
REDIS_PORT=${REDIS_PORT:-6379}
REDIS_PASSWORD=${REDIS_PASSWORD:-"banking_password"}

echo "🚀 Initializing Redis with Enterprise Loan Management System data..."

# Wait for Redis to be ready
echo "⏳ Waiting for Redis to be ready..."
until redis-cli -h $REDIS_HOST -p $REDIS_PORT -a $REDIS_PASSWORD ping >/dev/null 2>&1; do
    echo "Redis not ready yet, waiting 5 seconds..."
    sleep 5
done

echo "✅ Redis is ready!"

# Function to execute Redis commands
redis_exec() {
    redis-cli -h $REDIS_HOST -p $REDIS_PORT -a $REDIS_PASSWORD "$@"
}

# Banking Application Configuration
echo "⚙️ Setting up banking application configuration..."

# Application settings
redis_exec HSET "config:banking:app" \
    "name" "Enterprise Loan Management System" \
    "version" "1.0.0" \
    "environment" "development" \
    "max_loan_amount" "5000000" \
    "default_interest_rate" "6.75" \
    "max_credit_limit" "2000000" \
    "session_timeout" "1800" \
    "max_login_attempts" "3" \
    "password_expiry_days" "90"

# Database configuration
redis_exec HSET "config:banking:database" \
    "host" "postgres" \
    "port" "5432" \
    "name" "banking_db" \
    "max_connections" "100" \
    "connection_timeout" "30" \
    "query_timeout" "60"

# Kafka configuration
redis_exec HSET "config:banking:kafka" \
    "bootstrap_servers" "kafka:9092" \
    "consumer_group" "banking-consumers" \
    "batch_size" "100" \
    "max_poll_records" "500" \
    "session_timeout" "30000" \
    "heartbeat_interval" "3000"

# Security configuration
redis_exec HSET "config:banking:security" \
    "jwt_expiration" "3600" \
    "refresh_token_expiration" "86400" \
    "oauth_enabled" "true" \
    "dpop_enabled" "true" \
    "fapi_enabled" "true" \
    "mfa_required" "true" \
    "password_policy" "strict" \
    "encryption_algorithm" "AES-256-GCM"

# Feature flags
redis_exec HSET "config:banking:features" \
    "ml_fraud_detection" "true" \
    "real_time_notifications" "true" \
    "cross_region_sync" "true" \
    "open_banking_api" "true" \
    "advanced_analytics" "true" \
    "zero_trust_security" "true" \
    "automated_compliance" "true" \
    "disaster_recovery" "true"

echo "✅ Application configuration loaded!"

# Sample Customer Data (Cache)
echo "👥 Loading sample customer data..."

# Customer profiles
redis_exec HSET "customer:110e8400-e29b-41d4-a716-446655440001" \
    "customer_number" "CUST-001" \
    "name" "John Smith" \
    "email" "john.smith@email.com" \
    "phone" "+1-555-0101" \
    "status" "ACTIVE" \
    "risk_rating" "LOW" \
    "credit_score" "785" \
    "credit_limit" "150000" \
    "available_credit" "145000" \
    "last_activity" "2024-04-15T10:30:00Z" \
    "session_count" "25" \
    "preferred_channel" "MOBILE_APP"

redis_exec HSET "customer:110e8400-e29b-41d4-a716-446655440002" \
    "customer_number" "CUST-002" \
    "name" "Sarah Johnson" \
    "email" "sarah.johnson@email.com" \
    "phone" "+1-555-0102" \
    "status" "ACTIVE" \
    "risk_rating" "MEDIUM" \
    "credit_score" "720" \
    "credit_limit" "75000" \
    "available_credit" "70000" \
    "last_activity" "2024-04-15T09:45:00Z" \
    "session_count" "18" \
    "preferred_channel" "WEB_PORTAL"

redis_exec HSET "customer:110e8400-e29b-41d4-a716-446655440003" \
    "customer_number" "CUST-003" \
    "name" "Michael Brown" \
    "email" "michael.brown@email.com" \
    "phone" "+1-555-0103" \
    "status" "ACTIVE" \
    "risk_rating" "HIGH" \
    "credit_score" "650" \
    "credit_limit" "25000" \
    "available_credit" "22000" \
    "last_activity" "2024-04-14T16:20:00Z" \
    "session_count" "12" \
    "preferred_channel" "PHONE"

# Customer indices
redis_exec HSET "customer:index:email" \
    "john.smith@email.com" "110e8400-e29b-41d4-a716-446655440001" \
    "sarah.johnson@email.com" "110e8400-e29b-41d4-a716-446655440002" \
    "michael.brown@email.com" "110e8400-e29b-41d4-a716-446655440003"

redis_exec HSET "customer:index:phone" \
    "+1-555-0101" "110e8400-e29b-41d4-a716-446655440001" \
    "+1-555-0102" "110e8400-e29b-41d4-a716-446655440002" \
    "+1-555-0103" "110e8400-e29b-41d4-a716-446655440003"

echo "✅ Customer data loaded!"

# Sample Loan Data
echo "🏦 Loading sample loan data..."

# Active loans
redis_exec HSET "loan:990e8400-e29b-41d4-a716-446655440001" \
    "loan_number" "LOAN-001" \
    "customer_id" "110e8400-e29b-41d4-a716-446655440001" \
    "loan_type" "PERSONAL" \
    "principal_amount" "25000.00" \
    "outstanding_balance" "23500.00" \
    "interest_rate" "6.75" \
    "term_months" "60" \
    "remaining_terms" "57" \
    "monthly_payment" "486.87" \
    "next_payment_date" "2024-05-01" \
    "status" "ACTIVE" \
    "last_payment_date" "2024-04-01" \
    "last_payment_amount" "486.87"

redis_exec HSET "loan:990e8400-e29b-41d4-a716-446655440002" \
    "loan_number" "LOAN-002" \
    "customer_id" "110e8400-e29b-41d4-a716-446655440002" \
    "loan_type" "AUTO" \
    "principal_amount" "35000.00" \
    "outstanding_balance" "33200.00" \
    "interest_rate" "5.50" \
    "term_months" "72" \
    "remaining_terms" "69" \
    "monthly_payment" "569.23" \
    "next_payment_date" "2024-05-05" \
    "status" "ACTIVE" \
    "last_payment_date" "2024-04-05" \
    "last_payment_amount" "569.23"

redis_exec HSET "loan:990e8400-e29b-41d4-a716-446655440003" \
    "loan_number" "LOAN-003" \
    "customer_id" "110e8400-e29b-41d4-a716-446655440004" \
    "loan_type" "BUSINESS" \
    "principal_amount" "500000.00" \
    "outstanding_balance" "485000.00" \
    "interest_rate" "4.75" \
    "term_months" "120" \
    "remaining_terms" "117" \
    "monthly_payment" "5185.32" \
    "next_payment_date" "2024-05-01" \
    "status" "ACTIVE" \
    "last_payment_date" "2024-04-01" \
    "last_payment_amount" "5185.32"

# Loan indices
redis_exec HSET "loan:index:customer" \
    "110e8400-e29b-41d4-a716-446655440001" "990e8400-e29b-41d4-a716-446655440001" \
    "110e8400-e29b-41d4-a716-446655440002" "990e8400-e29b-41d4-a716-446655440002" \
    "110e8400-e29b-41d4-a716-446655440004" "990e8400-e29b-41d4-a716-446655440003"

echo "✅ Loan data loaded!"

# Session Management
echo "🔐 Setting up session management..."

# Active sessions
redis_exec SETEX "session:user:john.smith@email.com:session_001" 1800 \
    "{\"user_id\":\"john.smith@email.com\",\"customer_id\":\"110e8400-e29b-41d4-a716-446655440001\",\"roles\":[\"CUSTOMER\"],\"login_time\":\"2024-04-15T10:30:00Z\",\"ip_address\":\"192.168.1.100\",\"user_agent\":\"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36\"}"

redis_exec SETEX "session:user:sarah.johnson@email.com:session_002" 1800 \
    "{\"user_id\":\"sarah.johnson@email.com\",\"customer_id\":\"110e8400-e29b-41d4-a716-446655440002\",\"roles\":[\"CUSTOMER\"],\"login_time\":\"2024-04-15T09:45:00Z\",\"ip_address\":\"192.168.1.101\",\"user_agent\":\"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36\"}"

# OAuth tokens
redis_exec SETEX "oauth:access_token:abc123def456" 3600 \
    "{\"client_id\":\"banking-app\",\"user_id\":\"john.smith@email.com\",\"scope\":\"read write\",\"token_type\":\"Bearer\",\"expires_in\":3600}"

redis_exec SETEX "oauth:refresh_token:def456ghi789" 86400 \
    "{\"client_id\":\"banking-app\",\"user_id\":\"john.smith@email.com\",\"access_token\":\"abc123def456\"}"

# DPoP proof cache
redis_exec SETEX "dpop:proof:jti_001" 300 \
    "{\"jti\":\"jti_001\",\"htm\":\"POST\",\"htu\":\"https://api.banking.com/loans\",\"iat\":1713178200,\"exp\":1713178500}"

echo "✅ Session management configured!"

# Caching Configuration
echo "💾 Setting up caching patterns..."

# Customer lookup cache
redis_exec SETEX "cache:customer:lookup:CUST-001" 300 \
    "{\"customer_id\":\"110e8400-e29b-41d4-a716-446655440001\",\"name\":\"John Smith\",\"status\":\"ACTIVE\",\"risk_rating\":\"LOW\"}"

redis_exec SETEX "cache:customer:lookup:CUST-002" 300 \
    "{\"customer_id\":\"110e8400-e29b-41d4-a716-446655440002\",\"name\":\"Sarah Johnson\",\"status\":\"ACTIVE\",\"risk_rating\":\"MEDIUM\"}"

# Loan calculations cache
redis_exec SETEX "cache:loan:calculation:LOAN-001" 600 \
    "{\"loan_id\":\"990e8400-e29b-41d4-a716-446655440001\",\"monthly_payment\":486.87,\"total_interest\":4212.20,\"payoff_date\":\"2029-02-01\"}"

redis_exec SETEX "cache:loan:calculation:LOAN-002" 600 \
    "{\"loan_id\":\"990e8400-e29b-41d4-a716-446655440002\",\"monthly_payment\":569.23,\"total_interest\":5984.56,\"payoff_date\":\"2030-02-05\"}"

# ML model predictions cache
redis_exec SETEX "cache:ml:fraud:PAY-001" 900 \
    "{\"payment_id\":\"bb0e8400-e29b-41d4-a716-446655440001\",\"fraud_score\":0.0234,\"risk_level\":\"LOW\",\"confidence\":0.9876}"

redis_exec SETEX "cache:ml:credit:CUST-001" 1800 \
    "{\"customer_id\":\"110e8400-e29b-41d4-a716-446655440001\",\"credit_score\":785,\"risk_rating\":\"LOW\",\"recommendation\":\"APPROVE\"}"

echo "✅ Caching patterns configured!"

# Rate Limiting
echo "🚦 Setting up rate limiting..."

# API rate limits (per user per hour)
redis_exec SETEX "rate_limit:api:john.smith@email.com" 3600 "100"
redis_exec SETEX "rate_limit:api:sarah.johnson@email.com" 3600 "100"

# Payment processing rate limits (per user per day)
redis_exec SETEX "rate_limit:payment:john.smith@email.com" 86400 "10"
redis_exec SETEX "rate_limit:payment:sarah.johnson@email.com" 86400 "10"

# Login attempt rate limits (per IP per hour)
redis_exec SETEX "rate_limit:login:192.168.1.100" 3600 "10"
redis_exec SETEX "rate_limit:login:192.168.1.101" 3600 "10"

echo "✅ Rate limiting configured!"

# Monitoring and Metrics
echo "📊 Setting up monitoring and metrics..."

# Application metrics
redis_exec HSET "metrics:app:counters" \
    "total_customers" "6" \
    "total_loans" "4" \
    "total_payments" "5" \
    "active_sessions" "2" \
    "api_requests_today" "1250" \
    "successful_logins_today" "45" \
    "failed_logins_today" "3" \
    "fraud_alerts_today" "0"

redis_exec HSET "metrics:app:gauges" \
    "cpu_usage" "65.5" \
    "memory_usage" "78.3" \
    "disk_usage" "45.2" \
    "active_connections" "125" \
    "queue_depth" "8" \
    "cache_hit_rate" "94.5" \
    "response_time_avg" "125.8" \
    "error_rate" "0.02"

# Performance metrics
redis_exec HSET "metrics:performance:response_times" \
    "login_avg" "250" \
    "customer_lookup_avg" "45" \
    "loan_calculation_avg" "180" \
    "payment_processing_avg" "320" \
    "fraud_detection_avg" "85" \
    "compliance_check_avg" "150"

echo "✅ Monitoring and metrics configured!"

# Pub/Sub Channels
echo "📡 Setting up pub/sub channels..."

# Publish sample events
redis_exec PUBLISH "events:customer" \
    "{\"event_type\":\"customer_login\",\"customer_id\":\"110e8400-e29b-41d4-a716-446655440001\",\"timestamp\":\"2024-04-15T10:30:00Z\",\"channel\":\"MOBILE_APP\"}"

redis_exec PUBLISH "events:loan" \
    "{\"event_type\":\"payment_received\",\"loan_id\":\"990e8400-e29b-41d4-a716-446655440001\",\"amount\":486.87,\"timestamp\":\"2024-04-01T10:00:00Z\"}"

redis_exec PUBLISH "events:security" \
    "{\"event_type\":\"successful_authentication\",\"user_id\":\"john.smith@email.com\",\"method\":\"OAuth2.1+DPoP\",\"timestamp\":\"2024-04-15T10:30:00Z\"}"

redis_exec PUBLISH "events:ml" \
    "{\"event_type\":\"fraud_prediction\",\"entity_id\":\"bb0e8400-e29b-41d4-a716-446655440001\",\"prediction\":0.0234,\"confidence\":0.9876,\"timestamp\":\"2024-04-01T10:00:00Z\"}"

echo "✅ Pub/sub channels configured!"

# Distributed Locks
echo "🔒 Setting up distributed locks..."

# Sample locks for critical operations
redis_exec SETEX "lock:payment:processing:bb0e8400-e29b-41d4-a716-446655440001" 300 "processing_node_001"
redis_exec SETEX "lock:loan:calculation:990e8400-e29b-41d4-a716-446655440001" 60 "calculation_service_001"
redis_exec SETEX "lock:customer:update:110e8400-e29b-41d4-a716-446655440001" 30 "customer_service_001"

echo "✅ Distributed locks configured!"

# Sorted Sets for Rankings and Leaderboards
echo "🏆 Setting up sorted sets..."

# Customer credit scores (sorted by score)
redis_exec ZADD "ranking:credit_scores" 785 "110e8400-e29b-41d4-a716-446655440001"
redis_exec ZADD "ranking:credit_scores" 720 "110e8400-e29b-41d4-a716-446655440002"
redis_exec ZADD "ranking:credit_scores" 650 "110e8400-e29b-41d4-a716-446655440003"

# Loan amounts (sorted by amount)
redis_exec ZADD "ranking:loan_amounts" 25000 "990e8400-e29b-41d4-a716-446655440001"
redis_exec ZADD "ranking:loan_amounts" 35000 "990e8400-e29b-41d4-a716-446655440002"
redis_exec ZADD "ranking:loan_amounts" 500000 "990e8400-e29b-41d4-a716-446655440003"

# Risk scores (sorted by risk level)
redis_exec ZADD "ranking:risk_scores" 15 "110e8400-e29b-41d4-a716-446655440001"
redis_exec ZADD "ranking:risk_scores" 25 "110e8400-e29b-41d4-a716-446655440002"
redis_exec ZADD "ranking:risk_scores" 45 "110e8400-e29b-41d4-a716-446655440003"

echo "✅ Sorted sets configured!"

# Lists for Queues
echo "📋 Setting up queues..."

# Payment processing queue
redis_exec LPUSH "queue:payment:processing" \
    "{\"payment_id\":\"bb0e8400-e29b-41d4-a716-446655440001\",\"amount\":486.87,\"customer_id\":\"110e8400-e29b-41d4-a716-446655440001\"}" \
    "{\"payment_id\":\"bb0e8400-e29b-41d4-a716-446655440002\",\"amount\":569.23,\"customer_id\":\"110e8400-e29b-41d4-a716-446655440002\"}"

# Fraud analysis queue
redis_exec LPUSH "queue:fraud:analysis" \
    "{\"transaction_id\":\"trans_001\",\"amount\":25000,\"customer_id\":\"110e8400-e29b-41d4-a716-446655440001\",\"timestamp\":\"2024-04-15T10:30:00Z\"}"

# Notification queue
redis_exec LPUSH "queue:notifications" \
    "{\"type\":\"payment_reminder\",\"customer_id\":\"110e8400-e29b-41d4-a716-446655440001\",\"loan_id\":\"990e8400-e29b-41d4-a716-446655440001\",\"due_date\":\"2024-05-01\"}" \
    "{\"type\":\"loan_approved\",\"customer_id\":\"110e8400-e29b-41d4-a716-446655440002\",\"loan_id\":\"990e8400-e29b-41d4-a716-446655440002\",\"amount\":35000}"

echo "✅ Queues configured!"

# Health Check Data
echo "🏥 Setting up health check data..."

# Service health status
redis_exec HSET "health:services" \
    "database" "UP" \
    "kafka" "UP" \
    "elasticsearch" "UP" \
    "keycloak" "UP" \
    "ml_service" "UP" \
    "payment_service" "UP" \
    "customer_service" "UP" \
    "loan_service" "UP" \
    "federation_service" "UP"

# System health metrics
redis_exec HSET "health:system" \
    "uptime_seconds" "3600" \
    "memory_usage_percent" "78.3" \
    "cpu_usage_percent" "65.5" \
    "disk_usage_percent" "45.2" \
    "network_connections" "125" \
    "last_backup" "2024-04-15T02:00:00Z" \
    "last_health_check" "2024-04-15T10:30:00Z"

echo "✅ Health check data configured!"

# Final verification
echo "🔍 Verifying Redis setup..."

# Check database info
redis_exec INFO server | grep redis_version
redis_exec INFO memory | grep used_memory_human
redis_exec INFO clients | grep connected_clients

# Count keys by pattern
echo "📊 Redis data summary:"
echo "- Configuration keys: $(redis_exec KEYS "config:*" | wc -l)"
echo "- Customer keys: $(redis_exec KEYS "customer:*" | wc -l)"
echo "- Loan keys: $(redis_exec KEYS "loan:*" | wc -l)"
echo "- Session keys: $(redis_exec KEYS "session:*" | wc -l)"
echo "- Cache keys: $(redis_exec KEYS "cache:*" | wc -l)"
echo "- Metrics keys: $(redis_exec KEYS "metrics:*" | wc -l)"
echo "- Queue keys: $(redis_exec KEYS "queue:*" | wc -l)"
echo "- Health keys: $(redis_exec KEYS "health:*" | wc -l)"

total_keys=$(redis_exec DBSIZE)
echo "- Total keys: $total_keys"

echo "🎉 Redis initialization completed successfully!"
echo "📝 Redis is ready with comprehensive banking data"
echo "🔧 Configuration includes:"
echo "   - Application settings and feature flags"
echo "   - Sample customer, loan, and payment data"
echo "   - Session management and OAuth tokens"
echo "   - Caching patterns and rate limiting"
echo "   - Monitoring metrics and health checks"
echo "   - Pub/sub channels and distributed locks"
echo "   - Queues and sorted sets for rankings"
echo "🚀 Enterprise Loan Management System is ready for testing!"

exit 0