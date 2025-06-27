package com.banking.loan.application.results;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Summary loan information result
 */
public record LoanSummary(
    String loanId,
    BigDecimal amount,
    BigDecimal interestRate,
    Integer termMonths,
    String status,
    LocalDate applicationDate,
    LocalDate approvalDate,
    BigDecimal outstandingBalance,
    LocalDate nextPaymentDate,
    BigDecimal nextPaymentAmount,
    LocalDateTime lastModified
) {}
