package com.bank.loan.domain;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Loan Installment Value Object
 * 
 * Represents a single installment payment within a loan schedule.
 * Contains business logic for payment processing and due date tracking.
 */
public class LoanInstallment {
    
    private final LoanId loanId;
    private final CustomerId customerId;
    private final int installmentNumber;
    private final Money amount;
    private final LocalDate dueDate;
    private Money paidAmount;
    private LocalDateTime paidDate;
    private InstallmentStatus status;
    
    private LoanInstallment(LoanId loanId, CustomerId customerId, int installmentNumber, 
                           Money amount, LocalDate dueDate) {
        this.loanId = Objects.requireNonNull(loanId, "Loan ID cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.installmentNumber = installmentNumber;
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.dueDate = Objects.requireNonNull(dueDate, "Due date cannot be null");
        this.paidAmount = Money.zero(amount.getCurrency());
        this.status = InstallmentStatus.PENDING;
    }
    
    public static LoanInstallment create(LoanId loanId, CustomerId customerId, int installmentNumber,
                                        Money amount, LocalDate dueDate) {
        if (installmentNumber <= 0) {
            throw new IllegalArgumentException("Installment number must be positive");
        }
        if (amount.isNegative() || amount.isZero()) {
            throw new IllegalArgumentException("Installment amount must be positive");
        }
        return new LoanInstallment(loanId, customerId, installmentNumber, amount, dueDate);
    }
    
    public LoanId getLoanId() {
        return loanId;
    }
    
    public CustomerId getCustomerId() {
        return customerId;
    }
    
    public int getInstallmentNumber() {
        return installmentNumber;
    }
    
    public Money getAmount() {
        return amount;
    }
    
    public LocalDate getDueDate() {
        return dueDate;
    }
    
    public Money getPaidAmount() {
        return paidAmount;
    }
    
    public LocalDateTime getPaidDate() {
        return paidDate;
    }
    
    public InstallmentStatus getStatus() {
        return status;
    }
    
    public boolean isPaid() {
        return status == InstallmentStatus.PAID;
    }
    
    public boolean isOverdue() {
        return !isPaid() && dueDate.isBefore(LocalDate.now());
    }
    
    public Money getRemainingAmount() {
        return amount.subtract(paidAmount);
    }
    
    public void makePayment(Money paymentAmount) {
        if (paymentAmount == null || paymentAmount.isNegative() || paymentAmount.isZero()) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        
        if (isPaid()) {
            throw new IllegalStateException("Installment is already paid");
        }
        
        Money newPaidAmount = paidAmount.add(paymentAmount);
        
        if (newPaidAmount.compareTo(amount) > 0) {
            throw new IllegalArgumentException("Payment amount exceeds installment amount");
        }
        
        this.paidAmount = newPaidAmount;
        
        if (newPaidAmount.equals(amount)) {
            this.status = InstallmentStatus.PAID;
            this.paidDate = LocalDateTime.now();
        } else {
            this.status = InstallmentStatus.PARTIALLY_PAID;
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoanInstallment that = (LoanInstallment) o;
        return installmentNumber == that.installmentNumber &&
               Objects.equals(loanId, that.loanId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(loanId, installmentNumber);
    }
    
    @Override
    public String toString() {
        return String.format("LoanInstallment{loanId=%s, installmentNumber=%d, amount=%s, dueDate=%s, status=%s}",
                           loanId, installmentNumber, amount, dueDate, status);
    }
}