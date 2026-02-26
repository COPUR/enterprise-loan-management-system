package com.loanmanagement.loan.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LoanCreatedEvent(
        Long loanId,
        Long customerId,
        String principalAmount,
        String currency,
        BigDecimal interestRate,
        Integer termMonths,
        String monthlyPayment,
        LocalDateTime occurredAt
) {
    public LoanCreatedEvent(Long loanId, Long customerId, String principalAmount,
                          String currency, BigDecimal interestRate, Integer termMonths,
                          String monthlyPayment) {
        this(loanId, customerId, principalAmount, currency, interestRate, 
             termMonths, monthlyPayment, LocalDateTime.now());
    }
}