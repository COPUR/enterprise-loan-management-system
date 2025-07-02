package com.bank.customer.domain.port.in;

import com.bank.loan.loan.domain.customer.CustomerId;

import java.util.Objects;
import java.util.Optional;

/**
 * Command for updating existing customer information.
 */
public record UpdateCustomerCommand(
    CustomerId customerId,
    Optional<String> firstName,
    Optional<String> lastName,
    Optional<String> email,
    Optional<String> phoneNumber
) {
    
    public UpdateCustomerCommand {
        Objects.requireNonNull(customerId, "Customer ID is required");
        Objects.requireNonNull(firstName, "First name optional cannot be null");
        Objects.requireNonNull(lastName, "Last name optional cannot be null");
        Objects.requireNonNull(email, "Email optional cannot be null");
        Objects.requireNonNull(phoneNumber, "Phone number optional cannot be null");
        
        // Validate provided values
        firstName.ifPresent(name -> {
            if (name.trim().isEmpty()) {
                throw new IllegalArgumentException("First name cannot be empty when provided");
            }
        });
        
        lastName.ifPresent(name -> {
            if (name.trim().isEmpty()) {
                throw new IllegalArgumentException("Last name cannot be empty when provided");
            }
        });
        
        email.ifPresent(emailValue -> {
            if (emailValue.trim().isEmpty()) {
                throw new IllegalArgumentException("Email cannot be empty when provided");
            }
        });
        
        phoneNumber.ifPresent(phone -> {
            if (phone.trim().isEmpty()) {
                throw new IllegalArgumentException("Phone number cannot be empty when provided");
            }
        });
    }
}