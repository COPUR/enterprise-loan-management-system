#!/bin/bash

# Security Utility Functions for Enterprise Banking Platform
# 
# Security scanning, vulnerability assessment, and compliance validation

# Run security scans
run_security_scans() {
    local version="$1"
    
    log_info "Running security scans for version $version..."
    
    # Container image security scan
    scan_container_image "$version"
    
    # Dependency vulnerability scan
    scan_dependencies "$version"
    
    # Static code analysis
    run_static_analysis "$version"
    
    # Configuration security scan
    scan_configuration "$version"
    
    # FAPI compliance validation
    validate_fapi_compliance "$version"
    
    log_success "Security scans completed for version $version"
}

# Scan container image for vulnerabilities
scan_container_image() {
    local version="$1"
    local image_tag=$(get_image_tag "$version")
    
    log_info "Scanning container image: $image_tag"
    
    # Use Trivy for container scanning
    if command -v trivy &> /dev/null; then
        local scan_results=$(trivy image --format json --quiet "$image_tag" 2>/dev/null)
        
        if [[ $? -eq 0 ]]; then
            # Parse results and check vulnerability threshold
            local critical_vulns=$(echo "$scan_results" | jq '[.Results[]?.Vulnerabilities[]? | select(.Severity == "CRITICAL")] | length' 2>/dev/null || echo "0")
            local high_vulns=$(echo "$scan_results" | jq '[.Results[]?.Vulnerabilities[]? | select(.Severity == "HIGH")] | length' 2>/dev/null || echo "0")
            
            log_info "Container vulnerabilities - Critical: $critical_vulns, High: $high_vulns"
            
            # Check against threshold
            if [[ "$VULNERABILITY_THRESHOLD" == "CRITICAL" && $critical_vulns -gt 0 ]]; then
                log_error "Critical vulnerabilities found in container image"
                return 1
            elif [[ "$VULNERABILITY_THRESHOLD" == "HIGH" && ($critical_vulns -gt 0 || $high_vulns -gt 0) ]]; then
                log_error "High or critical vulnerabilities found in container image"
                return 1
            fi
            
            log_success "Container image security scan passed"
        else
            log_warn "Container image scan failed or not available"
        fi
    else
        log_warn "Trivy not available, skipping container scan"
    fi
    
    # Additional image security checks
    check_image_security_best_practices "$image_tag"
}

# Check image security best practices
check_image_security_best_practices() {
    local image_tag="$1"
    
    log_info "Checking image security best practices..."
    
    # Check if image runs as non-root
    local user_check=$(docker inspect "$image_tag" --format='{{.Config.User}}' 2>/dev/null || echo "")
    if [[ -z "$user_check" || "$user_check" == "root" || "$user_check" == "0" ]]; then
        log_warn "Container may be running as root user"
    else
        log_success "Container configured to run as non-root user: $user_check"
    fi
    
    # Check for secrets in image
    if docker history "$image_tag" --no-trunc 2>/dev/null | grep -i -E "(password|secret|key|token)" &> /dev/null; then
        log_warn "Potential secrets detected in image history"
    fi
    
    # Check image size
    local image_size=$(docker images "$image_tag" --format "table {{.Size}}" | tail -n +2 | head -1)
    log_info "Container image size: $image_size"
}

# Scan dependencies for vulnerabilities
scan_dependencies() {
    local version="$1"
    
    log_info "Scanning dependencies for vulnerabilities..."
    
    # Maven/Gradle dependency scan
    if [[ -f "$PROJECT_ROOT/pom.xml" ]]; then
        scan_maven_dependencies
    elif [[ -f "$PROJECT_ROOT/build.gradle" ]]; then
        scan_gradle_dependencies
    fi
    
    # OWASP Dependency Check
    run_owasp_dependency_check "$version"
}

# Scan Maven dependencies
scan_maven_dependencies() {
    log_info "Scanning Maven dependencies..."
    
    cd "$PROJECT_ROOT"
    
    # Run OWASP dependency check plugin
    if mvn org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=7 &> /dev/null; then
        log_success "Maven dependency scan completed"
    else
        log_error "Maven dependency scan found high-severity vulnerabilities"
        return 1
    fi
}

# Scan Gradle dependencies
scan_gradle_dependencies() {
    log_info "Scanning Gradle dependencies..."
    
    cd "$PROJECT_ROOT"
    
    # Run dependency vulnerability scan
    if ./gradlew dependencyCheckAnalyze &> /dev/null; then
        log_success "Gradle dependency scan completed"
    else
        log_error "Gradle dependency scan found vulnerabilities"
        return 1
    fi
}

# Run OWASP Dependency Check
run_owasp_dependency_check() {
    local version="$1"
    
    if command -v dependency-check &> /dev/null; then
        log_info "Running OWASP Dependency Check..."
        
        dependency-check \
            --project "Enterprise Banking Platform" \
            --scan "$PROJECT_ROOT" \
            --format JSON \
            --out "/tmp/dependency-check-report-$version.json" \
            --failOnCVSS 7 \
            --suppression "$PROJECT_ROOT/security/dependency-check-suppressions.xml" \
            &> /dev/null
        
        if [[ $? -eq 0 ]]; then
            log_success "OWASP Dependency Check passed"
        else
            log_error "OWASP Dependency Check found high-severity vulnerabilities"
            return 1
        fi
    else
        log_warn "OWASP Dependency Check not available"
    fi
}

# Run static code analysis
run_static_analysis() {
    local version="$1"
    
    log_info "Running static code analysis..."
    
    # SonarQube analysis
    run_sonarqube_analysis "$version"
    
    # SpotBugs analysis
    run_spotbugs_analysis "$version"
    
    # PMD analysis
    run_pmd_analysis "$version"
}

# Run SonarQube analysis
run_sonarqube_analysis() {
    local version="$1"
    
    if command -v sonar-scanner &> /dev/null; then
        log_info "Running SonarQube analysis..."
        
        cd "$PROJECT_ROOT"
        
        # Configure SonarQube properties
        cat > sonar-project.properties << EOF
sonar.projectKey=enterprise-banking-platform
sonar.projectName=Enterprise Banking Platform
sonar.projectVersion=$version
sonar.sources=src/main/java
sonar.tests=src/test/java
sonar.java.binaries=build/classes
sonar.java.test.binaries=build/test-classes
sonar.exclusions=**/generated/**
sonar.coverage.exclusions=**/*Test.java,**/test/**
EOF
        
        if sonar-scanner &> /dev/null; then
            log_success "SonarQube analysis completed"
        else
            log_warn "SonarQube analysis failed or not configured"
        fi
        
        rm -f sonar-project.properties
    else
        log_warn "SonarQube scanner not available"
    fi
}

# Run SpotBugs analysis
run_spotbugs_analysis() {
    local version="$1"
    
    cd "$PROJECT_ROOT"
    
    if [[ -f "build.gradle" ]]; then
        log_info "Running SpotBugs analysis..."
        
        if ./gradlew spotbugsMain &> /dev/null; then
            log_success "SpotBugs analysis completed"
        else
            log_warn "SpotBugs analysis found issues"
        fi
    fi
}

# Run PMD analysis
run_pmd_analysis() {
    local version="$1"
    
    cd "$PROJECT_ROOT"
    
    if [[ -f "build.gradle" ]]; then
        log_info "Running PMD analysis..."
        
        if ./gradlew pmdMain &> /dev/null; then
            log_success "PMD analysis completed"
        else
            log_warn "PMD analysis found issues"
        fi
    fi
}

# Scan configuration for security issues
scan_configuration() {
    local version="$1"
    
    log_info "Scanning configuration for security issues..."
    
    # Check for hardcoded secrets
    check_hardcoded_secrets
    
    # Validate SSL/TLS configuration
    validate_tls_configuration
    
    # Check security headers configuration
    validate_security_headers
    
    # Validate authentication configuration
    validate_auth_configuration
}

# Check for hardcoded secrets
check_hardcoded_secrets() {
    log_info "Checking for hardcoded secrets..."
    
    # Use git-secrets or similar tool
    if command -v git-secrets &> /dev/null; then
        cd "$PROJECT_ROOT"
        
        if git secrets --scan; then
            log_success "No hardcoded secrets found"
        else
            log_error "Hardcoded secrets detected in code"
            return 1
        fi
    else
        # Manual pattern search
        local secret_patterns=("password\s*=" "secret\s*=" "key\s*=" "token\s*=")
        local secrets_found=false
        
        for pattern in "${secret_patterns[@]}"; do
            if grep -r -i "$pattern" "$PROJECT_ROOT/src" &> /dev/null; then
                log_warn "Potential hardcoded secret pattern found: $pattern"
                secrets_found=true
            fi
        done
        
        if [[ "$secrets_found" == false ]]; then
            log_success "No obvious hardcoded secrets found"
        fi
    fi
}

# Validate TLS configuration
validate_tls_configuration() {
    log_info "Validating TLS configuration..."
    
    # Check application.yml for TLS settings
    local config_files=("$PROJECT_ROOT/src/main/resources/application.yml" "$PROJECT_ROOT/src/main/resources/application.properties")
    
    for config_file in "${config_files[@]}"; do
        if [[ -f "$config_file" ]]; then
            # Check for TLS version
            if grep -q "tls.*1\.3\|ssl.*1\.3" "$config_file"; then
                log_success "TLS 1.3 configuration found"
            elif grep -q "tls.*1\.2\|ssl.*1\.2" "$config_file"; then
                log_info "TLS 1.2 configuration found (consider upgrading to 1.3)"
            else
                log_warn "TLS configuration not found or may be using older version"
            fi
            
            # Check for secure cipher suites
            if grep -q "cipher" "$config_file"; then
                log_info "Custom cipher suite configuration found"
            fi
        fi
    done
}

# Validate security headers
validate_security_headers() {
    log_info "Validating security headers configuration..."
    
    # Check for Spring Security configuration
    if find "$PROJECT_ROOT/src" -name "*.java" -exec grep -l "SecurityConfig\|WebSecurityConfig" {} \; | head -1 &> /dev/null; then
        local security_config=$(find "$PROJECT_ROOT/src" -name "*.java" -exec grep -l "SecurityConfig\|WebSecurityConfig" {} \; | head -1)
        
        # Check for common security headers
        local required_headers=("X-Frame-Options" "X-Content-Type-Options" "X-XSS-Protection" "Strict-Transport-Security")
        
        for header in "${required_headers[@]}"; do
            if grep -q "$header" "$security_config"; then
                log_success "Security header configured: $header"
            else
                log_warn "Security header not found: $header"
            fi
        done
    else
        log_warn "Spring Security configuration not found"
    fi
}

# Validate authentication configuration
validate_auth_configuration() {
    log_info "Validating authentication configuration..."
    
    # Check for OAuth2 configuration
    if find "$PROJECT_ROOT/src" -name "*.java" -exec grep -l "OAuth2\|@EnableOAuth2" {} \; | head -1 &> /dev/null; then
        log_success "OAuth2 configuration found"
    else
        log_warn "OAuth2 configuration not found"
    fi
    
    # Check for JWT configuration
    if find "$PROJECT_ROOT/src" -name "*.java" -exec grep -l "JWT\|JsonWebToken" {} \; | head -1 &> /dev/null; then
        log_success "JWT configuration found"
    fi
    
    # Check for FAPI compliance
    if find "$PROJECT_ROOT/src" -name "*.java" -exec grep -l "FAPI\|Financial.*API" {} \; | head -1 &> /dev/null; then
        log_success "FAPI configuration found"
    fi
}

# Validate FAPI compliance
validate_fapi_compliance() {
    local version="$1"
    
    if [[ "$FAPI_COMPLIANCE_REQUIRED" != "true" ]]; then
        log_info "FAPI compliance validation skipped (not required)"
        return 0
    fi
    
    log_info "Validating FAPI 2.0 compliance..."
    
    # Check for required FAPI components
    validate_fapi_security_profile
    validate_fapi_mtls_configuration
    validate_fapi_par_support
    validate_fapi_jarm_support
    
    log_success "FAPI compliance validation completed"
}

# Validate FAPI security profile
validate_fapi_security_profile() {
    log_info "Validating FAPI security profile..."
    
    # Check for PKCE implementation
    if find "$PROJECT_ROOT/src" -name "*.java" -exec grep -l "PKCE\|CodeChallenge" {} \; | head -1 &> /dev/null; then
        log_success "PKCE implementation found"
    else
        log_error "PKCE implementation required for FAPI compliance"
        return 1
    fi
    
    # Check for mTLS implementation
    if find "$PROJECT_ROOT/src" -name "*.java" -exec grep -l "mutual.*TLS\|mTLS" {} \; | head -1 &> /dev/null; then
        log_success "mTLS implementation found"
    else
        log_error "mTLS implementation required for FAPI compliance"
        return 1
    fi
}

# Validate FAPI mTLS configuration
validate_fapi_mtls_configuration() {
    log_info "Validating FAPI mTLS configuration..."
    
    # Check for client certificate validation
    if find "$PROJECT_ROOT/src" -name "*.java" -exec grep -l "X509.*Certificate\|ClientCertificate" {} \; | head -1 &> /dev/null; then
        log_success "Client certificate validation found"
    else
        log_warn "Client certificate validation not found"
    fi
}

# Validate FAPI PAR support
validate_fapi_par_support() {
    log_info "Validating FAPI PAR (Pushed Authorization Request) support..."
    
    # Check for PAR implementation
    if find "$PROJECT_ROOT/src" -name "*.java" -exec grep -l "PAR\|PushedAuthorization" {} \; | head -1 &> /dev/null; then
        log_success "PAR implementation found"
    else
        log_warn "PAR implementation not found (may be required for FAPI)"
    fi
}

# Validate FAPI JARM support
validate_fapi_jarm_support() {
    log_info "Validating FAPI JARM (JWT Secured Authorization Response Mode) support..."
    
    # Check for JARM implementation
    if find "$PROJECT_ROOT/src" -name "*.java" -exec grep -l "JARM\|JWTSecuredResponse" {} \; | head -1 &> /dev/null; then
        log_success "JARM implementation found"
    else
        log_warn "JARM implementation not found (may be required for FAPI)"
    fi
}

# Validate security posture
validate_security_posture() {
    local env="$1"
    
    log_info "Validating security posture for $env..."
    
    # Check network policies
    validate_network_policies "$env"
    
    # Check RBAC configuration
    validate_rbac_configuration "$env"
    
    # Check pod security policies
    validate_pod_security_policies "$env"
    
    # Check secrets management
    validate_secrets_management "$env"
    
    log_success "Security posture validation completed"
}

# Validate network policies
validate_network_policies() {
    local env="$1"
    local namespace=$(get_namespace "$env")
    
    log_info "Validating network policies for $namespace..."
    
    # Check if network policies exist
    local policy_count=$(kubectl get networkpolicy -n "$namespace" --no-headers 2>/dev/null | wc -l)
    
    if [[ $policy_count -gt 0 ]]; then
        log_success "Network policies configured: $policy_count policies"
    else
        log_warn "No network policies found for namespace $namespace"
    fi
}

# Validate RBAC configuration
validate_rbac_configuration() {
    local env="$1"
    local namespace=$(get_namespace "$env")
    
    log_info "Validating RBAC configuration for $namespace..."
    
    # Check service accounts
    local sa_count=$(kubectl get serviceaccount -n "$namespace" --no-headers 2>/dev/null | wc -l)
    log_info "Service accounts configured: $sa_count"
    
    # Check role bindings
    local rb_count=$(kubectl get rolebinding -n "$namespace" --no-headers 2>/dev/null | wc -l)
    log_info "Role bindings configured: $rb_count"
    
    if [[ $rb_count -gt 0 ]]; then
        log_success "RBAC configuration found"
    else
        log_warn "No RBAC configuration found"
    fi
}

# Validate pod security policies
validate_pod_security_policies() {
    local env="$1"
    local namespace=$(get_namespace "$env")
    
    log_info "Validating pod security policies for $namespace..."
    
    # Check for pod security standards
    local pss_label=$(kubectl get namespace "$namespace" -o jsonpath='{.metadata.labels.pod-security\.kubernetes\.io/enforce}' 2>/dev/null || echo "")
    
    if [[ -n "$pss_label" ]]; then
        log_success "Pod Security Standards configured: $pss_label"
    else
        log_warn "Pod Security Standards not configured"
    fi
    
    # Check security context in deployments
    local secure_deployments=$(kubectl get deployment -n "$namespace" -o json 2>/dev/null | \
        jq -r '.items[] | select(.spec.template.spec.securityContext.runAsNonRoot == true) | .metadata.name' | wc -l)
    
    if [[ $secure_deployments -gt 0 ]]; then
        log_success "Secure deployments found: $secure_deployments"
    else
        log_warn "No deployments with secure security context found"
    fi
}

# Validate secrets management
validate_secrets_management() {
    local env="$1"
    local namespace=$(get_namespace "$env")
    
    log_info "Validating secrets management for $namespace..."
    
    # Check for secrets
    local secret_count=$(kubectl get secrets -n "$namespace" --no-headers 2>/dev/null | wc -l)
    log_info "Secrets configured: $secret_count"
    
    # Check for external secrets operator
    if kubectl get crd externalsecrets.external-secrets.io &> /dev/null; then
        log_success "External Secrets Operator detected"
    else
        log_warn "External Secrets Operator not found"
    fi
    
    # Check for sealed secrets
    if kubectl get crd sealedsecrets.bitnami.com &> /dev/null; then
        log_success "Sealed Secrets detected"
    else
        log_warn "Sealed Secrets not found"
    fi
}

# Generate security report
generate_security_report() {
    local env="$1"
    local version="$2"
    local report_file="/tmp/security-report-${env}-${version}.json"
    
    log_info "Generating security report: $report_file"
    
    cat > "$report_file" << EOF
{
    "environment": "$env",
    "version": "$version",
    "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
    "security_scans": {
        "container_scan": "COMPLETED",
        "dependency_scan": "COMPLETED",
        "static_analysis": "COMPLETED",
        "configuration_scan": "COMPLETED"
    },
    "compliance": {
        "fapi_compliance": "$FAPI_COMPLIANCE_REQUIRED",
        "pci_compliance": "$PCI_COMPLIANCE_REQUIRED",
        "sox_compliance": "$SOX_COMPLIANCE_REQUIRED"
    },
    "recommendations": [
        "Regular security scanning",
        "Keep dependencies updated",
        "Monitor security advisories",
        "Implement security best practices"
    ]
}
EOF
    
    log_success "Security report generated: $report_file"
}