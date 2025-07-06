package com.bank.loan.loan.exception;

/**
 * Exception thrown when a loan is not found
 */
public class LoanNotFoundException extends RuntimeException {
    
    public LoanNotFoundException(String message) {
        super(message);
    }
    
    public LoanNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}