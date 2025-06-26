package com.banking.loan.application.results;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LoanSummary(
    String loanId,
    String customerId,
    BigDecimal amount,
    BigDecimal remainingAmount,
    String status,
    String loanType,
    LocalDateTime createdAt,
    Integer totalInstallments,
    Integer paidInstallments
) {}