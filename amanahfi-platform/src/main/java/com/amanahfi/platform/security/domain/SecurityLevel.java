package com.amanahfi.platform.security.domain;

/**
 * Security levels for operations
 */
public enum SecurityLevel {
    
    /**
     * Low security - basic authentication required
     */
    LOW(1),
    
    /**
     * Medium security - strong authentication required
     */
    MEDIUM(2),
    
    /**
     * High security - strong authentication + mTLS required
     */
    HIGH(3),
    
    /**
     * Critical security - strong authentication + mTLS + elevated privileges required
     */
    CRITICAL(4);
    
    private final int level;
    
    SecurityLevel(int level) {
        this.level = level;
    }
    
    public int getLevel() {
        return level;
    }
    
    /**
     * Check if this security level meets or exceeds the required level
     */
    public boolean meetsOrExceeds(SecurityLevel required) {
        return this.level >= required.level;
    }
    
    /**
     * Get the higher security level between two levels
     */
    public static SecurityLevel max(SecurityLevel level1, SecurityLevel level2) {
        return level1.level >= level2.level ? level1 : level2;
    }
    
    /**
     * Get the lower security level between two levels
     */
    public static SecurityLevel min(SecurityLevel level1, SecurityLevel level2) {
        return level1.level <= level2.level ? level1 : level2;
    }
    
    /**
     * Check if security level requires mTLS
     */
    public boolean requiresMutualTLS() {
        return this.level >= HIGH.level;
    }
    
    /**
     * Check if security level requires elevated privileges
     */
    public boolean requiresElevatedPrivileges() {
        return this.level >= CRITICAL.level;
    }
}