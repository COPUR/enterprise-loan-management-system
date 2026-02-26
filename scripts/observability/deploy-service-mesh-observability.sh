#!/bin/bash

# Deploy Service Mesh Observability Infrastructure
# Distributed Tracing, Metrics Collection, and Logging for Enterprise Banking
# Jaeger, Prometheus, Grafana, Loki, Fluentd, OpenTelemetry Integration

set -euo pipefail

# Script Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"
K8S_DIR="$PROJECT_ROOT/k8s"
OBSERVABILITY_DIR="$K8S_DIR/observability"

# Default Values
DEFAULT_NAMESPACE="observability"
DEFAULT_BANKING_NAMESPACE="banking-system"
DEFAULT_ENVIRONMENT="production"
DEFAULT_DRY_RUN="true"

# Command Line Arguments
NAMESPACE="${1:-$DEFAULT_NAMESPACE}"
BANKING_NAMESPACE="${2:-$DEFAULT_BANKING_NAMESPACE}"
ENVIRONMENT="${3:-$DEFAULT_ENVIRONMENT}"
DRY_RUN="${4:-$DEFAULT_DRY_RUN}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Logging functions
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

warn() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARNING: $1${NC}"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR: $1${NC}"
    exit 1
}

info() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')] INFO: $1${NC}"
}

success() {
    echo -e "${CYAN}[$(date +'%Y-%m-%d %H:%M:%S')] SUCCESS: $1${NC}"
}

# Print banner
print_banner() {
    echo -e "${PURPLE}"
    echo "╔══════════════════════════════════════════════════════════════════════════════╗"
    echo "║            Service Mesh Observability Deployment                            ║"
    echo "║        Distributed Tracing, Metrics, and Logging Infrastructure             ║"
    echo "║                                                                              ║"
    echo "║  Observability Namespace: $NAMESPACE"
    echo "║  Banking Namespace:       $BANKING_NAMESPACE"
    echo "║  Environment:             $ENVIRONMENT"
    echo "║  Dry Run:                 $DRY_RUN"
    echo "║                                                                              ║"
    echo "║  Components: Jaeger, Prometheus, Grafana, Loki, Fluentd, OpenTelemetry     ║"
    echo "╚══════════════════════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

# Validate prerequisites
validate_prerequisites() {
    log "Validating deployment prerequisites..."
    
    # Check if required tools are installed
    local required_tools=("kubectl" "helm" "jq")
    
    for tool in "${required_tools[@]}"; do
        if ! command -v "$tool" &> /dev/null; then
            error "$tool is not installed or not in PATH"
        fi
    done
    
    # Check kubectl access
    if ! kubectl cluster-info &> /dev/null; then
        error "kubectl cannot access Kubernetes cluster"
    fi
    
    # Check if Istio is installed
    if ! kubectl get namespace istio-system &> /dev/null; then
        error "Istio is not installed. Service mesh observability requires Istio."
    fi
    
    # Check if required CRDs exist
    local required_crds=(
        "servicemonitors.monitoring.coreos.com"
        "prometheusrules.monitoring.coreos.com"
        "jaegers.jaegertracing.io"
    )
    
    for crd in "${required_crds[@]}"; do
        if ! kubectl get crd "$crd" &> /dev/null 2>&1; then
            warn "CRD $crd not found. Will attempt to install required operators."
        fi
    done
    
    # Check if required files exist
    if [[ ! -f "$OBSERVABILITY_DIR/service-mesh-observability.yaml" ]]; then
        error "Required file not found: $OBSERVABILITY_DIR/service-mesh-observability.yaml"
    fi
    
    success "Prerequisites validation completed"
}

# Install required operators
install_required_operators() {
    log "Installing required operators..."
    
    # Add Helm repositories
    if [[ "$DRY_RUN" == "false" ]]; then
        helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
        helm repo add grafana https://grafana.github.io/helm-charts
        helm repo add jaegertracing https://jaegertracing.github.io/helm-charts
        helm repo add open-telemetry https://open-telemetry.github.io/opentelemetry-helm-charts
        helm repo update
        info "Helm repositories added and updated"
    else
        info "Would add and update Helm repositories"
    fi
    
    # Install Prometheus Operator
    if ! kubectl get deployment prometheus-operator -n "$NAMESPACE" &> /dev/null; then
        if [[ "$DRY_RUN" == "false" ]]; then
            helm upgrade --install prometheus-operator prometheus-community/kube-prometheus-stack \
                --namespace "$NAMESPACE" \
                --create-namespace \
                --set prometheus.prometheusSpec.retention=30d \
                --set prometheus.prometheusSpec.storageSpec.volumeClaimTemplate.spec.storageClassName=fast-ssd \
                --set prometheus.prometheusSpec.storageSpec.volumeClaimTemplate.spec.accessModes[0]=ReadWriteOnce \
                --set prometheus.prometheusSpec.storageSpec.volumeClaimTemplate.spec.resources.requests.storage=100Gi \
                --set grafana.enabled=true \
                --set grafana.adminPassword=admin123 \
                --set alertmanager.enabled=true \
                --wait --timeout=600s
            info "Prometheus Operator installed"
        else
            info "Would install Prometheus Operator"
        fi
    else
        info "Prometheus Operator already installed"
    fi
    
    # Install Jaeger Operator
    if ! kubectl get deployment jaeger-operator -n "$NAMESPACE" &> /dev/null; then
        if [[ "$DRY_RUN" == "false" ]]; then
            kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -
            kubectl apply -f https://github.com/jaegertracing/jaeger-operator/releases/download/v1.41.0/jaeger-operator.yaml -n "$NAMESPACE"
            
            # Wait for operator to be ready
            kubectl wait --for=condition=available --timeout=300s deployment/jaeger-operator -n "$NAMESPACE"
            info "Jaeger Operator installed"
        else
            info "Would install Jaeger Operator"
        fi
    else
        info "Jaeger Operator already installed"
    fi
    
    # Install Loki
    if ! kubectl get deployment loki -n "$NAMESPACE" &> /dev/null; then
        if [[ "$DRY_RUN" == "false" ]]; then
            helm upgrade --install loki grafana/loki-stack \
                --namespace "$NAMESPACE" \
                --set loki.persistence.enabled=true \
                --set loki.persistence.storageClassName=fast-ssd \
                --set loki.persistence.size=50Gi \
                --set promtail.enabled=true \
                --set fluent-bit.enabled=false \
                --set grafana.enabled=false \
                --wait --timeout=300s
            info "Loki installed"
        else
            info "Would install Loki"
        fi
    else
        info "Loki already installed"
    fi
    
    success "Required operators installation completed"
}

# Setup namespaces and RBAC
setup_namespaces_and_rbac() {
    log "Setting up namespaces and RBAC..."
    
    # Create observability namespace
    if ! kubectl get namespace "$NAMESPACE" &> /dev/null; then
        if [[ "$DRY_RUN" == "false" ]]; then
            kubectl create namespace "$NAMESPACE"
            info "Created namespace: $NAMESPACE"
        else
            info "Would create namespace: $NAMESPACE"
        fi
    else
        info "Namespace $NAMESPACE already exists"
    fi
    
    # Label namespaces for observability
    local namespaces=("$NAMESPACE" "$BANKING_NAMESPACE" "istio-system")
    local observability_labels=(
        "observability.banking/enabled=true"
        "monitoring.banking/level=comprehensive"
        "tracing.banking/enabled=true"
        "logging.banking/level=debug"
    )
    
    for ns in "${namespaces[@]}"; do
        if kubectl get namespace "$ns" &> /dev/null; then
            for label in "${observability_labels[@]}"; do
                if [[ "$DRY_RUN" == "false" ]]; then
                    kubectl label namespace "$ns" "$label" --overwrite=true
                else
                    info "Would apply label to namespace $ns: $label"
                fi
            done
        fi
    done
    
    # Create service account for observability components
    if [[ "$DRY_RUN" == "false" ]]; then
        cat << EOF | kubectl apply -f -
apiVersion: v1
kind: ServiceAccount
metadata:
  name: observability-sa
  namespace: $NAMESPACE
  labels:
    app: observability
    component: service-account
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: observability-reader
  labels:
    app: observability
    component: rbac
rules:
- apiGroups: [""]
  resources: ["pods", "services", "endpoints", "nodes", "namespaces"]
  verbs: ["get", "list", "watch"]
- apiGroups: ["apps"]
  resources: ["deployments", "replicasets"]
  verbs: ["get", "list", "watch"]
- apiGroups: ["extensions"]
  resources: ["ingresses"]
  verbs: ["get", "list", "watch"]
- apiGroups: ["networking.istio.io"]
  resources: ["*"]
  verbs: ["get", "list", "watch"]
- apiGroups: ["security.istio.io"]
  resources: ["*"]
  verbs: ["get", "list", "watch"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: observability-reader-binding
  labels:
    app: observability
    component: rbac
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: observability-reader
subjects:
- kind: ServiceAccount
  name: observability-sa
  namespace: $NAMESPACE
EOF
        info "Created observability service account and RBAC"
    else
        info "Would create observability service account and RBAC"
    fi
    
    success "Namespaces and RBAC setup completed"
}

# Deploy observability infrastructure
deploy_observability_infrastructure() {
    log "Deploying observability infrastructure..."
    
    # Apply main observability configuration
    if [[ "$DRY_RUN" == "false" ]]; then
        kubectl apply -f "$OBSERVABILITY_DIR/service-mesh-observability.yaml"
        info "Applied observability infrastructure configuration"
    else
        info "Would apply observability infrastructure from: $OBSERVABILITY_DIR/service-mesh-observability.yaml"
    fi
    
    # Wait for components to be ready
    if [[ "$DRY_RUN" == "false" ]]; then
        log "Waiting for observability components to be ready..."
        
        # Wait for Jaeger
        local max_wait=300
        local wait_count=0
        
        while [[ $wait_count -lt $max_wait ]]; do
            if kubectl get jaeger banking-jaeger -n "$NAMESPACE" &> /dev/null; then
                if kubectl wait --for=condition=ready --timeout=60s jaeger/banking-jaeger -n "$NAMESPACE" 2>/dev/null; then
                    success "Jaeger is ready"
                    break
                fi
            fi
            
            sleep 10
            ((wait_count+=10))
            info "Waiting for Jaeger to be ready... (${wait_count}s/${max_wait}s)"
        done
        
        # Wait for ServiceMonitors
        if kubectl get servicemonitor banking-services-metrics -n "$NAMESPACE" &> /dev/null; then
            success "ServiceMonitors are active"
        else
            warn "ServiceMonitors not found"
        fi
    fi
    
    success "Observability infrastructure deployed"
}

# Configure Istio for observability
configure_istio_observability() {
    log "Configuring Istio for enhanced observability..."
    
    if [[ "$DRY_RUN" == "false" ]]; then
        # Enable Istio telemetry
        cat << EOF | kubectl apply -f -
apiVersion: install.istio.io/v1alpha1
kind: IstioOperator
metadata:
  name: banking-observability
  namespace: istio-system
spec:
  values:
    telemetry:
      v2:
        enabled: true
        prometheus:
          service:
            - providers:
              - prometheus
        stackdriver:
          enabled: false
        accessLogPolicy:
          enabled: true
          providers:
          - name: otel
        
    pilot:
      env:
        EXTERNAL_ISTIOD: false
        PILOT_ENABLE_WORKLOAD_ENTRY_AUTOREGISTRATION: true
        PILOT_ENABLE_CROSS_CLUSTER_WORKLOAD_ENTRY: true
        PILOT_TRACE_SAMPLING: 1.0
        
    proxy:
      tracer: jaeger
      
    global:
      tracer:
        jaeger:
          service: jaeger-collector.$NAMESPACE.svc.cluster.local
          port: 14250
      meshConfig:
        defaultConfig:
          proxyStatsMatcher:
            inclusionRegexps:
            - ".*outlier_detection.*"
            - ".*circuit_breakers.*"
            - ".*upstream_rq_retry.*"
            - ".*upstream_rq_pending.*"
            - ".*_cx_.*"
            exclusionRegexps:
            - ".*osconfig.*"
        extensionProviders:
        - name: jaeger
          envoyExtAuthzHttp:
            service: jaeger-collector.$NAMESPACE.svc.cluster.local
            port: 14250
        - name: prometheus
          prometheus:
            service: prometheus-operated.$NAMESPACE.svc.cluster.local
            port: 9090
EOF
        info "Applied Istio observability configuration"
    else
        info "Would configure Istio for observability"
    fi
    
    success "Istio observability configuration completed"
}

# Deploy OpenTelemetry Collector
deploy_otel_collector() {
    log "Deploying OpenTelemetry Collector..."
    
    if [[ "$DRY_RUN" == "false" ]]; then
        # Create OpenTelemetry Collector deployment
        cat << EOF | kubectl apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: otel-collector
  namespace: $NAMESPACE
  labels:
    app: otel-collector
    component: observability
spec:
  replicas: 3
  selector:
    matchLabels:
      app: otel-collector
  template:
    metadata:
      labels:
        app: otel-collector
    spec:
      serviceAccountName: observability-sa
      containers:
      - name: otel-collector
        image: otel/opentelemetry-collector-contrib:0.89.0
        args:
        - --config=/etc/otel-collector-config/config.yaml
        volumeMounts:
        - name: config
          mountPath: /etc/otel-collector-config
        ports:
        - containerPort: 4317
          name: otlp-grpc
        - containerPort: 4318
          name: otlp-http
        - containerPort: 8889
          name: prometheus
        - containerPort: 13133
          name: health
        livenessProbe:
          httpGet:
            path: /
            port: 13133
        readinessProbe:
          httpGet:
            path: /
            port: 13133
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
      volumes:
      - name: config
        configMap:
          name: otel-collector-config
---
apiVersion: v1
kind: Service
metadata:
  name: otel-collector
  namespace: $NAMESPACE
  labels:
    app: otel-collector
    monitoring: banking-enabled
spec:
  selector:
    app: otel-collector
  ports:
  - port: 4317
    name: otlp-grpc
    targetPort: 4317
  - port: 4318
    name: otlp-http
    targetPort: 4318
  - port: 8889
    name: prometheus
    targetPort: 8889
  - port: 13133
    name: health
    targetPort: 13133
EOF
        info "Deployed OpenTelemetry Collector"
    else
        info "Would deploy OpenTelemetry Collector"
    fi
    
    success "OpenTelemetry Collector deployment completed"
}

# Deploy Fluentd for log aggregation
deploy_fluentd() {
    log "Deploying Fluentd for log aggregation..."
    
    if [[ "$DRY_RUN" == "false" ]]; then
        cat << EOF | kubectl apply -f -
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: fluentd-banking
  namespace: $NAMESPACE
  labels:
    app: fluentd
    component: logging
spec:
  selector:
    matchLabels:
      app: fluentd
  template:
    metadata:
      labels:
        app: fluentd
    spec:
      serviceAccountName: observability-sa
      tolerations:
      - key: node-role.kubernetes.io/master
        effect: NoSchedule
      containers:
      - name: fluentd
        image: fluent/fluentd-kubernetes-daemonset:v1.16-debian-elasticsearch7-1
        env:
        - name: FLUENT_CONF
          value: fluent.conf
        - name: FLUENTD_SYSTEMD_CONF
          value: disable
        - name: ENVIRONMENT
          value: $ENVIRONMENT
        volumeMounts:
        - name: config
          mountPath: /fluentd/etc/fluent.conf
          subPath: fluent.conf
        - name: varlog
          mountPath: /var/log
        - name: varlibdockercontainers
          mountPath: /var/lib/docker/containers
          readOnly: true
        - name: fluentd-buffer
          mountPath: /var/log/fluentd-buffers
        resources:
          requests:
            memory: "256Mi"
            cpu: "100m"
          limits:
            memory: "512Mi"
            cpu: "500m"
      volumes:
      - name: config
        configMap:
          name: fluentd-banking-config
      - name: varlog
        hostPath:
          path: /var/log
      - name: varlibdockercontainers
        hostPath:
          path: /var/lib/docker/containers
      - name: fluentd-buffer
        emptyDir: {}
EOF
        info "Deployed Fluentd DaemonSet"
    else
        info "Would deploy Fluentd DaemonSet"
    fi
    
    success "Fluentd deployment completed"
}

# Validate observability deployment
validate_observability_deployment() {
    log "Validating observability deployment..."
    
    if [[ "$DRY_RUN" == "true" ]]; then
        info "Skipping validation in dry-run mode"
        return
    fi
    
    # Check Jaeger
    if kubectl get jaeger banking-jaeger -n "$NAMESPACE" &> /dev/null; then
        local jaeger_status
        jaeger_status=$(kubectl get jaeger banking-jaeger -n "$NAMESPACE" -o jsonpath='{.status.phase}')
        if [[ "$jaeger_status" == "Running" ]]; then
            success "✅ Jaeger is running"
        else
            warn "⚠️  Jaeger status: $jaeger_status"
        fi
    else
        warn "⚠️  Jaeger not found"
    fi
    
    # Check Prometheus
    if kubectl get prometheus -n "$NAMESPACE" &> /dev/null; then
        success "✅ Prometheus is deployed"
    else
        warn "⚠️  Prometheus not found"
    fi
    
    # Check ServiceMonitors
    local service_monitors
    service_monitors=$(kubectl get servicemonitor -n "$NAMESPACE" --no-headers 2>/dev/null | wc -l)
    if [[ $service_monitors -gt 0 ]]; then
        success "✅ $service_monitors ServiceMonitors deployed"
    else
        warn "⚠️  No ServiceMonitors found"
    fi
    
    # Check Loki
    if kubectl get deployment loki -n "$NAMESPACE" &> /dev/null; then
        local loki_ready
        loki_ready=$(kubectl get deployment loki -n "$NAMESPACE" -o jsonpath='{.status.readyReplicas}')
        if [[ "$loki_ready" -gt 0 ]]; then
            success "✅ Loki is ready ($loki_ready replicas)"
        else
            warn "⚠️  Loki not ready"
        fi
    else
        warn "⚠️  Loki not found"
    fi
    
    # Check OpenTelemetry Collector
    if kubectl get deployment otel-collector -n "$NAMESPACE" &> /dev/null; then
        local otel_ready
        otel_ready=$(kubectl get deployment otel-collector -n "$NAMESPACE" -o jsonpath='{.status.readyReplicas}')
        if [[ "$otel_ready" -gt 0 ]]; then
            success "✅ OpenTelemetry Collector is ready ($otel_ready replicas)"
        else
            warn "⚠️  OpenTelemetry Collector not ready"
        fi
    else
        warn "⚠️  OpenTelemetry Collector not found"
    fi
    
    # Check Fluentd
    if kubectl get daemonset fluentd-banking -n "$NAMESPACE" &> /dev/null; then
        local fluentd_ready
        fluentd_ready=$(kubectl get daemonset fluentd-banking -n "$NAMESPACE" -o jsonpath='{.status.numberReady}')
        local fluentd_desired
        fluentd_desired=$(kubectl get daemonset fluentd-banking -n "$NAMESPACE" -o jsonpath='{.status.desiredNumberScheduled}')
        if [[ "$fluentd_ready" -eq "$fluentd_desired" && "$fluentd_ready" -gt 0 ]]; then
            success "✅ Fluentd is ready ($fluentd_ready/$fluentd_desired nodes)"
        else
            warn "⚠️  Fluentd not fully ready ($fluentd_ready/$fluentd_desired nodes)"
        fi
    else
        warn "⚠️  Fluentd not found"
    fi
    
    success "Observability deployment validation completed"
}

# Test observability stack
test_observability_stack() {
    log "Testing observability stack..."
    
    if [[ "$DRY_RUN" == "true" ]]; then
        info "Skipping testing in dry-run mode"
        return
    fi
    
    # Port forward to access services
    info "Setting up port forwards for testing..."
    
    # Jaeger UI
    kubectl port-forward -n "$NAMESPACE" svc/banking-jaeger-query 16686:16686 &
    local jaeger_pid=$!
    
    # Grafana
    kubectl port-forward -n "$NAMESPACE" svc/prometheus-grafana 3000:80 &
    local grafana_pid=$!
    
    # Prometheus
    kubectl port-forward -n "$NAMESPACE" svc/prometheus-operated 9090:9090 &
    local prometheus_pid=$!
    
    sleep 10
    
    # Test Jaeger
    if curl -s --max-time 5 "http://localhost:16686/api/services" | jq -e '.data | length > 0' &> /dev/null; then
        success "✅ Jaeger API is accessible and has services"
    else
        warn "⚠️  Jaeger API test failed"
    fi
    
    # Test Prometheus
    if curl -s --max-time 5 "http://localhost:9090/-/ready" &> /dev/null; then
        success "✅ Prometheus is ready"
    else
        warn "⚠️  Prometheus readiness test failed"
    fi
    
    # Test Grafana
    if curl -s --max-time 5 "http://localhost:3000/api/health" | grep -q "ok"; then
        success "✅ Grafana health check passed"
    else
        warn "⚠️  Grafana health check failed"
    fi
    
    # Clean up port forwards
    kill $jaeger_pid $grafana_pid $prometheus_pid 2>/dev/null || true
    
    success "Observability stack testing completed"
}

# Generate access information
generate_access_information() {
    log "Generating access information..."
    
    local access_file="$PROJECT_ROOT/observability-access-info-$ENVIRONMENT-$(date +%Y%m%d-%H%M%S).md"
    
    cat > "$access_file" << EOF
# Service Mesh Observability Access Information

**Environment:** $ENVIRONMENT  
**Deployment Date:** $(date)  
**Namespaces:** $NAMESPACE, $BANKING_NAMESPACE  

## Access URLs

### Jaeger Tracing
- **Local Access:** \`kubectl port-forward -n $NAMESPACE svc/banking-jaeger-query 16686:16686\`
- **URL:** http://localhost:16686
- **Features:** Distributed tracing, service dependencies, trace analysis

### Grafana Dashboards
- **Local Access:** \`kubectl port-forward -n $NAMESPACE svc/prometheus-grafana 3000:80\`
- **URL:** http://localhost:3000
- **Username:** admin
- **Password:** \`kubectl get secret -n $NAMESPACE prometheus-grafana -o jsonpath='{.data.admin-password}' | base64 -d\`
- **Dashboards:** Banking Overview, Transactions, Service Mesh

### Prometheus Metrics
- **Local Access:** \`kubectl port-forward -n $NAMESPACE svc/prometheus-operated 9090:9090\`
- **URL:** http://localhost:9090
- **Features:** Metrics queries, alerting, targets status

### Loki Logs
- **Access via Grafana:** Explore > Loki datasource
- **Direct Access:** \`kubectl port-forward -n $NAMESPACE svc/loki 3100:3100\`
- **Features:** Log aggregation, correlation with traces and metrics

## Key Metrics

### Banking Business Metrics
- \`banking_transactions_total\` - Total banking transactions
- \`banking_payment_processing_duration_seconds\` - Payment processing time
- \`banking_customer_operations_total\` - Customer operations count
- \`banking_loan_applications_total\` - Loan application metrics

### Service Mesh Metrics
- \`istio_requests_total\` - Service-to-service requests
- \`istio_request_duration_milliseconds\` - Request latency
- \`istio_tcp_connections_opened_total\` - TCP connections

### Banking Protocol Metrics
- \`envoy_banking_swift_messages_total\` - SWIFT message processing
- \`envoy_banking_iso20022_messages_total\` - ISO 20022 processing
- \`envoy_banking_compliance_violations_total\` - Compliance violations

### Security Metrics
- \`istio_requests_total{security_policy="mutual_tls"}\` - mTLS requests
- \`istio_certificate_expiration_timestamp\` - Certificate expiry

## Sample Queries

### Business Queries
\`\`\`promql
# Transaction rate by type
sum(rate(banking_transactions_total[5m])) by (transaction_type)

# Payment processing success rate
sum(rate(banking_transactions_total{status="success"}[5m])) / sum(rate(banking_transactions_total[5m])) * 100

# High-value transaction count
sum(banking_transactions_total{amount_range="high"})
\`\`\`

### Performance Queries
\`\`\`promql
# Service request rate
sum(rate(istio_requests_total{destination_service_namespace="$BANKING_NAMESPACE"}[5m])) by (destination_service_name)

# P99 latency
histogram_quantile(0.99, sum(rate(istio_request_duration_milliseconds_bucket{destination_service_namespace="$BANKING_NAMESPACE"}[5m])) by (destination_service_name, le))

# Error rate
sum(rate(istio_requests_total{destination_service_namespace="$BANKING_NAMESPACE",response_code!~"2.."}[5m])) / sum(rate(istio_requests_total{destination_service_namespace="$BANKING_NAMESPACE"}[5m])) * 100
\`\`\`

### Security Queries
\`\`\`promql
# mTLS adoption rate
sum(rate(istio_requests_total{security_policy="mutual_tls"}[5m])) / sum(rate(istio_requests_total[5m])) * 100

# Certificate expiry (days)
(istio_certificate_expiration_timestamp - time()) / 86400
\`\`\`

## Log Queries (Loki)

### Banking Application Logs
\`\`\`logql
{app="banking"} |= "ERROR" | json | line_format "{{.timestamp}} [{{.level}}] {{.message}}"
\`\`\`

### Transaction Logs
\`\`\`logql
{app="banking",transaction_id!="unknown"} | json | line_format "{{.timestamp}} TXN:{{.transaction_id}} {{.message}}"
\`\`\`

### Compliance Logs
\`\`\`logql
{app="envoy",compliance_verified="true"} | json | line_format "{{.timestamp}} {{.banking_protocol}} - {{.method}} {{.path}}"
\`\`\`

## Alerting

### Critical Alerts
- **Service Down:** Any banking service becomes unavailable
- **High Error Rate:** Error rate > 1% for more than 2 minutes
- **Compliance Violation:** Any compliance violation detected
- **Certificate Expiry:** Certificates expiring within 7 days

### Warning Alerts
- **High Latency:** P99 latency > 2 seconds for 3 minutes
- **Low Transaction Volume:** Transaction rate drops below normal
- **mTLS Issues:** mTLS usage rate < 95%

## Troubleshooting

### Common Commands
\`\`\`bash
# Check observability components
kubectl get all -n $NAMESPACE

# Check Jaeger traces
kubectl logs -n $NAMESPACE deployment/banking-jaeger-collector

# Check Prometheus targets
kubectl port-forward -n $NAMESPACE svc/prometheus-operated 9090:9090
# Visit http://localhost:9090/targets

# Check Loki logs
kubectl logs -n $NAMESPACE deployment/loki

# Check Fluentd status
kubectl logs -n $NAMESPACE daemonset/fluentd-banking
\`\`\`

### Performance Issues
1. Check resource usage: \`kubectl top pods -n $NAMESPACE\`
2. Review metrics retention: \`kubectl describe prometheus -n $NAMESPACE\`
3. Check log ingestion rate: \`kubectl logs -n $NAMESPACE deployment/loki\`

### Data Issues
1. Verify ServiceMonitors: \`kubectl get servicemonitor -n $NAMESPACE\`
2. Check Prometheus configuration: \`kubectl get prometheus -n $NAMESPACE -o yaml\`
3. Validate Fluentd configuration: \`kubectl get configmap fluentd-banking-config -n $NAMESPACE -o yaml\`

## Architecture Components

### Tracing Flow
1. **Application** → OpenTelemetry SDK → **Jaeger Collector**
2. **Envoy Sidecar** → Jaeger Agent → **Jaeger Collector**
3. **Jaeger Collector** → **Elasticsearch** → **Jaeger Query** → **Jaeger UI**

### Metrics Flow
1. **Application** → Prometheus metrics endpoint
2. **Envoy Sidecar** → Prometheus metrics endpoint
3. **Prometheus** → scrapes metrics → **Grafana** → visualizes

### Logging Flow
1. **Application** → container logs → **Fluentd**
2. **Envoy** → access logs → **Fluentd**
3. **Fluentd** → processes and forwards → **Loki**
4. **Loki** → **Grafana** → log exploration

---
*Generated on $(date) by Service Mesh Observability deployment script*
EOF
    
    success "Access information generated: $access_file"
}

# Cleanup function
cleanup() {
    info "Cleaning up temporary files..."
    # Kill any background port-forward processes
    jobs -p | xargs -r kill 2>/dev/null || true
}

# Error handling
handle_error() {
    error "Service mesh observability deployment failed at step: $1"
    cleanup
    exit 1
}

# Main deployment function
main() {
    # Set up error handling
    trap 'handle_error "Unknown step"' ERR
    
    print_banner
    
    # Deployment steps
    validate_prerequisites || handle_error "Prerequisites validation"
    install_required_operators || handle_error "Operators installation"
    setup_namespaces_and_rbac || handle_error "Namespaces and RBAC setup"
    deploy_observability_infrastructure || handle_error "Observability infrastructure deployment"
    configure_istio_observability || handle_error "Istio observability configuration"
    deploy_otel_collector || handle_error "OpenTelemetry Collector deployment"
    deploy_fluentd || handle_error "Fluentd deployment"
    validate_observability_deployment || handle_error "Observability deployment validation"
    test_observability_stack || handle_error "Observability stack testing"
    generate_access_information || handle_error "Access information generation"
    
    cleanup
    
    if [[ "$DRY_RUN" == "false" ]]; then
        success "🎉 Service Mesh Observability deployment completed successfully!"
        
        echo -e "\n${GREEN}Deployment Summary:${NC}"
        echo -e "  Observability Namespace: ${CYAN}$NAMESPACE${NC}"
        echo -e "  Banking Namespace: ${CYAN}$BANKING_NAMESPACE${NC}"
        echo -e "  Environment: ${CYAN}$ENVIRONMENT${NC}"
        echo -e "  Status: ${GREEN}✅ Successfully Deployed${NC}"
        
        echo -e "\n${GREEN}Observability Stack:${NC}"
        echo -e "  Distributed Tracing: ${GREEN}✅ Jaeger${NC}"
        echo -e "  Metrics Collection: ${GREEN}✅ Prometheus${NC}"
        echo -e "  Visualization: ${GREEN}✅ Grafana${NC}"
        echo -e "  Log Aggregation: ${GREEN}✅ Loki${NC}"
        echo -e "  Log Collection: ${GREEN}✅ Fluentd${NC}"
        echo -e "  Telemetry: ${GREEN}✅ OpenTelemetry${NC}"
        
        echo -e "\n${YELLOW}Quick Access Commands:${NC}"
        echo -e "  Jaeger UI: ${CYAN}kubectl port-forward -n $NAMESPACE svc/banking-jaeger-query 16686:16686${NC}"
        echo -e "  Grafana: ${CYAN}kubectl port-forward -n $NAMESPACE svc/prometheus-grafana 3000:80${NC}"
        echo -e "  Prometheus: ${CYAN}kubectl port-forward -n $NAMESPACE svc/prometheus-operated 9090:9090${NC}"
        echo -e "  Access Info: ${CYAN}Check generated access information file${NC}"
    else
        info "Dry run completed successfully. Review the configuration and run with DRY_RUN=false to deploy."
    fi
}

# Print usage information
usage() {
    echo "Usage: $0 [OBSERVABILITY_NAMESPACE] [BANKING_NAMESPACE] [ENVIRONMENT] [DRY_RUN]"
    echo
    echo "Arguments:"
    echo "  OBSERVABILITY_NAMESPACE  Observability namespace (default: observability)"
    echo "  BANKING_NAMESPACE        Banking services namespace (default: banking-system)" 
    echo "  ENVIRONMENT              Environment name (default: production)"
    echo "  DRY_RUN                  Dry run mode: true, false (default: true)"
    echo
    echo "Examples:"
    echo "  $0 observability banking-system production false"
    echo "  $0 monitoring banking-dev development true"
    echo "  $0 obs banking-staging staging false"
    echo
    echo "Components:"
    echo "  - Jaeger: Distributed tracing with Elasticsearch storage"
    echo "  - Prometheus: Metrics collection with banking-specific rules"
    echo "  - Grafana: Visualization with banking dashboards"
    echo "  - Loki: Log aggregation and correlation"
    echo "  - Fluentd: Log collection and processing"
    echo "  - OpenTelemetry: Modern telemetry collection"
    echo "  - Istio Telemetry: Service mesh observability"
    echo
    exit 0
}

# Handle command line arguments
if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
    usage
fi

# Execute main function
main "$@"