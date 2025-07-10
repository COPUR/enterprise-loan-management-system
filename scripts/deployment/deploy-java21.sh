#!/bin/bash

# =====================================================
# Java 21 Deployment Automation Script
# Enterprise Loan Management System
# =====================================================

set -euo pipefail

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
BUILD_DIR="${PROJECT_ROOT}/build"
DEPLOYMENT_ENV="${DEPLOYMENT_ENV:-development}"
JAVA_VERSION="21"
APP_NAME="loan-management-system"
DOCKER_REGISTRY="${DOCKER_REGISTRY:-localhost:5000}"

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
error_exit() {
    log_error "$1"
    exit 1
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites for Java 21 deployment..."
    
    # Check Java version
    if ! command -v java &> /dev/null; then
        error_exit "Java is not installed"
    fi
    
    JAVA_VERSION_OUTPUT=$(java -version 2>&1 | head -n1)
    if [[ ! "$JAVA_VERSION_OUTPUT" =~ "21" ]]; then
        error_exit "Java 21 is required but found: $JAVA_VERSION_OUTPUT"
    fi
    log_success "Java 21 is installed"
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        error_exit "Docker is not installed"
    fi
    log_success "Docker is available"
    
    # Check Kubernetes (kubectl)
    if ! command -v kubectl &> /dev/null; then
        log_warning "kubectl not found - Kubernetes deployment will be skipped"
        SKIP_K8S=true
    else
        log_success "kubectl is available"
        SKIP_K8S=false
    fi
    
    # Check Gradle
    if ! command -v ./gradlew &> /dev/null && ! command -v gradle &> /dev/null; then
        error_exit "Gradle wrapper or Gradle is not available"
    fi
    log_success "Gradle is available"
}

# Build application with Java 21 optimizations
build_application() {
    log_info "Building application with Java 21 optimizations..."
    
    cd "$PROJECT_ROOT"
    
    # Clean previous builds
    ./gradlew clean
    
    # Run tests with Java 21 features
    log_info "Running tests with Java 21 features..."
    ./gradlew test --parallel --build-cache \
        -Dorg.gradle.jvmargs="-XX:+UseZGC -Xmx4g --enable-preview" \
        -Pjava21.features.enabled=true \
        -Pjava21.virtual.threads=true \
        -Pjava21.pattern.matching=true
    
    # Build JAR with optimizations
    log_info "Building optimized JAR..."
    ./gradlew bootJar --parallel --build-cache \
        -Pjava21.optimizations=true \
        -Pprofile="$DEPLOYMENT_ENV"
    
    # Verify JAR was created
    JAR_FILE=$(find build/libs -name "*.jar" | head -n1)
    if [[ ! -f "$JAR_FILE" ]]; then
        error_exit "JAR file not found in build/libs"
    fi
    
    log_success "Application built successfully: $JAR_FILE"
    
    # Run performance benchmarks
    if [[ "$DEPLOYMENT_ENV" == "production" ]]; then
        log_info "Running performance benchmarks..."
        ./gradlew performanceTest \
            -Pjava21.benchmarks=true \
            -Pjava21.virtual.threads.benchmark=true \
            -Pjava21.pattern.matching.benchmark=true
    fi
}

# Build Docker image with Java 21
build_docker_image() {
    log_info "Building Docker image with Java 21..."
    
    cd "$PROJECT_ROOT"
    
    # Generate build info
    BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ')
    GIT_COMMIT=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
    VERSION=$(./gradlew properties -q | grep "version:" | awk '{print $2}')
    
    # Build Docker image
    docker build \
        --file docker/Dockerfile.java21 \
        --tag "${DOCKER_REGISTRY}/${APP_NAME}:java21-${VERSION}" \
        --tag "${DOCKER_REGISTRY}/${APP_NAME}:java21-latest" \
        --build-arg BUILD_DATE="$BUILD_DATE" \
        --build-arg GIT_COMMIT="$GIT_COMMIT" \
        --build-arg VERSION="$VERSION" \
        --build-arg JAVA_VERSION="21" \
        --build-arg DEPLOYMENT_ENV="$DEPLOYMENT_ENV" \
        .
    
    log_success "Docker image built: ${DOCKER_REGISTRY}/${APP_NAME}:java21-${VERSION}"
    
    # Test Docker image
    log_info "Testing Docker image..."
    docker run --rm \
        -e SPRING_PROFILES_ACTIVE="test,java21" \
        -p 8080:8080 \
        --name "${APP_NAME}-test" \
        "${DOCKER_REGISTRY}/${APP_NAME}:java21-${VERSION}" \
        --spring.main.banner-mode=off \
        --server.shutdown=graceful \
        --management.server.port=8081 &
    
    DOCKER_PID=$!
    
    # Wait for application to start
    log_info "Waiting for application to start..."
    for i in {1..60}; do
        if curl -sf http://localhost:8080/actuator/health >/dev/null 2>&1; then
            log_success "Application started successfully"
            break
        fi
        if [[ $i -eq 60 ]]; then
            error_exit "Application failed to start within 60 seconds"
        fi
        sleep 1
    done
    
    # Test Java 21 features
    log_info "Testing Java 21 features..."
    
    # Test Virtual Threads endpoint
    if curl -sf http://localhost:8080/actuator/metrics/virtual.threads.active >/dev/null 2>&1; then
        log_success "Virtual Threads are working"
    else
        log_warning "Virtual Threads metrics not available"
    fi
    
    # Test Pattern Matching endpoint
    if curl -sf http://localhost:8080/actuator/metrics/pattern.matching.operations >/dev/null 2>&1; then
        log_success "Pattern Matching metrics are working"
    else
        log_warning "Pattern Matching metrics not available"
    fi
    
    # Stop test container
    docker stop "${APP_NAME}-test" >/dev/null 2>&1 || true
}

# Deploy to Kubernetes
deploy_to_kubernetes() {
    if [[ "$SKIP_K8S" == "true" ]]; then
        log_warning "Skipping Kubernetes deployment - kubectl not available"
        return
    fi
    
    log_info "Deploying to Kubernetes with Java 21 optimizations..."
    
    cd "$PROJECT_ROOT"
    
    # Apply namespace
    kubectl apply -f k8s/namespace.yaml
    
    # Apply configmaps
    kubectl apply -f k8s/configmap-java21.yaml
    
    # Apply secrets
    if [[ -f "k8s/secrets-${DEPLOYMENT_ENV}.yaml" ]]; then
        kubectl apply -f "k8s/secrets-${DEPLOYMENT_ENV}.yaml"
    fi
    
    # Update deployment with new image
    VERSION=$(./gradlew properties -q | grep "version:" | awk '{print $2}')
    
    # Replace image tag in deployment
    sed "s|{{IMAGE_TAG}}|java21-${VERSION}|g" k8s/deployment-java21.yaml > /tmp/deployment-java21.yaml
    sed -i "s|{{DEPLOYMENT_ENV}}|${DEPLOYMENT_ENV}|g" /tmp/deployment-java21.yaml
    
    # Apply deployment
    kubectl apply -f /tmp/deployment-java21.yaml
    
    # Apply service
    kubectl apply -f k8s/service.yaml
    
    # Apply ingress if exists
    if [[ -f "k8s/ingress-${DEPLOYMENT_ENV}.yaml" ]]; then
        kubectl apply -f "k8s/ingress-${DEPLOYMENT_ENV}.yaml"
    fi
    
    # Wait for rollout
    log_info "Waiting for deployment rollout..."
    kubectl rollout status deployment/loan-management-java21 --timeout=300s
    
    # Verify pods are running
    kubectl get pods -l app=loan-management,version=java21
    
    log_success "Kubernetes deployment completed"
}

# Run health checks
run_health_checks() {
    log_info "Running health checks..."
    
    # Determine service URL
    if [[ "$SKIP_K8S" == "false" ]]; then
        # Get service URL from Kubernetes
        SERVICE_IP=$(kubectl get service loan-management-service -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "")
        if [[ -z "$SERVICE_IP" ]]; then
            SERVICE_IP=$(kubectl get service loan-management-service -o jsonpath='{.spec.clusterIP}')
        fi
        SERVICE_URL="http://${SERVICE_IP}:8080"
        
        # Port forward for testing if no external IP
        if [[ "$SERVICE_IP" =~ ^10\.|^172\.|^192\.168\. ]]; then
            log_info "Using port-forward for health checks..."
            kubectl port-forward service/loan-management-service 8080:8080 &
            PORT_FORWARD_PID=$!
            SERVICE_URL="http://localhost:8080"
            sleep 5
        fi
    else
        SERVICE_URL="http://localhost:8080"
    fi
    
    # Health check
    log_info "Checking application health at $SERVICE_URL..."
    for i in {1..30}; do
        if curl -sf "${SERVICE_URL}/actuator/health" >/dev/null 2>&1; then
            log_success "Health check passed"
            break
        fi
        if [[ $i -eq 30 ]]; then
            error_exit "Health check failed after 30 attempts"
        fi
        sleep 2
    done
    
    # Java 21 specific checks
    log_info "Checking Java 21 features..."
    
    # Check Virtual Threads metrics
    VT_METRICS=$(curl -s "${SERVICE_URL}/actuator/metrics/virtual.threads.active" | jq -r '.measurements[0].value // "N/A"' 2>/dev/null || echo "N/A")
    log_info "Virtual Threads active: $VT_METRICS"
    
    # Check Pattern Matching metrics
    PM_METRICS=$(curl -s "${SERVICE_URL}/actuator/metrics/pattern.matching.operations" | jq -r '.measurements[0].value // "N/A"' 2>/dev/null || echo "N/A")
    log_info "Pattern Matching operations: $PM_METRICS"
    
    # Check JVM metrics
    JVM_VERSION=$(curl -s "${SERVICE_URL}/actuator/info" | jq -r '.java.version // "N/A"' 2>/dev/null || echo "N/A")
    log_info "JVM Version: $JVM_VERSION"
    
    # Cleanup port-forward if used
    if [[ -n "${PORT_FORWARD_PID:-}" ]]; then
        kill $PORT_FORWARD_PID 2>/dev/null || true
    fi
    
    log_success "All health checks passed"
}

# Performance validation
validate_performance() {
    log_info "Running performance validation..."
    
    if [[ "$DEPLOYMENT_ENV" != "production" ]]; then
        log_info "Skipping performance validation for non-production environment"
        return
    fi
    
    cd "$PROJECT_ROOT"
    
    # Run performance tests against deployed application
    ./gradlew performanceValidation \
        -Pperformance.target.url="$SERVICE_URL" \
        -Pjava21.performance.validation=true \
        -Pjava21.virtual.threads.validation=true \
        -Pjava21.pattern.matching.validation=true
    
    log_success "Performance validation completed"
}

# Rollback function
rollback_deployment() {
    log_error "Deployment failed - initiating rollback..."
    
    if [[ "$SKIP_K8S" == "false" ]]; then
        # Rollback Kubernetes deployment
        kubectl rollout undo deployment/loan-management-java21
        kubectl rollout status deployment/loan-management-java21 --timeout=300s
        log_info "Kubernetes deployment rolled back"
    fi
    
    # Additional rollback steps can be added here
    log_info "Rollback completed"
}

# Cleanup function
cleanup() {
    log_info "Cleaning up..."
    
    # Remove temporary files
    rm -f /tmp/deployment-java21.yaml
    
    # Stop any running test containers
    docker stop "${APP_NAME}-test" >/dev/null 2>&1 || true
    docker rm "${APP_NAME}-test" >/dev/null 2>&1 || true
    
    # Kill port-forward if running
    if [[ -n "${PORT_FORWARD_PID:-}" ]]; then
        kill $PORT_FORWARD_PID 2>/dev/null || true
    fi
}

# Main deployment function
main() {
    log_info "Starting Java 21 deployment for environment: $DEPLOYMENT_ENV"
    
    # Set trap for cleanup
    trap cleanup EXIT
    trap rollback_deployment ERR
    
    # Execute deployment steps
    check_prerequisites
    build_application
    build_docker_image
    
    # Push Docker image if registry is configured
    if [[ "$DOCKER_REGISTRY" != "localhost:5000" ]]; then
        log_info "Pushing Docker image to registry..."
        VERSION=$(./gradlew properties -q | grep "version:" | awk '{print $2}')
        docker push "${DOCKER_REGISTRY}/${APP_NAME}:java21-${VERSION}"
        docker push "${DOCKER_REGISTRY}/${APP_NAME}:java21-latest"
        log_success "Docker image pushed to registry"
    fi
    
    deploy_to_kubernetes
    run_health_checks
    validate_performance
    
    log_success "Java 21 deployment completed successfully!"
    
    # Display deployment information
    echo
    echo "================================"
    echo "Deployment Information"
    echo "================================"
    echo "Environment: $DEPLOYMENT_ENV"
    echo "Java Version: 21"
    echo "Application: $APP_NAME"
    echo "Image: ${DOCKER_REGISTRY}/${APP_NAME}:java21-latest"
    if [[ "$SKIP_K8S" == "false" ]]; then
        echo "Kubernetes Namespace: loan-management"
        echo "Service: loan-management-service"
    fi
    echo "Health Check: ${SERVICE_URL:-N/A}/actuator/health"
    echo "Metrics: ${SERVICE_URL:-N/A}/actuator/metrics"
    echo "================================"
}

# Script execution
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --env)
                DEPLOYMENT_ENV="$2"
                shift 2
                ;;
            --registry)
                DOCKER_REGISTRY="$2"
                shift 2
                ;;
            --skip-tests)
                SKIP_TESTS=true
                shift
                ;;
            --skip-k8s)
                SKIP_K8S=true
                shift
                ;;
            --help)
                echo "Usage: $0 [OPTIONS]"
                echo "Options:"
                echo "  --env ENV          Deployment environment (default: development)"
                echo "  --registry URL     Docker registry URL (default: localhost:5000)"
                echo "  --skip-tests       Skip running tests"
                echo "  --skip-k8s         Skip Kubernetes deployment"
                echo "  --help             Show this help message"
                exit 0
                ;;
            *)
                error_exit "Unknown option: $1"
                ;;
        esac
    done
    
    main
fi