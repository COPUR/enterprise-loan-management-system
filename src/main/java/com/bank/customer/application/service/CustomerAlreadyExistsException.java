package com.bank.customer.application.service;

/**
 * Application exception thrown when attempting to create a customer that already exists.
 */
public class CustomerAlreadyExistsException extends RuntimeException {
    
    public CustomerAlreadyExistsException(String message) {
        super(message);
    }
    
    public CustomerAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}