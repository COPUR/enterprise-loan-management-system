package com.amanahfi.platform.cbdc.domain;

import com.amanahfi.platform.shared.domain.Money;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Objects;

/**
 * Digital Dirham transaction value object
 */
@Getter
@ToString
@Builder
public class Transaction {
    private final String transactionId;
    private final String fromWalletId;
    private final String toWalletId;
    private final Money amount;
    private final String reference;
    private final TransferType transferType;
    private TransactionStatus status;
    private final Instant createdAt;
    private Instant completedAt;
    private String cordaTransactionHash;
    private String failureReason;
    
    // Confirm transaction with Corda hash
    public void confirm(String cordaTransactionHash) {
        Objects.requireNonNull(cordaTransactionHash, "Corda transaction hash cannot be null");
        
        if (status != TransactionStatus.PENDING) {
            throw new IllegalStateException("Can only confirm pending transactions");
        }
        
        this.status = TransactionStatus.CONFIRMED;
        this.cordaTransactionHash = cordaTransactionHash;
        this.completedAt = Instant.now();
    }
    
    // Complete transaction
    public void complete() {
        if (status != TransactionStatus.CONFIRMED) {
            throw new IllegalStateException("Can only complete confirmed transactions");
        }
        
        this.status = TransactionStatus.COMPLETED;
        if (this.completedAt == null) {
            this.completedAt = Instant.now();
        }
    }
    
    // Fail transaction
    public void fail(String reason) {
        Objects.requireNonNull(reason, "Failure reason cannot be null");
        
        this.status = TransactionStatus.FAILED;
        this.failureReason = reason;
        this.completedAt = Instant.now();
    }
    
    // Cancel transaction
    public void cancel() {
        if (status != TransactionStatus.PENDING) {
            throw new IllegalStateException("Can only cancel pending transactions");
        }
        
        this.status = TransactionStatus.CANCELLED;
        this.completedAt = Instant.now();
    }
    
    // Check if transaction is complete
    public boolean isComplete() {
        return status == TransactionStatus.COMPLETED;
    }
    
    // Check if transaction is pending
    public boolean isPending() {
        return status == TransactionStatus.PENDING;
    }
    
    // Check if transaction failed
    public boolean isFailed() {
        return status == TransactionStatus.FAILED;
    }
}