package com.bank.loanmanagement.loan.domain.loan;

import com.bank.loanmanagement.loan.sharedkernel.domain.Money;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * LoanInstallment Domain Value Object - Clean DDD Implementation
 * 
 * Represents a single installment payment in a loan schedule.
 * Contains business logic for payment processing, status management,
 * and overdue checking.
 * 
 * Pure domain model without infrastructure dependencies.
 */
public class LoanInstallment {
    
    private Long id;
    private LoanId loanId;
    private Integer installmentNumber;
    private LocalDate dueDate;
    private Money principalAmount;
    private Money interestAmount;
    private Money totalAmount;
    private Money paidAmount;
    private InstallmentStatus status;
    private LocalDateTime paidDate;
    private String paymentReference;

    // Private constructor for domain creation
    private LoanInstallment(
        LoanId loanId,
        Integer installmentNumber,
        LocalDate dueDate,
        Money principalAmount,
        Money interestAmount,
        Money totalAmount
    ) {
        this.loanId = Objects.requireNonNull(loanId, "Loan ID cannot be null");
        this.installmentNumber = Objects.requireNonNull(installmentNumber, "Installment number cannot be null");
        this.dueDate = Objects.requireNonNull(dueDate, "Due date cannot be null");
        this.principalAmount = Objects.requireNonNull(principalAmount, "Principal amount cannot be null");
        this.interestAmount = Objects.requireNonNull(interestAmount, "Interest amount cannot be null");
        this.totalAmount = Objects.requireNonNull(totalAmount, "Total amount cannot be null");
        
        // Initialize defaults
        this.status = InstallmentStatus.PENDING;
        this.paidAmount = Money.zero(totalAmount.getCurrency());
        
        // Validate business rules
        validateInstallmentCreation();
    }

    // Factory method for creating new installments
    public static LoanInstallment create(
        LoanId loanId,
        Integer installmentNumber,
        LocalDate dueDate,
        Money principalAmount,
        Money interestAmount,
        Money totalAmount
    ) {
        return new LoanInstallment(loanId, installmentNumber, dueDate, principalAmount, interestAmount, totalAmount);
    }

    // Default constructor for JPA reconstruction
    protected LoanInstallment() {}

    // Business logic: Mark installment as paid
    public void markAsPaid(Money amount, String paymentReference) {
        if (this.status == InstallmentStatus.PAID) {
            throw new IllegalStateException("Installment is already paid");
        }
        
        Objects.requireNonNull(amount, "Payment amount cannot be null");
        Objects.requireNonNull(paymentReference, "Payment reference cannot be null");
        
        if (amount.isGreaterThan(getRemainingAmount())) {
            throw new IllegalArgumentException("Payment amount cannot exceed remaining amount");
        }
        
        if (this.paidAmount == null) {
            this.paidAmount = amount;
        } else {
            this.paidAmount = this.paidAmount.add(amount);
        }
        
        this.paidDate = LocalDateTime.now();
        this.paymentReference = paymentReference;
        
        if (this.paidAmount.isGreaterThanOrEqualTo(this.totalAmount)) {
            this.status = InstallmentStatus.PAID;
        } else {
            this.status = InstallmentStatus.PARTIALLY_PAID;
        }
    }

    // Business logic: Check if installment is overdue
    public boolean isOverdue() {
        return status == InstallmentStatus.PENDING && dueDate.isBefore(LocalDate.now());
    }

    // Business logic: Calculate remaining amount to be paid
    public Money getRemainingAmount() {
        if (paidAmount == null || paidAmount.isZero()) {
            return totalAmount;
        }
        return totalAmount.subtract(paidAmount);
    }

    // Business logic: Check if installment is fully paid
    public boolean isFullyPaid() {
        return status == InstallmentStatus.PAID;
    }

    // Business logic: Check if installment is partially paid
    public boolean isPartiallyPaid() {
        return status == InstallmentStatus.PARTIALLY_PAID;
    }

    // Business logic: Check if installment is pending
    public boolean isPending() {
        return status == InstallmentStatus.PENDING;
    }

    // Business logic: Get days overdue
    public long getDaysOverdue() {
        if (!isOverdue()) {
            return 0;
        }
        return LocalDate.now().toEpochDay() - dueDate.toEpochDay();
    }

    // Private helper methods
    private void validateInstallmentCreation() {
        if (installmentNumber <= 0) {
            throw new IllegalArgumentException("Installment number must be positive");
        }
        
        if (dueDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Due date cannot be in the past");
        }
        
        if (principalAmount.isZeroOrNegative()) {
            throw new IllegalArgumentException("Principal amount must be positive");
        }
        
        if (interestAmount.isZeroOrNegative()) {
            throw new IllegalArgumentException("Interest amount must be positive");
        }
        
        if (totalAmount.isZeroOrNegative()) {
            throw new IllegalArgumentException("Total amount must be positive");
        }
        
        // Validate that total amount equals principal + interest
        Money calculatedTotal = principalAmount.add(interestAmount);
        if (!totalAmount.equals(calculatedTotal)) {
            throw new IllegalArgumentException("Total amount must equal principal + interest amounts");
        }
    }

    // Getters
    public Long getId() { return id; }
    public LoanId getLoanId() { return loanId; }
    public Integer getInstallmentNumber() { return installmentNumber; }
    public LocalDate getDueDate() { return dueDate; }
    public Money getPrincipalAmount() { return principalAmount; }
    public Money getInterestAmount() { return interestAmount; }
    public Money getTotalAmount() { return totalAmount; }
    public Money getPaidAmount() { return paidAmount; }
    public InstallmentStatus getStatus() { return status; }
    public LocalDateTime getPaidDate() { return paidDate; }
    public String getPaymentReference() { return paymentReference; }

    // Public setters for reconstruction from persistence
    public void setId(Long id) { this.id = id; }
    public void setLoanId(LoanId loanId) { this.loanId = loanId; }
    public void setInstallmentNumber(Integer installmentNumber) { this.installmentNumber = installmentNumber; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public void setPrincipalAmount(Money principalAmount) { this.principalAmount = principalAmount; }
    public void setInterestAmount(Money interestAmount) { this.interestAmount = interestAmount; }
    public void setTotalAmount(Money totalAmount) { this.totalAmount = totalAmount; }
    public void setPaidAmount(Money paidAmount) { this.paidAmount = paidAmount; }
    public void setStatus(InstallmentStatus status) { this.status = status; }
    public void setPaidDate(LocalDateTime paidDate) { this.paidDate = paidDate; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoanInstallment that = (LoanInstallment) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "LoanInstallment{" +
                "id=" + id +
                ", loanId=" + loanId +
                ", installmentNumber=" + installmentNumber +
                ", dueDate=" + dueDate +
                ", totalAmount=" + totalAmount +
                ", status=" + status +
                '}';
    }
}