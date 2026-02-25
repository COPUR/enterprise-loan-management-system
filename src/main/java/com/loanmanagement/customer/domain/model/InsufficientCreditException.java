package com.loanmanagement.customer.domain.model;

/**
 * Domain exception thrown when attempting to reserve more credit than available
 * This is a business rule violation and should be handled appropriately
 */
public class InsufficientCreditException extends RuntimeException {
    
    public InsufficientCreditException(String message) {
        super(message);
    }
    
    public InsufficientCreditException(String message, Throwable cause) {
        super(message, cause);
    }
}