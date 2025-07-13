package com.bank.customer.domain;

/**
 * Domain Exception indicating that a customer has insufficient credit
 * for a requested operation.
 */
public class InsufficientCreditException extends RuntimeException {
    
    public InsufficientCreditException(String message) {
        super(message);
    }
    
    public InsufficientCreditException(String message, Throwable cause) {
        super(message, cause);
    }
}