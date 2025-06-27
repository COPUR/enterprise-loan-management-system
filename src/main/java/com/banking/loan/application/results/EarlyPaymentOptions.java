package com.banking.loan.application.results;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record EarlyPaymentOptions(
    String loanId,
    BigDecimal currentBalance,
    BigDecimal earlyPaymentAmount,
    BigDecimal savingsAmount,
    LocalDateTime calculationDate,
    List<PaymentOption> options
) {
    public record PaymentOption(
        String optionType,
        BigDecimal amount,
        BigDecimal penalty,
        BigDecimal savings,
        String description
    ) {}
}