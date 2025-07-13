package com.bank.payment.domain;

import com.bank.shared.kernel.domain.AggregateRoot;
import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Payment Aggregate Root
 * 
 * Represents a financial payment with its processing lifecycle,
 * validation rules, and business logic for payment management.
 */
public class Payment extends AggregateRoot<PaymentId> {
    
    private PaymentId paymentId;
    private CustomerId customerId;
    private AccountId fromAccountId;
    private AccountId toAccountId;
    private Money amount;
    private Money fee;
    private PaymentType paymentType;
    private PaymentStatus status;
    private String description;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;
    
    // Private constructor for JPA
    protected Payment() {}
    
    private Payment(PaymentId paymentId, CustomerId customerId, AccountId fromAccountId,
                   AccountId toAccountId, Money amount, PaymentType paymentType, String description) {
        this.paymentId = Objects.requireNonNull(paymentId, "Payment ID cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.fromAccountId = Objects.requireNonNull(fromAccountId, "From account ID cannot be null");
        this.toAccountId = Objects.requireNonNull(toAccountId, "To account ID cannot be null");
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.paymentType = Objects.requireNonNull(paymentType, "Payment type cannot be null");
        this.description = description;
        this.fee = paymentType.calculateFee(amount);
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        validatePaymentData();
        
        // Domain event
        addDomainEvent(new PaymentCreatedEvent(paymentId, customerId, amount, paymentType));
    }
    
    public static Payment create(PaymentId paymentId, CustomerId customerId, AccountId fromAccountId,
                                AccountId toAccountId, Money amount, PaymentType paymentType, String description) {
        return new Payment(paymentId, customerId, fromAccountId, toAccountId, amount, paymentType, description);
    }
    
    private void validatePaymentData() {
        if (amount.isNegative() || amount.isZero()) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("From and to accounts cannot be the same");
        }
    }
    
    @Override
    public PaymentId getId() {
        return paymentId;
    }
    
    public CustomerId getCustomerId() {
        return customerId;
    }
    
    public AccountId getFromAccountId() {
        return fromAccountId;
    }
    
    public AccountId getToAccountId() {
        return toAccountId;
    }
    
    public Money getAmount() {
        return amount;
    }
    
    public Money getFee() {
        return fee;
    }
    
    public Money getTotalAmount() {
        return amount.add(fee);
    }
    
    public PaymentType getPaymentType() {
        return paymentType;
    }
    
    public PaymentStatus getStatus() {
        return status;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getFailureReason() {
        return failureReason;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void confirm() {
        if (!status.canBeConfirmed()) {
            throw new IllegalStateException("Payment cannot be confirmed in current status: " + status);
        }
        this.status = PaymentStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new PaymentCompletedEvent(paymentId, customerId, amount));
    }
    
    public void fail(String reason) {
        if (!status.canBeFailed()) {
            throw new IllegalStateException("Payment cannot be failed in current status: " + status);
        }
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new PaymentFailedEvent(paymentId, customerId, reason));
    }
    
    public void cancel(String reason) {
        if (!status.canBeCancelled()) {
            throw new IllegalStateException("Payment cannot be cancelled in current status: " + status);
        }
        this.status = PaymentStatus.CANCELLED;
        this.failureReason = reason;
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new PaymentCancelledEvent(paymentId, customerId, reason));
    }
    
    public void refund() {
        if (!status.canBeRefunded()) {
            throw new IllegalStateException("Payment cannot be refunded in current status: " + status);
        }
        this.status = PaymentStatus.REFUNDED;
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new PaymentRefundedEvent(paymentId, customerId, amount));
    }
    
    public void markAsProcessing() {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending payments can be marked as processing");
        }
        this.status = PaymentStatus.PROCESSING;
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new PaymentProcessingEvent(paymentId, customerId));
    }
    
    public boolean isCompleted() {
        return status == PaymentStatus.COMPLETED;
    }
    
    public boolean isFailed() {
        return status == PaymentStatus.FAILED;
    }
    
    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }
}