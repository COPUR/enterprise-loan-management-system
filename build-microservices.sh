#!/bin/bash

# Enterprise Banking System - Microservices Build Script
# Builds Docker images for all microservices with Istio integration

set -e

echo "🏦 Building Enterprise Banking System Microservices..."

# Configuration
REGISTRY=${DOCKER_REGISTRY:-"docker.io"}
NAMESPACE=${DOCKER_NAMESPACE:-"banking"}
VERSION=${VERSION:-"1.0.0"}
PLATFORMS=${PLATFORMS:-"linux/amd64,linux/arm64"}

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
    
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed"
        exit 1
    fi
    
    if ! command -v gradle &> /dev/null && ! command -v ./gradlew &> /dev/null; then
        print_error "Gradle is not available"
        exit 1
    fi
    
    print_success "Prerequisites check passed"
}

# Build individual microservice
build_microservice() {
    local service=$1
    local dockerfile=$2
    local port=$3
    
    print_status "Building $service..."
    
    # Build Docker image
    docker build \
        -f $dockerfile \
        -t ${REGISTRY}/${NAMESPACE}/${service}:${VERSION} \
        -t ${REGISTRY}/${NAMESPACE}/${service}:latest \
        --build-arg SERVICE_NAME=$service \
        --build-arg SERVICE_PORT=$port \
        .
    
    if [ $? -eq 0 ]; then
        print_success "$service image built successfully"
    else
        print_error "Failed to build $service image"
        exit 1
    fi
}

# Main build process
main() {
    print_status "Starting microservices build process..."
    print_status "Registry: $REGISTRY"
    print_status "Namespace: $NAMESPACE"
    print_status "Version: $VERSION"
    
    check_prerequisites
    
    # Clean and test
    if [ "$SKIP_TESTS" != "true" ]; then
        print_status "Running tests..."
        if command -v ./gradlew &> /dev/null; then
            ./gradlew clean test
        else
            gradle clean test
        fi
        
        if [ $? -ne 0 ]; then
            print_warning "Tests failed, but continuing with build..."
        fi
    else
        print_status "Skipping tests as requested..."
        if command -v ./gradlew &> /dev/null; then
            ./gradlew clean build -x test
        else
            gradle clean build -x test
        fi
    fi
    
    # Build microservices
    print_status "Building microservice images..."
    
    build_microservice "customer-service" "Dockerfile.customer-service" "8081"
    build_microservice "loan-service" "Dockerfile.loan-service" "8082"
    build_microservice "payment-service" "Dockerfile.payment-service" "8083"
    build_microservice "party-service" "Dockerfile.party-service" "8084"
    
    # Display built images
    print_status "Built images:"
    docker images | grep "${NAMESPACE}/" | grep "${VERSION}"
    
    print_success "All microservices built successfully!"
    
    # Optional: Push to registry
    if [ "$PUSH_IMAGES" == "true" ]; then
        print_status "Pushing images to registry..."
        docker push ${REGISTRY}/${NAMESPACE}/customer-service:${VERSION}
        docker push ${REGISTRY}/${NAMESPACE}/loan-service:${VERSION}
        docker push ${REGISTRY}/${NAMESPACE}/payment-service:${VERSION}
        docker push ${REGISTRY}/${NAMESPACE}/party-service:${VERSION}
        print_success "Images pushed to registry"
    fi
    
    print_success "Build process completed!"
    echo ""
    echo "🚀 To deploy to Kubernetes with Istio:"
    echo "   helm upgrade --install banking-system k8s/helm-charts/enterprise-loan-system -f k8s/helm-charts/enterprise-loan-system/values-istio.yaml"
    echo ""
    echo "📊 To check deployment status:"
    echo "   kubectl get pods -n banking-system"
    echo "   kubectl get svc -n banking-system"
    echo "   istioctl proxy-status"
}

# Help function
show_help() {
    echo "Enterprise Banking System - Microservices Build Script"
    echo ""
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "OPTIONS:"
    echo "  -h, --help           Show this help message"
    echo "  -v, --version VER    Set version tag (default: 1.0.0)"
    echo "  -r, --registry REG   Set Docker registry (default: docker.io)"
    echo "  -n, --namespace NS   Set Docker namespace (default: banking)"
    echo "  -p, --push           Push images to registry after build"
    echo "  --skip-tests         Skip running tests"
    echo ""
    echo "ENVIRONMENT VARIABLES:"
    echo "  DOCKER_REGISTRY      Docker registry URL"
    echo "  DOCKER_NAMESPACE     Docker namespace"
    echo "  VERSION              Image version"
    echo "  PUSH_IMAGES          Set to 'true' to push images"
    echo ""
    echo "EXAMPLES:"
    echo "  $0                                    # Basic build"
    echo "  $0 --version 2.0.0 --push           # Build and push version 2.0.0"
    echo "  PUSH_IMAGES=true $0                  # Build and push using environment"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -v|--version)
            VERSION="$2"
            shift 2
            ;;
        -r|--registry)
            REGISTRY="$2"
            shift 2
            ;;
        -n|--namespace)
            NAMESPACE="$2"
            shift 2
            ;;
        -p|--push)
            PUSH_IMAGES="true"
            shift
            ;;
        --skip-tests)
            SKIP_TESTS="true"
            shift
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