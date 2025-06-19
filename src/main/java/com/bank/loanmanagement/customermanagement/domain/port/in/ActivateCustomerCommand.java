package com.bank.loanmanagement.customermanagement.domain.port.in;

import com.bank.loanmanagement.customermanagement.domain.model.CustomerId;

import java.util.Objects;

/**
 * Command for activating a pending customer account.
 */
public record ActivateCustomerCommand(
    CustomerId customerId
) {
    
    public ActivateCustomerCommand {
        Objects.requireNonNull(customerId, "Customer ID is required");
    }
}