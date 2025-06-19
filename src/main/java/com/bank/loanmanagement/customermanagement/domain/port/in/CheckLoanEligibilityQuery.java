package com.bank.loanmanagement.customermanagement.domain.port.in;

import com.bank.loanmanagement.customermanagement.domain.model.CustomerId;
import com.bank.loanmanagement.sharedkernel.domain.Money;

import java.util.Objects;

/**
 * Query for checking if a customer is eligible for a loan of specific amount.
 */
public record CheckLoanEligibilityQuery(
    CustomerId customerId,
    Money loanAmount
) {
    
    public CheckLoanEligibilityQuery {
        Objects.requireNonNull(customerId, "Customer ID is required");
        Objects.requireNonNull(loanAmount, "Loan amount is required");
        
        if (!loanAmount.isPositive()) {
            throw new IllegalArgumentException("Loan amount must be positive");
        }
    }
}