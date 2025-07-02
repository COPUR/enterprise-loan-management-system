package com.loanmanagement.application.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Request DTO for loan creation.
 * Validation constraints externalized per 12-Factor App principles.
 */
public record CreateLoanRequest(
    @NotNull(message = "Customer ID is required")
    @Positive(message = "Customer ID must be positive")
    Long customerId,

    @NotNull(message = "Loan amount is required")
    @DecimalMin(value = "${loan.validation.amount.min:1000}", message = "Loan amount too low")
    @DecimalMax(value = "${loan.validation.amount.max:500000}", message = "Loan amount too high")
    BigDecimal amount,

    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "${loan.validation.interest-rate.min:0.01}", message = "Interest rate too low")
    @DecimalMax(value = "${loan.validation.interest-rate.max:0.50}", message = "Interest rate too high")
    BigDecimal interestRate,

    @NotNull(message = "Number of installments is required")
    @Min(value = 1, message = "Must have at least 1 installment")
    @Max(value = 60, message = "Cannot exceed 60 installments")
    Integer numberOfInstallments
) {}

// application/dto/CreateLoanResponse.java


// application/dto/LoanDto.java

// application/dto/InstallmentDto.java

// application/dto/PayLoanRequest.java

// application/dto/PayLoanResponse.java
