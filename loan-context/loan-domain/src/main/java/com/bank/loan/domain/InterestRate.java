package com.bank.loan.domain;

import com.bank.shared.kernel.domain.ValueObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object representing an interest rate for loans
 */
public final class InterestRate implements ValueObject {
    
    private final BigDecimal annualRate;
    
    private InterestRate(BigDecimal annualRate) {
        this.annualRate = Objects.requireNonNull(annualRate, "Annual rate cannot be null")
                .setScale(4, RoundingMode.HALF_UP);
    }
    
    public static InterestRate of(BigDecimal annualRate) {
        if (annualRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Interest rate cannot be negative");
        }
        if (annualRate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Interest rate cannot exceed 100%");
        }
        return new InterestRate(annualRate);
    }
    
    public static InterestRate of(double annualRate) {
        return of(BigDecimal.valueOf(annualRate));
    }
    
    public static InterestRate zero() {
        return new InterestRate(BigDecimal.ZERO);
    }
    
    public BigDecimal getAnnualRate() {
        return annualRate;
    }
    
    public BigDecimal getMonthlyRate() {
        return annualRate.divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);
    }
    
    public BigDecimal getDailyRate() {
        return annualRate.divide(BigDecimal.valueOf(365), 8, RoundingMode.HALF_UP);
    }
    
    public boolean isZero() {
        return annualRate.compareTo(BigDecimal.ZERO) == 0;
    }
    
    public boolean isNegative() {
        return annualRate.compareTo(BigDecimal.ZERO) < 0;
    }
    
    @Override
    public boolean isEmpty() {
        return isZero();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        InterestRate that = (InterestRate) obj;
        return Objects.equals(annualRate, that.annualRate);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(annualRate);
    }
    
    @Override
    public String toString() {
        return annualRate.toPlainString() + "%";
    }
}