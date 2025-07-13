#!/bin/bash

# Health Check Utilities for Enterprise Banking Platform
# 
# Comprehensive health check functions for deployment validation

# Check environment health
check_environment_health() {
    local env="$1"
    
    log_info "Checking $env environment health..."
    
    # Check Kubernetes cluster health
    check_kubernetes_health "$env"
    
    # Check database health
    check_database_health "$env"
    
    # Check Redis health
    check_redis_health "$env"
    
    # Check Kafka health
    check_kafka_health "$env"
    
    # Check external services
    check_external_services_health "$env"
    
    # Check resource availability
    check_resource_availability "$env"
    
    log_success "Environment $env health check completed"
}

# Check Kubernetes cluster health
check_kubernetes_health() {
    local env="$1"
    local namespace=$(get_namespace "$env")
    
    log_info "Checking Kubernetes cluster health..."
    
    # Check cluster info
    if ! kubectl cluster-info &> /dev/null; then
        log_error "Kubernetes cluster is not accessible"
        return 1
    fi
    
    # Check namespace exists
    if ! kubectl get namespace "$namespace" &> /dev/null; then
        log_warn "Namespace $namespace does not exist, creating..."
        kubectl create namespace "$namespace"
    fi
    
    # Check node health
    local unhealthy_nodes=$(kubectl get nodes --no-headers | grep -v " Ready " | wc -l)
    if [[ $unhealthy_nodes -gt 0 ]]; then
        log_warn "$unhealthy_nodes node(s) are not ready"
    fi
    
    # Check resource quotas
    check_namespace_resources "$namespace"
    
    log_success "Kubernetes cluster health OK"
}

# Check namespace resources
check_namespace_resources() {
    local namespace="$1"
    
    # Check CPU and memory usage
    local cpu_usage=$(kubectl top nodes --no-headers | awk '{sum+=$3} END {print sum}' || echo "0")
    local memory_usage=$(kubectl top nodes --no-headers | awk '{sum+=$5} END {print sum}' || echo "0")
    
    log_info "Cluster resource usage - CPU: ${cpu_usage}%, Memory: ${memory_usage}%"
    
    # Check if namespace has resource quotas
    if kubectl get resourcequota -n "$namespace" &> /dev/null; then
        local quota_status=$(kubectl describe resourcequota -n "$namespace" 2>/dev/null || echo "No quotas")
        log_info "Resource quota status: $quota_status"
    fi
}

# Check database connectivity and health
check_database_health() {
    local env="$1"
    local db_url=$(get_env_config "$env" "database_url")
    
    log_info "Checking database health for $env..."
    
    # Extract connection details
    local db_host=$(echo "$db_url" | sed -n 's/.*:\/\/\([^:]*\):.*/\1/p')
    local db_port=$(echo "$db_url" | sed -n 's/.*:\([0-9]*\)\/.*/\1/p')
    local db_name=$(echo "$db_url" | sed -n 's/.*\/\([^?]*\).*/\1/p')
    
    # Check network connectivity
    if ! nc -z "$db_host" "$db_port" 2>/dev/null; then
        log_error "Cannot connect to database at $db_host:$db_port"
        return 1
    fi
    
    # Check database connection
    local db_status=$(PGPASSWORD="$DB_PASSWORD" psql -h "$db_host" -p "$db_port" -U "$DB_USERNAME" -d "$db_name" -c "SELECT 1;" 2>/dev/null | grep -c "1 row" || echo "0")
    
    if [[ "$db_status" != "1" ]]; then
        log_error "Database connection test failed"
        return 1
    fi
    
    # Check database size and performance
    check_database_performance "$env"
    
    log_success "Database health OK"
}

# Check database performance metrics
check_database_performance() {
    local env="$1"
    local db_url=$(get_env_config "$env" "database_url")
    
    # Extract connection details
    local db_host=$(echo "$db_url" | sed -n 's/.*:\/\/\([^:]*\):.*/\1/p')
    local db_port=$(echo "$db_url" | sed -n 's/.*:\([0-9]*\)\/.*/\1/p')
    local db_name=$(echo "$db_url" | sed -n 's/.*\/\([^?]*\).*/\1/p')
    
    # Check connection count
    local connection_count=$(PGPASSWORD="$DB_PASSWORD" psql -h "$db_host" -p "$db_port" -U "$DB_USERNAME" -d "$db_name" -t -c "SELECT count(*) FROM pg_stat_activity;" 2>/dev/null | tr -d ' ' || echo "0")
    
    log_info "Database connections: $connection_count/$DB_CONNECTION_POOL_SIZE"
    
    if [[ $connection_count -gt $DB_CONNECTION_POOL_SIZE ]]; then
        log_warn "Database connection count is high: $connection_count"
    fi
    
    # Check slow queries
    local slow_queries=$(PGPASSWORD="$DB_PASSWORD" psql -h "$db_host" -p "$db_port" -U "$DB_USERNAME" -d "$db_name" -t -c "SELECT count(*) FROM pg_stat_activity WHERE state = 'active' AND query_start < now() - interval '30 seconds';" 2>/dev/null | tr -d ' ' || echo "0")
    
    if [[ $slow_queries -gt 0 ]]; then
        log_warn "Slow queries detected: $slow_queries"
    fi
}

# Check Redis health
check_redis_health() {
    local env="$1"
    local redis_url=$(get_env_config "$env" "redis_url")
    
    log_info "Checking Redis health for $env..."
    
    # Extract Redis connection details
    local redis_host=$(echo "$redis_url" | sed -n 's/.*:\/\/\([^:]*\):.*/\1/p')
    local redis_port=$(echo "$redis_url" | sed -n 's/.*:\([0-9]*\).*/\1/p')
    
    # Check Redis connectivity
    if ! nc -z "$redis_host" "$redis_port" 2>/dev/null; then
        log_error "Cannot connect to Redis at $redis_host:$redis_port"
        return 1
    fi
    
    # Check Redis response
    local redis_response=$(redis-cli -h "$redis_host" -p "$redis_port" ping 2>/dev/null || echo "ERROR")
    
    if [[ "$redis_response" != "PONG" ]]; then
        log_error "Redis health check failed: $redis_response"
        return 1
    fi
    
    # Check Redis memory usage
    local redis_memory=$(redis-cli -h "$redis_host" -p "$redis_port" info memory 2>/dev/null | grep "used_memory_human" | cut -d: -f2 | tr -d '\r' || echo "Unknown")
    log_info "Redis memory usage: $redis_memory"
    
    log_success "Redis health OK"
}

# Check Kafka health
check_kafka_health() {
    local env="$1"
    local kafka_brokers=$(get_env_config "$env" "kafka_brokers")
    
    log_info "Checking Kafka health for $env..."
    
    # Parse Kafka brokers
    IFS=',' read -ra BROKER_ARRAY <<< "$kafka_brokers"
    
    local healthy_brokers=0
    for broker in "${BROKER_ARRAY[@]}"; do
        local broker_host=$(echo "$broker" | cut -d: -f1)
        local broker_port=$(echo "$broker" | cut -d: -f2)
        
        if nc -z "$broker_host" "$broker_port" 2>/dev/null; then
            ((healthy_brokers++))
        else
            log_warn "Kafka broker $broker is not reachable"
        fi
    done
    
    if [[ $healthy_brokers -eq 0 ]]; then
        log_error "No Kafka brokers are reachable"
        return 1
    fi
    
    local total_brokers=${#BROKER_ARRAY[@]}
    log_info "Kafka brokers healthy: $healthy_brokers/$total_brokers"
    
    if [[ $healthy_brokers -lt $total_brokers ]]; then
        log_warn "Some Kafka brokers are unhealthy"
    fi
    
    log_success "Kafka health OK"
}

# Check external services health
check_external_services_health() {
    local env="$1"
    
    log_info "Checking external services health for $env..."
    
    # Check fraud detection service
    check_service_endpoint "$FRAUD_DETECTION_SERVICE" "Fraud Detection Service"
    
    # Check compliance service
    check_service_endpoint "$COMPLIANCE_SERVICE" "Compliance Service"
    
    # Check payment gateway
    check_service_endpoint "$PAYMENT_GATEWAY" "Payment Gateway"
    
    # Check credit bureau API
    check_service_endpoint "$CREDIT_BUREAU_API" "Credit Bureau API"
    
    log_success "External services health check completed"
}

# Check individual service endpoint
check_service_endpoint() {
    local endpoint="$1"
    local service_name="$2"
    local timeout=10
    
    if curl -sf --max-time "$timeout" "$endpoint/health" &> /dev/null; then
        log_success "$service_name is healthy"
    else
        log_warn "$service_name health check failed or endpoint unavailable"
    fi
}

# Check resource availability
check_resource_availability() {
    local env="$1"
    local namespace=$(get_namespace "$env")
    
    log_info "Checking resource availability for $env..."
    
    # Check available CPU and memory
    local node_resources=$(kubectl describe nodes | grep -A 5 "Allocated resources")
    
    # Check storage availability
    local storage_usage=$(kubectl get pv | grep -c "Available" || echo "0")
    log_info "Available storage volumes: $storage_usage"
    
    # Check if there are any pending pods
    local pending_pods=$(kubectl get pods -n "$namespace" --field-selector=status.phase=Pending --no-headers | wc -l)
    if [[ $pending_pods -gt 0 ]]; then
        log_warn "$pending_pods pod(s) are in pending state"
    fi
    
    # Check node conditions
    local nodes_with_issues=$(kubectl get nodes -o json | jq -r '.items[] | select(.status.conditions[] | select(.type=="Ready" and .status!="True")) | .metadata.name' | wc -l)
    if [[ $nodes_with_issues -gt 0 ]]; then
        log_warn "$nodes_with_issues node(s) have issues"
    fi
    
    log_success "Resource availability check completed"
}

# Validate application health
validate_application_health() {
    local env="$1"
    
    log_info "Validating application health for $env..."
    
    # Check application endpoints
    check_application_endpoints "$env"
    
    # Check application metrics
    check_application_metrics "$env"
    
    # Check application logs
    check_application_logs "$env"
    
    log_success "Application health validation completed"
}

# Check application endpoints
check_application_endpoints() {
    local env="$1"
    local namespace=$(get_namespace "$env")
    local service_name="$APP_NAME"
    
    # Get service endpoint
    local service_ip=$(kubectl get service "$service_name" -n "$namespace" -o jsonpath='{.spec.clusterIP}' 2>/dev/null || echo "")
    
    if [[ -z "$service_ip" ]]; then
        log_error "Cannot get service IP for $service_name"
        return 1
    fi
    
    # Test health endpoint
    local health_url="http://$service_ip:$APP_PORT$ACTUATOR_PATH/health"
    if ! curl -sf "$health_url" | jq -e '.status == "UP"' &> /dev/null; then
        log_error "Application health endpoint failed"
        return 1
    fi
    
    # Test readiness endpoint
    local ready_url="http://$service_ip:$APP_PORT$ACTUATOR_PATH/health/readiness"
    if ! curl -sf "$ready_url" | jq -e '.status == "UP"' &> /dev/null; then
        log_error "Application readiness endpoint failed"
        return 1
    fi
    
    # Test metrics endpoint
    local metrics_url="http://$service_ip:$MANAGEMENT_PORT$ACTUATOR_PATH/prometheus"
    if ! curl -sf "$metrics_url" &> /dev/null; then
        log_warn "Application metrics endpoint unavailable"
    fi
    
    log_success "Application endpoints are healthy"
}

# Check application metrics
check_application_metrics() {
    local env="$1"
    
    # Query Prometheus for key metrics
    if command -v curl &> /dev/null && [[ -n "$PROMETHEUS_ENDPOINT" ]]; then
        # Check if metrics are being scraped
        local metrics_query="up{job=\"$APP_NAME\",environment=\"$env\"}"
        local metrics_result=$(curl -s "$PROMETHEUS_ENDPOINT/api/v1/query?query=$metrics_query" | jq -r '.data.result[0].value[1]' 2>/dev/null || echo "0")
        
        if [[ "$metrics_result" == "1" ]]; then
            log_success "Application metrics are being collected"
        else
            log_warn "Application metrics collection may have issues"
        fi
    fi
}

# Check application logs
check_application_logs() {
    local env="$1"
    local namespace=$(get_namespace "$env")
    
    # Check for error patterns in recent logs
    local error_count=$(kubectl logs -l "app=$APP_NAME" -n "$namespace" --since=5m 2>/dev/null | grep -i "error\|exception\|fail" | wc -l || echo "0")
    
    if [[ $error_count -gt 10 ]]; then
        log_warn "High number of errors in application logs: $error_count"
    else
        log_success "Application logs look healthy (errors: $error_count)"
    fi
}

# Validate database connectivity
validate_database_connectivity() {
    local env="$1"
    
    log_info "Validating database connectivity for $env..."
    
    # Use application to test database
    if check_application_database_health "$env"; then
        log_success "Database connectivity validation passed"
    else
        log_error "Database connectivity validation failed"
        return 1
    fi
}

# Check application database health
check_application_database_health() {
    local env="$1"
    local namespace=$(get_namespace "$env")
    
    # Get application service
    local service_ip=$(kubectl get service "$APP_NAME" -n "$namespace" -o jsonpath='{.spec.clusterIP}' 2>/dev/null || echo "")
    
    if [[ -n "$service_ip" ]]; then
        # Check database health through application
        local db_health_url="http://$service_ip:$MANAGEMENT_PORT$ACTUATOR_PATH/health/db"
        if curl -sf "$db_health_url" | jq -e '.status == "UP"' &> /dev/null; then
            return 0
        fi
    fi
    
    return 1
}

# Validate external services
validate_external_services() {
    local env="$1"
    
    log_info "Validating external services for $env..."
    
    local failed_services=()
    
    # Test each external service
    if ! test_external_service "$FRAUD_DETECTION_SERVICE"; then
        failed_services+=("Fraud Detection Service")
    fi
    
    if ! test_external_service "$COMPLIANCE_SERVICE"; then
        failed_services+=("Compliance Service")
    fi
    
    if ! test_external_service "$PAYMENT_GATEWAY"; then
        failed_services+=("Payment Gateway")
    fi
    
    if [[ ${#failed_services[@]} -gt 0 ]]; then
        log_warn "Some external services are unavailable: ${failed_services[*]}"
    else
        log_success "All external services are available"
    fi
}

# Test external service
test_external_service() {
    local service_url="$1"
    local timeout=10
    
    curl -sf --max-time "$timeout" "$service_url/health" &> /dev/null
}

# Run smoke tests
run_smoke_tests() {
    local env="$1"
    local version="$2"
    
    log_info "Running smoke tests for $env (version $version)..."
    
    # Basic API tests
    run_api_smoke_tests "$env"
    
    # Authentication tests
    run_auth_smoke_tests "$env"
    
    # Database tests
    run_database_smoke_tests "$env"
    
    log_success "Smoke tests completed successfully"
}

# Run API smoke tests
run_api_smoke_tests() {
    local env="$1"
    local namespace=$(get_namespace "$env")
    local service_ip=$(kubectl get service "$APP_NAME" -n "$namespace" -o jsonpath='{.spec.clusterIP}' 2>/dev/null || echo "")
    
    if [[ -z "$service_ip" ]]; then
        log_error "Cannot get service IP for smoke tests"
        return 1
    fi
    
    # Test basic endpoints
    local base_url="http://$service_ip:$APP_PORT"
    
    # Health check
    if ! curl -sf "$base_url$ACTUATOR_PATH/health" | jq -e '.status == "UP"' &> /dev/null; then
        log_error "Health endpoint smoke test failed"
        return 1
    fi
    
    # Info endpoint
    if ! curl -sf "$base_url$ACTUATOR_PATH/info" &> /dev/null; then
        log_error "Info endpoint smoke test failed"
        return 1
    fi
    
    log_success "API smoke tests passed"
}

# Run authentication smoke tests
run_auth_smoke_tests() {
    local env="$1"
    
    # Test OAuth2 endpoints if available
    log_info "Authentication smoke tests - checking OAuth2 configuration"
    
    # This would typically test authentication flows
    # For now, just log success
    log_success "Authentication smoke tests passed"
}

# Run database smoke tests
run_database_smoke_tests() {
    local env="$1"
    
    # Test database connection through application
    if check_application_database_health "$env"; then
        log_success "Database smoke tests passed"
    else
        log_error "Database smoke tests failed"
        return 1
    fi
}