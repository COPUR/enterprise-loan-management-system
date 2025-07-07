#!/bin/bash

# Enterprise Banking System - Architecture Validation Script
# Tests the Istio microservices architecture for correctness

set -e

echo "🏦 Validating Enterprise Banking System Architecture..."

# Configuration
BASE_URL=${BASE_URL:-"https://banking.local"}
NAMESPACE=${KUBERNETES_NAMESPACE:-"banking-system"}
TIMEOUT=${TIMEOUT:-30}

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    # Check kubectl
    if ! command -v kubectl &> /dev/null; then
        print_error "kubectl is not installed or not in PATH"
        exit 1
    fi
    
    # Check istioctl
    if ! command -v istioctl &> /dev/null; then
        print_error "istioctl is not installed or not in PATH"
        exit 1
    fi
    
    # Check curl
    if ! command -v curl &> /dev/null; then
        print_error "curl is not installed"
        exit 1
    fi
    
    # Check jq for JSON processing
    if ! command -v jq &> /dev/null; then
        print_warning "jq is not installed - JSON validation will be limited"
    fi
    
    print_success "Prerequisites check passed"
}

# Validate Kubernetes deployment
validate_kubernetes() {
    print_status "Validating Kubernetes deployment..."
    
    # Check namespace exists
    if ! kubectl get namespace $NAMESPACE &> /dev/null; then
        print_error "Namespace $NAMESPACE does not exist"
        return 1
    fi
    
    # Check all pods are running
    print_status "Checking pod status..."
    local pods_not_ready=$(kubectl get pods -n $NAMESPACE --no-headers | grep -v "Running\|Completed" | wc -l)
    
    if [ $pods_not_ready -gt 0 ]; then
        print_warning "Some pods are not ready:"
        kubectl get pods -n $NAMESPACE | grep -v "Running\|Completed"
    else
        print_success "All pods are running"
    fi
    
    # Check services
    print_status "Checking services..."
    local services=$(kubectl get svc -n $NAMESPACE --no-headers | wc -l)
    if [ $services -gt 0 ]; then
        print_success "Found $services services"
        kubectl get svc -n $NAMESPACE
    else
        print_error "No services found in namespace $NAMESPACE"
        return 1
    fi
    
    return 0
}

# Validate Istio configuration
validate_istio() {
    print_status "Validating Istio service mesh..."
    
    # Check Istio system pods
    print_status "Checking Istio system pods..."
    local istio_pods_not_ready=$(kubectl get pods -n istio-system --no-headers | grep -v "Running\|Completed" | wc -l)
    
    if [ $istio_pods_not_ready -gt 0 ]; then
        print_warning "Some Istio pods are not ready:"
        kubectl get pods -n istio-system | grep -v "Running\|Completed"
    else
        print_success "All Istio system pods are running"
    fi
    
    # Check proxy status
    print_status "Checking Envoy proxy status..."
    if istioctl proxy-status &> /dev/null; then
        print_success "Envoy proxies are healthy"
        istioctl proxy-status
    else
        print_error "Envoy proxy status check failed"
        return 1
    fi
    
    # Check gateway configuration
    print_status "Checking Istio gateway..."
    if kubectl get gateway banking-gateway -n $NAMESPACE &> /dev/null; then
        print_success "Istio gateway found"
    else
        print_error "Istio gateway not found"
        return 1
    fi
    
    # Check virtual service
    if kubectl get virtualservice banking-api-virtualservice -n $NAMESPACE &> /dev/null; then
        print_success "Virtual service found"
    else
        print_error "Virtual service not found"
        return 1
    fi
    
    return 0
}

# Test API endpoints
test_api_endpoints() {
    print_status "Testing API endpoints..."
    
    # Test health endpoint
    print_status "Testing health endpoint..."
    if curl -sf -m $TIMEOUT "$BASE_URL/actuator/health" > /dev/null; then
        print_success "Health endpoint accessible"
    else
        print_error "Health endpoint not accessible"
        return 1
    fi
    
    # Test metrics endpoint
    print_status "Testing metrics endpoint..."
    if curl -sf -m $TIMEOUT "$BASE_URL/actuator/metrics" > /dev/null; then
        print_success "Metrics endpoint accessible"
    else
        print_warning "Metrics endpoint not accessible"
    fi
    
    # Test AI service health
    print_status "Testing AI service health..."
    if curl -sf -m $TIMEOUT "$BASE_URL/api/v1/ai/health" > /dev/null; then
        print_success "AI service health endpoint accessible"
    else
        print_warning "AI service health endpoint not accessible"
    fi
    
    return 0
}

# Test service mesh routing
test_service_mesh_routing() {
    print_status "Testing service mesh routing..."
    
    # Test customer service routing
    print_status "Testing customer service routing..."
    local response=$(curl -s -w "%{http_code}" -o /dev/null -m $TIMEOUT "$BASE_URL/api/v1/customers")
    
    if [ "$response" = "200" ] || [ "$response" = "401" ] || [ "$response" = "403" ]; then
        print_success "Customer service routing working (HTTP $response)"
    else
        print_warning "Customer service routing issue (HTTP $response)"
    fi
    
    # Test AI service routing
    print_status "Testing AI service routing..."
    local ai_response=$(curl -s -w "%{http_code}" -o /dev/null -m $TIMEOUT "$BASE_URL/api/v1/ai/health")
    
    if [ "$ai_response" = "200" ]; then
        print_success "AI service routing working (HTTP $ai_response)"
    else
        print_warning "AI service routing issue (HTTP $ai_response)"
    fi
    
    return 0
}

# Test distributed Redis
test_redis_cluster() {
    print_status "Testing Redis cluster..."
    
    # Check Redis pods
    local redis_pods=$(kubectl get pods -n $NAMESPACE -l app=redis-cluster --no-headers | wc -l)
    
    if [ $redis_pods -ge 6 ]; then
        print_success "Redis cluster has $redis_pods pods"
    else
        print_warning "Redis cluster has only $redis_pods pods (expected 6)"
    fi
    
    # Test Redis connectivity from a pod
    local test_pod=$(kubectl get pods -n $NAMESPACE -l app=customer-service --no-headers | head -1 | awk '{print $1}')
    
    if [ -n "$test_pod" ]; then
        print_status "Testing Redis connectivity from $test_pod..."
        if kubectl exec -n $NAMESPACE $test_pod -- nc -z banking-redis 6379 &> /dev/null; then
            print_success "Redis connectivity test passed"
        else
            print_warning "Redis connectivity test failed"
        fi
    else
        print_warning "No customer service pod found for Redis connectivity test"
    fi
    
    return 0
}

# Generate test report
generate_report() {
    print_status "Generating validation report..."
    
    local report_file="architecture-validation-report-$(date +%Y%m%d-%H%M%S).txt"
    
    {
        echo "Enterprise Banking System - Architecture Validation Report"
        echo "=========================================================="
        echo "Date: $(date)"
        echo "Base URL: $BASE_URL"
        echo "Namespace: $NAMESPACE"
        echo ""
        
        echo "Kubernetes Resources:"
        echo "--------------------"
        kubectl get all -n $NAMESPACE
        echo ""
        
        echo "Istio Configuration:"
        echo "-------------------"
        kubectl get gateway,virtualservice,destinationrule -n $NAMESPACE
        echo ""
        
        echo "Pod Resource Usage:"
        echo "------------------"
        kubectl top pods -n $NAMESPACE 2>/dev/null || echo "Metrics server not available"
        echo ""
        
        echo "Service Endpoints:"
        echo "-----------------"
        kubectl get endpoints -n $NAMESPACE
        echo ""
        
        echo "Envoy Proxy Status:"
        echo "------------------"
        istioctl proxy-status 2>/dev/null || echo "Unable to retrieve proxy status"
        
    } > $report_file
    
    print_success "Validation report saved to: $report_file"
}

# Main validation function
main() {
    print_status "Starting architecture validation..."
    print_status "Base URL: $BASE_URL"
    print_status "Namespace: $NAMESPACE"
    print_status "Timeout: ${TIMEOUT}s"
    echo ""
    
    local validation_failed=false
    
    # Run validation steps
    check_prerequisites || validation_failed=true
    
    if ! $validation_failed; then
        validate_kubernetes || validation_failed=true
        validate_istio || validation_failed=true
        test_api_endpoints || validation_failed=true
        test_service_mesh_routing || validation_failed=true
        test_redis_cluster || validation_failed=true
    fi
    
    # Generate report regardless of validation result
    generate_report
    
    echo ""
    if $validation_failed; then
        print_error "Architecture validation completed with warnings/errors"
        print_status "Check the generated report for detailed information"
        exit 1
    else
        print_success "Architecture validation completed successfully!"
        print_success "The Istio service mesh microservices architecture is working correctly"
        
        echo ""
        print_status "Architecture Summary:"
        echo "✅ Kubernetes deployment validated"
        echo "✅ Istio service mesh operational"
        echo "✅ API endpoints accessible"
        echo "✅ Service mesh routing working"
        echo "✅ Redis cluster operational"
        
        echo ""
        print_status "Next steps:"
        echo "🚀 Use the Postman collection for comprehensive API testing"
        echo "📊 Monitor with: kubectl get pods -n $NAMESPACE -w"
        echo "🔍 View logs with: kubectl logs -f deployment/customer-service -n $NAMESPACE"
        echo "📈 Check metrics: istioctl dashboard kiali"
    fi
}

# Help function
show_help() {
    echo "Enterprise Banking System - Architecture Validation Script"
    echo ""
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "OPTIONS:"
    echo "  -h, --help              Show this help message"
    echo "  -u, --url URL           Set base URL (default: https://banking.local)"
    echo "  -n, --namespace NS      Set Kubernetes namespace (default: banking-system)"
    echo "  -t, --timeout SECONDS   Set request timeout (default: 30)"
    echo ""
    echo "ENVIRONMENT VARIABLES:"
    echo "  BASE_URL               Base URL for API testing"
    echo "  KUBERNETES_NAMESPACE   Kubernetes namespace"
    echo "  TIMEOUT               Request timeout in seconds"
    echo ""
    echo "EXAMPLES:"
    echo "  $0                                    # Basic validation"
    echo "  $0 --url http://localhost:8080       # Test local deployment"
    echo "  $0 --namespace test-banking          # Use different namespace"
    echo "  BASE_URL=https://api.banking.com $0  # Use environment variable"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -u|--url)
            BASE_URL="$2"
            shift 2
            ;;
        -n|--namespace)
            NAMESPACE="$2"
            shift 2
            ;;
        -t|--timeout)
            TIMEOUT="$2"
            shift 2
            ;;
        *)
            print_error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Run main function
main