package com.banking.loan.application.exceptions;

public class LoanRejectionException extends RuntimeException {
    public LoanRejectionException(String message) {
        super(message);
    }
    
    public LoanRejectionException(String message, Throwable cause) {
        super(message, cause);
    }
}