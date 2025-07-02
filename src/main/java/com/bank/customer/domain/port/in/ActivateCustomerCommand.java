package com.bank.customer.domain.port.in;

import com.bank.loan.loan.domain.customer.CustomerId;

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