package com.bank.loanmanagement.loan.domain.loan;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Domain value object for loan terms
 */
public class LoanTerm {

    private final int months;

    private LoanTerm(int months) {
        if (months <= 0) {
            throw new IllegalArgumentException("Loan term must be positive");
        }
        if (months > 480) { // 40 years max
            throw new IllegalArgumentException("Loan term cannot exceed 40 years");
        }
        this.months = months;
    }

    public static LoanTerm ofMonths(int months) {
        return new LoanTerm(months);
    }

    public static LoanTerm ofYears(int years) {
        return new LoanTerm(years * 12);
    }

    public static LoanTerm ofYears(double years) {
        return new LoanTerm((int) Math.round(years * 12));
    }

    public int getMonths() {
        return months;
    }

    public BigDecimal getYears() {
        return new BigDecimal(months).divide(new BigDecimal("12"), 1, BigDecimal.ROUND_HALF_UP);
    }

    public boolean isShortTerm() {
        return months <= 36; // 3 years or less
    }

    public boolean isLongTerm() {
        return months >= 240; // 20 years or more
    }

    public LoanTerm extend(int additionalMonths) {
        return new LoanTerm(this.months + additionalMonths);
    }

    public LoanTerm reduce(int fewerMonths) {
        return new LoanTerm(this.months - fewerMonths);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LoanTerm loanTerm)) return false;
        return months == loanTerm.months;
    }

    @Override
    public int hashCode() {
        return Objects.hash(months);
    }

    @Override
    public String toString() {
        if (months % 12 == 0) {
            return (months / 12) + " year" + (months == 12 ? "" : "s");
        }
        return months + " month" + (months == 1 ? "" : "s");
    }
}