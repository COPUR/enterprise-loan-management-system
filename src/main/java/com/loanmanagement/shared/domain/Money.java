package com.loanmanagement.shared.domain;

import java.math.BigDecimal;

/**
 * Money value object for banking precision financial calculations
 * Following DDD principles for shared kernel
 */
public class Money {
    
    private final com.loanmanagement.shared.domain.model.Money delegate;
    
    private Money(com.loanmanagement.shared.domain.model.Money delegate) {
        this.delegate = delegate;
    }
    
    public Money(String amount, String currency) {
        this.delegate = new com.loanmanagement.shared.domain.model.Money(amount, currency);
    }
    
    public static Money of(String currency, BigDecimal amount) {
        return new Money(com.loanmanagement.shared.domain.model.Money.of(amount, currency));
    }
    
    public static Money of(BigDecimal amount, String currency) {
        return new Money(com.loanmanagement.shared.domain.model.Money.of(amount, currency));
    }
    
    public static Money zero(String currency) {
        return new Money(com.loanmanagement.shared.domain.model.Money.zero(currency));
    }
    
    public Money add(Money other) {
        return new Money(this.delegate.add(other.delegate));
    }
    
    public Money subtract(Money other) {
        return new Money(this.delegate.subtract(other.delegate));
    }
    
    public Money multiply(BigDecimal multiplier) {
        return new Money(this.delegate.multiply(multiplier));
    }
    
    public Money divide(BigDecimal divisor) {
        return new Money(this.delegate.divide(divisor));
    }
    
    public boolean isGreaterThan(Money other) {
        return this.delegate.isGreaterThan(other.delegate);
    }
    
    public boolean isLessThan(Money other) {
        return this.delegate.isLessThan(other.delegate);
    }
    
    public boolean isZero() {
        return this.delegate.isZero();
    }
    
    public boolean isPositive() {
        return this.delegate.isPositive();
    }
    
    public boolean isNegative() {
        return this.delegate.isNegative();
    }
    
    public BigDecimal getAmount() {
        return delegate.getAmount();
    }
    
    public String getCurrency() {
        return delegate.getCurrency();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return delegate.equals(money.delegate);
    }
    
    @Override
    public int hashCode() {
        return delegate.hashCode();
    }
    
    @Override
    public String toString() {
        return delegate.toString();
    }
}