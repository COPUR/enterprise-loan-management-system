package com.bank.loanmanagement.domain.payment;

import com.bank.loanmanagement.domain.customer.CustomerId;
import com.bank.loanmanagement.domain.loan.LoanId;
import com.bank.loanmanagement.domain.shared.AggregateRoot;
import com.bank.loanmanagement.domain.shared.Money;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_loan", columnList = "loanId"),
    @Index(name = "idx_payment_customer", columnList = "customerId"),
    @Index(name = "idx_payment_date", columnList = "paymentDate"),
    @Index(name = "idx_payment_status", columnList = "status"),
    @Index(name = "idx_payment_reference", columnList = "paymentReference")
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends AggregateRoot<PaymentId> {
    
    @EmbeddedId
    private PaymentId id;
    
    @Embedded
    private LoanId loanId;
    
    @Embedded
    private CustomerId customerId;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "payment_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "payment_currency"))
    })
    private Money amount;
    
    @Column(nullable = false)
    private LocalDate paymentDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;
    
    @Column(unique = true, nullable = false)
    private String paymentReference;
    
    private String externalTransactionId;
    private String description;
    private String processedBy;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @Version
    private Long version;
    
    public void process(String processedBy) {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending payments can be processed");
        }
        this.status = PaymentStatus.PROCESSED;
        this.processedBy = processedBy;
        addDomainEvent(new PaymentProcessedEvent(this.id.getValue(), this.loanId.getValue(), this.customerId.getValue(), this.amount));
    }
    
    public void fail(String reason) {
        if (this.status == PaymentStatus.PROCESSED) {
            throw new IllegalStateException("Processed payments cannot be failed");
        }
        this.status = PaymentStatus.FAILED;
        this.description = reason;
        addDomainEvent(new PaymentFailedEvent(this.id.getValue(), this.loanId.getValue(), this.customerId.getValue(), reason));
    }
    
    public void cancel(String reason) {
        if (this.status == PaymentStatus.PROCESSED) {
            throw new IllegalStateException("Processed payments cannot be cancelled");
        }
        this.status = PaymentStatus.CANCELLED;
        this.description = reason;
        addDomainEvent(new PaymentCancelledEvent(this.id.getValue(), this.loanId.getValue(), this.customerId.getValue(), reason));
    }
    
    public void reverse(String reason) {
        if (this.status != PaymentStatus.PROCESSED) {
            throw new IllegalStateException("Only processed payments can be reversed");
        }
        this.status = PaymentStatus.REVERSED;
        this.description = reason;
        addDomainEvent(new PaymentReversedEvent(this.id.getValue(), this.loanId.getValue(), this.customerId.getValue(), this.amount, reason));
    }
    
    public boolean isSuccessful() {
        return status == PaymentStatus.PROCESSED;
    }
    
    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }
    
    @Override
    public PaymentId getId() {
        return id;
    }
    
    // Factory method for creating new payments
    public static Payment createNew(LoanId loanId, CustomerId customerId, Money amount, PaymentMethod paymentMethod, 
                                   String paymentReference, String description) {
        return Payment.builder()
                .id(PaymentId.generate())
                .loanId(loanId)
                .customerId(customerId)
                .amount(amount)
                .paymentMethod(paymentMethod)
                .paymentReference(paymentReference)
                .description(description)
                .status(PaymentStatus.PENDING)
                .paymentDate(LocalDate.now())
                .build();
    }
    
    // Getters for GraphQL compatibility
    public Money getPaymentAmount() {
        return amount;
    }
    
    public LocalDateTime getPaymentDate() {
        return paymentDate.atStartOfDay();
    }
    
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }
    
    public String getPaymentReference() {
        return paymentReference;
    }
    
    public PaymentStatus getStatus() {
        return status;
    }
    
    public Money getProcessingFee() {
        // Simple processing fee calculation - 1% of payment amount
        return Money.of(amount.getAmount().multiply(java.math.BigDecimal.valueOf(0.01)), amount.getCurrency());
    }
    
    public Money getTotalAmount() {
        return Money.of(amount.getAmount().add(getProcessingFee().getAmount()), amount.getCurrency());
    }
}