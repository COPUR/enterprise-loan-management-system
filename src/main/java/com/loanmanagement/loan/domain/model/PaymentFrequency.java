package com.loanmanagement.loan.domain.model;

/**
 * Payment Frequency Enumeration
 * Defines how often loan payments are made
 */
public enum PaymentFrequency {
    WEEKLY("Weekly"),
    BI_WEEKLY("Bi-Weekly"),
    MONTHLY("Monthly"),
    QUARTERLY("Quarterly"),
    SEMI_ANNUALLY("Semi-Annually"),
    ANNUALLY("Annually");

    private final String displayName;

    PaymentFrequency(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get number of payments per year
     */
    public int getPaymentsPerYear() {
        return switch (this) {
            case WEEKLY -> 52;
            case BI_WEEKLY -> 26;
            case MONTHLY -> 12;
            case QUARTERLY -> 4;
            case SEMI_ANNUALLY -> 2;
            case ANNUALLY -> 1;
        };
    }

    /**
     * Check if this frequency is more frequent than another
     */
    public boolean isMoreFrequentThan(PaymentFrequency other) {
        return this.getPaymentsPerYear() > other.getPaymentsPerYear();
    }

    @Override
    public String toString() {
        return displayName;
    }
}