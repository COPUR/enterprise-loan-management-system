package com.banking.loan.domain.loan;

import java.math.BigDecimal;

public record PaymentAmount(BigDecimal value, String currency) {
    public static PaymentAmount of(BigDecimal value, String currency) {
        return new PaymentAmount(value, currency);
    }
    
    public static PaymentAmount of(double value, String currency) {
        return new PaymentAmount(BigDecimal.valueOf(value), currency);
    }
}