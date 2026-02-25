package com.loanmanagement.party.domain;

/**
 * Party type enumeration for the banking system
 * Defines different types of parties in the system
 */
public enum PartyType {
    INDIVIDUAL("Individual Person"),
    ORGANIZATION("Organization/Company"),
    GOVERNMENT("Government Entity"),
    FINANCIAL_INSTITUTION("Financial Institution"),
    TRUST("Trust"),
    PARTNERSHIP("Partnership"),
    CORPORATION("Corporation"),
    NON_PROFIT("Non-Profit Organization");
    
    private final String description;
    
    PartyType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isIndividual() {
        return this == INDIVIDUAL;
    }
    
    public boolean isOrganization() {
        return this != INDIVIDUAL;
    }
}