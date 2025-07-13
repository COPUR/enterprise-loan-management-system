package com.amanahfi.onboarding.domain.customer;

/**
 * Document Status Enumeration
 * Tracks the verification status of KYC documents
 */
public enum DocumentStatus {
    /**
     * Document uploaded but not yet reviewed
     */
    UPLOADED,
    
    /**
     * Document under review by KYC officer
     */
    UNDER_REVIEW,
    
    /**
     * Document approved
     */
    APPROVED,
    
    /**
     * Document rejected - needs resubmission
     */
    REJECTED,
    
    /**
     * Document expired
     */
    EXPIRED
}