package com.bank.loanmanagement.loan.security.exceptions;

/**
 * Exception thrown for FAPI (Financial-grade API) security violations
 * Follows DDD exception modeling principles
 */
public class FAPISecurityException extends RuntimeException {
    
    public FAPISecurityException(String message) {
        super(message);
    }
    
    public FAPISecurityException(String message, Throwable cause) {
        super(message, cause);
    }
}