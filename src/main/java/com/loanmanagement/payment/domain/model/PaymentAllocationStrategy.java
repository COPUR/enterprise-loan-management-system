package com.loanmanagement.payment.domain.model;

/**
 * Enum representing different payment allocation strategies.
 */
public enum PaymentAllocationStrategy {
    INTEREST_FIRST("Interest First"),
    PRINCIPAL_FIRST("Principal First"),
    FEES_FIRST("Fees First"),
    PROPORTIONAL("Proportional"),
    FIFO("First In, First Out"),
    LIFO("Last In, First Out"),
    HIGHEST_INTEREST_RATE_FIRST("Highest Interest Rate First"),
    LOWEST_BALANCE_FIRST("Lowest Balance First"),
    HIGHEST_BALANCE_FIRST("Highest Balance First"),
    REGULATORY_REQUIRED("Regulatory Required"),
    CUSTOM("Custom");

    private final String displayName;

    PaymentAllocationStrategy(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isStandardStrategy() {
        return this == INTEREST_FIRST || this == PRINCIPAL_FIRST || this == FEES_FIRST || this == PROPORTIONAL;
    }

    public boolean requiresCustomLogic() {
        return this == CUSTOM || this == REGULATORY_REQUIRED;
    }
}