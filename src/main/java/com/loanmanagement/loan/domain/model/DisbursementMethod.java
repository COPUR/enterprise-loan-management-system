package com.loanmanagement.loan.domain.model;

/**
 * Disbursement Method Enumeration
 * Defines the various methods for disbursing loan funds
 */
public enum DisbursementMethod {
    /**
     * Direct deposit to customer's bank account
     */
    DIRECT_DEPOSIT("Direct Deposit", "Funds deposited directly to bank account", 1),
    
    /**
     * ACH transfer to customer's account
     */
    ACH_TRANSFER("ACH Transfer", "Electronic transfer via ACH network", 1),
    
    /**
     * Wire transfer for immediate availability
     */
    WIRE_TRANSFER("Wire Transfer", "Wire transfer for same-day availability", 0),
    
    /**
     * Physical check mailed to customer
     */
    CHECK("Check", "Physical check mailed to customer address", 5);

    private final String displayName;
    private final String description;
    private final int standardProcessingDays;

    DisbursementMethod(String displayName, String description, int standardProcessingDays) {
        this.displayName = displayName;
        this.description = description;
        this.standardProcessingDays = standardProcessingDays;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getStandardProcessingDays() {
        return standardProcessingDays;
    }

    /**
     * Check if this method is electronic
     */
    public boolean isElectronic() {
        return this != CHECK;
    }

    /**
     * Check if this method provides immediate availability
     */
    public boolean isImmediate() {
        return this == WIRE_TRANSFER;
    }

    /**
     * Check if this method requires bank account verification
     */
    public boolean requiresBankVerification() {
        return this == DIRECT_DEPOSIT || this == ACH_TRANSFER || this == WIRE_TRANSFER;
    }

    /**
     * Get estimated availability date based on disbursement date
     */
    public java.time.LocalDate getEstimatedAvailabilityDate(java.time.LocalDate disbursementDate) {
        return disbursementDate.plusDays(standardProcessingDays);
    }

    @Override
    public String toString() {
        return displayName;
    }
}