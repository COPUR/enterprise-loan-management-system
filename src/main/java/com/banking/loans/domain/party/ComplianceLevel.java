package com.banking.loans.domain.party;

/**
 * ComplianceLevel Enumeration for Banking Compliance
 * 
 * Represents different levels of compliance verification for parties
 * in the banking system. This is used for KYC (Know Your Customer),
 * AML (Anti-Money Laundering), and other regulatory requirements.
 */
public enum ComplianceLevel {
    
    /**
     * Low compliance level - Basic verification completed
     * Suitable for limited banking services with lower transaction limits
     */
    LOW("Low Compliance", "Basic identity verification completed", 1000.00),
    
    /**
     * Standard compliance level - Enhanced verification completed
     * Suitable for most banking services with standard transaction limits
     */
    STANDARD("Standard Compliance", "Enhanced verification and documentation completed", 25000.00),
    
    /**
     * High compliance level - Full verification completed
     * Suitable for all banking services including high-value transactions
     */
    HIGH("High Compliance", "Full KYC/AML verification completed", Double.MAX_VALUE),
    
    /**
     * Restricted compliance level - Account under review or restricted
     * Limited or no access to banking services
     */
    RESTRICTED("Restricted", "Account under compliance review or restricted", 0.00),
    
    /**
     * Suspended compliance level - Account suspended due to compliance issues
     * No access to banking services
     */
    SUSPENDED("Suspended", "Account suspended due to compliance violations", 0.00);
    
    private final String displayName;
    private final String description;
    private final double transactionLimit;
    
    ComplianceLevel(String displayName, String description, double transactionLimit) {
        this.displayName = displayName;
        this.description = description;
        this.transactionLimit = transactionLimit;
    }
    
    /**
     * Get the display name for the compliance level
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Get the description of the compliance level
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the transaction limit for this compliance level
     */
    public double getTransactionLimit() {
        return transactionLimit;
    }
    
    /**
     * Check if this compliance level allows transactions
     */
    public boolean allowsTransactions() {
        return transactionLimit > 0.00;
    }
    
    /**
     * Check if a transaction amount is within the limit for this compliance level
     */
    public boolean isWithinLimit(double amount) {
        return amount <= transactionLimit;
    }
    
    /**
     * Check if this compliance level is active (not restricted or suspended)
     */
    public boolean isActive() {
        return this != RESTRICTED && this != SUSPENDED;
    }
    
    /**
     * Get the next higher compliance level, if available
     */
    public ComplianceLevel getNextLevel() {
        return switch (this) {
            case LOW -> STANDARD;
            case STANDARD -> HIGH;
            case HIGH -> HIGH; // Already at highest level
            case RESTRICTED -> LOW; // Can be restored to low
            case SUSPENDED -> LOW; // Can be restored to low
        };
    }
    
    /**
     * Check if this compliance level meets the minimum requirement
     */
    public boolean meetsMinimumRequirement(ComplianceLevel minimumRequired) {
        if (minimumRequired == null) {
            return true;
        }
        
        // Restricted and suspended never meet requirements
        if (!this.isActive()) {
            return false;
        }
        
        // Check if current level is at least the minimum required
        return switch (minimumRequired) {
            case LOW -> this == LOW || this == STANDARD || this == HIGH;
            case STANDARD -> this == STANDARD || this == HIGH;
            case HIGH -> this == HIGH;
            case RESTRICTED, SUSPENDED -> false;
        };
    }
}