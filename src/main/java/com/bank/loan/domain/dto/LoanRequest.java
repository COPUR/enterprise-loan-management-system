package com.bank.loan.domain.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating a simple loan (used in SimpleLoanController tests)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanRequest {
    
    @NotBlank(message = "Customer ID is required")
    private String customerId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal amount;
    
    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.0", message = "Interest rate cannot be negative")
    @DecimalMax(value = "1.0", message = "Interest rate cannot exceed 100%")
    private BigDecimal interestRate;
    
    @NotNull(message = "Number of installments is required")
    @Min(value = 1, message = "Number of installments must be at least 1")
    private Integer numberOfInstallments;
}