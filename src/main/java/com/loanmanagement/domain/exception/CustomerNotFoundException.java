package com.loanmanagement.domain.exception;

/**
 * Domain exception thrown when a customer is not found.
 * Follows DDD principles by representing a business rule violation.
 */
public class CustomerNotFoundException extends RuntimeException {

    private final Long customerId;

    public CustomerNotFoundException(Long customerId) {
        super("Customer not found with ID: " + customerId);
        this.customerId = customerId;
    }

    public CustomerNotFoundException(String message) {
        super(message);
        this.customerId = null;
    }

    public Long getCustomerId() {
        return customerId;
    }
}
