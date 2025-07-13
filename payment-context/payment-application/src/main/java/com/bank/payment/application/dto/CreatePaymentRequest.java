package com.bank.payment.application.dto;

import com.bank.payment.domain.PaymentType;
import com.bank.shared.kernel.domain.Money;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * DTO for creating a new payment
 * 
 * Functional Requirements:
 * - FR-009: Payment Processing & Settlement
 * - FR-010: Payment Validation & Compliance
 */
public record CreatePaymentRequest(
    @NotBlank(message = "Customer ID is required")
    String customerId,
    
    @NotBlank(message = "From account ID is required")
    String fromAccountId,
    
    @NotBlank(message = "To account ID is required")
    String toAccountId,
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    BigDecimal amount,
    
    String currency,
    
    @NotNull(message = "Payment type is required")
    PaymentType paymentType,
    
    String description
) {
    
    public Money getAmountAsMoney() {
        Currency curr = currency != null ? Currency.getInstance(currency) : Currency.getInstance("USD");
        return Money.of(amount, curr);
    }
    
    // Business validation
    public void validate() {
        if (amount.compareTo(BigDecimal.valueOf(0.01)) < 0) {
            throw new IllegalArgumentException("Minimum payment amount is $0.01");
        }
        if (amount.compareTo(BigDecimal.valueOf(100000000)) > 0) {
            throw new IllegalArgumentException("Maximum payment amount is $100,000,000");
        }
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("From and to accounts cannot be the same");
        }
    }
}