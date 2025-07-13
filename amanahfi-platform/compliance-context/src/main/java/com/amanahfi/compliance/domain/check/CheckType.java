package com.amanahfi.compliance.domain.check;

/**
 * Types of compliance checks performed in the system
 */
public enum CheckType {
    /**
     * Customer onboarding KYC verification
     */
    CUSTOMER_ONBOARDING,
    
    /**
     * Ongoing transaction monitoring
     */
    TRANSACTION_MONITORING,
    
    /**
     * Contract validation for Islamic finance compliance
     */
    CONTRACT_VALIDATION,
    
    /**
     * Contract review for compliance and legal validation
     */
    CONTRACT_REVIEW,
    
    /**
     * Periodic customer review
     */
    PERIODIC_REVIEW,
    
    /**
     * Suspicious activity investigation
     */
    SUSPICIOUS_ACTIVITY,
    
    /**
     * High-risk customer enhanced due diligence
     */
    ENHANCED_DUE_DILIGENCE,
    
    /**
     * Sanctions screening
     */
    SANCTIONS_SCREENING
}