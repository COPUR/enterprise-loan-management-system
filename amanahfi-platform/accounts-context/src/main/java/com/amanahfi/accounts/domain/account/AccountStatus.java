package com.amanahfi.accounts.domain.account;

/**
 * Account status enumeration
 * Following UAE banking regulations and Islamic banking principles
 */
public enum AccountStatus {
    /**
     * Account is active and operational
     */
    ACTIVE,
    
    /**
     * Account is frozen due to investigation or compliance issues
     */
    FROZEN,
    
    /**
     * Account is temporarily suspended
     */
    SUSPENDED,
    
    /**
     * Account is closed permanently
     */
    CLOSED,
    
    /**
     * Account is pending activation (new accounts)
     */
    PENDING_ACTIVATION
}