package com.amanahfi.platform.tenant.domain;

/**
 * Tenant status enumeration
 */
public enum TenantStatus {
    
    /**
     * Tenant is being set up
     */
    PROVISIONING,
    
    /**
     * Tenant is active and operational
     */
    ACTIVE,
    
    /**
     * Tenant is temporarily suspended
     */
    SUSPENDED,
    
    /**
     * Tenant is being decommissioned
     */
    DECOMMISSIONING,
    
    /**
     * Tenant is permanently deactivated
     */
    DEACTIVATED,
    
    /**
     * Tenant is under maintenance
     */
    MAINTENANCE;
    
    /**
     * Check if tenant is operational
     */
    public boolean isOperational() {
        return this == ACTIVE;
    }
    
    /**
     * Check if tenant can be accessed
     */
    public boolean isAccessible() {
        return this == ACTIVE || this == MAINTENANCE;
    }
    
    /**
     * Check if tenant is being terminated
     */
    public boolean isTerminating() {
        return this == DECOMMISSIONING || this == DEACTIVATED;
    }
}