package com.amanahfi.murabaha.domain.contract;

/**
 * Contract status enumeration for Murabaha lifecycle
 * Following Islamic finance approval and execution process
 */
public enum ContractStatus {
    /**
     * Contract created but pending review
     */
    DRAFT,
    
    /**
     * Contract approved by Sharia Supervisory Board
     */
    SHARIA_APPROVED,
    
    /**
     * Contract is active and installments are due
     */
    ACTIVE,
    
    /**
     * Contract completed successfully
     */
    COMPLETED,
    
    /**
     * Contract settled early
     */
    SETTLED,
    
    /**
     * Contract in default due to non-payment
     */
    DEFAULTED,
    
    /**
     * Contract cancelled before activation
     */
    CANCELLED
}