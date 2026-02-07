package com.bank.loan.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Credit Loan Installment entity for integration tests
 */
@Entity
@Table(name = "credit_loan_installments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditLoanInstallment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "loan_id", nullable = false)
    private Long loanId;
    
    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;
    
    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;
    
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    
    @Column(name = "is_paid")
    private Boolean isPaid;
    
    @Column(name = "paid_amount", precision = 15, scale = 2)
    private BigDecimal paidAmount;
    
    @Column(name = "paid_date")
    private LocalDateTime paidDate;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (isPaid == null) {
            isPaid = false;
        }
        if (paidAmount == null) {
            paidAmount = BigDecimal.ZERO;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Factory method to create a new installment
     */
    public static CreditLoanInstallment create(Long loanId,
                                               Integer installmentNumber,
                                               BigDecimal amount,
                                               LocalDate dueDate) {
        Objects.requireNonNull(installmentNumber, "Installment number cannot be null");
        Objects.requireNonNull(amount, "Installment amount cannot be null");
        Objects.requireNonNull(dueDate, "Due date cannot be null");

        if (installmentNumber <= 0) {
            throw new IllegalArgumentException("Installment number must be positive");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Installment amount must be positive");
        }

        return CreditLoanInstallment.builder()
            .loanId(loanId)
            .installmentNumber(installmentNumber)
            .amount(amount)
            .dueDate(dueDate)
            .isPaid(false)
            .paidAmount(BigDecimal.ZERO)
            .build();
    }
    
    /**
     * Get remaining amount to be paid
     */
    public BigDecimal getRemainingAmount() {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal paid = paidAmount != null ? paidAmount : BigDecimal.ZERO;
        BigDecimal remaining = amount.subtract(paid);
        return remaining.compareTo(BigDecimal.ZERO) >= 0 ? remaining : BigDecimal.ZERO;
    }
    
    /**
     * Check if installment is overdue
     */
    public boolean isOverdue() {
        if (dueDate == null) {
            return false;
        }
        boolean paid = Boolean.TRUE.equals(isPaid);
        return !paid && dueDate.isBefore(LocalDate.now());
    }

    /**
     * Apply a payment to this installment
     */
    public void applyPayment(BigDecimal paymentAmount, LocalDateTime paymentDate) {
        Objects.requireNonNull(paymentAmount, "Payment amount cannot be null");
        if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }

        if (Boolean.TRUE.equals(isPaid)) {
            return;
        }

        BigDecimal currentPaid = paidAmount != null ? paidAmount : BigDecimal.ZERO;
        BigDecimal newPaid = currentPaid.add(paymentAmount);
        this.paidAmount = newPaid;

        if (amount != null && newPaid.compareTo(amount) >= 0) {
            this.isPaid = true;
            this.paidDate = paymentDate != null ? paymentDate : LocalDateTime.now();
        }

        this.updatedAt = LocalDateTime.now();
    }
}
