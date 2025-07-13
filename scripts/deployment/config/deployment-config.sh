#!/bin/bash

# Deployment Configuration for Enterprise Banking Platform
# 
# Centralized configuration for all deployment environments and settings

# Environment-specific configurations
declare -A ENVIRONMENT_CONFIG

# Staging Environment
ENVIRONMENT_CONFIG[staging_database_url]="jdbc:postgresql://staging-db.banking.local:5432/banking_staging"
ENVIRONMENT_CONFIG[staging_redis_url]="redis://staging-redis.banking.local:6379"
ENVIRONMENT_CONFIG[staging_kafka_brokers]="staging-kafka-1.banking.local:9092,staging-kafka-2.banking.local:9092"
ENVIRONMENT_CONFIG[staging_namespace]="banking-staging"
ENVIRONMENT_CONFIG[staging_replicas]="2"
ENVIRONMENT_CONFIG[staging_cpu_limit]="2000m"
ENVIRONMENT_CONFIG[staging_memory_limit]="4Gi"
ENVIRONMENT_CONFIG[staging_load_balancer]="staging-lb.banking.local"

# Production Environment
ENVIRONMENT_CONFIG[production_database_url]="jdbc:postgresql://prod-db-cluster.banking.local:5432/banking_production"
ENVIRONMENT_CONFIG[production_redis_url]="redis://prod-redis-cluster.banking.local:6379"
ENVIRONMENT_CONFIG[production_kafka_brokers]="prod-kafka-1.banking.local:9092,prod-kafka-2.banking.local:9092,prod-kafka-3.banking.local:9092"
ENVIRONMENT_CONFIG[production_namespace]="banking-production"
ENVIRONMENT_CONFIG[production_replicas]="6"
ENVIRONMENT_CONFIG[production_cpu_limit]="4000m"
ENVIRONMENT_CONFIG[production_memory_limit]="8Gi"
ENVIRONMENT_CONFIG[production_load_balancer]="prod-lb.banking.local"

# Disaster Recovery Environment
ENVIRONMENT_CONFIG[dr_database_url]="jdbc:postgresql://dr-db-cluster.banking.local:5432/banking_dr"
ENVIRONMENT_CONFIG[dr_redis_url]="redis://dr-redis-cluster.banking.local:6379"
ENVIRONMENT_CONFIG[dr_kafka_brokers]="dr-kafka-1.banking.local:9092,dr-kafka-2.banking.local:9092"
ENVIRONMENT_CONFIG[dr_namespace]="banking-dr"
ENVIRONMENT_CONFIG[dr_replicas]="4"
ENVIRONMENT_CONFIG[dr_cpu_limit]="3000m"
ENVIRONMENT_CONFIG[dr_memory_limit]="6Gi"
ENVIRONMENT_CONFIG[dr_load_balancer]="dr-lb.banking.local"

# Application Configuration
APP_NAME="enterprise-banking-platform"
APP_PORT="8080"
MANAGEMENT_PORT="8081"
ACTUATOR_PATH="/actuator"

# Container Registry Configuration
CONTAINER_REGISTRY="registry.banking.local"
IMAGE_REPOSITORY="banking/enterprise-platform"
IMAGE_TAG_PREFIX="v"

# Kubernetes Configuration
KUBECTL_CONFIG_PATH="$HOME/.kube/config"
HELM_CHART_PATH="$PROJECT_ROOT/k8s/helm-charts/enterprise-loan-system"

# Database Configuration
DB_MIGRATION_TIMEOUT="600" # 10 minutes
DB_BACKUP_RETENTION_DAYS="30"
DB_CONNECTION_POOL_SIZE="20"

# Health Check Configuration
HEALTH_CHECK_TIMEOUT="300" # 5 minutes
HEALTH_CHECK_INTERVAL="10" # 10 seconds
HEALTH_CHECK_RETRIES="30"
READINESS_PROBE_DELAY="60" # 1 minute
LIVENESS_PROBE_DELAY="120" # 2 minutes

# Security Configuration
SECURITY_SCAN_TIMEOUT="1800" # 30 minutes
VULNERABILITY_THRESHOLD="HIGH" # CRITICAL, HIGH, MEDIUM, LOW
COMPLIANCE_CHECKS_ENABLED="true"
FAPI_COMPLIANCE_REQUIRED="true"

# Performance Thresholds
MAX_RESPONSE_TIME_MS="2000"
MIN_THROUGHPUT_TPS="1000"
MAX_ERROR_RATE_PERCENT="1"
MAX_CPU_UTILIZATION_PERCENT="80"
MAX_MEMORY_UTILIZATION_PERCENT="85"

# Deployment Strategy Configuration
BLUE_GREEN_TRAFFIC_SWITCH_INTERVAL="60" # seconds
ROLLING_BATCH_SIZE_PERCENT="25"
CANARY_INITIAL_TRAFFIC_PERCENT="5"
CANARY_MONITOR_DURATION="300" # 5 minutes

# Monitoring and Alerting
PROMETHEUS_ENDPOINT="http://prometheus.monitoring.banking.local:9090"
GRAFANA_ENDPOINT="http://grafana.monitoring.banking.local:3000"
ALERTMANAGER_ENDPOINT="http://alertmanager.monitoring.banking.local:9093"
JAEGER_ENDPOINT="http://jaeger.monitoring.banking.local:16686"

# Notification Configuration
SLACK_WEBHOOK_URL="${SLACK_WEBHOOK_URL:-}"
TEAMS_WEBHOOK_URL="${TEAMS_WEBHOOK_URL:-}"
EMAIL_NOTIFICATION_LIST="devops@banking.local,platform@banking.local"
SMS_NOTIFICATION_LIST="${SMS_NOTIFICATION_LIST:-}"

# Logging Configuration
CENTRALIZED_LOGGING_ENDPOINT="https://logs.banking.local/api/v1/logs"
LOG_RETENTION_DAYS="90"
LOG_LEVEL="INFO"

# Backup and Recovery Configuration
BACKUP_STORAGE_PATH="/backups/banking-platform"
BACKUP_RETENTION_VERSIONS="10"
RECOVERY_TIMEOUT="1800" # 30 minutes

# External Service Endpoints
FRAUD_DETECTION_SERVICE="https://fraud-api.banking.local"
COMPLIANCE_SERVICE="https://compliance-api.banking.local"
PAYMENT_GATEWAY="https://payment-gateway.banking.local"
CREDIT_BUREAU_API="https://credit-bureau.banking.local"

# Feature Flags
FEATURE_FRAUD_DETECTION_ML="true"
FEATURE_REAL_TIME_ANALYTICS="true"
FEATURE_ADVANCED_AUDIT_LOGGING="true"
FEATURE_PERFORMANCE_MONITORING="true"

# Compliance and Regulatory
PCI_COMPLIANCE_REQUIRED="true"
SOX_COMPLIANCE_REQUIRED="true"
GDPR_COMPLIANCE_REQUIRED="true"
BASEL_III_COMPLIANCE_REQUIRED="true"
AML_COMPLIANCE_REQUIRED="true"

# Development and Testing
AUTOMATED_TESTING_ENABLED="true"
PERFORMANCE_TESTING_ENABLED="true"
SECURITY_TESTING_ENABLED="true"
LOAD_TESTING_DURATION="600" # 10 minutes
LOAD_TESTING_CONCURRENCY="100"

# Maintenance Windows
MAINTENANCE_WINDOW_START="02:00"
MAINTENANCE_WINDOW_END="04:00"
MAINTENANCE_WINDOW_TIMEZONE="UTC"
MAINTENANCE_NOTIFICATION_ADVANCE_HOURS="24"

# API Rate Limiting
API_RATE_LIMIT_REQUESTS_PER_MINUTE="1000"
API_RATE_LIMIT_BURST_SIZE="100"
API_THROTTLING_ENABLED="true"

# Cache Configuration
REDIS_CACHE_TTL="3600" # 1 hour
REDIS_MAX_CONNECTIONS="100"
CACHE_WARMING_ENABLED="true"

# Message Queue Configuration
KAFKA_RETENTION_HOURS="168" # 7 days
KAFKA_PARTITION_COUNT="12"
KAFKA_REPLICATION_FACTOR="3"
KAFKA_COMPRESSION_TYPE="snappy"

# SSL/TLS Configuration
TLS_VERSION="1.3"
CERTIFICATE_VALIDATION_ENABLED="true"
MTLS_REQUIRED="true"

# Container Configuration
CONTAINER_RESTART_POLICY="Always"
CONTAINER_IMAGE_PULL_POLICY="IfNotPresent"
CONTAINER_SECURITY_CONTEXT_USER="1000"
CONTAINER_SECURITY_CONTEXT_GROUP="1000"
CONTAINER_READONLY_ROOT_FILESYSTEM="true"

# Resource Quotas
NAMESPACE_CPU_LIMIT="32000m"
NAMESPACE_MEMORY_LIMIT="64Gi"
NAMESPACE_STORAGE_LIMIT="500Gi"
NAMESPACE_POD_LIMIT="100"

# Networking
SERVICE_MESH_ENABLED="true"
ISTIO_ENABLED="true"
NETWORK_POLICIES_ENABLED="true"
INGRESS_CLASS="nginx"

# Secrets Management
SECRETS_MANAGER="kubernetes"
SECRET_ROTATION_ENABLED="true"
SECRET_ROTATION_INTERVAL_DAYS="90"

# Utility functions for configuration access
get_env_config() {
    local env="$1"
    local key="$2"
    local config_key="${env}_${key}"
    echo "${ENVIRONMENT_CONFIG[$config_key]:-}"
}

validate_environment() {
    local env="$1"
    case "$env" in
        staging|production|dr)
            return 0
            ;;
        *)
            return 1
            ;;
    esac
}

get_image_tag() {
    local version="$1"
    echo "${CONTAINER_REGISTRY}/${IMAGE_REPOSITORY}:${IMAGE_TAG_PREFIX}${version}"
}

get_namespace() {
    local env="$1"
    get_env_config "$env" "namespace"
}

get_replica_count() {
    local env="$1"
    get_env_config "$env" "replicas"
}

is_production_environment() {
    local env="$1"
    [[ "$env" == "production" ]]
}

is_feature_enabled() {
    local feature="$1"
    local feature_var="FEATURE_${feature^^}"
    [[ "${!feature_var:-false}" == "true" ]]
}

# Export configuration for use in other scripts
export APP_NAME APP_PORT MANAGEMENT_PORT
export CONTAINER_REGISTRY IMAGE_REPOSITORY IMAGE_TAG_PREFIX
export HEALTH_CHECK_TIMEOUT HEALTH_CHECK_INTERVAL HEALTH_CHECK_RETRIES
export MAX_RESPONSE_TIME_MS MIN_THROUGHPUT_TPS MAX_ERROR_RATE_PERCENT