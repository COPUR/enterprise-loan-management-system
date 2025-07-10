#!/bin/bash

# CI/CD Pipeline Validation Script
# Validates all pipeline components without requiring full Docker/K8s setup

set -euo pipefail

# Color codes for output
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m' # No Color

# Configuration
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
readonly TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# Logging functions
log() {
    local level="$1"
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    case "$level" in
        "INFO")  echo -e "${GREEN}[INFO]${NC} ${timestamp} - $message" ;;
        "WARN")  echo -e "${YELLOW}[WARN]${NC} ${timestamp} - $message" ;;
        "ERROR") echo -e "${RED}[ERROR]${NC} ${timestamp} - $message" ;;
        "SUCCESS") echo -e "${GREEN}[SUCCESS]${NC} ${timestamp} - $message" ;;
    esac
}

# Validation functions
validate_gradle_build() {
    log "INFO" "Validating Gradle build configuration..."
    
    if [ ! -f "$PROJECT_ROOT/build.gradle" ]; then
        log "ERROR" "build.gradle not found"
        return 1
    fi
    
    if [ ! -f "$PROJECT_ROOT/gradlew" ]; then
        log "ERROR" "Gradle wrapper not found"
        return 1
    fi
    
    # Check if gradlew is executable
    if [ ! -x "$PROJECT_ROOT/gradlew" ]; then
        log "WARN" "Making gradlew executable"
        chmod +x "$PROJECT_ROOT/gradlew"
    fi
    
    # Test Gradle version
    cd "$PROJECT_ROOT"
    if GRADLE_USER_HOME=/tmp/gradle ./gradlew --version --no-daemon &>/dev/null; then
        log "SUCCESS" "Gradle wrapper is functional"
    else
        log "ERROR" "Gradle wrapper test failed"
        return 1
    fi
    
    # Validate build.gradle syntax
    if GRADLE_USER_HOME=/tmp/gradle ./gradlew help --no-daemon &>/dev/null; then
        log "SUCCESS" "Gradle build configuration is valid"
    else
        log "ERROR" "Gradle build configuration has issues"
        return 1
    fi
    
    return 0
}

validate_java_source() {
    log "INFO" "Validating Java source structure..."
    
    local main_app_found=false
    local required_dirs=("src/main/java" "src/test/java" "src/main/resources")
    
    for dir in "${required_dirs[@]}"; do
        if [ ! -d "$PROJECT_ROOT/$dir" ]; then
            log "ERROR" "Required directory $dir not found"
            return 1
        fi
    done
    
    # Check for main application class
    if find "$PROJECT_ROOT/src/main/java" -name "*Application.java" | grep -q .; then
        log "SUCCESS" "Spring Boot application classes found"
        main_app_found=true
    fi
    
    if [ "$main_app_found" = false ]; then
        log "ERROR" "No Spring Boot application class found"
        return 1
    fi
    
    # Check for basic structure
    local java_files_count
    java_files_count=$(find "$PROJECT_ROOT/src/main/java" -name "*.java" | wc -l)
    
    if [ "$java_files_count" -gt 0 ]; then
        log "SUCCESS" "Found $java_files_count Java source files"
    else
        log "ERROR" "No Java source files found"
        return 1
    fi
    
    return 0
}

validate_docker_config() {
    log "INFO" "Validating Docker configuration..."
    
    if [ ! -f "$PROJECT_ROOT/Dockerfile" ]; then
        log "ERROR" "Dockerfile not found"
        return 1
    fi
    
    # Check Dockerfile syntax
    if grep -q "FROM" "$PROJECT_ROOT/Dockerfile"; then
        log "SUCCESS" "Dockerfile has valid FROM instruction"
    else
        log "ERROR" "Dockerfile is missing FROM instruction"
        return 1
    fi
    
    # Check for multi-stage build
    if grep -q "AS builder\|AS runtime\|AS testing" "$PROJECT_ROOT/Dockerfile"; then
        log "SUCCESS" "Dockerfile uses multi-stage build"
    else
        log "WARN" "Dockerfile doesn't use multi-stage build"
    fi
    
    # Check Docker Compose files
    local compose_files_found=0
    for compose_file in docker/compose/*.yml; do
        if [ -f "$compose_file" ]; then
            if docker-compose -f "$compose_file" config --quiet 2>/dev/null; then
                log "SUCCESS" "Docker Compose file valid: $(basename "$compose_file")"
                ((compose_files_found++))
            else
                log "ERROR" "Docker Compose file invalid: $(basename "$compose_file")"
                return 1
            fi
        fi
    done
    
    if [ $compose_files_found -gt 0 ]; then
        log "SUCCESS" "Found $compose_files_found valid Docker Compose files"
    else
        log "ERROR" "No valid Docker Compose files found"
        return 1
    fi
    
    return 0
}

validate_kubernetes_config() {
    log "INFO" "Validating Kubernetes configuration..."
    
    if [ ! -d "$PROJECT_ROOT/k8s" ]; then
        log "ERROR" "k8s directory not found"
        return 1
    fi
    
    # Check if kubectl is available
    if command -v kubectl &> /dev/null; then
        # Validate key manifests
        local manifests=("k8s/manifests/namespace.yaml" "k8s/manifests/deployment.yaml" "k8s/manifests/service.yaml")
        
        for manifest in "${manifests[@]}"; do
            if [ -f "$PROJECT_ROOT/$manifest" ]; then
                if kubectl --dry-run=client apply -f "$PROJECT_ROOT/$manifest" &>/dev/null; then
                    log "SUCCESS" "Kubernetes manifest valid: $(basename "$manifest")"
                else
                    log "ERROR" "Kubernetes manifest invalid: $(basename "$manifest")"
                    return 1
                fi
            else
                log "WARN" "Kubernetes manifest not found: $(basename "$manifest")"
            fi
        done
        
        # Check Helm charts
        if [ -d "$PROJECT_ROOT/k8s/helm-charts" ]; then
            if command -v helm &> /dev/null; then
                for chart_dir in "$PROJECT_ROOT/k8s/helm-charts"/*; do
                    if [ -d "$chart_dir" ] && [ -f "$chart_dir/Chart.yaml" ]; then
                        if helm lint "$chart_dir" &>/dev/null; then
                            log "SUCCESS" "Helm chart valid: $(basename "$chart_dir")"
                        else
                            log "ERROR" "Helm chart invalid: $(basename "$chart_dir")"
                            return 1
                        fi
                    fi
                done
            else
                log "WARN" "Helm not available for chart validation"
            fi
        fi
        
    else
        log "WARN" "kubectl not available for Kubernetes validation"
    fi
    
    return 0
}

validate_github_workflows() {
    log "INFO" "Validating GitHub Actions workflows..."
    
    if [ ! -d "$PROJECT_ROOT/.github/workflows" ]; then
        log "ERROR" ".github/workflows directory not found"
        return 1
    fi
    
    local workflow_found=false
    for workflow in "$PROJECT_ROOT/.github/workflows"/*.yml "$PROJECT_ROOT/.github/workflows"/*.yaml; do
        if [ -f "$workflow" ]; then
            # Basic YAML syntax validation
            if grep -q "on:\|jobs:" "$workflow"; then
                log "SUCCESS" "GitHub workflow valid: $(basename "$workflow")"
                workflow_found=true
            else
                log "ERROR" "GitHub workflow invalid: $(basename "$workflow")"
                return 1
            fi
        fi
    done
    
    if [ "$workflow_found" = false ]; then
        log "ERROR" "No GitHub workflows found"
        return 1
    fi
    
    return 0
}

validate_scripts() {
    log "INFO" "Validating deployment scripts..."
    
    if [ ! -d "$PROJECT_ROOT/scripts" ]; then
        log "ERROR" "scripts directory not found"
        return 1
    fi
    
    # Check for essential scripts
    local essential_scripts=("scripts/deploy-e2e.sh")
    
    for script in "${essential_scripts[@]}"; do
        if [ -f "$PROJECT_ROOT/$script" ]; then
            if bash -n "$PROJECT_ROOT/$script" 2>/dev/null; then
                log "SUCCESS" "Script syntax valid: $(basename "$script")"
            else
                log "ERROR" "Script syntax invalid: $(basename "$script")"
                return 1
            fi
        else
            log "WARN" "Essential script not found: $(basename "$script")"
        fi
    done
    
    return 0
}

validate_security_config() {
    log "INFO" "Validating security configuration..."
    
    # Check for security-related files
    local security_files=(".gitignore" "k8s/istio/security-policies.yaml")
    
    for file in "${security_files[@]}"; do
        if [ -f "$PROJECT_ROOT/$file" ]; then
            log "SUCCESS" "Security file found: $(basename "$file")"
        else
            log "WARN" "Security file not found: $(basename "$file")"
        fi
    done
    
    # Check for hardcoded secrets (basic check) - exclude configuration classes
    if grep -r -i "password\s*=" "$PROJECT_ROOT/src" --include="*.java" 2>/dev/null | grep -v -E "(test|example|sample|Properties|Config|configuration)" | head -1; then
        log "ERROR" "Potential hardcoded secrets found in source code"
        return 1
    else
        log "SUCCESS" "No obvious hardcoded secrets found"
    fi
    
    return 0
}

# Main validation function
main() {
    log "INFO" "Starting CI/CD Pipeline Validation"
    log "INFO" "======================================"
    
    local validation_results=()
    local overall_success=true
    
    # Run all validations
    local validations=(
        "validate_gradle_build"
        "validate_java_source"
        "validate_docker_config"
        "validate_kubernetes_config"
        "validate_github_workflows"
        "validate_scripts"
        "validate_security_config"
    )
    
    for validation in "${validations[@]}"; do
        log "INFO" "Running $validation..."
        if $validation; then
            validation_results+=("✅ $validation: PASSED")
        else
            validation_results+=("❌ $validation: FAILED")
            overall_success=false
        fi
        echo
    done
    
    # Print summary
    log "INFO" "Validation Summary"
    log "INFO" "=================="
    
    for result in "${validation_results[@]}"; do
        echo "$result"
    done
    
    echo
    if [ "$overall_success" = true ]; then
        log "SUCCESS" "All CI/CD pipeline validations PASSED!"
        log "INFO" "The CI/CD pipeline is ready for execution."
        exit 0
    else
        log "ERROR" "Some CI/CD pipeline validations FAILED!"
        log "INFO" "Please fix the issues before running the pipeline."
        exit 1
    fi
}

# Execute main function
main "$@"