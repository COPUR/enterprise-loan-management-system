package com.amanahfi.platform.tenant.domain;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Tenant context for current execution
 */
@Value
@Builder
public class TenantContext {
    
    /**
     * Current tenant identifier
     */
    TenantId tenantId;
    
    /**
     * Current user identifier
     */
    String userId;
    
    /**
     * Current session identifier
     */
    String sessionId;
    
    /**
     * Request correlation ID
     */
    String correlationId;
    
    /**
     * Request timestamp
     */
    Instant requestTime;
    
    /**
     * Client IP address
     */
    String clientIp;
    
    /**
     * User agent
     */
    String userAgent;
    
    /**
     * Selected language
     */
    String language;
    
    /**
     * Selected currency
     */
    String currency;
    
    /**
     * Selected timezone
     */
    String timezone;
    
    /**
     * Whether request is from admin user
     */
    boolean isAdmin;
    
    /**
     * Whether request has elevated privileges
     */
    boolean hasElevatedPrivileges;
    
    /**
     * Additional context attributes
     */
    java.util.Map<String, String> attributes;
    
    /**
     * Validate tenant context
     */
    public void validate() {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID is required");
        }
        
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required");
        }
        
        if (correlationId == null || correlationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Correlation ID is required");
        }
        
        if (requestTime == null) {
            throw new IllegalArgumentException("Request time is required");
        }
    }
    
    /**
     * Get attribute value
     */
    public String getAttribute(String key) {
        return attributes != null ? attributes.get(key) : null;
    }
    
    /**
     * Check if attribute exists
     */
    public boolean hasAttribute(String key) {
        return attributes != null && attributes.containsKey(key);
    }
}