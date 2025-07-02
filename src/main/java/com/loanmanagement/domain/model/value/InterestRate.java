// domain/model/value/InterestRate.java
package com.loanmanagement.domain.model.value;

import java.math.BigDecimal;

public class InterestRate {
    private static final BigDecimal MIN_RATE = new BigDecimal("0.1");
    private static final BigDecimal MAX_RATE = new BigDecimal("0.5");
    
    private final BigDecimal rate;
    
    public InterestRate(BigDecimal rate) {
        if (rate == null || rate.compareTo(MIN_RATE) < 0 || rate.compareTo(MAX_RATE) > 0) {
            throw new IllegalArgumentException(
                String.format("Interest rate must be between %s and %s", MIN_RATE, MAX_RATE)
            );
        }
        this.rate = rate;
    }
    
    public Money calculateInterest(Money principal) {
        return principal.multiply(rate);
    }
    
    public BigDecimal getValue() {
        return rate;
    }
}