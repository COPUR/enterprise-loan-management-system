package com.bank.payment.application;

/**
 * Exception thrown when an account is invalid for payment processing
 */
public class InvalidAccountException extends RuntimeException {
    
    public InvalidAccountException(String message) {
        super(message);
    }
    
    public InvalidAccountException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static InvalidAccountException forAccount(String accountId, String reason) {
        return new InvalidAccountException(String.format("Invalid account %s: %s", accountId, reason));
    }
    
    public static InvalidAccountException fromAccount(String accountId) {
        return new InvalidAccountException("Invalid from account: " + accountId);
    }
    
    public static InvalidAccountException toAccount(String accountId) {
        return new InvalidAccountException("Invalid to account: " + accountId);
    }
}