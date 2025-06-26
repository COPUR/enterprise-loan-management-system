package com.banking.loan.application.results;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record LoanDetails(
    String loanId,
    String customerId,
    BigDecimal amount,
    BigDecimal remainingAmount,
    String status,
    String loanType,
    Integer termInMonths,
    LocalDateTime createdAt,
    LocalDateTime approvedAt,
    List<InstallmentDetails> installments
) {
    public record InstallmentDetails(
        Integer installmentNumber,
        BigDecimal amount,
        BigDecimal remainingAmount,
        LocalDateTime dueDate,
        String status
    ) {}
}