package com.bank.loanmanagement.customermanagement.application.service;

/**
 * Application exception thrown when a requested customer cannot be found.
 */
public class CustomerNotFoundException extends RuntimeException {
    
    public CustomerNotFoundException(String message) {
        super(message);
    }
    
    public CustomerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}