package com.amanahfi.platform.security.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Manages security context for the current execution thread
 */
@Component
@Slf4j
public class SecurityContextManager {
    
    private static final ThreadLocal<SecurityContext> SECURITY_CONTEXT = new ThreadLocal<>();
    
    /**
     * Set the security context for the current thread
     */
    public void setSecurityContext(SecurityContext context) {
        if (context != null) {
            context.validate();
            SECURITY_CONTEXT.set(context);
            log.debug("Security context set for user: {}, tenant: {}", 
                context.getPrincipal() != null ? context.getPrincipal().getUserId() : "anonymous",
                context.getPrincipal() != null ? context.getPrincipal().getTenantId() : "none");
        } else {
            log.warn("Attempted to set null security context");
        }
    }
    
    /**
     * Get the current security context
     */
    public Optional<SecurityContext> getCurrentSecurityContext() {
        return Optional.ofNullable(SECURITY_CONTEXT.get());
    }
    
    /**
     * Get the current security principal
     */
    public Optional<SecurityPrincipal> getCurrentPrincipal() {
        return getCurrentSecurityContext().map(SecurityContext::getPrincipal);
    }
    
    /**
     * Get the current user ID
     */
    public Optional<String> getCurrentUserId() {
        return getCurrentPrincipal().map(SecurityPrincipal::getUserId);
    }
    
    /**
     * Get the current correlation ID
     */
    public Optional<String> getCurrentCorrelationId() {
        return getCurrentSecurityContext().map(SecurityContext::getCorrelationId);
    }
    
    /**
     * Check if current user is authenticated
     */
    public boolean isAuthenticated() {
        return getCurrentSecurityContext()
            .map(SecurityContext::isAuthenticated)
            .orElse(false);
    }
    
    /**
     * Check if current user is admin
     */
    public boolean isAdmin() {
        return getCurrentSecurityContext()
            .map(SecurityContext::isAdmin)
            .orElse(false);
    }
    
    /**
     * Check if current user has role
     */
    public boolean hasRole(String role) {
        return getCurrentSecurityContext()
            .map(context -> context.hasRole(role))
            .orElse(false);
    }
    
    /**
     * Check if current user has permission
     */
    public boolean hasPermission(String permission) {
        return getCurrentSecurityContext()
            .map(context -> context.hasPermission(permission))
            .orElse(false);
    }
    
    /**
     * Check if current user has elevated privileges
     */
    public boolean hasElevatedPrivileges() {
        return getCurrentSecurityContext()
            .map(SecurityContext::hasElevatedPrivileges)
            .orElse(false);
    }
    
    /**
     * Check if current request uses strong authentication
     */
    public boolean usesStrongAuthentication() {
        return getCurrentSecurityContext()
            .map(SecurityContext::usesStrongAuthentication)
            .orElse(false);
    }
    
    /**
     * Check if current request uses mTLS
     */
    public boolean usesMutualTLS() {
        return getCurrentSecurityContext()
            .map(SecurityContext::usesMutualTLS)
            .orElse(false);
    }
    
    /**
     * Check if current request is from trusted source
     */
    public boolean isTrustedSource() {
        return getCurrentSecurityContext()
            .map(SecurityContext::isTrustedSource)
            .orElse(false);
    }
    
    /**
     * Clear the security context for the current thread
     */
    public void clearSecurityContext() {
        SecurityContext context = SECURITY_CONTEXT.get();
        if (context != null && context.getPrincipal() != null) {
            log.debug("Clearing security context for user: {}", 
                context.getPrincipal().getUserId());
        }
        SECURITY_CONTEXT.remove();
    }
    
    /**
     * Execute a task with a specific security context
     */
    public <T> T executeWithSecurityContext(SecurityContext context, java.util.function.Supplier<T> task) {
        SecurityContext previousContext = SECURITY_CONTEXT.get();
        try {
            setSecurityContext(context);
            return task.get();
        } finally {
            if (previousContext != null) {
                setSecurityContext(previousContext);
            } else {
                clearSecurityContext();
            }
        }
    }
    
    /**
     * Execute a task with a specific security context (no return value)
     */
    public void executeWithSecurityContext(SecurityContext context, Runnable task) {
        SecurityContext previousContext = SECURITY_CONTEXT.get();
        try {
            setSecurityContext(context);
            task.run();
        } finally {
            if (previousContext != null) {
                setSecurityContext(previousContext);
            } else {
                clearSecurityContext();
            }
        }
    }
    
    /**
     * Ensure security context is set
     */
    public void ensureSecurityContext() {
        if (getCurrentSecurityContext().isEmpty()) {
            throw new SecurityException("No security context set for current thread");
        }
    }
    
    /**
     * Ensure user is authenticated
     */
    public void ensureAuthenticated() {
        if (!isAuthenticated()) {
            throw new SecurityException("User is not authenticated");
        }
    }
    
    /**
     * Ensure user has required role
     */
    public void ensureRole(String role) {
        ensureAuthenticated();
        if (!hasRole(role)) {
            throw new SecurityException("User does not have required role: " + role);
        }
    }
    
    /**
     * Ensure user has required permission
     */
    public void ensurePermission(String permission) {
        ensureAuthenticated();
        if (!hasPermission(permission)) {
            throw new SecurityException("User does not have required permission: " + permission);
        }
    }
    
    /**
     * Ensure user has elevated privileges
     */
    public void ensureElevatedPrivileges() {
        ensureAuthenticated();
        if (!hasElevatedPrivileges()) {
            throw new SecurityException("User does not have elevated privileges");
        }
    }
    
    /**
     * Ensure strong authentication
     */
    public void ensureStrongAuthentication() {
        ensureAuthenticated();
        if (!usesStrongAuthentication()) {
            throw new SecurityException("Strong authentication required");
        }
    }
    
    /**
     * Ensure mTLS authentication
     */
    public void ensureMutualTLS() {
        ensureAuthenticated();
        if (!usesMutualTLS()) {
            throw new SecurityException("Mutual TLS authentication required");
        }
    }
    
    /**
     * Ensure security level meets requirements
     */
    public void ensureSecurityLevel(SecurityLevel requiredLevel) {
        SecurityContext context = getCurrentSecurityContext()
            .orElseThrow(() -> new SecurityException("No security context available"));
        
        if (!context.meetsSecurityLevel()) {
            throw new SecurityException("Security level requirements not met. Required: " + requiredLevel);
        }
    }
}