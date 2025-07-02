// shared-kernel/domain/value/InterestRate.java
package com.loanmanagement.sharedkernel;

import java.math.BigDecimal;
import java.util.Objects;

public final class InterestRate {
    private static final BigDecimal MINIMUM_RATE = new BigDecimal("0.1");
    private static final BigDecimal MAXIMUM_RATE = new BigDecimal("0.5");
    
    private final BigDecimal value;
    
    private InterestRate(BigDecimal value) {
        validateRate(value);
        this.value = value;
    }
    
    public static InterestRate of(BigDecimal value) {
        return new InterestRate(value);
    }
    
    public static InterestRate of(String value) {
        return of(new BigDecimal(value));
    }
    
    private void validateRate(BigDecimal rate) {
        if (rate == null) {
            throw new IllegalArgumentException("Interest rate cannot be null");
        }
        if (rate.compareTo(MINIMUM_RATE) < 0 || rate.compareTo(MAXIMUM_RATE) > 0) {
            throw new IllegalArgumentException(
                String.format("Interest rate must be between %s and %s", 
                    MINIMUM_RATE, MAXIMUM_RATE)
            );
        }
    }
    
    public Money calculateInterestAmount(Money principal) {
        return principal.multiply(value);
    }
    
    public Money calculateTotalAmount(Money principal) {
        return principal.add(calculateInterestAmount(principal));
    }
    
    public BigDecimal getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InterestRate that = (InterestRate) o;
        return Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
