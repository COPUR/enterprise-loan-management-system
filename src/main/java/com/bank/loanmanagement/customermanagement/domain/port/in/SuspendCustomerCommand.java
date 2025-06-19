package com.bank.loanmanagement.customermanagement.domain.port.in;

import com.bank.loanmanagement.customermanagement.domain.model.CustomerId;

import java.util.Objects;

/**
 * Command for suspending an active customer account.
 */
public record SuspendCustomerCommand(
    CustomerId customerId,
    String reason
) {
    
    public SuspendCustomerCommand {
        Objects.requireNonNull(customerId, "Customer ID is required");
        Objects.requireNonNull(reason, "Suspension reason is required");
        
        if (reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Suspension reason cannot be empty");
        }
    }
}