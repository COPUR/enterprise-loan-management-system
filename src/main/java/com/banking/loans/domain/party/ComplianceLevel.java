package com.banking.loans.domain.party;

/**
 * ComplianceLevel enumeration for banking domain
 * Defines different levels of compliance and verification for parties
 */
public enum ComplianceLevel {
    /**
     * Basic compliance level - minimal verification
     */
    BASIC,
    
    /**
     * Enhanced compliance level - additional verification required
     */
    ENHANCED,
    
    /**
     * Premium compliance level - full KYC/AML verification
     */
    PREMIUM,
    
    /**
     * Institutional compliance level - for corporate and institutional clients
     */
    INSTITUTIONAL,
    
    /**
     * Regulatory compliance level - for regulatory reporting and oversight
     */
    REGULATORY
}