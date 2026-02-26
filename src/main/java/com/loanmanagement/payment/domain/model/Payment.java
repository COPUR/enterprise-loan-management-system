package com.loanmanagement.payment.domain.model;

import com.loanmanagement.shared.domain.model.Money;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "payments")
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "loan_id", nullable = false)
    private Long loanId;
    
    @Column(name = "amount", nullable = false)
    private String amount;
    
    @Column(name = "currency", nullable = false)
    private String currency;
    
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    
    @Column(name = "payment_date")
    private LocalDate paymentDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private PaymentType type;
    
    @Column(name = "penalty_amount")
    private String penaltyAmount;
    
    @Column(name = "discount_amount")
    private String discountAmount;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    protected Payment() {
    }
    
    public Payment(Long loanId, Money amount, LocalDate dueDate, PaymentType type) {
        this.loanId = Objects.requireNonNull(loanId, "Loan ID cannot be null");
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null").getAmount().toString();
        this.currency = amount.getCurrency();
        this.dueDate = Objects.requireNonNull(dueDate, "Due date cannot be null");
        this.type = Objects.requireNonNull(type, "Payment type cannot be null");
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        validatePaymentAmount();
    }
    
    public void processPayment(LocalDate paymentDate) {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending payments can be processed");
        }
        
        this.paymentDate = Objects.requireNonNull(paymentDate, "Payment date cannot be null");
        this.status = PaymentStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
        
        // Apply penalty or discount based on payment timing
        if (paymentDate.isAfter(dueDate)) {
            applyLatePenalty();
        } else if (paymentDate.isBefore(dueDate.minusDays(7))) {
            applyEarlyPaymentDiscount();
        }
    }
    
    public void markAsOverdue() {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending payments can be marked as overdue");
        }
        
        this.status = PaymentStatus.OVERDUE;
        this.updatedAt = LocalDateTime.now();
        applyLatePenalty();
    }
    
    public void cancel() {
        if (status == PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Completed payments cannot be cancelled");
        }
        
        this.status = PaymentStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }
    
    public Money getAmount() {
        return Money.of(new BigDecimal(amount), currency);
    }
    
    public Money getTotalAmount() {
        Money baseAmount = getAmount();
        
        if (penaltyAmount != null) {
            Money penalty = Money.of(new BigDecimal(penaltyAmount), currency);
            baseAmount = baseAmount.add(penalty);
        }
        
        if (discountAmount != null) {
            Money discount = Money.of(new BigDecimal(discountAmount), currency);
            baseAmount = baseAmount.subtract(discount);
        }
        
        return baseAmount;
    }
    
    public Money getPenaltyAmount() {
        return penaltyAmount != null ? Money.of(new BigDecimal(penaltyAmount), currency) : Money.zero(currency);
    }
    
    public Money getDiscountAmount() {
        return discountAmount != null ? Money.of(new BigDecimal(discountAmount), currency) : Money.zero(currency);
    }
    
    public boolean isOverdue() {
        return status == PaymentStatus.OVERDUE || 
               (status == PaymentStatus.PENDING && LocalDate.now().isAfter(dueDate));
    }
    
    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }
    
    public boolean isCompleted() {
        return status == PaymentStatus.COMPLETED;
    }
    
    public int getDaysOverdue() {
        if (!isOverdue()) {
            return 0;
        }
        
        LocalDate referenceDate = paymentDate != null ? paymentDate : LocalDate.now();
        return (int) java.time.temporal.ChronoUnit.DAYS.between(dueDate, referenceDate);
    }
    
    private void validatePaymentAmount() {
        Money paymentAmount = getAmount();
        if (paymentAmount.isZero() || paymentAmount.isNegative()) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        
        Money maxPayment = Money.of(new BigDecimal("100000"), currency);
        if (paymentAmount.isGreaterThan(maxPayment)) {
            throw new IllegalArgumentException("Payment amount cannot exceed " + maxPayment);
        }
    }
    
    private void applyLatePenalty() {
        int daysOverdue = getDaysOverdue();
        if (daysOverdue > 0) {
            BigDecimal penaltyRate = BigDecimal.valueOf(0.001); // 0.1% per day
            BigDecimal penaltyMultiplier = penaltyRate.multiply(BigDecimal.valueOf(daysOverdue));
            Money penalty = getAmount().multiply(penaltyMultiplier);
            this.penaltyAmount = penalty.getAmount().toString();
        }
    }
    
    private void applyEarlyPaymentDiscount() {
        BigDecimal discountRate = BigDecimal.valueOf(0.005); // 0.5% discount
        Money discount = getAmount().multiply(discountRate);
        this.discountAmount = discount.getAmount().toString();
    }
    
    // Getters
    public Long getId() {
        return id;
    }
    
    public Long getLoanId() {
        return loanId;
    }
    
    public LocalDate getDueDate() {
        return dueDate;
    }
    
    public LocalDate getPaymentDate() {
        return paymentDate;
    }
    
    public PaymentStatus getStatus() {
        return status;
    }
    
    public PaymentType getType() {
        return type;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Payment payment = (Payment) o;
        return Objects.equals(id, payment.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Payment{" +
                "id=" + id +
                ", loanId=" + loanId +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", dueDate=" + dueDate +
                ", status=" + status +
                ", type=" + type +
                '}';
    }
}