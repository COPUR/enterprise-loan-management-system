package com.bank.loanmanagement.customermanagement.domain.port.in;

import com.bank.loanmanagement.customermanagement.domain.model.CustomerId;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Command for creating a new customer.
 * Encapsulates all required data for customer creation with validation.
 */
public record CreateCustomerCommand(
    String firstName,
    String lastName,
    String email,
    String phoneNumber,
    BigDecimal initialCreditLimit
) {
    
    public CreateCustomerCommand {
        Objects.requireNonNull(firstName, "First name is required");
        Objects.requireNonNull(lastName, "Last name is required");
        Objects.requireNonNull(email, "Email is required");
        Objects.requireNonNull(phoneNumber, "Phone number is required");
        Objects.requireNonNull(initialCreditLimit, "Initial credit limit is required");
        
        if (firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name cannot be empty");
        }
        
        if (lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name cannot be empty");
        }
        
        if (email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        
        if (phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty");
        }
        
        if (initialCreditLimit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Initial credit limit must be positive");
        }
        
        if (initialCreditLimit.compareTo(BigDecimal.valueOf(10_000_000)) > 0) {
            throw new IllegalArgumentException("Initial credit limit cannot exceed $10,000,000");
        }
    }
}