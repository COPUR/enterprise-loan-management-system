package com.bank.loanmanagement.loan.application.exceptions;

public class ComplianceViolationException extends RuntimeException {
    public ComplianceViolationException(String message) {
        super(message);
    }
    
    public ComplianceViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}