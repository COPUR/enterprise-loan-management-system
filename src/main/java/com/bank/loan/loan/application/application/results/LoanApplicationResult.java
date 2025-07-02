package com.bank.loanmanagement.loan.application.results;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LoanApplicationResult(
    String loanId,
    String applicationReference,
    String customerId,
    BigDecimal amount,
    String status,
    LocalDateTime createdAt,
    String message
) {}