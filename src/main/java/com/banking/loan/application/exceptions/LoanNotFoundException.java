package com.banking.loan.application.exceptions;

public class LoanNotFoundException extends RuntimeException {
    public LoanNotFoundException(String message) {
        super(message);
    }
    
    public LoanNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}