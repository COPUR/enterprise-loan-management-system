package com.loanmanagement.payment.domain.model;

import java.time.Period;

/**
 * Enum representing different payment frequencies.
 */
public enum PaymentFrequency {
    DAILY("Daily", Period.ofDays(1)),
    WEEKLY("Weekly", Period.ofWeeks(1)),
    BI_WEEKLY("Bi-weekly", Period.ofWeeks(2)),
    SEMI_MONTHLY("Semi-monthly", Period.ofDays(15)),
    MONTHLY("Monthly", Period.ofMonths(1)),
    BI_MONTHLY("Bi-monthly", Period.ofMonths(2)),
    QUARTERLY("Quarterly", Period.ofMonths(3)),
    SEMI_ANNUALLY("Semi-annually", Period.ofMonths(6)),
    ANNUALLY("Annually", Period.ofYears(1)),
    ONE_TIME("One-time", Period.ZERO);

    private final String displayName;
    private final Period period;

    PaymentFrequency(String displayName, Period period) {
        this.displayName = displayName;
        this.period = period;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Period getPeriod() {
        return period;
    }

    public boolean isRecurring() {
        return this != ONE_TIME;
    }

    public int getPaymentsPerYear() {
        if (this == ONE_TIME) return 1;
        if (this == DAILY) return 365;
        if (this == WEEKLY) return 52;
        if (this == BI_WEEKLY) return 26;
        if (this == SEMI_MONTHLY) return 24;
        if (this == MONTHLY) return 12;
        if (this == BI_MONTHLY) return 6;
        if (this == QUARTERLY) return 4;
        if (this == SEMI_ANNUALLY) return 2;
        if (this == ANNUALLY) return 1;
        return 1;
    }
}