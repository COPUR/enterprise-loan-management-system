package com.bank.customer.domain.port.in;

import com.bank.loan.loan.domain.customer.CustomerId;

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