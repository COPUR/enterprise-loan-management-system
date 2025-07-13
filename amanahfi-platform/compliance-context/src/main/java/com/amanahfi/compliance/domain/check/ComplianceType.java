package com.amanahfi.compliance.domain.check;

/**
 * Types of compliance frameworks
 */
public enum ComplianceType {
    /**
     * Anti-Money Laundering compliance
     */
    AML,
    
    /**
     * Islamic finance (Sharia) compliance
     */
    SHARIA,
    
    /**
     * Know Your Customer compliance
     */
    KYC,
    
    /**
     * Counter Financing of Terrorism
     */
    CFT,
    
    /**
     * General regulatory compliance
     */
    REGULATORY
}