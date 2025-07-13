package com.bank.customer.application;

/**
 * Exception thrown when a customer cannot be found
 */
public class CustomerNotFoundException extends RuntimeException {
    
    public CustomerNotFoundException(String message) {
        super(message);
    }
    
    public CustomerNotFoundException(String customerId, Throwable cause) {
        super("Customer not found with ID: " + customerId, cause);
    }
    
    public static CustomerNotFoundException withId(String customerId) {
        return new CustomerNotFoundException("Customer not found with ID: " + customerId);
    }
}