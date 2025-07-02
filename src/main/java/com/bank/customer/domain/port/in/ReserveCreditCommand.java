package com.bank.customer.domain.port.in;

import com.bank.loan.loan.domain.customer.CustomerId;
import com.bank.loan.sharedkernel.domain.Money;

import java.util.Objects;

/**
 * Command for reserving credit for a customer.
 */
public record ReserveCreditCommand(
    CustomerId customerId,
    Money amount,
    String reason
) {
    
    public ReserveCreditCommand {
        Objects.requireNonNull(customerId, "Customer ID is required");
        Objects.requireNonNull(amount, "Amount is required");
        Objects.requireNonNull(reason, "Reason is required");
        
        if (reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason cannot be empty");
        }
        
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
}