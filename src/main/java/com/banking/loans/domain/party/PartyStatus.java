package com.banking.loans.domain.party;

/**
 * PartyStatus Enumeration for Banking System
 * 
 * Represents the current status of a party (customer, employee, etc.)
 * in the banking system. This is used for access control, compliance,
 * and business rule enforcement.
 */
public enum PartyStatus {
    
    /**
     * Active status - Party is active and can access all authorized services
     */
    ACTIVE("Active", "Party is active and can access authorized services"),
    
    /**
     * Inactive status - Party is temporarily inactive
     * May be due to temporary suspension or voluntary deactivation
     */
    INACTIVE("Inactive", "Party is temporarily inactive"),
    
    /**
     * Pending status - Party registration is pending approval or verification
     */
    PENDING("Pending", "Party registration is pending approval or verification"),
    
    /**
     * Suspended status - Party is suspended due to policy violations or compliance issues
     */
    SUSPENDED("Suspended", "Party is suspended due to policy violations or compliance issues"),
    
    /**
     * Blocked status - Party is blocked due to security concerns or fraud detection
     */
    BLOCKED("Blocked", "Party is blocked due to security concerns or fraud detection"),
    
    /**
     * Closed status - Party account is permanently closed
     */
    CLOSED("Closed", "Party account is permanently closed"),
    
    /**
     * Under Review status - Party is under compliance or security review
     */
    UNDER_REVIEW("Under Review", "Party is under compliance or security review");
    
    private final String displayName;
    private final String description;
    
    PartyStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    /**
     * Get the display name for the party status
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Get the description of the party status
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if this status allows the party to access banking services
     */
    public boolean allowsAccess() {
        return this == ACTIVE;
    }
    
    /**
     * Check if this status allows limited access to banking services
     */
    public boolean allowsLimitedAccess() {
        return this == ACTIVE || this == UNDER_REVIEW;
    }
    
    /**
     * Check if this status is a temporary state that can be changed
     */
    public boolean isTemporary() {
        return this == PENDING || this == SUSPENDED || this == UNDER_REVIEW || this == INACTIVE;
    }
    
    /**
     * Check if this status is permanent
     */
    public boolean isPermanent() {
        return this == CLOSED || this == BLOCKED;
    }
    
    /**
     * Check if this status indicates a compliance or security concern
     */
    public boolean indicatesConcern() {
        return this == SUSPENDED || this == BLOCKED || this == UNDER_REVIEW;
    }
    
    /**
     * Get the next logical status for activation flow
     */
    public PartyStatus getActivationStatus() {
        return switch (this) {
            case PENDING -> ACTIVE;
            case INACTIVE -> ACTIVE;
            case SUSPENDED -> UNDER_REVIEW; // Requires review before activation
            case UNDER_REVIEW -> ACTIVE;
            case BLOCKED -> UNDER_REVIEW; // Requires review before any change
            case CLOSED -> CLOSED; // Cannot be reactivated
            case ACTIVE -> ACTIVE; // Already active
        };
    }
    
    /**
     * Check if transition to another status is allowed
     */
    public boolean canTransitionTo(PartyStatus newStatus) {
        if (newStatus == null || newStatus == this) {
            return false;
        }
        
        return switch (this) {
            case PENDING -> newStatus == ACTIVE || newStatus == INACTIVE || newStatus == BLOCKED;
            case ACTIVE -> newStatus != PENDING; // Can go to any other status except pending
            case INACTIVE -> newStatus == ACTIVE || newStatus == SUSPENDED || newStatus == CLOSED;
            case SUSPENDED -> newStatus == UNDER_REVIEW || newStatus == BLOCKED || newStatus == CLOSED;
            case UNDER_REVIEW -> newStatus == ACTIVE || newStatus == SUSPENDED || newStatus == BLOCKED || newStatus == CLOSED;
            case BLOCKED -> newStatus == UNDER_REVIEW || newStatus == CLOSED;
            case CLOSED -> false; // No transitions allowed from closed
        };
    }
}