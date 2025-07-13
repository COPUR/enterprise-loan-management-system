package com.bank.loan.application.dto;

import com.bank.shared.kernel.domain.Money;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * DTO for creating a new loan application
 * 
 * Functional Requirements:
 * - FR-005: Loan Application & Origination
 * - FR-006: Credit Assessment & Risk Evaluation
 */
public record CreateLoanRequest(
    @NotBlank(message = "Customer ID is required")
    String customerId,
    
    @NotNull(message = "Principal amount is required")
    @Positive(message = "Principal amount must be positive")
    BigDecimal principalAmount,
    
    String currency,
    
    @NotNull(message = "Annual interest rate is required")
    @Positive(message = "Interest rate must be positive")
    BigDecimal annualInterestRate,
    
    @NotNull(message = "Term in months is required")
    @Positive(message = "Term must be positive")
    Integer termInMonths
) {
    
    public Money getPrincipalAsMoney() {
        Currency curr = currency != null ? Currency.getInstance(currency) : Currency.getInstance("USD");
        return Money.of(principalAmount, curr);
    }
    
    // Business validation
    public void validate() {
        if (principalAmount.compareTo(BigDecimal.valueOf(1000)) < 0) {
            throw new IllegalArgumentException("Minimum loan amount is $1,000");
        }
        if (principalAmount.compareTo(BigDecimal.valueOf(10000000)) > 0) {
            throw new IllegalArgumentException("Maximum loan amount is $10,000,000");
        }
        if (annualInterestRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Interest rate must be positive");
        }
        if (annualInterestRate.compareTo(BigDecimal.valueOf(50)) > 0) {
            throw new IllegalArgumentException("Interest rate cannot exceed 50%");
        }
        if (termInMonths < 6) {
            throw new IllegalArgumentException("Minimum loan term is 6 months");
        }
        if (termInMonths > 480) { // 40 years
            throw new IllegalArgumentException("Maximum loan term is 40 years");
        }
    }
}