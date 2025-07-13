package com.bank.loan.domain.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value object representing monetary amounts with currency
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Money {
    
    private BigDecimal amount;
    private String currency;
    
    public Money(BigDecimal amount) {
        this(amount, "USD");
    }
    
    public static Money of(BigDecimal amount, String currency) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");
        return new Money(amount.setScale(2, RoundingMode.HALF_UP), currency.toUpperCase());
    }
    
    public static Money of(double amount, String currency) {
        return of(BigDecimal.valueOf(amount), currency);
    }
    
    public static Money zero(String currency) {
        return of(BigDecimal.ZERO, currency);
    }
    
    public static Money usd(BigDecimal amount) {
        return of(amount, "USD");
    }
    
    public static Money usd(double amount) {
        return of(amount, "USD");
    }
    
    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }
    
    public Money subtract(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }
    
    public Money multiply(BigDecimal factor) {
        return new Money(this.amount.multiply(factor, MathContext.DECIMAL64), this.currency);
    }
    
    public Money multiply(double factor) {
        return multiply(BigDecimal.valueOf(factor));
    }
    
    public Money divide(BigDecimal divisor) {
        return new Money(this.amount.divide(divisor, MathContext.DECIMAL64), this.currency);
    }
    
    public Money divide(double divisor) {
        return divide(BigDecimal.valueOf(divisor));
    }
    
    public boolean isGreaterThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }
    
    public boolean isLessThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }
    
    public boolean isEqualTo(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) == 0;
    }
    
    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }
    
    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public boolean isNegative() {
        return this.amount.compareTo(BigDecimal.ZERO) < 0;
    }
    
    public Money abs() {
        return new Money(this.amount.abs(), this.currency);
    }
    
    public Money negate() {
        return new Money(this.amount.negate(), this.currency);
    }
    
    private void validateSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                String.format("Cannot perform operation on different currencies: %s and %s", 
                    this.currency, other.currency));
        }
    }
    
    @Override
    public String toString() {
        return String.format("%s %s", currency, amount.toPlainString());
    }
}