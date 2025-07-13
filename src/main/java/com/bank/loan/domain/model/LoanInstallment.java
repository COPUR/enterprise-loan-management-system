package com.bank.loan.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing a single installment of a loan
 */
@Entity
@Table(name = "loan_installments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanInstallment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "loan_id", nullable = false)
    private Long loanId;
    
    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "installment_amount", precision = 15, scale = 2)),
        @AttributeOverride(name = "currency", column = @Column(name = "currency"))
    })
    private Money installmentAmount;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "principal_amount", precision = 15, scale = 2)),
        @AttributeOverride(name = "currency", column = @Column(name = "principal_currency"))
    })
    private Money principalAmount;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "interest_amount", precision = 15, scale = 2)),
        @AttributeOverride(name = "currency", column = @Column(name = "interest_currency"))
    })
    private Money interestAmount;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "paid_amount", precision = 15, scale = 2)),
        @AttributeOverride(name = "currency", column = @Column(name = "paid_currency"))
    })
    private Money paidAmount;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "remaining_balance", precision = 15, scale = 2)),
        @AttributeOverride(name = "currency", column = @Column(name = "balance_currency"))
    })
    private Money remainingBalance;
    
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    
    @Column(name = "paid_date")
    private LocalDateTime paidDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InstallmentStatus status;
    
    @Column(name = "late_fee", precision = 15, scale = 2)
    private BigDecimal lateFee;
    
    @Column(name = "penalty_amount", precision = 15, scale = 2)
    private BigDecimal penaltyAmount;
    
    @Column(name = "days_overdue")
    private Integer daysOverdue;
    
    @Column(name = "payment_reference")
    private String paymentReference;
    
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
        if (status == null) {
            status = InstallmentStatus.PENDING;
        }
        if (paidAmount == null && installmentAmount != null) {
            paidAmount = Money.zero(installmentAmount.getCurrency());
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Factory method to create a new installment
     */
    public static LoanInstallment create(Long loanId, Integer installmentNumber, 
                                       Money installmentAmount, Money principalAmount, 
                                       Money interestAmount, LocalDate dueDate, 
                                       Money remainingBalance) {
        Objects.requireNonNull(loanId, "Loan ID cannot be null");
        Objects.requireNonNull(installmentNumber, "Installment number cannot be null");
        Objects.requireNonNull(installmentAmount, "Installment amount cannot be null");
        Objects.requireNonNull(dueDate, "Due date cannot be null");
        
        return LoanInstallment.builder()
            .loanId(loanId)
            .installmentNumber(installmentNumber)
            .installmentAmount(installmentAmount)
            .principalAmount(principalAmount != null ? principalAmount : installmentAmount)
            .interestAmount(interestAmount != null ? interestAmount : Money.zero(installmentAmount.getCurrency()))
            .paidAmount(Money.zero(installmentAmount.getCurrency()))
            .remainingBalance(remainingBalance != null ? remainingBalance : Money.zero(installmentAmount.getCurrency()))
            .dueDate(dueDate)
            .status(InstallmentStatus.PENDING)
            .daysOverdue(0)
            .build();
    }
    
    /**
     * Make a payment against this installment
     */
    public Money makePayment(Money paymentAmount, String paymentReference) {
        Objects.requireNonNull(paymentAmount, "Payment amount cannot be null");
        
        if (!canAcceptPayment()) {
            throw new IllegalStateException("Installment cannot accept payment in current status: " + status);
        }
        
        Money currentPaid = this.paidAmount != null ? this.paidAmount : Money.zero(installmentAmount.getCurrency());
        Money newPaidAmount = currentPaid.add(paymentAmount);
        
        this.paidAmount = newPaidAmount;
        this.paymentReference = paymentReference;
        this.updatedAt = LocalDateTime.now();
        
        // Check if installment is now fully paid
        if (newPaidAmount.isEqualTo(installmentAmount) || newPaidAmount.isGreaterThan(installmentAmount)) {
            this.status = InstallmentStatus.PAID;
            this.paidDate = LocalDateTime.now();
            // Return any overpayment
            if (newPaidAmount.isGreaterThan(installmentAmount)) {
                return newPaidAmount.subtract(installmentAmount);
            }
        } else {
            this.status = InstallmentStatus.PARTIALLY_PAID;
        }
        
        return Money.zero(paymentAmount.getCurrency());
    }
    
    /**
     * Check if installment can accept payment
     */
    public boolean canAcceptPayment() {
        return status.canAcceptPayment();
    }
    
    /**
     * Check if installment is paid
     */
    public boolean isPaid() {
        return status.isPaid();
    }
    
    /**
     * Check if installment is overdue
     */
    public boolean isOverdue() {
        return status.isOverdue() || (dueDate.isBefore(LocalDate.now()) && !isPaid());
    }
    
    /**
     * Calculate days overdue
     */
    public int calculateDaysOverdue() {
        if (isPaid() || dueDate.isAfter(LocalDate.now())) {
            return 0;
        }
        return (int) java.time.temporal.ChronoUnit.DAYS.between(dueDate, LocalDate.now());
    }
    
    /**
     * Update overdue status
     */
    public void updateOverdueStatus() {
        if (!isPaid()) {
            int daysOverdue = calculateDaysOverdue();
            this.daysOverdue = daysOverdue;
            if (daysOverdue > 0) {
                this.status = InstallmentStatus.OVERDUE;
            } else if (dueDate.equals(LocalDate.now())) {
                this.status = InstallmentStatus.DUE;
            }
        }
    }
    
    /**
     * Get remaining amount to be paid
     */
    public Money getRemainingAmount() {
        if (paidAmount == null) {
            return installmentAmount;
        }
        return installmentAmount.subtract(paidAmount);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoanInstallment that = (LoanInstallment) o;
        return Objects.equals(loanId, that.loanId) && 
               Objects.equals(installmentNumber, that.installmentNumber);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(loanId, installmentNumber);
    }
}