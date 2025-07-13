package com.bank.shared.domain.exception;

/**
 * Exception thrown when an invalid account is referenced
 */
public class InvalidAccountException extends BusinessException {
    
    private final String accountId;
    private final String accountType;
    private final String reason;
    
    public InvalidAccountException(String accountId, String accountType, String reason) {
        super(String.format("Invalid account - ID: %s, Type: %s, Reason: %s", 
            accountId, accountType, reason));
        this.accountId = accountId;
        this.accountType = accountType;
        this.reason = reason;
    }
    
    public InvalidAccountException(String accountId, String reason) {
        super(String.format("Invalid account %s: %s", accountId, reason));
        this.accountId = accountId;
        this.accountType = null;
        this.reason = reason;
    }
    
    public InvalidAccountException(String message) {
        super(message);
        this.accountId = null;
        this.accountType = null;
        this.reason = null;
    }
    
    public String getAccountId() {
        return accountId;
    }
    
    public String getAccountType() {
        return accountType;
    }
    
    public String getReason() {
        return reason;
    }
}