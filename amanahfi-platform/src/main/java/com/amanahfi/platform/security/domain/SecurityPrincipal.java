package com.amanahfi.platform.security.domain;

import com.amanahfi.platform.tenant.domain.TenantId;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Set;

/**
 * Security principal representing an authenticated user
 */
@Value
@Builder
public class SecurityPrincipal {
    
    /**
     * Unique user identifier
     */
    String userId;
    
    /**
     * Username or email
     */
    String username;
    
    /**
     * User's full name
     */
    String fullName;
    
    /**
     * User's email address
     */
    String email;
    
    /**
     * Tenant the user belongs to
     */
    TenantId tenantId;
    
    /**
     * User roles
     */
    Set<String> roles;
    
    /**
     * User permissions
     */
    Set<String> permissions;
    
    /**
     * Authentication method used
     */
    AuthenticationMethod authenticationMethod;
    
    /**
     * Whether user is authenticated
     */
    boolean authenticated;
    
    /**
     * Whether user has elevated privileges
     */
    boolean elevated;
    
    /**
     * Authentication timestamp
     */
    Instant authenticatedAt;
    
    /**
     * Token expiration time
     */
    Instant expiresAt;
    
    /**
     * Session identifier
     */
    String sessionId;
    
    /**
     * Client IP address
     */
    String clientIp;
    
    /**
     * User agent
     */
    String userAgent;
    
    /**
     * Check if user has a specific role
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
    
    /**
     * Check if user has any of the specified roles
     */
    public boolean hasAnyRole(Set<String> requiredRoles) {
        if (roles == null || requiredRoles == null) {
            return false;
        }
        return roles.stream().anyMatch(requiredRoles::contains);
    }
    
    /**
     * Check if user has a specific permission
     */
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }
    
    /**
     * Check if user has any of the specified permissions
     */
    public boolean hasAnyPermission(Set<String> requiredPermissions) {
        if (permissions == null || requiredPermissions == null) {
            return false;
        }
        return permissions.stream().anyMatch(requiredPermissions::contains);
    }
    
    /**
     * Check if user is admin
     */
    public boolean isAdmin() {
        return hasRole("ADMIN") || hasRole("TENANT_ADMIN");
    }
    
    /**
     * Check if user is system admin
     */
    public boolean isSystemAdmin() {
        return hasRole("SYSTEM_ADMIN");
    }
    
    /**
     * Check if authentication is expired
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
    
    /**
     * Check if authentication is valid
     */
    public boolean isValid() {
        return authenticated && !isExpired();
    }
    
    /**
     * Validate security principal
     */
    public void validate() {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        
        if (authenticatedAt == null) {
            throw new IllegalArgumentException("Authentication timestamp cannot be null");
        }
        
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID cannot be null or empty");
        }
    }
}