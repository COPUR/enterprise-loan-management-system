package com.banking.loans.domain.party;

/**
 * PartyStatus enumeration for banking domain
 * Defines the different statuses a party can have in the banking system
 */
public enum PartyStatus {
    /**
     * Party is active and can access the system
     */
    ACTIVE,
    
    /**
     * Party is inactive (temporarily disabled)
     */
    INACTIVE,
    
    /**
     * Party is suspended due to compliance or security issues
     */
    SUSPENDED,
    
    /**
     * Party is locked due to security violations
     */
    LOCKED,
    
    /**
     * Party is pending activation (new account setup)
     */
    PENDING,
    
    /**
     * Party account has been closed/terminated
     */
    CLOSED
}