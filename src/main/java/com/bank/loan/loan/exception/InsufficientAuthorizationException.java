package com.bank.loan.loan.exception;

/**
 * Exception thrown when user lacks sufficient authorization for an operation
 */
public class InsufficientAuthorizationException extends RuntimeException {
    
    public InsufficientAuthorizationException(String message) {
        super(message);
    }
    
    public InsufficientAuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }
}