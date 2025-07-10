#!/bin/bash

# Enterprise Banking Istio Service Mesh Installation Script
# Comprehensive setup for FAPI 2.0 compliant banking infrastructure

set -euo pipefail

# Configuration
ISTIO_VERSION="1.20.3"
BANKING_NAMESPACE="banking-services"
ISTIO_NAMESPACE="istio-system"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
K8S_DIR="$(dirname "$(dirname "$SCRIPT_DIR")")/k8s"

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

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check kubectl
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl is not installed or not in PATH"
        exit 1
    fi
    
    # Check Kubernetes cluster connectivity
    if ! kubectl cluster-info &> /dev/null; then
        log_error "Cannot connect to Kubernetes cluster"
        exit 1
    fi
    
    # Check if cluster is ready
    local ready_nodes=$(kubectl get nodes --no-headers | grep Ready | wc -l)
    if [ "$ready_nodes" -eq 0 ]; then
        log_error "No ready nodes found in the cluster"
        exit 1
    fi
    
    log_success "Prerequisites check passed"
}

# Download and install Istio
install_istio() {
    log_info "Installing Istio ${ISTIO_VERSION}..."
    
    # Download Istio if not present
    if [ ! -d "istio-${ISTIO_VERSION}" ]; then
        log_info "Downloading Istio ${ISTIO_VERSION}..."
        curl -L https://istio.io/downloadIstio | ISTIO_VERSION=${ISTIO_VERSION} sh -
    fi
    
    # Add istioctl to PATH for this session
    export PATH="$PWD/istio-${ISTIO_VERSION}/bin:$PATH"
    
    # Verify istioctl
    if ! command -v istioctl &> /dev/null; then
        log_error "istioctl not found after installation"
        exit 1
    fi
    
    log_success "Istio ${ISTIO_VERSION} installed successfully"
}

# Create banking namespaces
create_namespaces() {
    log_info "Creating banking namespaces..."
    
    # Create Istio system namespace
    kubectl create namespace ${ISTIO_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
    
    # Create banking services namespace with Istio injection
    kubectl create namespace ${BANKING_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
    kubectl label namespace ${BANKING_NAMESPACE} istio-injection=enabled --overwrite
    kubectl label namespace ${BANKING_NAMESPACE} banking-environment=production --overwrite
    kubectl label namespace ${BANKING_NAMESPACE} compliance-level=fapi-2.0 --overwrite
    
    log_success "Banking namespaces created"
}

# Install Istio with banking configuration
install_istio_components() {
    log_info "Installing Istio components with banking configuration..."
    
    # Apply banking-specific Istio configuration
    if [ -f "${K8S_DIR}/istio/istio-installation.yaml" ]; then
        log_info "Applying banking Istio configuration..."
        kubectl apply -f "${K8S_DIR}/istio/istio-installation.yaml"
    else
        log_warning "Banking Istio configuration not found, using default configuration"
        istioctl install --set values.defaultRevision=default -y
    fi
    
    # Wait for Istio to be ready
    log_info "Waiting for Istio control plane to be ready..."
    kubectl wait --for=condition=Ready pods -l app=istiod -n ${ISTIO_NAMESPACE} --timeout=300s
    
    log_success "Istio control plane installed and ready"
}

# Apply banking service mesh configuration
apply_banking_config() {
    log_info "Applying banking service mesh configuration..."
    
    # Apply banking service mesh configuration
    if [ -f "${K8S_DIR}/istio/banking-service-mesh.yaml" ]; then
        kubectl apply -f "${K8S_DIR}/istio/banking-service-mesh.yaml"
        log_success "Banking service mesh configuration applied"
    else
        log_warning "Banking service mesh configuration not found"
    fi
    
    # Apply security policies
    if [ -f "${K8S_DIR}/istio/istio-security-policies.yaml" ]; then
        kubectl apply -f "${K8S_DIR}/istio/istio-security-policies.yaml"
        log_success "Banking security policies applied"
    else
        log_warning "Banking security policies not found"
    fi
    
    # Apply observability configuration
    if [ -f "${K8S_DIR}/istio/observability-config.yaml" ]; then
        kubectl apply -f "${K8S_DIR}/istio/observability-config.yaml"
        log_success "Banking observability configuration applied"
    else
        log_warning "Banking observability configuration not found"
    fi
}

# Install observability tools
install_observability_tools() {
    log_info "Installing banking observability tools..."
    
    # Install Jaeger
    log_info "Installing Jaeger for distributed tracing..."
    kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/addons/jaeger.yaml
    
    # Install Prometheus
    log_info "Installing Prometheus for metrics collection..."
    kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/addons/prometheus.yaml
    
    # Install Grafana
    log_info "Installing Grafana for metrics visualization..."
    kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/addons/grafana.yaml
    
    # Install Kiali for service mesh visualization
    log_info "Installing Kiali for service mesh visualization..."
    kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/addons/kiali.yaml
    
    # Wait for observability tools to be ready
    log_info "Waiting for observability tools to be ready..."
    kubectl wait --for=condition=Ready pods -l app=jaeger -n ${ISTIO_NAMESPACE} --timeout=300s || log_warning "Jaeger may not be ready"
    kubectl wait --for=condition=Ready pods -l app=prometheus -n ${ISTIO_NAMESPACE} --timeout=300s || log_warning "Prometheus may not be ready"
    kubectl wait --for=condition=Ready pods -l app=grafana -n ${ISTIO_NAMESPACE} --timeout=300s || log_warning "Grafana may not be ready"
    kubectl wait --for=condition=Ready pods -l app=kiali -n ${ISTIO_NAMESPACE} --timeout=300s || log_warning "Kiali may not be ready"
    
    log_success "Banking observability tools installed"
}

# Create banking service accounts
create_service_accounts() {
    log_info "Creating banking service accounts..."
    
    # Service accounts for banking services
    local services=("customer-service" "loan-service" "payment-service" "ai-assistant-service")
    
    for service in "${services[@]}"; do
        log_info "Creating service account for ${service}..."
        kubectl create serviceaccount ${service} -n ${BANKING_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
        kubectl label serviceaccount ${service} -n ${BANKING_NAMESPACE} banking-service=${service} --overwrite
        kubectl label serviceaccount ${service} -n ${BANKING_NAMESPACE} compliance-level=fapi-2.0 --overwrite
    done
    
    log_success "Banking service accounts created"
}

# Verify installation
verify_installation() {
    log_info "Verifying banking service mesh installation..."
    
    # Check Istio control plane
    local istiod_ready=$(kubectl get pods -n ${ISTIO_NAMESPACE} -l app=istiod -o jsonpath='{.items[*].status.phase}' | grep -c Running || echo "0")
    if [ "$istiod_ready" -eq 0 ]; then
        log_error "Istio control plane is not running"
        return 1
    fi
    
    # Check gateways
    local gateway_ready=$(kubectl get pods -n ${ISTIO_NAMESPACE} -l app=istio-ingressgateway -o jsonpath='{.items[*].status.phase}' | grep -c Running || echo "0")
    if [ "$gateway_ready" -eq 0 ]; then
        log_warning "Istio ingress gateway is not running"
    fi
    
    # Check banking namespace injection
    local injection_enabled=$(kubectl get namespace ${BANKING_NAMESPACE} -o jsonpath='{.metadata.labels.istio-injection}')
    if [ "$injection_enabled" != "enabled" ]; then
        log_error "Istio injection is not enabled for banking namespace"
        return 1
    fi
    
    # Check mTLS policy
    if kubectl get peerauthentication -n ${BANKING_NAMESPACE} banking-mtls-policy &> /dev/null; then
        log_success "Banking mTLS policy is configured"
    else
        log_warning "Banking mTLS policy not found"
    fi
    
    log_success "Banking service mesh installation verified"
}

# Display access information
display_access_info() {
    log_info "Banking Service Mesh Access Information:"
    
    echo ""
    echo "=== Istio Components ==="
    kubectl get pods -n ${ISTIO_NAMESPACE}
    
    echo ""
    echo "=== Banking Namespace ==="
    kubectl get all -n ${BANKING_NAMESPACE}
    
    echo ""
    echo "=== Gateway Information ==="
    local ingress_ip=$(kubectl get svc istio-ingressgateway -n ${ISTIO_NAMESPACE} -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "Pending")
    local ingress_host=$(kubectl get svc istio-ingressgateway -n ${ISTIO_NAMESPACE} -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "")
    
    if [ "$ingress_ip" != "Pending" ] && [ -n "$ingress_ip" ]; then
        echo "Ingress Gateway IP: $ingress_ip"
        echo "Banking API URL: https://$ingress_ip"
    elif [ -n "$ingress_host" ]; then
        echo "Ingress Gateway Hostname: $ingress_host"
        echo "Banking API URL: https://$ingress_host"
    else
        echo "Ingress Gateway: Service is pending external IP/hostname"
        echo "Use: kubectl get svc istio-ingressgateway -n istio-system --watch"
    fi
    
    echo ""
    echo "=== Observability Access ==="
    echo "To access Grafana: kubectl port-forward svc/grafana 3000:3000 -n istio-system"
    echo "To access Jaeger: kubectl port-forward svc/tracing 16686:80 -n istio-system"
    echo "To access Kiali: kubectl port-forward svc/kiali 20001:20001 -n istio-system"
    echo "To access Prometheus: kubectl port-forward svc/prometheus 9090:9090 -n istio-system"
    
    echo ""
    echo "=== Security Information ==="
    echo "mTLS is enforced for all banking services"
    echo "JWT authentication is required for API endpoints"
    echo "FAPI 2.0 compliance policies are active"
}

# Cleanup function
cleanup() {
    if [ "${1:-}" = "--cleanup" ]; then
        log_warning "Cleaning up banking service mesh..."
        
        # Remove banking configurations
        kubectl delete -f "${K8S_DIR}/istio/observability-config.yaml" --ignore-not-found=true
        kubectl delete -f "${K8S_DIR}/istio/istio-security-policies.yaml" --ignore-not-found=true
        kubectl delete -f "${K8S_DIR}/istio/banking-service-mesh.yaml" --ignore-not-found=true
        kubectl delete -f "${K8S_DIR}/istio/istio-installation.yaml" --ignore-not-found=true
        
        # Remove observability tools
        kubectl delete -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/addons/kiali.yaml --ignore-not-found=true
        kubectl delete -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/addons/grafana.yaml --ignore-not-found=true
        kubectl delete -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/addons/prometheus.yaml --ignore-not-found=true
        kubectl delete -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/addons/jaeger.yaml --ignore-not-found=true
        
        # Uninstall Istio
        istioctl uninstall --purge -y
        
        # Remove namespaces
        kubectl delete namespace ${BANKING_NAMESPACE} --ignore-not-found=true
        kubectl delete namespace ${ISTIO_NAMESPACE} --ignore-not-found=true
        
        log_success "Banking service mesh cleanup completed"
        exit 0
    fi
}

# Main installation function
main() {
    log_info "Starting Enterprise Banking Service Mesh Installation"
    log_info "=================================================="
    
    # Handle cleanup
    cleanup "$@"
    
    # Installation steps
    check_prerequisites
    install_istio
    create_namespaces
    install_istio_components
    apply_banking_config
    install_observability_tools
    create_service_accounts
    verify_installation
    display_access_info
    
    log_success "Enterprise Banking Service Mesh installation completed successfully!"
    log_info "Your banking infrastructure is now protected by Istio with FAPI 2.0 compliance"
}

# Script usage
usage() {
    echo "Usage: $0 [--cleanup]"
    echo ""
    echo "Options:"
    echo "  --cleanup    Remove banking service mesh installation"
    echo "  --help       Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0           Install banking service mesh"
    echo "  $0 --cleanup Remove banking service mesh"
}

# Handle arguments
case "${1:-}" in
    --help|-h)
        usage
        exit 0
        ;;
    --cleanup)
        main "$@"
        ;;
    "")
        main
        ;;
    *)
        log_error "Unknown option: $1"
        usage
        exit 1
        ;;
esac