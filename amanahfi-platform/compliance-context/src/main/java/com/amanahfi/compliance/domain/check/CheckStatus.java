package com.amanahfi.compliance.domain.check;

/**
 * Compliance check status enumeration
 */
public enum CheckStatus {
    /**
     * Check created but not yet started
     */
    PENDING,
    
    /**
     * Automated screening in progress
     */
    IN_PROGRESS,
    
    /**
     * Requires manual review by compliance officer
     */
    REQUIRES_REVIEW,
    
    /**
     * Under investigation by compliance team
     */
    UNDER_INVESTIGATION,
    
    /**
     * Check completed and approved
     */
    APPROVED,
    
    /**
     * Check completed and rejected
     */
    REJECTED,
    
    /**
     * Check expired without completion
     */
    EXPIRED
}