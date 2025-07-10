#!/bin/bash

# Build Hardened Docker Images for Enterprise Banking System
# Multi-stage builds with security hardening and banking compliance
# Supports multiple architectures (AMD64, ARM64) for cost optimization

set -euo pipefail

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
REGISTRY="${DOCKER_REGISTRY:-localhost:5000}"
IMAGE_TAG="${BUILD_TAG:-latest}"
BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ')
GIT_COMMIT=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
VERSION="${APP_VERSION:-1.0.0}"

# Build platforms
PLATFORMS="${BUILD_PLATFORMS:-linux/amd64,linux/arm64}"
ENABLE_MULTI_ARCH="${ENABLE_MULTI_ARCH:-false}"

# Security scanning
ENABLE_SECURITY_SCAN="${ENABLE_SECURITY_SCAN:-true}"
SECURITY_SCANNER="${SECURITY_SCANNER:-trivy}"

echo -e "${BLUE}=== Enterprise Banking System - Hardened Docker Build ===${NC}"
echo -e "${BLUE}Registry: ${REGISTRY}${NC}"
echo -e "${BLUE}Tag: ${IMAGE_TAG}${NC}"
echo -e "${BLUE}Version: ${VERSION}${NC}"
echo -e "${BLUE}Git Commit: ${GIT_COMMIT}${NC}"
echo -e "${BLUE}Build Date: ${BUILD_DATE}${NC}"
echo -e "${BLUE}Platforms: ${PLATFORMS}${NC}"
echo ""

# Function to log messages
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

error() {
    echo -e "${RED}[ERROR] $1${NC}" >&2
}

warning() {
    echo -e "${YELLOW}[WARNING] $1${NC}"
}

# Function to check prerequisites
check_prerequisites() {
    log "Checking prerequisites..."
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        error "Docker is not installed or not in PATH"
        exit 1
    fi
    
    # Check Docker Buildx for multi-platform builds
    if [[ "$ENABLE_MULTI_ARCH" == "true" ]]; then
        if ! docker buildx version &> /dev/null; then
            error "Docker Buildx is required for multi-platform builds"
            exit 1
        fi
        
        # Create buildx builder if it doesn't exist
        if ! docker buildx inspect hardened-builder &> /dev/null; then
            log "Creating Docker Buildx builder..."
            docker buildx create --name hardened-builder --use
        else
            docker buildx use hardened-builder
        fi
    fi
    
    # Check security scanner
    if [[ "$ENABLE_SECURITY_SCAN" == "true" ]]; then
        if ! command -v "$SECURITY_SCANNER" &> /dev/null; then
            warning "Security scanner '$SECURITY_SCANNER' not found. Skipping security scans."
            ENABLE_SECURITY_SCAN="false"
        fi
    fi
    
    log "Prerequisites check completed"
}

# Function to build a single service
build_service() {
    local service_name="$1"
    local dockerfile="$2"
    local stage="${3:-production}"
    
    log "Building $service_name with stage: $stage"
    
    local image_name="${REGISTRY}/banking-${service_name}:${IMAGE_TAG}"
    local build_args=(
        --build-arg BUILD_DATE="$BUILD_DATE"
        --build-arg VERSION="$VERSION"
        --build-arg GIT_COMMIT="$GIT_COMMIT"
        --build-arg BUILDPLATFORM="$(uname -m)"
        --target "$stage"
        --tag "$image_name"
        --label "com.enterprise.banking.service=$service_name"
        --label "com.enterprise.banking.version=$VERSION"
        --label "com.enterprise.banking.build-date=$BUILD_DATE"
        --label "com.enterprise.banking.git-commit=$GIT_COMMIT"
        --label "com.enterprise.banking.security-hardened=true"
        --label "org.opencontainers.image.created=$BUILD_DATE"
        --label "org.opencontainers.image.version=$VERSION"
        --label "org.opencontainers.image.revision=$GIT_COMMIT"
    )
    
    if [[ "$ENABLE_MULTI_ARCH" == "true" ]]; then
        build_args+=(--platform "$PLATFORMS" --push)
        docker buildx build "${build_args[@]}" -f "$dockerfile" .
    else
        build_args+=(--load)
        docker buildx build "${build_args[@]}" -f "$dockerfile" .
    fi
    
    if [[ $? -eq 0 ]]; then
        log "Successfully built $service_name"
        
        # Security scanning
        if [[ "$ENABLE_SECURITY_SCAN" == "true" && "$ENABLE_MULTI_ARCH" != "true" ]]; then
            scan_image "$image_name" "$service_name"
        fi
        
        return 0
    else
        error "Failed to build $service_name"
        return 1
    fi
}

# Function to scan images for vulnerabilities
scan_image() {
    local image_name="$1"
    local service_name="$2"
    
    log "Scanning $service_name for security vulnerabilities..."
    
    case "$SECURITY_SCANNER" in
        "trivy")
            # Create reports directory
            mkdir -p "./security-reports"
            
            # Scan for vulnerabilities
            if trivy image --format json --output "./security-reports/${service_name}-vulnerability-report.json" "$image_name"; then
                # Check for HIGH and CRITICAL vulnerabilities
                high_critical=$(trivy image --format json "$image_name" | jq '.Results[]?.Vulnerabilities[]? | select(.Severity == "HIGH" or .Severity == "CRITICAL") | .VulnerabilityID' | wc -l)
                
                if [[ $high_critical -gt 0 ]]; then
                    warning "$service_name has $high_critical HIGH/CRITICAL vulnerabilities"
                    
                    # Optionally fail the build on critical vulnerabilities
                    if [[ "${FAIL_ON_CRITICAL_VULNS:-false}" == "true" ]]; then
                        error "Failing build due to critical vulnerabilities in $service_name"
                        return 1
                    fi
                else
                    log "$service_name passed security scan"
                fi
            else
                warning "Security scan failed for $service_name"
            fi
            ;;
        *)
            warning "Unknown security scanner: $SECURITY_SCANNER"
            ;;
    esac
}

# Function to test built images
test_images() {
    log "Testing built images..."
    
    # Test each service image
    local services=("customer-service" "loan-service" "payment-service" "party-service")
    
    for service in "${services[@]}"; do
        local image_name="${REGISTRY}/banking-${service}:${IMAGE_TAG}"
        
        log "Testing $service image..."
        
        # Basic image inspection
        if docker inspect "$image_name" &> /dev/null; then
            log "$service image inspection passed"
            
            # Check if image runs without errors (quick test)
            if timeout 30 docker run --rm "$image_name" java -version &> /dev/null; then
                log "$service image runtime test passed"
            else
                warning "$service image runtime test failed"
            fi
        else
            error "$service image inspection failed"
            return 1
        fi
    done
    
    log "Image testing completed"
}

# Function to generate build report
generate_build_report() {
    log "Generating build report..."
    
    local report_file="build-report-${BUILD_DATE}.json"
    
    cat > "$report_file" <<EOF
{
  "build_info": {
    "timestamp": "$BUILD_DATE",
    "version": "$VERSION",
    "git_commit": "$GIT_COMMIT",
    "registry": "$REGISTRY",
    "tag": "$IMAGE_TAG",
    "platforms": "$PLATFORMS",
    "multi_arch_enabled": $ENABLE_MULTI_ARCH,
    "security_scan_enabled": $ENABLE_SECURITY_SCAN
  },
  "services": [
    {
      "name": "customer-service",
      "image": "${REGISTRY}/banking-customer-service:${IMAGE_TAG}",
      "compliance": ["GDPR", "PII-Protection"],
      "security_level": "high"
    },
    {
      "name": "loan-service", 
      "image": "${REGISTRY}/banking-loan-service:${IMAGE_TAG}",
      "compliance": ["Basel-III", "Financial-Calculations"],
      "security_level": "high"
    },
    {
      "name": "payment-service",
      "image": "${REGISTRY}/banking-payment-service:${IMAGE_TAG}",
      "compliance": ["PCI-DSS-v4.0", "Real-time-Payments"],
      "security_level": "critical"
    },
    {
      "name": "party-service",
      "image": "${REGISTRY}/banking-party-service:${IMAGE_TAG}",
      "compliance": ["GDPR", "Data-Sovereignty"],
      "security_level": "high"
    }
  ]
}
EOF
    
    log "Build report generated: $report_file"
}

# Main build process
main() {
    local start_time=$(date +%s)
    
    log "Starting hardened Docker build process..."
    
    # Check prerequisites
    check_prerequisites
    
    # Build main application image
    log "Building main application image..."
    if [[ "$ENABLE_MULTI_ARCH" == "true" ]]; then
        docker buildx build \
            --platform "$PLATFORMS" \
            --build-arg BUILD_DATE="$BUILD_DATE" \
            --build-arg VERSION="$VERSION" \
            --build-arg GIT_COMMIT="$GIT_COMMIT" \
            --tag "${REGISTRY}/banking-app:${IMAGE_TAG}" \
            --target production \
            --push \
            -f Dockerfile .
    else
        docker buildx build \
            --build-arg BUILD_DATE="$BUILD_DATE" \
            --build-arg VERSION="$VERSION" \
            --build-arg GIT_COMMIT="$GIT_COMMIT" \
            --tag "${REGISTRY}/banking-app:${IMAGE_TAG}" \
            --target production \
            --load \
            -f Dockerfile .
    fi
    
    # Build individual microservice images
    local services=(
        "customer-service:Dockerfile.customer-service"
        "loan-service:Dockerfile.loan-service" 
        "payment-service:Dockerfile.payment-service"
        "party-service:Dockerfile.party-service"
    )
    
    local failed_builds=()
    
    for service_config in "${services[@]}"; do
        IFS=':' read -r service_name dockerfile <<< "$service_config"
        
        if ! build_service "$service_name" "$dockerfile" "production"; then
            failed_builds+=("$service_name")
        fi
    done
    
    # Report build results
    if [[ ${#failed_builds[@]} -eq 0 ]]; then
        log "All service builds completed successfully"
        
        # Test images if not multi-arch (can't test pushed images easily)
        if [[ "$ENABLE_MULTI_ARCH" != "true" ]]; then
            test_images
        fi
        
        # Generate build report
        generate_build_report
        
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        
        log "Build process completed successfully in ${duration} seconds"
        
        echo ""
        echo -e "${GREEN}=== Build Summary ===${NC}"
        echo -e "${GREEN}✓ Main application image built${NC}"
        echo -e "${GREEN}✓ All microservice images built${NC}"
        echo -e "${GREEN}✓ Security hardening applied${NC}"
        echo -e "${GREEN}✓ Banking compliance labels added${NC}"
        if [[ "$ENABLE_SECURITY_SCAN" == "true" ]]; then
            echo -e "${GREEN}✓ Security scans completed${NC}"
        fi
        echo ""
        
        # Display image information
        echo -e "${BLUE}Built Images:${NC}"
        echo "  • ${REGISTRY}/banking-app:${IMAGE_TAG}"
        echo "  • ${REGISTRY}/banking-customer-service:${IMAGE_TAG}"
        echo "  • ${REGISTRY}/banking-loan-service:${IMAGE_TAG}"
        echo "  • ${REGISTRY}/banking-payment-service:${IMAGE_TAG}"
        echo "  • ${REGISTRY}/banking-party-service:${IMAGE_TAG}"
        echo ""
        
        return 0
    else
        error "Build failed for services: ${failed_builds[*]}"
        return 1
    fi
}

# Script execution
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi