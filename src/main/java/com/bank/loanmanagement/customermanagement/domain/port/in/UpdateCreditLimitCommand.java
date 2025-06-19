package com.bank.loanmanagement.customermanagement.domain.port.in;

import com.bank.loanmanagement.customermanagement.domain.model.CustomerId;
import com.bank.loanmanagement.sharedkernel.domain.Money;

import java.util.Objects;

/**
 * Command for updating a customer's credit limit.
 */
public record UpdateCreditLimitCommand(
    CustomerId customerId,
    Money newCreditLimit,
    String reason
) {
    
    public UpdateCreditLimitCommand {
        Objects.requireNonNull(customerId, "Customer ID is required");
        Objects.requireNonNull(newCreditLimit, "New credit limit is required");
        Objects.requireNonNull(reason, "Reason is required");
        
        if (reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason cannot be empty");
        }
        
        if (!newCreditLimit.isPositive()) {
            throw new IllegalArgumentException("Credit limit must be positive");
        }
    }
}