package com.bank.loan.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Payment entity representing a payment made against a loan
 */
@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "payment_reference", unique = true, nullable = false)
    private String paymentReference;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "value", column = @Column(name = "loan_id"))
    })
    private LoanId loanId;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "value", column = @Column(name = "customer_id"))
    })
    private CustomerId customerId;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "payment_amount", precision = 15, scale = 2)),
        @AttributeOverride(name = "currency", column = @Column(name = "payment_currency"))
    })
    private Money paymentAmount;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "processing_fee_amount", precision = 15, scale = 2)),
        @AttributeOverride(name = "currency", column = @Column(name = "processing_fee_currency"))
    })
    private Money processingFee;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type")
    private PaymentType paymentType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Setter
    private PaymentStatus status;
    
    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;
    
    @Column(name = "processed_date")
    @Setter
    private LocalDateTime processedDate;
    
    @Column(name = "processed_by")
    @Setter
    private String processedBy;
    
    @Column(name = "description")
    @Setter
    private String description;
    
    @Column(name = "transaction_reference")
    private String transactionReference;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @Setter
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (paymentDate == null) {
            paymentDate = LocalDateTime.now();
        }
        if (status == null) {
            status = PaymentStatus.PENDING;
        }
        if (paymentType == null) {
            paymentType = PaymentType.REGULAR_INSTALLMENT;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Factory method to create a new payment
     */
    public static Payment createNew(LoanId loanId, CustomerId customerId, Money amount, 
                                  PaymentMethod method, String reference, String description) {
        Objects.requireNonNull(loanId, "Loan ID cannot be null");
        Objects.requireNonNull(customerId, "Customer ID cannot be null");
        Objects.requireNonNull(amount, "Payment amount cannot be null");
        Objects.requireNonNull(method, "Payment method cannot be null");
        Objects.requireNonNull(reference, "Payment reference cannot be null");
        
        Money processingFee = calculateProcessingFee(amount, method);
        
        return Payment.builder()
            .loanId(loanId)
            .customerId(customerId)
            .paymentAmount(amount)
            .processingFee(processingFee)
            .paymentMethod(method)
            .paymentType(PaymentType.REGULAR_INSTALLMENT)
            .status(PaymentStatus.PENDING)
            .paymentReference(reference)
            .description(description)
            .paymentDate(LocalDateTime.now())
            .build();
    }
    
    /**
     * Calculate processing fee based on payment amount and method
     */
    private static Money calculateProcessingFee(Money amount, PaymentMethod method) {
        BigDecimal feeRate = BigDecimal.valueOf(method.getProcessingFeeRate());
        return amount.multiply(feeRate);
    }
    
    /**
     * Get total amount including processing fee
     */
    public Money getTotalAmount() {
        return paymentAmount.add(processingFee);
    }
    
    /**
     * Process the payment
     */
    public void process(String processedBy) {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending payments can be processed");
        }
        this.status = PaymentStatus.PROCESSED;
        this.processedBy = processedBy;
        this.processedDate = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Fail the payment with a reason
     */
    public void fail(String reason) {
        if (status == PaymentStatus.PROCESSED) {
            throw new IllegalStateException("Processed payments cannot be failed");
        }
        this.status = PaymentStatus.FAILED;
        this.description = reason;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Cancel the payment with a reason
     */
    public void cancel(String reason) {
        if (status == PaymentStatus.PROCESSED) {
            throw new IllegalStateException("Processed payments cannot be cancelled");
        }
        this.status = PaymentStatus.CANCELLED;
        this.description = reason;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Reverse a processed payment
     */
    public void reverse(String reason) {
        if (status != PaymentStatus.PROCESSED) {
            throw new IllegalStateException("Only processed payments can be reversed");
        }
        this.status = PaymentStatus.REVERSED;
        this.description = reason;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Check if payment is successful
     */
    public boolean isSuccessful() {
        return status.isSuccessful();
    }
    
    /**
     * Check if payment is pending
     */
    public boolean isPending() {
        return status.isPending();
    }
    
    /**
     * Get payment ID for domain operations
     */
    public PaymentId getPaymentId() {
        return id != null ? PaymentId.fromLong(id) : PaymentId.generate();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Payment payment = (Payment) o;
        return Objects.equals(paymentReference, payment.paymentReference);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(paymentReference);
    }
}