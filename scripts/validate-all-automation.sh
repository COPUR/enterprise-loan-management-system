#!/bin/bash
# Comprehensive automation validation script for Enterprise Banking System
# Validates all scripts, Docker builds, CI/CD pipeline, and deployment automation

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test results tracking
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
WARNINGS=0

# Function to print test results
log_test() {
    local status="$1"
    local message="$2"
    local details="${3:-}"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    case "$status" in
        "PASS")
            echo -e "${GREEN}✓ PASS${NC}: $message"
            PASSED_TESTS=$((PASSED_TESTS + 1))
            ;;
        "FAIL")
            echo -e "${RED}✗ FAIL${NC}: $message"
            if [[ -n "$details" ]]; then
                echo -e "${RED}    Details: $details${NC}"
            fi
            FAILED_TESTS=$((FAILED_TESTS + 1))
            ;;
        "WARN")
            echo -e "${YELLOW}⚠ WARN${NC}: $message"
            if [[ -n "$details" ]]; then
                echo -e "${YELLOW}    Details: $details${NC}"
            fi
            WARNINGS=$((WARNINGS + 1))
            ;;
        "INFO")
            echo -e "${BLUE}ℹ INFO${NC}: $message"
            ;;
    esac
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check script permissions and syntax
check_script() {
    local script_path="$1"
    local script_name=$(basename "$script_path")
    
    if [[ ! -f "$script_path" ]]; then
        log_test "FAIL" "Script not found: $script_name"
        return 1
    fi
    
    if [[ ! -x "$script_path" ]]; then
        log_test "WARN" "Script not executable: $script_name"
        chmod +x "$script_path" 2>/dev/null || log_test "FAIL" "Cannot make script executable: $script_name"
    else
        log_test "PASS" "Script executable: $script_name"
    fi
    
    # Basic syntax check
    if bash -n "$script_path" 2>/dev/null; then
        log_test "PASS" "Script syntax valid: $script_name"
    else
        log_test "FAIL" "Script syntax error: $script_name"
    fi
}

# Function to validate Gradle build
validate_gradle_build() {
    log_test "INFO" "Validating Gradle build system"
    
    if [[ ! -f "./gradlew" ]]; then
        log_test "FAIL" "Gradle wrapper not found"
        return 1
    fi
    
    if [[ ! -x "./gradlew" ]]; then
        log_test "WARN" "Gradle wrapper not executable"
        chmod +x ./gradlew
    fi
    
    # Test Gradle version
    if ./gradlew --version >/dev/null 2>&1; then
        local gradle_version=$(./gradlew --version | grep "Gradle" | head -1)
        log_test "PASS" "Gradle accessible: $gradle_version"
    else
        log_test "FAIL" "Gradle wrapper not working"
        return 1
    fi
    
    # Test basic build (without problematic tasks)
    log_test "INFO" "Testing Gradle build (this may take a few minutes)"
    if ./gradlew clean bootJar -x test -x cyclonedxBom -x copyContracts --no-daemon >/dev/null 2>&1; then
        log_test "PASS" "Gradle build successful"
        
        # Check if JAR was created
        if [[ -f "build/libs/enterprise-loan-management-system.jar" ]]; then
            local jar_size=$(stat -f%z "build/libs/enterprise-loan-management-system.jar" 2>/dev/null || stat -c%s "build/libs/enterprise-loan-management-system.jar" 2>/dev/null)
            log_test "PASS" "Application JAR created (${jar_size} bytes)"
        else
            log_test "FAIL" "Application JAR not found after build"
        fi
    else
        log_test "FAIL" "Gradle build failed"
    fi
}

# Function to validate Docker configuration
validate_docker() {
    log_test "INFO" "Validating Docker configuration"
    
    if ! command_exists docker; then
        log_test "FAIL" "Docker not installed"
        return 1
    fi
    
    if ! docker info >/dev/null 2>&1; then
        log_test "FAIL" "Docker daemon not running"
        return 1
    fi
    
    log_test "PASS" "Docker is running"
    
    # Check Dockerfile
    if [[ -f "Dockerfile" ]]; then
        log_test "PASS" "Dockerfile exists"
        
        # Basic Dockerfile validation
        if grep -q "FROM.*eclipse-temurin:21" Dockerfile; then
            log_test "PASS" "Dockerfile uses correct Java base image"
        else
            log_test "WARN" "Dockerfile may not use recommended Java base image"
        fi
        
        # Check for security best practices
        if grep -q "USER.*banking" Dockerfile; then
            log_test "PASS" "Dockerfile uses non-root user"
        else
            log_test "WARN" "Dockerfile may run as root user"
        fi
        
        # Check for health checks
        if grep -q "HEALTHCHECK" Dockerfile; then
            log_test "PASS" "Dockerfile includes health checks"
        else
            log_test "WARN" "Dockerfile missing health checks"
        fi
    else
        log_test "FAIL" "Dockerfile not found"
    fi
    
    # Check for required Docker scripts
    local docker_scripts=("docker/entrypoint.sh" "docker/healthcheck.sh" "docker/k8s-entrypoint.sh" "docker/k8s-healthcheck.sh" "docker/k8s-readiness.sh")
    for script in "${docker_scripts[@]}"; do
        if [[ -f "$script" ]]; then
            log_test "PASS" "Docker script exists: $(basename $script)"
        else
            log_test "WARN" "Docker script missing: $(basename $script)"
        fi
    done
}

# Function to validate CI/CD pipeline
validate_cicd() {
    log_test "INFO" "Validating CI/CD pipeline configuration"
    
    if [[ -f ".github/workflows/ci-cd-enterprise-banking.yml" ]]; then
        log_test "PASS" "GitHub Actions workflow exists"
        
        # Check for required jobs
        local required_jobs=("architecture-validation" "unit-testing" "security-compliance" "docker-build")
        for job in "${required_jobs[@]}"; do
            if grep -q "$job:" .github/workflows/ci-cd-enterprise-banking.yml; then
                log_test "PASS" "CI/CD job defined: $job"
            else
                log_test "FAIL" "CI/CD job missing: $job"
            fi
        done
        
        # Check for security scanning
        if grep -q "OWASP\|security\|trivy" .github/workflows/ci-cd-enterprise-banking.yml; then
            log_test "PASS" "Security scanning included in CI/CD"
        else
            log_test "WARN" "Security scanning may be missing from CI/CD"
        fi
        
        # Check for comprehensive testing
        if grep -q "load.*test\|performance.*test\|e2e.*test" .github/workflows/ci-cd-enterprise-banking.yml; then
            log_test "PASS" "Comprehensive testing included"
        else
            log_test "WARN" "Advanced testing may be missing"
        fi
    else
        log_test "FAIL" "GitHub Actions workflow not found"
    fi
}

# Function to validate Kubernetes manifests
validate_kubernetes() {
    log_test "INFO" "Validating Kubernetes deployment manifests"
    
    if command_exists kubectl; then
        log_test "PASS" "kubectl is available"
        
        # Check if manifests directory exists
        if [[ -d "k8s" ]]; then
            log_test "PASS" "Kubernetes manifests directory exists"
            
            # Validate YAML syntax for manifest files
            local yaml_files=$(find k8s -name "*.yaml" -o -name "*.yml" 2>/dev/null)
            if [[ -n "$yaml_files" ]]; then
                local yaml_count=$(echo "$yaml_files" | wc -l)
                log_test "PASS" "Found $yaml_count Kubernetes YAML files"
                
                # Try dry-run validation (if cluster is available)
                if kubectl cluster-info >/dev/null 2>&1; then
                    log_test "PASS" "Kubernetes cluster accessible"
                    
                    # Validate some key manifests
                    if [[ -d "k8s/manifests" ]]; then
                        if kubectl apply --dry-run=client -f k8s/manifests/ >/dev/null 2>&1; then
                            log_test "PASS" "Kubernetes manifests are valid"
                        else
                            log_test "WARN" "Some Kubernetes manifests may have issues"
                        fi
                    fi
                else
                    log_test "INFO" "No Kubernetes cluster available for validation"
                fi
            else
                log_test "WARN" "No Kubernetes YAML files found"
            fi
        else
            log_test "WARN" "Kubernetes manifests directory not found"
        fi
    else
        log_test "INFO" "kubectl not available - skipping Kubernetes validation"
    fi
}

# Function to validate Istio configuration
validate_istio() {
    log_test "INFO" "Validating Istio service mesh configuration"
    
    # Check for Istio manifests
    if [[ -d "k8s/istio" ]]; then
        log_test "PASS" "Istio configuration directory exists"
        
        local istio_files=("gateway.yaml" "security-policies.yaml" "observability.yaml")
        for file in "${istio_files[@]}"; do
            if [[ -f "k8s/istio/$file" ]]; then
                log_test "PASS" "Istio config exists: $file"
            else
                log_test "WARN" "Istio config missing: $file"
            fi
        done
    else
        log_test "WARN" "Istio configuration directory not found"
    fi
    
    # Check Istio setup script
    if [[ -f "scripts/setup-local-istio.sh" ]]; then
        log_test "PASS" "Istio setup script exists"
        check_script "scripts/setup-local-istio.sh"
    else
        log_test "WARN" "Istio setup script not found"
    fi
    
    # Check if Istio is running (if available)
    if command_exists kubectl && kubectl cluster-info >/dev/null 2>&1; then
        if kubectl get namespace istio-system >/dev/null 2>&1; then
            log_test "PASS" "Istio system namespace exists"
            
            local istio_pods=$(kubectl get pods -n istio-system --no-headers 2>/dev/null | wc -l)
            if [[ $istio_pods -gt 0 ]]; then
                log_test "PASS" "Istio pods running ($istio_pods pods)"
                
                # Check if port forwarding script works
                if [[ -f "port-forward-services.sh" ]] && [[ -x "port-forward-services.sh" ]]; then
                    log_test "PASS" "Port forwarding script available"
                else
                    log_test "WARN" "Port forwarding script not available or not executable"
                fi
            else
                log_test "INFO" "No Istio pods currently running"
            fi
        else
            log_test "INFO" "Istio not currently deployed"
        fi
    fi
}

# Function to validate essential scripts
validate_scripts() {
    log_test "INFO" "Validating essential automation scripts"
    
    # Check root-level scripts
    local root_scripts=(
        "dev-start.sh"
        "dev-test.sh" 
        "dev-reset-db.sh"
        "compile-and-run.sh"
        "run-simple.sh"
    )
    
    for script in "${root_scripts[@]}"; do
        if [[ -f "$script" ]]; then
            check_script "$script"
        else
            log_test "WARN" "Development script not found: $script"
        fi
    done
    
    # Check scripts directory
    if [[ -d "scripts" ]]; then
        log_test "PASS" "Scripts directory exists"
        
        local script_count=$(find scripts -name "*.sh" -type f 2>/dev/null | wc -l)
        log_test "INFO" "Found $script_count shell scripts in scripts/ directory"
        
        # Check key automation scripts
        local key_scripts=(
            "scripts/run-validation.sh"
            "scripts/hexagonal-architecture-validation.sh"
            "scripts/performance-benchmark.sh"
            "scripts/run-regression-tests.sh"
        )
        
        for script in "${key_scripts[@]}"; do
            if [[ -f "$script" ]]; then
                check_script "$script"
            else
                log_test "WARN" "Key automation script not found: $(basename $script)"
            fi
        done
    else
        log_test "WARN" "Scripts directory not found"
    fi
}

# Function to validate archived source comparison
validate_archived_sources() {
    log_test "INFO" "Validating archived source analysis"
    
    if [[ -d "archive" ]]; then
        log_test "PASS" "Archive directory exists"
        
        # Check for backup source code
        if [[ -d "archive/backup-code" ]]; then
            log_test "PASS" "Backup code directory exists"
            
            local backup_files=$(find archive/backup-code -type f 2>/dev/null | wc -l)
            log_test "INFO" "Found $backup_files files in backup archive"
        else
            log_test "WARN" "Backup code directory not found"
        fi
        
        # Check for logs
        if [[ -d "archive/logs" ]]; then
            log_test "PASS" "Archived logs directory exists"
        else
            log_test "INFO" "No archived logs directory"
        fi
    else
        log_test "INFO" "No archive directory found"
    fi
}

# Function to check build tool issues
validate_build_issues() {
    log_test "INFO" "Checking for known build issues"
    
    # Check for CycloneDX plugin issue
    if grep -q "cyclonedx" build.gradle 2>/dev/null; then
        log_test "WARN" "CycloneDX plugin detected - may cause build issues"
        log_test "INFO" "Recommendation: Use -x cyclonedxBom to exclude problematic task"
    fi
    
    # Check for Spring Cloud Contract issue
    if grep -q "spring-cloud-contract" build.gradle 2>/dev/null; then
        log_test "WARN" "Spring Cloud Contract detected - may require contract files"
        log_test "INFO" "Recommendation: Use -x copyContracts if no contracts are defined"
    fi
    
    # Check Gradle wrapper version
    if [[ -f "gradle/wrapper/gradle-wrapper.properties" ]]; then
        local gradle_version=$(grep "distributionUrl" gradle/wrapper/gradle-wrapper.properties | sed 's/.*gradle-\([0-9.]*\)-.*/\1/')
        if [[ -n "$gradle_version" ]]; then
            log_test "PASS" "Gradle wrapper version: $gradle_version"
            
            # Check if version supports current Java
            local java_version=$(java -version 2>&1 | grep -o '"[0-9.]*"' | head -1 | tr -d '"' | cut -d. -f1)
            if [[ "$java_version" -ge 21 ]] && [[ "${gradle_version%%.*}" -ge 8 ]]; then
                log_test "PASS" "Gradle version compatible with Java $java_version"
            else
                log_test "WARN" "Gradle version may not be compatible with Java $java_version"
            fi
        fi
    fi
}

# Main validation function
main() {
    echo -e "${BLUE}🏦 Enterprise Banking System - Automation Validation${NC}"
    echo -e "${BLUE}==========================================================${NC}"
    echo ""
    
    # Run all validations
    validate_scripts
    validate_gradle_build
    validate_docker
    validate_cicd
    validate_kubernetes
    validate_istio
    validate_archived_sources
    validate_build_issues
    
    echo ""
    echo -e "${BLUE}==========================================================${NC}"
    echo -e "${BLUE}📊 VALIDATION SUMMARY${NC}"
    echo -e "${BLUE}==========================================================${NC}"
    echo -e "Total Tests: $TOTAL_TESTS"
    echo -e "${GREEN}Passed: $PASSED_TESTS${NC}"
    echo -e "${RED}Failed: $FAILED_TESTS${NC}"
    echo -e "${YELLOW}Warnings: $WARNINGS${NC}"
    
    local success_rate=$((PASSED_TESTS * 100 / TOTAL_TESTS))
    echo -e "Success Rate: ${success_rate}%"
    
    echo ""
    if [[ $FAILED_TESTS -eq 0 ]]; then
        echo -e "${GREEN}✅ All critical validations passed!${NC}"
        if [[ $WARNINGS -gt 0 ]]; then
            echo -e "${YELLOW}⚠️  Please review warnings for potential improvements${NC}"
        fi
        exit 0
    else
        echo -e "${RED}❌ Some validations failed. Please review and fix the issues.${NC}"
        exit 1
    fi
}

# Execute main function
main "$@"