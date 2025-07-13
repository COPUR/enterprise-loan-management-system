package com.bank.payment.infrastructure.fraud;

import com.bank.payment.domain.PaymentType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction Context for Behavioral Analysis
 * 
 * Encapsulates transaction data for behavioral pattern analysis
 */
public class TransactionContext {
    
    private final BigDecimal amount;
    private final PaymentType paymentType;
    private final LocalDateTime timestamp;
    private final String fromAccount;
    private final String toAccount;
    private final String description;
    
    private TransactionContext(Builder builder) {
        this.amount = builder.amount;
        this.paymentType = builder.paymentType;
        this.timestamp = builder.timestamp;
        this.fromAccount = builder.fromAccount;
        this.toAccount = builder.toAccount;
        this.description = builder.description;
    }
    
    // Getters
    public BigDecimal getAmount() { return amount; }
    public PaymentType getPaymentType() { return paymentType; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getFromAccount() { return fromAccount; }
    public String getToAccount() { return toAccount; }
    public String getDescription() { return description; }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private BigDecimal amount;
        private PaymentType paymentType;
        private LocalDateTime timestamp;
        private String fromAccount;
        private String toAccount;
        private String description;
        
        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }
        
        public Builder paymentType(PaymentType paymentType) {
            this.paymentType = paymentType;
            return this;
        }
        
        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder fromAccount(String fromAccount) {
            this.fromAccount = fromAccount;
            return this;
        }
        
        public Builder toAccount(String toAccount) {
            this.toAccount = toAccount;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public TransactionContext build() {
            return new TransactionContext(this);
        }
    }
}