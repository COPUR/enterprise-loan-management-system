package com.banking.loan.domain.loan;

import java.math.BigDecimal;

public record InterestRate(BigDecimal value) {
    public static InterestRate of(BigDecimal value) {
        return new InterestRate(value);
    }
    
    public static InterestRate percentage(double percentage) {
        return new InterestRate(BigDecimal.valueOf(percentage));
    }
    
    /**
     * Get monthly interest rate from annual rate
     * Following DDD principle: domain logic encapsulated in value objects
     */
    public BigDecimal getMonthlyRate() {
        return value.divide(BigDecimal.valueOf(100 * 12), 6, java.math.RoundingMode.HALF_UP);
    }
}