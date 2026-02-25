package com.loanmanagement.party.domain;

/**
 * Party status enumeration for the banking system
 * Defines different states a party can be in
 */
public enum PartyStatus {
    ACTIVE("Active and operational"),
    INACTIVE("Inactive but can be reactivated"),
    SUSPENDED("Suspended due to compliance or security issues"),
    BLOCKED("Blocked due to fraud or regulatory action"),
    PENDING_VERIFICATION("Pending identity verification"),
    PENDING_APPROVAL("Pending approval for activation"),
    ARCHIVED("Archived and no longer in use");
    
    private final String description;
    
    PartyStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isActive() {
        return this == ACTIVE;
    }
    
    public boolean canTransact() {
        return this == ACTIVE;
    }
    
    public boolean isPending() {
        return this == PENDING_VERIFICATION || this == PENDING_APPROVAL;
    }
    
    public boolean isBlocked() {
        return this == BLOCKED || this == SUSPENDED;
    }
}