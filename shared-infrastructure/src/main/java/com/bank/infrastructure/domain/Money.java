package com.bank.infrastructure.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Money value object for banking operations
 * 
 * Immutable representation of monetary amounts with currency.
 * Follows DDD principles for value objects.
 */
public record Money(
    String currency,
    BigDecimal amount
) {
    public Money {
        Objects.requireNonNull(currency, "Currency cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Amount cannot have more than 2 decimal places");
        }
    }
    
    public Money(String currency, double amount) {
        this(currency, BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP));
    }
    
    public Money(String currency, long amount) {
        this(currency, BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP));
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public Money add(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add different currencies");
        }
        return new Money(currency, amount.add(other.amount));
    }
    
    public Money subtract(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot subtract different currencies");
        }
        return new Money(currency, amount.subtract(other.amount));
    }
    
    public Money multiply(BigDecimal factor) {
        return new Money(currency, amount.multiply(factor).setScale(2, RoundingMode.HALF_UP));
    }
    
    public Money divide(BigDecimal divisor) {
        return new Money(currency, amount.divide(divisor, 2, RoundingMode.HALF_UP));
    }
    
    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }
    
    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }
    
    public String toFormattedString() {
        return String.format("%s %s", currency, amount.toPlainString());
    }
    
    @Override
    public String toString() {
        return toFormattedString();
    }
}