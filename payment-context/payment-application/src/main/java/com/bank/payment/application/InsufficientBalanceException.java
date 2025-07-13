package com.bank.payment.application;

/**
 * Exception thrown when account has insufficient balance for payment
 */
public class InsufficientBalanceException extends RuntimeException {
    
    public InsufficientBalanceException(String message) {
        super(message);
    }
    
    public InsufficientBalanceException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static InsufficientBalanceException forAccount(String accountId, String requestedAmount, String availableAmount) {
        return new InsufficientBalanceException(
            String.format("Account %s has insufficient balance. Requested: %s, Available: %s", 
                accountId, requestedAmount, availableAmount));
    }
}