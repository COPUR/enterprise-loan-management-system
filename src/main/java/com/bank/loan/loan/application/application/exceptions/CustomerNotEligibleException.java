package com.bank.loanmanagement.loan.application.exceptions;

public class CustomerNotEligibleException extends RuntimeException {
    public CustomerNotEligibleException(String message) {
        super(message);
    }
    
    public CustomerNotEligibleException(String message, Throwable cause) {
        super(message, cause);
    }
}