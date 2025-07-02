package com.bank.loan.loan.domain.customer;

import com.bank.loan.sharedkernel.domain.Money;

/**
 * Domain exception thrown when attempting to reserve credit that exceeds available limit.
 */
public class InsufficientCreditException extends RuntimeException {
    
    private final CustomerId customerId;
    private final Money requestedAmount;
    private final Money availableAmount;
    
    public InsufficientCreditException(CustomerId customerId, Money requestedAmount, Money availableAmount) {
        super(String.format(
            "Insufficient credit for customer %s: requested %s, available %s",
            customerId, requestedAmount, availableAmount
        ));
        this.customerId = customerId;
        this.requestedAmount = requestedAmount;
        this.availableAmount = availableAmount;
    }
    
    public CustomerId getCustomerId() {
        return customerId;
    }
    
    public Money getRequestedAmount() {
        return requestedAmount;
    }
    
    public Money getAvailableAmount() {
        return availableAmount;
    }
}