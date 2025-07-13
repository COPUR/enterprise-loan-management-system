package com.bank.customer.application.dto;

import com.bank.shared.kernel.domain.Money;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * DTO for creating a new customer
 * 
 * Functional Requirements:
 * - FR-001: Customer Registration
 * - FR-002: KYC Information Collection
 * - FR-003: Credit Limit Assignment
 */
public record CreateCustomerRequest(
    @NotBlank(message = "First name is required")
    String firstName,
    
    @NotBlank(message = "Last name is required") 
    String lastName,
    
    @Email(message = "Valid email is required")
    @NotBlank(message = "Email is required")
    String email,
    
    String phoneNumber,
    
    @NotNull(message = "Initial credit limit is required")
    @Positive(message = "Credit limit must be positive")
    BigDecimal initialCreditLimit,
    
    String currency
) {
    
    public Money getCreditLimitAsMoney() {
        Currency curr = currency != null ? Currency.getInstance(currency) : Currency.getInstance("USD");
        return Money.of(initialCreditLimit, curr);
    }
    
    // Business validation
    public void validate() {
        if (firstName.trim().length() < 2) {
            throw new IllegalArgumentException("First name must be at least 2 characters");
        }
        if (lastName.trim().length() < 2) {
            throw new IllegalArgumentException("Last name must be at least 2 characters");
        }
        if (initialCreditLimit.compareTo(BigDecimal.valueOf(1000)) < 0) {
            throw new IllegalArgumentException("Minimum credit limit is $1,000");
        }
        if (initialCreditLimit.compareTo(BigDecimal.valueOf(1000000)) > 0) {
            throw new IllegalArgumentException("Maximum credit limit is $1,000,000");
        }
    }
}