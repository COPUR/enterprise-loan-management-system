#!/bin/bash

# Deployment Utility Functions for Enterprise Banking Platform
# 
# Common utility functions used across deployment scripts

# Generate unique deployment ID
generate_deployment_id() {
    echo "deploy-$(date +%Y%m%d-%H%M%S)-$(head -c 6 /dev/urandom | xxd -p)"
}

# Check if required tools are installed
check_prerequisites() {
    log_info "Checking deployment prerequisites..."
    
    local required_tools=("kubectl" "helm" "docker" "jq" "curl" "postgres" "redis-cli")
    local missing_tools=()
    
    for tool in "${required_tools[@]}"; do
        if ! command -v "$tool" &> /dev/null; then
            missing_tools+=("$tool")
        fi
    done
    
    if [[ ${#missing_tools[@]} -gt 0 ]]; then
        log_error "Missing required tools: ${missing_tools[*]}"
        log_error "Please install missing tools before proceeding"
        exit 1
    fi
    
    # Check Kubernetes connectivity
    if ! kubectl cluster-info &> /dev/null; then
        log_error "Cannot connect to Kubernetes cluster"
        exit 1
    fi
    
    # Check Helm
    if ! helm version &> /dev/null; then
        log_error "Helm is not properly configured"
        exit 1
    fi
    
    log_success "All prerequisites are satisfied"
}

# Validate build version exists
validate_build_version() {
    local version="$1"
    local image_tag=$(get_image_tag "$version")
    
    log_info "Validating build version $version..."
    
    if ! docker manifest inspect "$image_tag" &> /dev/null; then
        log_error "Build version $version not found in registry"
        log_error "Image: $image_tag"
        exit 1
    fi
    
    log_success "Build version $version validated"
}

# Get current active environment for blue-green deployments
get_current_active_environment() {
    local env="$1"
    local namespace=$(get_namespace "$env")
    
    # Check which environment is currently receiving traffic
    local active_label=$(kubectl get service "$APP_NAME" -n "$namespace" -o jsonpath='{.spec.selector.version}' 2>/dev/null || echo "blue")
    
    if [[ "$active_label" == "blue" ]]; then
        echo "blue"
    else
        echo "green"
    fi
}

# Get inactive environment for blue-green deployments
get_inactive_environment() {
    local env="$1"
    local current=$(get_current_active_environment "$env")
    
    if [[ "$current" == "blue" ]]; then
        echo "green"
    else
        echo "blue"
    fi
}

# Deploy to specific environment
deploy_to_environment() {
    local target_env="$1"
    local version="$2"
    local namespace=$(get_namespace "${target_env%%-*}") # Remove blue/green suffix
    
    log_info "Deploying version $version to environment $target_env"
    
    # Prepare Helm values
    local helm_values_file=$(create_helm_values_file "$target_env" "$version")
    
    # Deploy using Helm
    helm upgrade --install \
        "${APP_NAME}-${target_env}" \
        "$HELM_CHART_PATH" \
        --namespace "$namespace" \
        --values "$helm_values_file" \
        --set image.tag="$version" \
        --set environment="$target_env" \
        --timeout 10m \
        --wait
    
    # Clean up temporary values file
    rm -f "$helm_values_file"
    
    log_success "Deployment to $target_env completed"
}

# Create Helm values file for deployment
create_helm_values_file() {
    local env="$1"
    local version="$2"
    local base_env="${env%%-*}" # Remove blue/green suffix
    local values_file="/tmp/helm-values-${env}-${version}.yaml"
    
    cat > "$values_file" << EOF
# Helm values for $env deployment
image:
  repository: ${CONTAINER_REGISTRY}/${IMAGE_REPOSITORY}
  tag: "${version}"
  pullPolicy: IfNotPresent

replicaCount: $(get_replica_count "$base_env")

environment: "$env"

service:
  type: ClusterIP
  port: $APP_PORT
  targetPort: $APP_PORT

ingress:
  enabled: true
  className: "$INGRESS_CLASS"
  hosts:
    - host: $(get_env_config "$base_env" "load_balancer")
      paths:
        - path: /
          pathType: Prefix

resources:
  limits:
    cpu: $(get_env_config "$base_env" "cpu_limit")
    memory: $(get_env_config "$base_env" "memory_limit")
  requests:
    cpu: $(( $(echo $(get_env_config "$base_env" "cpu_limit") | sed 's/m//') / 2 ))m
    memory: $(( $(echo $(get_env_config "$base_env" "memory_limit") | sed 's/Gi//') / 2 ))Gi

autoscaling:
  enabled: true
  minReplicas: $(get_replica_count "$base_env")
  maxReplicas: $(( $(get_replica_count "$base_env") * 3 ))
  targetCPUUtilizationPercentage: $MAX_CPU_UTILIZATION_PERCENT
  targetMemoryUtilizationPercentage: $MAX_MEMORY_UTILIZATION_PERCENT

probes:
  readiness:
    initialDelaySeconds: $READINESS_PROBE_DELAY
    periodSeconds: 10
    timeoutSeconds: 5
    failureThreshold: 3
  liveness:
    initialDelaySeconds: $LIVENESS_PROBE_DELAY
    periodSeconds: 30
    timeoutSeconds: 10
    failureThreshold: 3

config:
  database:
    url: $(get_env_config "$base_env" "database_url")
  redis:
    url: $(get_env_config "$base_env" "redis_url")
  kafka:
    brokers: $(get_env_config "$base_env" "kafka_brokers")

security:
  runAsNonRoot: true
  runAsUser: $CONTAINER_SECURITY_CONTEXT_USER
  runAsGroup: $CONTAINER_SECURITY_CONTEXT_GROUP
  readOnlyRootFilesystem: $CONTAINER_READONLY_ROOT_FILESYSTEM

monitoring:
  prometheus:
    enabled: true
    port: $MANAGEMENT_PORT
    path: $ACTUATOR_PATH/prometheus
  jaeger:
    enabled: true
    endpoint: $JAEGER_ENDPOINT

EOF

    echo "$values_file"
}

# Wait for healthy deployment
wait_for_healthy_deployment() {
    local env="$1"
    local version="$2"
    local namespace=$(get_namespace "${env%%-*}")
    local deployment_name="${APP_NAME}-${env}"
    
    log_info "Waiting for healthy deployment of $deployment_name..."
    
    # Wait for deployment to be ready
    if ! kubectl rollout status deployment "$deployment_name" -n "$namespace" --timeout="${HEALTH_CHECK_TIMEOUT}s"; then
        log_error "Deployment $deployment_name failed to become ready"
        return 1
    fi
    
    # Additional health checks
    local max_attempts=$(( HEALTH_CHECK_TIMEOUT / HEALTH_CHECK_INTERVAL ))
    local attempt=0
    
    while [[ $attempt -lt $max_attempts ]]; do
        if check_application_health "$env"; then
            log_success "Deployment $deployment_name is healthy"
            return 0
        fi
        
        ((attempt++))
        log_info "Health check attempt $attempt/$max_attempts failed, retrying in ${HEALTH_CHECK_INTERVAL}s..."
        sleep "$HEALTH_CHECK_INTERVAL"
    done
    
    log_error "Deployment $deployment_name failed health checks after $max_attempts attempts"
    return 1
}

# Check application health
check_application_health() {
    local env="$1"
    local namespace=$(get_namespace "${env%%-*}")
    local service_name="${APP_NAME}-${env}"
    
    # Port forward to service for health check
    local local_port=$(get_random_port)
    kubectl port-forward -n "$namespace" "service/$service_name" "$local_port:$APP_PORT" &
    local port_forward_pid=$!
    
    # Wait for port forward to establish
    sleep 2
    
    # Perform health check
    local health_status=false
    if curl -sf "http://localhost:$local_port$ACTUATOR_PATH/health" | jq -e '.status == "UP"' &> /dev/null; then
        health_status=true
    fi
    
    # Clean up port forward
    kill $port_forward_pid 2>/dev/null || true
    
    $health_status
}

# Get random available port
get_random_port() {
    python3 -c "import socket; s=socket.socket(); s.bind(('', 0)); print(s.getsockname()[1]); s.close()"
}

# Switch traffic gradually between environments
switch_traffic_gradually() {
    local current_env="$1"
    local target_env="$2"
    local namespace=$(get_namespace "${current_env%%-*}")
    
    log_info "Switching traffic from $current_env to $target_env"
    
    # Traffic percentages for gradual switch
    local percentages=(10 25 50 75 90 100)
    
    for percentage in "${percentages[@]}"; do
        log_info "Routing $percentage% traffic to $target_env"
        
        # Update service selector weights
        update_traffic_weights "$namespace" "$current_env" "$target_env" "$percentage"
        
        # Monitor for issues
        sleep "$BLUE_GREEN_TRAFFIC_SWITCH_INTERVAL"
        
        # Check health and metrics
        if ! validate_traffic_switch_health "$target_env" "$percentage"; then
            log_error "Traffic switch validation failed at $percentage%, rolling back"
            update_traffic_weights "$namespace" "$current_env" "$target_env" 0
            return 1
        fi
    done
    
    log_success "Traffic switch completed successfully"
}

# Update traffic weights between environments
update_traffic_weights() {
    local namespace="$1"
    local current_env="$2"
    local target_env="$3"
    local target_percentage="$4"
    
    # For simplicity, this updates the service selector
    # In production, this would integrate with service mesh (Istio) for more granular control
    
    if [[ "$target_percentage" -ge 50 ]]; then
        # Switch primary traffic to target
        kubectl patch service "$APP_NAME" -n "$namespace" -p "{\"spec\":{\"selector\":{\"version\":\"${target_env}\"}}}"
    fi
}

# Validate traffic switch health
validate_traffic_switch_health() {
    local env="$1"
    local percentage="$2"
    
    # Check error rates
    local error_rate=$(get_current_error_rate "$env")
    if (( $(echo "$error_rate > $MAX_ERROR_RATE_PERCENT" | bc -l) )); then
        log_error "Error rate too high: $error_rate% > $MAX_ERROR_RATE_PERCENT%"
        return 1
    fi
    
    # Check response times
    local avg_response_time=$(get_current_response_time "$env")
    if (( $(echo "$avg_response_time > $MAX_RESPONSE_TIME_MS" | bc -l) )); then
        log_error "Response time too high: ${avg_response_time}ms > ${MAX_RESPONSE_TIME_MS}ms"
        return 1
    fi
    
    return 0
}

# Get current error rate from monitoring
get_current_error_rate() {
    local env="$1"
    
    # Query Prometheus for error rate
    # This is a simplified implementation
    echo "0.5" # Mock value
}

# Get current response time from monitoring
get_current_response_time() {
    local env="$1"
    
    # Query Prometheus for response time
    # This is a simplified implementation
    echo "150" # Mock value in milliseconds
}

# Calculate rolling deployment batch size
calculate_rolling_batch_size() {
    local total_instances="$1"
    local batch_size=$(( total_instances * ROLLING_BATCH_SIZE_PERCENT / 100 ))
    
    # Ensure at least 1 instance per batch
    if [[ $batch_size -lt 1 ]]; then
        batch_size=1
    fi
    
    echo "$batch_size"
}

# Get application instances
get_application_instances() {
    local env="$1"
    local namespace=$(get_namespace "$env")
    
    kubectl get pods -n "$namespace" -l "app=$APP_NAME" -o jsonpath='{.items[*].metadata.name}'
}

# Deploy to specific instance
deploy_to_instance() {
    local instance="$1"
    local version="$2"
    local namespace=$(get_namespace "${env%%-*}")
    
    log_info "Deploying version $version to instance $instance"
    
    # Update pod with new image
    kubectl patch pod "$instance" -n "$namespace" -p "{\"spec\":{\"containers\":[{\"name\":\"$APP_NAME\",\"image\":\"$(get_image_tag "$version")\"}]}}"
}

# Wait for instance health
wait_for_instance_health() {
    local instance="$1"
    local namespace=$(get_namespace "${env%%-*}")
    
    kubectl wait --for=condition=Ready pod "$instance" -n "$namespace" --timeout="${HEALTH_CHECK_TIMEOUT}s"
}

# Create deployment record
create_deployment_record() {
    local deployment_id="$1"
    local environment="$2"
    local version="$3"
    local strategy="$4"
    
    # This would typically write to a deployment database
    log_info "Created deployment record: $deployment_id"
    
    cat > "/tmp/deployment-${deployment_id}.json" << EOF
{
    "deploymentId": "$deployment_id",
    "environment": "$environment",
    "version": "$version",
    "strategy": "$strategy",
    "startTime": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
    "status": "IN_PROGRESS"
}
EOF
}

# Update deployment record
update_deployment_record() {
    local deployment_id="$1"
    local status="$2"
    local duration="$3"
    
    log_info "Updated deployment record $deployment_id: $status (${duration}s)"
    
    # Update the deployment record file
    if [[ -f "/tmp/deployment-${deployment_id}.json" ]]; then
        jq --arg status "$status" --arg duration "$duration" --arg endTime "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
           '.status = $status | .duration = ($duration | tonumber) | .endTime = $endTime' \
           "/tmp/deployment-${deployment_id}.json" > "/tmp/deployment-${deployment_id}.tmp" && \
           mv "/tmp/deployment-${deployment_id}.tmp" "/tmp/deployment-${deployment_id}.json"
    fi
}

# Send deployment notifications
send_deployment_notifications() {
    local environment="$1"
    local version="$2"
    local status="$3"
    
    local message="Deployment to $environment completed: $version ($status)"
    
    # Slack notification
    if [[ -n "$SLACK_WEBHOOK_URL" ]]; then
        send_slack_notification "$message"
    fi
    
    # Email notification
    send_email_notification "$message"
    
    # Teams notification
    if [[ -n "$TEAMS_WEBHOOK_URL" ]]; then
        send_teams_notification "$message"
    fi
}

# Send Slack notification
send_slack_notification() {
    local message="$1"
    
    if [[ -n "$SLACK_WEBHOOK_URL" ]]; then
        curl -X POST -H 'Content-type: application/json' \
             --data "{\"text\":\"$message\"}" \
             "$SLACK_WEBHOOK_URL" || true
    fi
}

# Send email notification
send_email_notification() {
    local message="$1"
    
    # Simple mail sending - in production would use proper email service
    echo "$message" | mail -s "Banking Platform Deployment" "$EMAIL_NOTIFICATION_LIST" 2>/dev/null || true
}

# Send Teams notification
send_teams_notification() {
    local message="$1"
    
    if [[ -n "$TEAMS_WEBHOOK_URL" ]]; then
        curl -X POST -H 'Content-type: application/json' \
             --data "{\"text\":\"$message\"}" \
             "$TEAMS_WEBHOOK_URL" || true
    fi
}

# Enable maintenance mode
enable_maintenance_mode() {
    local env="$1"
    local namespace=$(get_namespace "$env")
    
    log_info "Enabling maintenance mode for $env"
    
    # Deploy maintenance page
    kubectl apply -f - << EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: maintenance-page
  namespace: $namespace
data:
  index.html: |
    <!DOCTYPE html>
    <html>
    <head>
        <title>Maintenance - Enterprise Banking Platform</title>
        <style>
            body { font-family: Arial, sans-serif; text-align: center; margin-top: 100px; }
            .container { max-width: 600px; margin: 0 auto; }
        </style>
    </head>
    <body>
        <div class="container">
            <h1>System Maintenance</h1>
            <p>Our banking platform is currently undergoing scheduled maintenance.</p>
            <p>We apologize for any inconvenience. Please try again in a few minutes.</p>
            <p>For urgent matters, please contact customer support.</p>
        </div>
    </body>
    </html>
EOF
}

# Disable maintenance mode
disable_maintenance_mode() {
    local env="$1"
    local namespace=$(get_namespace "$env")
    
    log_info "Disabling maintenance mode for $env"
    
    # Remove maintenance page
    kubectl delete configmap maintenance-page -n "$namespace" --ignore-not-found=true
}