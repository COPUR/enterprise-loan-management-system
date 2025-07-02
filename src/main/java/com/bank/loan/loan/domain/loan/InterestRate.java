package com.bank.loanmanagement.loan.domain.loan;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Domain value object for interest rates
 */
public class InterestRate {

    private final BigDecimal annualRate;

    private InterestRate(BigDecimal annualRate) {
        this.annualRate = Objects.requireNonNull(annualRate, "Annual rate cannot be null");
        
        if (annualRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Interest rate cannot be negative");
        }
        
        if (annualRate.compareTo(new BigDecimal("1.0")) > 0) {
            throw new IllegalArgumentException("Interest rate cannot exceed 100%");
        }
    }

    public static InterestRate of(BigDecimal annualRate) {
        return new InterestRate(annualRate);
    }

    public static InterestRate ofPercentage(double percentage) {
        return new InterestRate(BigDecimal.valueOf(percentage / 100.0));
    }

    public BigDecimal getAnnualRate() {
        return annualRate;
    }

    public BigDecimal getPercentage() {
        return annualRate.multiply(new BigDecimal("100"));
    }

    public BigDecimal getMonthlyRate() {
        return annualRate.divide(new BigDecimal("12"), 10, java.math.RoundingMode.HALF_UP);
    }

    public boolean isLowerThan(InterestRate other) {
        return this.annualRate.compareTo(other.annualRate) < 0;
    }

    public boolean isHigherThan(InterestRate other) {
        return this.annualRate.compareTo(other.annualRate) > 0;
    }

    public InterestRate add(InterestRate other) {
        return new InterestRate(this.annualRate.add(other.annualRate));
    }

    public InterestRate subtract(InterestRate other) {
        BigDecimal result = this.annualRate.subtract(other.annualRate);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Result would be negative interest rate");
        }
        return new InterestRate(result);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InterestRate that)) return false;
        return Objects.equals(annualRate, that.annualRate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(annualRate);
    }

    @Override
    public String toString() {
        return String.format("%.2f%%", getPercentage().doubleValue());
    }
}