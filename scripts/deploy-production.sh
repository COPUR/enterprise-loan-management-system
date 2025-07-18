#!/bin/bash

# Enterprise Banking Platform Production Deployment Script
# This script handles blue-green deployment with comprehensive health checks

set -euo pipefail

# Configuration
CLUSTER_NAME="banking-production-cluster"
NAMESPACE="banking"
REGION="us-east-1"
ECR_REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"
SERVICES=("customer-service" "loan-service" "payment-service")
DEPLOYMENT_TIMEOUT=600
HEALTH_CHECK_TIMEOUT=300

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Error handling
cleanup_on_error() {
    log_error "Deployment failed. Initiating rollback..."
    rollback_deployment
    exit 1
}

trap cleanup_on_error ERR

# Validate prerequisites
validate_prerequisites() {
    log_info "Validating prerequisites..."
    
    # Check required tools
    command -v kubectl >/dev/null 2>&1 || { log_error "kubectl is required but not installed"; exit 1; }
    command -v aws >/dev/null 2>&1 || { log_error "AWS CLI is required but not installed"; exit 1; }
    command -v jq >/dev/null 2>&1 || { log_error "jq is required but not installed"; exit 1; }
    
    # Check AWS credentials
    aws sts get-caller-identity >/dev/null 2>&1 || { log_error "AWS credentials not configured"; exit 1; }
    
    # Check Kubernetes context
    kubectl config current-context | grep -q "${CLUSTER_NAME}" || { 
        log_error "Not connected to production cluster"; 
        exit 1; 
    }
    
    # Check namespace
    kubectl get namespace "${NAMESPACE}" >/dev/null 2>&1 || {
        log_error "Namespace ${NAMESPACE} does not exist";
        exit 1;
    }
    
    log_success "Prerequisites validated"
}

# Update kubeconfig
update_kubeconfig() {
    log_info "Updating kubeconfig for cluster ${CLUSTER_NAME}..."
    aws eks update-kubeconfig --name "${CLUSTER_NAME}" --region "${REGION}"
    log_success "Kubeconfig updated"
}

# Pre-deployment checks
pre_deployment_checks() {
    log_info "Running pre-deployment checks..."
    
    # Check cluster health
    kubectl get nodes --no-headers | grep -v Ready && {
        log_error "Some nodes are not ready";
        exit 1;
    }
    
    # Check current deployment health
    for service in "${SERVICES[@]}"; do
        if kubectl get deployment "${service}-blue" -n "${NAMESPACE}" >/dev/null 2>&1; then
            replicas=$(kubectl get deployment "${service}-blue" -n "${NAMESPACE}" -o jsonpath='{.status.readyReplicas}')
            if [[ "${replicas}" -eq 0 ]]; then
                log_error "Current blue deployment for ${service} has no ready replicas"
                exit 1
            fi
        fi
    done
    
    # Check database connectivity
    log_info "Checking database connectivity..."
    kubectl run db-test --image=postgres:16 --rm -i --restart=Never -- \
        psql -h "${DB_HOST}" -U "${DB_USER}" -c "SELECT 1" >/dev/null 2>&1 || {
        log_error "Database connectivity check failed";
        exit 1;
    }
    
    # Check Redis connectivity
    log_info "Checking Redis connectivity..."
    kubectl run redis-test --image=redis:7-alpine --rm -i --restart=Never -- \
        redis-cli -h "${REDIS_HOST}" ping | grep -q PONG || {
        log_error "Redis connectivity check failed";
        exit 1;
    }
    
    log_success "Pre-deployment checks passed"
}

# Deploy green environment
deploy_green() {
    log_info "Deploying green environment..."
    
    for service in "${SERVICES[@]}"; do
        log_info "Deploying ${service} green..."
        
        # Create green deployment
        kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${service}-green
  namespace: ${NAMESPACE}
  labels:
    app: ${service}
    version: green
spec:
  replicas: 3
  selector:
    matchLabels:
      app: ${service}
      version: green
  template:
    metadata:
      labels:
        app: ${service}
        version: green
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      containers:
      - name: ${service}
        image: ${ECR_REGISTRY}/enterprise-banking/${service}:${IMAGE_TAG}
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: DB_HOST
          valueFrom:
            secretKeyRef:
              name: database-credentials
              key: host
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: database-credentials
              key: password
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
EOF
        
        # Wait for deployment to be ready
        log_info "Waiting for ${service} green deployment to be ready..."
        kubectl wait --for=condition=available --timeout=${DEPLOYMENT_TIMEOUT}s \
            deployment/${service}-green -n ${NAMESPACE}
        
        log_success "${service} green deployment ready"
    done
    
    log_success "Green environment deployed"
}

# Run health checks
run_health_checks() {
    log_info "Running health checks on green environment..."
    
    for service in "${SERVICES[@]}"; do
        log_info "Health checking ${service}..."
        
        # Get service port
        case $service in
            "customer-service") port=8080 ;;
            "loan-service") port=8081 ;;
            "payment-service") port=8082 ;;
        esac
        
        # Port forward to green pods
        kubectl port-forward "deployment/${service}-green" "${port}:8080" -n "${NAMESPACE}" &
        port_forward_pid=$!
        
        # Wait for port forward
        sleep 5
        
        # Run health check
        for i in {1..10}; do
            if curl -s "http://localhost:${port}/actuator/health" | jq -r '.status' | grep -q "UP"; then
                log_success "${service} health check passed"
                break
            fi
            if [[ $i -eq 10 ]]; then
                log_error "${service} health check failed"
                kill $port_forward_pid
                exit 1
            fi
            sleep 10
        done
        
        kill $port_forward_pid
    done
    
    log_success "All health checks passed"
}

# Run integration tests
run_integration_tests() {
    log_info "Running integration tests on green environment..."
    
    # Create test job
    kubectl apply -f - <<EOF
apiVersion: batch/v1
kind: Job
metadata:
  name: integration-tests-${BUILD_NUMBER}
  namespace: ${NAMESPACE}
spec:
  ttlSecondsAfterFinished: 3600
  template:
    spec:
      containers:
      - name: integration-tests
        image: ${ECR_REGISTRY}/enterprise-banking/integration-tests:${IMAGE_TAG}
        env:
        - name: TARGET_ENVIRONMENT
          value: "green"
        - name: BASE_URL
          value: "http://api-gateway.${NAMESPACE}.svc.cluster.local"
        command: ["/bin/bash", "-c"]
        args:
        - |
          echo "Running integration tests..."
          newman run /tests/Banking-API-Tests.postman_collection.json \
            -e /tests/Green-Environment.postman_environment.json \
            --reporters cli,junit \
            --reporter-junit-export /tmp/test-results.xml
          
          if [ $? -eq 0 ]; then
            echo "Integration tests passed"
          else
            echo "Integration tests failed"
            exit 1
          fi
      restartPolicy: Never
  backoffLimit: 1
EOF
    
    # Wait for job completion
    log_info "Waiting for integration tests to complete..."
    kubectl wait --for=condition=complete --timeout=${HEALTH_CHECK_TIMEOUT}s \
        job/integration-tests-${BUILD_NUMBER} -n ${NAMESPACE}
    
    # Check job status
    job_status=$(kubectl get job integration-tests-${BUILD_NUMBER} -n ${NAMESPACE} -o jsonpath='{.status.conditions[0].type}')
    if [[ "$job_status" != "Complete" ]]; then
        log_error "Integration tests failed"
        kubectl logs job/integration-tests-${BUILD_NUMBER} -n ${NAMESPACE}
        exit 1
    fi
    
    log_success "Integration tests passed"
}

# Switch traffic to green
switch_to_green() {
    log_info "Switching traffic to green environment..."
    
    for service in "${SERVICES[@]}"; do
        log_info "Switching ${service} traffic to green..."
        
        # Update service selector
        kubectl patch service "${service}" -n "${NAMESPACE}" \
            -p '{"spec":{"selector":{"version":"green"}}}'
        
        # Verify traffic switch
        sleep 5
        endpoints=$(kubectl get endpoints "${service}" -n "${NAMESPACE}" -o jsonpath='{.subsets[0].addresses[*].ip}')
        if [[ -z "$endpoints" ]]; then
            log_error "No endpoints found for ${service} after traffic switch"
            exit 1
        fi
        
        log_success "${service} traffic switched to green"
    done
    
    log_success "Traffic switched to green environment"
}

# Run smoke tests
run_smoke_tests() {
    log_info "Running smoke tests on production traffic..."
    
    # Wait for traffic to stabilize
    sleep 30
    
    # Run smoke tests
    for service in "${SERVICES[@]}"; do
        case $service in
            "customer-service") endpoint="/api/v1/customers/health" ;;
            "loan-service") endpoint="/api/v1/loans/health" ;;
            "payment-service") endpoint="/api/v1/payments/health" ;;
        esac
        
        for i in {1..5}; do
            if curl -s -f "https://banking.example.com${endpoint}" >/dev/null; then
                log_success "${service} smoke test passed"
                break
            fi
            if [[ $i -eq 5 ]]; then
                log_error "${service} smoke test failed"
                exit 1
            fi
            sleep 10
        done
    done
    
    log_success "Smoke tests passed"
}

# Scale down blue environment
scale_down_blue() {
    log_info "Scaling down blue environment..."
    
    for service in "${SERVICES[@]}"; do
        if kubectl get deployment "${service}-blue" -n "${NAMESPACE}" >/dev/null 2>&1; then
            log_info "Scaling down ${service} blue..."
            kubectl scale deployment "${service}-blue" --replicas=0 -n "${NAMESPACE}"
            log_success "${service} blue scaled down"
        fi
    done
    
    log_success "Blue environment scaled down"
}

# Database migration
run_database_migration() {
    log_info "Running database migration..."
    
    # Create migration job
    kubectl apply -f - <<EOF
apiVersion: batch/v1
kind: Job
metadata:
  name: db-migration-${BUILD_NUMBER}
  namespace: ${NAMESPACE}
spec:
  ttlSecondsAfterFinished: 3600
  template:
    spec:
      containers:
      - name: db-migration
        image: ${ECR_REGISTRY}/enterprise-banking/db-migration:${IMAGE_TAG}
        env:
        - name: DB_HOST
          valueFrom:
            secretKeyRef:
              name: database-credentials
              key: host
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: database-credentials
              key: password
        command: ["/bin/bash", "-c"]
        args:
        - |
          echo "Running database migration..."
          flyway -url=jdbc:postgresql://${DB_HOST}:5432/banking \
                 -user=${DB_USER} \
                 -password=${DB_PASSWORD} \
                 -locations=filesystem:/migrations \
                 migrate
          
          if [ $? -eq 0 ]; then
            echo "Database migration completed successfully"
          else
            echo "Database migration failed"
            exit 1
          fi
      restartPolicy: Never
  backoffLimit: 1
EOF
    
    # Wait for migration completion
    kubectl wait --for=condition=complete --timeout=${HEALTH_CHECK_TIMEOUT}s \
        job/db-migration-${BUILD_NUMBER} -n ${NAMESPACE}
    
    # Check migration status
    job_status=$(kubectl get job db-migration-${BUILD_NUMBER} -n ${NAMESPACE} -o jsonpath='{.status.conditions[0].type}')
    if [[ "$job_status" != "Complete" ]]; then
        log_error "Database migration failed"
        kubectl logs job/db-migration-${BUILD_NUMBER} -n ${NAMESPACE}
        exit 1
    fi
    
    log_success "Database migration completed"
}

# Rollback function
rollback_deployment() {
    log_warning "Initiating rollback to blue environment..."
    
    # Scale up blue deployment
    for service in "${SERVICES[@]}"; do
        if kubectl get deployment "${service}-blue" -n "${NAMESPACE}" >/dev/null 2>&1; then
            log_info "Scaling up ${service} blue..."
            kubectl scale deployment "${service}-blue" --replicas=3 -n "${NAMESPACE}"
            kubectl wait --for=condition=available --timeout=${DEPLOYMENT_TIMEOUT}s \
                deployment/${service}-blue -n ${NAMESPACE}
        fi
    done
    
    # Switch traffic back to blue
    for service in "${SERVICES[@]}"; do
        kubectl patch service "${service}" -n "${NAMESPACE}" \
            -p '{"spec":{"selector":{"version":"blue"}}}'
    done
    
    # Remove green deployment
    for service in "${SERVICES[@]}"; do
        if kubectl get deployment "${service}-green" -n "${NAMESPACE}" >/dev/null 2>&1; then
            kubectl delete deployment "${service}-green" -n "${NAMESPACE}"
        fi
    done
    
    log_success "Rollback completed"
}

# Send notifications
send_notifications() {
    local status=$1
    local message=$2
    
    # Slack notification
    if [[ -n "${SLACK_WEBHOOK_URL:-}" ]]; then
        curl -X POST -H 'Content-type: application/json' \
            --data "{\"text\":\"🏦 Banking Platform Deployment ${status}: ${message}\"}" \
            "${SLACK_WEBHOOK_URL}"
    fi
    
    # Email notification
    if [[ -n "${EMAIL_NOTIFICATION_ENDPOINT:-}" ]]; then
        curl -X POST -H 'Content-type: application/json' \
            --data "{\"subject\":\"Banking Platform Deployment ${status}\",\"body\":\"${message}\"}" \
            "${EMAIL_NOTIFICATION_ENDPOINT}"
    fi
    
    # PagerDuty (for failures)
    if [[ "$status" == "FAILED" && -n "${PAGERDUTY_API_KEY:-}" ]]; then
        curl -X POST "https://events.pagerduty.com/v2/enqueue" \
            -H "Authorization: Token token=${PAGERDUTY_API_KEY}" \
            -H "Content-Type: application/json" \
            -d "{
                \"routing_key\": \"${PAGERDUTY_ROUTING_KEY}\",
                \"event_action\": \"trigger\",
                \"payload\": {
                    \"summary\": \"Banking Platform Deployment Failed\",
                    \"source\": \"deployment-script\",
                    \"severity\": \"critical\",
                    \"custom_details\": {
                        \"message\": \"${message}\",
                        \"build_number\": \"${BUILD_NUMBER}\",
                        \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"
                    }
                }
            }"
    fi
}

# Main deployment function
main() {
    log_info "Starting production deployment for build ${BUILD_NUMBER}"
    
    # Validate inputs
    if [[ -z "${IMAGE_TAG:-}" ]]; then
        log_error "IMAGE_TAG environment variable is required"
        exit 1
    fi
    
    if [[ -z "${BUILD_NUMBER:-}" ]]; then
        log_error "BUILD_NUMBER environment variable is required"
        exit 1
    fi
    
    # Run deployment steps
    validate_prerequisites
    update_kubeconfig
    pre_deployment_checks
    
    # Database migration (if needed)
    if [[ "${RUN_DB_MIGRATION:-false}" == "true" ]]; then
        run_database_migration
    fi
    
    deploy_green
    run_health_checks
    run_integration_tests
    switch_to_green
    run_smoke_tests
    scale_down_blue
    
    # Success notification
    send_notifications "SUCCESS" "Deployment ${BUILD_NUMBER} completed successfully"
    
    log_success "Production deployment completed successfully!"
    log_info "Build: ${BUILD_NUMBER}"
    log_info "Image Tag: ${IMAGE_TAG}"
    log_info "Deployment Time: $(date)"
}

# Execute main function
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi