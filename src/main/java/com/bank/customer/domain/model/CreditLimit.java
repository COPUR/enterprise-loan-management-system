package com.bank.loan.loan.domain.customer;

import com.bank.loan.sharedkernel.domain.Money;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value object representing a customer's credit limit.
 * Encapsulates business rules for credit limit validation.
 */
public final class CreditLimit {
    
    private static final Money MINIMUM_CREDIT_LIMIT = Money.usd(BigDecimal.valueOf(1000));
    private static final Money MAXIMUM_CREDIT_LIMIT = Money.usd(BigDecimal.valueOf(10_000_000));
    
    private final Money amount;
    
    private CreditLimit(Money amount) {
        this.amount = validateCreditLimit(amount);
    }
    
    public static CreditLimit of(Money amount) {
        return new CreditLimit(amount);
    }
    
    public static CreditLimit ofUsd(BigDecimal amount) {
        return new CreditLimit(Money.usd(amount));
    }
    
    private Money validateCreditLimit(Money amount) {
        Objects.requireNonNull(amount, "Credit limit amount cannot be null");
        
        if (amount.isLessThan(MINIMUM_CREDIT_LIMIT)) {
            throw new IllegalArgumentException(
                String.format("Credit limit cannot be less than %s", MINIMUM_CREDIT_LIMIT)
            );
        }
        
        if (amount.isGreaterThan(MAXIMUM_CREDIT_LIMIT)) {
            throw new IllegalArgumentException(
                String.format("Credit limit cannot exceed %s", MAXIMUM_CREDIT_LIMIT)
            );
        }
        
        return amount;
    }
    
    public Money getAmount() {
        return amount;
    }
    
    public CreditLimit increase(Money additionalAmount) {
        return new CreditLimit(amount.add(additionalAmount));
    }
    
    public CreditLimit decrease(Money reductionAmount) {
        return new CreditLimit(amount.subtract(reductionAmount));
    }
    
    public boolean isWithinLimit(Money requestedAmount) {
        return requestedAmount.isLessThanOrEqualTo(amount);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreditLimit that = (CreditLimit) o;
        return Objects.equals(amount, that.amount);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(amount);
    }
    
    @Override
    public String toString() {
        return "CreditLimit{" + amount + "}";
    }
}