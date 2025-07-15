package com.bank.customer.domain;

import com.bank.shared.kernel.domain.Money;
import com.bank.shared.kernel.domain.ValueObject;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value Object representing a customer's credit profile
 * 
 * Encapsulates credit limit, used credit, and business rules
 * for credit management.
 */
public final class CreditProfile implements ValueObject {
    
    private final Money creditLimit;
    private final Money usedCredit;
    
    private CreditProfile(Money creditLimit, Money usedCredit) {
        this.creditLimit = Objects.requireNonNull(creditLimit, "Credit limit cannot be null");
        this.usedCredit = Objects.requireNonNull(usedCredit, "Used credit cannot be null");
        
        if (creditLimit.isNegative()) {
            throw new IllegalArgumentException("Credit limit cannot be negative");
        }
        if (usedCredit.isNegative()) {
            throw new IllegalArgumentException("Used credit cannot be negative");
        }
        if (usedCredit.compareTo(creditLimit) > 0) {
            throw new IllegalArgumentException("Used credit cannot exceed credit limit");
        }
    }
    
    public static CreditProfile create(Money creditLimit) {
        Money zeroAmount = Money.zero(creditLimit.getCurrency());
        return new CreditProfile(creditLimit, zeroAmount);
    }
    
    public static CreditProfile create(Money creditLimit, Money usedCredit) {
        return new CreditProfile(creditLimit, usedCredit);
    }
    
    public Money getCreditLimit() {
        return creditLimit;
    }
    
    public Money getUsedCredit() {
        return usedCredit;
    }
    
    public Money getAvailableCredit() {
        return creditLimit.subtract(usedCredit);
    }
    
    public boolean canBorrow(Money amount) {
        if (amount == null || amount.isNegative() || amount.isZero()) {
            return false;
        }
        return getAvailableCredit().compareTo(amount) >= 0;
    }
    
    public CreditProfile reserveCredit(Money amount) {
        if (!canBorrow(amount)) {
            throw new IllegalArgumentException("Insufficient available credit");
        }
        return new CreditProfile(creditLimit, usedCredit.add(amount));
    }
    
    public CreditProfile releaseCredit(Money amount) {
        Money newUsedCredit = usedCredit.subtract(amount);
        if (newUsedCredit.isNegative()) {
            newUsedCredit = Money.zero(usedCredit.getCurrency());
        }
        return new CreditProfile(creditLimit, newUsedCredit);
    }
    
    public CreditProfile updateCreditLimit(Money newCreditLimit) {
        if (newCreditLimit.compareTo(usedCredit) < 0) {
            throw new IllegalArgumentException("New credit limit cannot be less than used credit");
        }
        return new CreditProfile(newCreditLimit, usedCredit);
    }
    
    public BigDecimal getCreditUtilizationRatio() {
        if (creditLimit.isZero()) {
            return BigDecimal.ZERO;
        }
        return usedCredit.getAmount().divide(creditLimit.getAmount(), 4, java.math.RoundingMode.HALF_UP);
    }
    
    @Override
    public boolean isEmpty() {
        return creditLimit.isZero();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CreditProfile that = (CreditProfile) obj;
        return Objects.equals(creditLimit, that.creditLimit) && Objects.equals(usedCredit, that.usedCredit);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(creditLimit, usedCredit);
    }
    
    @Override
    public String toString() {
        return String.format("CreditProfile{limit=%s, used=%s, available=%s}", 
            creditLimit, usedCredit, getAvailableCredit());
    }
}