package com.banking.loan.domain.loan;

import java.math.BigDecimal;

public record LoanAmount(BigDecimal value, String currency) {
    public static LoanAmount of(BigDecimal value, String currency) {
        return new LoanAmount(value, currency);
    }
    
    public static LoanAmount of(double value, String currency) {
        return new LoanAmount(BigDecimal.valueOf(value), currency);
    }
    
    public LoanAmount add(LoanAmount other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add amounts with different currencies");
        }
        return new LoanAmount(this.value.add(other.value), this.currency);
    }
    
    public LoanAmount subtract(LoanAmount other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot subtract amounts with different currencies");
        }
        return new LoanAmount(this.value.subtract(other.value), this.currency);
    }
}