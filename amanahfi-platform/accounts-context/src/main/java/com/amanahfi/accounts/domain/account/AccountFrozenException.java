package com.amanahfi.accounts.domain.account;

/**
 * Exception thrown when attempting operations on a frozen account
 */
public class AccountFrozenException extends RuntimeException {
    
    public AccountFrozenException(String message) {
        super(message);
    }
    
    public AccountFrozenException(String message, Throwable cause) {
        super(message, cause);
    }
}