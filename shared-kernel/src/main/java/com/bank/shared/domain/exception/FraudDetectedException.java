package com.bank.shared.domain.exception;

/**
 * Exception thrown when fraud is detected in a transaction
 */
public class FraudDetectedException extends SecurityException {
    
    private final String transactionId;
    private final String paymentId;
    private final Integer riskScore;
    private final String fraudReason;
    
    public FraudDetectedException(String transactionId, String paymentId, Integer riskScore, String fraudReason) {
        super(String.format("Fraud detected - Transaction: %s, Payment: %s, Risk Score: %d, Reason: %s", 
            transactionId, paymentId, riskScore, fraudReason));
        this.transactionId = transactionId;
        this.paymentId = paymentId;
        this.riskScore = riskScore;
        this.fraudReason = fraudReason;
    }
    
    public FraudDetectedException(String message) {
        super(message);
        this.transactionId = null;
        this.paymentId = null;
        this.riskScore = null;
        this.fraudReason = null;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public String getPaymentId() {
        return paymentId;
    }
    
    public Integer getRiskScore() {
        return riskScore;
    }
    
    public String getFraudReason() {
        return fraudReason;
    }
}