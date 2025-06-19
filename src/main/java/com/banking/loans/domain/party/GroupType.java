package com.banking.loans.domain.party;

/**
 * GroupType enumeration for banking domain
 * Defines different types of groups in the banking system
 */
public enum GroupType {
    /**
     * Organizational department (IT, Loans, Compliance, etc.)
     */
    DEPARTMENT,
    
    /**
     * Functional team within a department
     */
    TEAM,
    
    /**
     * Functional group for specific business processes
     */
    FUNCTIONAL,
    
    /**
     * Security group for access control
     */
    SECURITY,
    
    /**
     * Project-based group for temporary assignments
     */
    PROJECT,
    
    /**
     * Geographic group (branch, region, etc.)
     */
    GEOGRAPHIC,
    
    /**
     * Compliance and regulatory group
     */
    COMPLIANCE,
    
    /**
     * Audit and oversight group
     */
    AUDIT,
    
    /**
     * Business line group (retail, commercial, investment, etc.)
     */
    BUSINESS_LINE,
    
    /**
     * Risk management group
     */
    RISK_MANAGEMENT
}