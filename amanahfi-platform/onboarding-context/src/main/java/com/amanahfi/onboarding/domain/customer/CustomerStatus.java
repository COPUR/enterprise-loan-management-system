package com.amanahfi.onboarding.domain.customer;

/**
 * Customer Status Lifecycle
 * Represents the various states a customer can be in during their lifecycle
 */
public enum CustomerStatus {
    /**
     * Customer registered but KYC not completed
     */
    PENDING_KYC,
    
    /**
     * KYC documents submitted, awaiting verification
     */
    KYC_UNDER_REVIEW,
    
    /**
     * KYC rejected, customer needs to resubmit
     */
    KYC_REJECTED,
    
    /**
     * KYC approved, customer is active
     */
    ACTIVE,
    
    /**
     * Customer account suspended
     */
    SUSPENDED,
    
    /**
     * Customer account closed
     */
    CLOSED,
    
    /**
     * Customer account blocked due to compliance issues
     */
    BLOCKED
}