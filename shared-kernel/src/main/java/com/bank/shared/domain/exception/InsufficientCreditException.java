package com.bank.shared.domain.exception;

/**
 * Exception thrown when a customer has insufficient credit for a requested operation
 */
public class InsufficientCreditException extends BusinessException {
    
    private final String customerId;
    private final Integer currentCreditScore;
    private final Integer requiredCreditScore;
    
    public InsufficientCreditException(String customerId, Integer currentCreditScore, Integer requiredCreditScore) {
        super(String.format("Insufficient credit for customer %s. Current score: %d, Required: %d", 
            customerId, currentCreditScore, requiredCreditScore));
        this.customerId = customerId;
        this.currentCreditScore = currentCreditScore;
        this.requiredCreditScore = requiredCreditScore;
    }
    
    public InsufficientCreditException(String customerId, String message) {
        super(message);
        this.customerId = customerId;
        this.currentCreditScore = null;
        this.requiredCreditScore = null;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public Integer getCurrentCreditScore() {
        return currentCreditScore;
    }
    
    public Integer getRequiredCreditScore() {
        return requiredCreditScore;
    }
}