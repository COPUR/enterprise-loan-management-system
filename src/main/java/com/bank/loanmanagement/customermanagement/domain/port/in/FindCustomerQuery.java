package com.bank.loanmanagement.customermanagement.domain.port.in;

import com.bank.loanmanagement.customermanagement.domain.model.CustomerId;

import java.util.Objects;

/**
 * Query for finding a customer by ID.
 */
public record FindCustomerQuery(
    CustomerId customerId
) {
    
    public FindCustomerQuery {
        Objects.requireNonNull(customerId, "Customer ID is required");
    }
}