package com.bank.ml.anomaly.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Transaction Data Model
 * Represents transaction information for ML-based fraud detection
 */
public class TransactionData {
    
    private final String transactionId;
    private final String fromAccount;
    private final String toAccount;
    private final double amount;
    private final LocalDateTime timestamp;
    private final String transactionType;
    private final Map<String, Object> context;
    
    public TransactionData(String transactionId, String fromAccount, String toAccount, 
                          double amount, LocalDateTime timestamp, String transactionType,
                          Map<String, Object> context) {
        this.transactionId = transactionId;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.timestamp = timestamp;
        this.transactionType = transactionType;
        this.context = context;
    }
    
    // Getters
    public String getTransactionId() { return transactionId; }
    public String getFromAccount() { return fromAccount; }
    public String getToAccount() { return toAccount; }
    public double getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getTransactionType() { return transactionType; }
    public Map<String, Object> getContext() { return context; }
}