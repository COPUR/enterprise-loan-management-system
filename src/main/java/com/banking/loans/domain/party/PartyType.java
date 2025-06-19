package com.banking.loans.domain.party;

/**
 * PartyType enumeration for banking domain
 * Defines the different types of parties in the banking system
 */
public enum PartyType {
    /**
     * Individual person (customer, employee, etc.)
     */
    INDIVIDUAL,
    
    /**
     * Organization (corporate customer, vendor, partner, etc.)
     */
    ORGANIZATION,
    
    /**
     * Service account for system integrations
     */
    SERVICE_ACCOUNT,
    
    /**
     * Internal system user
     */
    SYSTEM_USER,
    
    /**
     * External API client
     */
    API_CLIENT
}