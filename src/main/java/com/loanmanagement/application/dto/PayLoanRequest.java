package com.loanmanagement.application.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for loan payment.
 * Follows clean code principles with proper validation and documentation.
 */
public record PayLoanRequest(
    @NotNull(message = "Loan ID is required")
    @Positive(message = "Loan ID must be positive")
    Long loanId,

    @NotNull(message = "Payment amount is required")
    @Positive(message = "Payment amount must be positive")
    @DecimalMax(value = "${loan.validation.max-payment:1000000}", message = "Payment amount too large")
    BigDecimal amount,

    @NotNull(message = "Payment date is required")
    LocalDate paymentDate
) {}
