package com.bank.loan.domain.model;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value object representing an interest rate
 */
@Embeddable
@Getter
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class InterestRate {
    
    private BigDecimal rate;
    
    public InterestRate(BigDecimal rate) {
        Objects.requireNonNull(rate, "Interest rate cannot be null");
        if (rate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Interest rate cannot be negative");
        }
        if (rate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Interest rate cannot exceed 100% (1.0)");
        }
        this.rate = rate.setScale(4, RoundingMode.HALF_UP);
    }
    
    public InterestRate(double rate) {
        this(BigDecimal.valueOf(rate));
    }
    
    public static InterestRate of(BigDecimal rate) {
        return new InterestRate(rate);
    }
    
    public static InterestRate of(double rate) {
        return new InterestRate(rate);
    }
    
    public static InterestRate zero() {
        return new InterestRate(BigDecimal.ZERO);
    }
    
    public BigDecimal asDecimal() {
        return rate;
    }
    
    public double asDouble() {
        return rate.doubleValue();
    }
    
    public BigDecimal asPercentage() {
        return rate.multiply(BigDecimal.valueOf(100));
    }
    
    public Money calculateInterest(Money principal) {
        Objects.requireNonNull(principal, "Principal cannot be null");
        return principal.multiply(rate);
    }
    
    public boolean isZero() {
        return rate.compareTo(BigDecimal.ZERO) == 0;
    }
    
    public boolean isGreaterThan(InterestRate other) {
        return rate.compareTo(other.rate) > 0;
    }
    
    public boolean isLessThan(InterestRate other) {
        return rate.compareTo(other.rate) < 0;
    }
}