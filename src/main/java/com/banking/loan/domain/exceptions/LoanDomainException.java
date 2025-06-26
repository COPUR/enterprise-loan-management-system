package com.banking.loan.domain.exceptions;

public class LoanDomainException extends RuntimeException {
    public LoanDomainException(String message) {
        super(message);
    }
    
    public LoanDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}