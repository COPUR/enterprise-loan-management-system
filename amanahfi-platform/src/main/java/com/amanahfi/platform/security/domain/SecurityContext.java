package com.amanahfi.platform.security.domain;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Optional;

/**
 * Security context for the current execution
 */
@Value
@Builder
public class SecurityContext {
    
    /**
     * Current security principal
     */
    SecurityPrincipal principal;
    
    /**
     * Request correlation ID
     */
    String correlationId;
    
    /**
     * Security level required for operation
     */
    SecurityLevel requiredSecurityLevel;
    
    /**
     * Whether operation requires elevated privileges
     */
    boolean requiresElevatedPrivileges;
    
    /**
     * Whether operation is from trusted source
     */
    boolean trustedSource;
    
    /**
     * Client certificate details (for mTLS)
     */
    ClientCertificate clientCertificate;
    
    /**
     * Request timestamp
     */
    Instant requestTime;
    
    /**
     * Additional security attributes
     */
    java.util.Map<String, String> securityAttributes;
    
    /**
     * Check if user is authenticated
     */
    public boolean isAuthenticated() {
        return principal != null && principal.isAuthenticated();
    }
    
    /**
     * Check if authentication is valid
     */
    public boolean isValid() {
        return principal != null && principal.isValid();
    }
    
    /**
     * Check if user has required role
     */
    public boolean hasRole(String role) {
        return principal != null && principal.hasRole(role);
    }
    
    /**
     * Check if user has required permission
     */
    public boolean hasPermission(String permission) {
        return principal != null && principal.hasPermission(permission);
    }
    
    /**
     * Check if user is admin
     */
    public boolean isAdmin() {
        return principal != null && principal.isAdmin();
    }
    
    /**
     * Check if user has elevated privileges
     */
    public boolean hasElevatedPrivileges() {
        return principal != null && principal.isElevated();
    }
    
    /**
     * Check if request uses strong authentication
     */
    public boolean usesStrongAuthentication() {
        return principal != null && 
               principal.getAuthenticationMethod().isStrong();
    }
    
    /**
     * Check if request uses mTLS
     */
    public boolean usesMutualTLS() {
        return clientCertificate != null && clientCertificate.isValid();
    }
    
    /**
     * Check if security level meets requirements
     */
    public boolean meetsSecurityLevel() {
        if (requiredSecurityLevel == null) {
            return true;
        }
        
        return switch (requiredSecurityLevel) {
            case LOW -> isAuthenticated();
            case MEDIUM -> isAuthenticated() && usesStrongAuthentication();
            case HIGH -> isAuthenticated() && usesStrongAuthentication() && usesMutualTLS();
            case CRITICAL -> isAuthenticated() && usesStrongAuthentication() && 
                           usesMutualTLS() && hasElevatedPrivileges();
        };
    }
    
    /**
     * Get security attribute
     */
    public Optional<String> getSecurityAttribute(String key) {
        return securityAttributes != null ? 
            Optional.ofNullable(securityAttributes.get(key)) : 
            Optional.empty();
    }
    
    /**
     * Validate security context
     */
    public void validate() {
        if (correlationId == null || correlationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Correlation ID cannot be null or empty");
        }
        
        if (requestTime == null) {
            throw new IllegalArgumentException("Request time cannot be null");
        }
        
        if (principal != null) {
            principal.validate();
        }
        
        if (clientCertificate != null) {
            clientCertificate.validate();
        }
        
        if (requiresElevatedPrivileges && !hasElevatedPrivileges()) {
            throw new SecurityException("Operation requires elevated privileges");
        }
        
        if (!meetsSecurityLevel()) {
            throw new SecurityException("Security level requirements not met");
        }
    }
}