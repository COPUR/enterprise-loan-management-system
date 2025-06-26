package com.banking.loan.application.exceptions;

public class LoanApprovalException extends RuntimeException {
    public LoanApprovalException(String message) {
        super(message);
    }
    
    public LoanApprovalException(String message, Throwable cause) {
        super(message, cause);
    }
}