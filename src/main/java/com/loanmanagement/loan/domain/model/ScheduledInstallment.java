package com.loanmanagement.loan.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.With;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Individual scheduled installment payment.
 */
@Data
@Builder
@With
public class ScheduledInstallment {
    
    @NotNull
    private final LoanId loanId;
    
    @NotNull
    private final String installmentId;
    
    @NotNull
    @Positive
    private final Integer installmentNumber;
    
    @NotNull
    private final LocalDate dueDate;
    
    @NotNull
    @PositiveOrZero
    private final BigDecimal principalAmount;
    
    @NotNull
    @PositiveOrZero
    private final BigDecimal interestAmount;
    
    @PositiveOrZero
    private final BigDecimal feesAmount;
    
    @PositiveOrZero
    private final BigDecimal escrowAmount;
    
    @NotNull
    @PositiveOrZero
    private final BigDecimal totalAmount;
    
    @NotNull
    @PositiveOrZero
    private final BigDecimal remainingBalance;
    
    private final boolean isPaid;
    
    private final LocalDate paidDate;
    
    private final BigDecimal paidAmount;
    
    private final PaymentStatus paymentStatus;
    
    private final boolean isOverdue;
    
    private final Integer daysPastDue;
    
    private final BigDecimal lateFeeAmount;
    
    private final String paymentMethod;
    
    private final String paymentReference;
    
    private final boolean isPartialPayment;
    
    private final BigDecimal outstandingAmount;
    
    private final LocalDate lastModifiedDate;
    
    /**
     * Calculates the outstanding amount for this installment.
     */
    public BigDecimal calculateOutstandingAmount() {
        if (isPaid) {
            return BigDecimal.ZERO;
        }
        
        if (paidAmount == null) {
            return totalAmount;
        }
        
        return totalAmount.subtract(paidAmount).max(BigDecimal.ZERO);
    }
    
    /**
     * Calculates days past due.
     */
    public Integer calculateDaysPastDue() {
        if (isPaid || dueDate == null) {
            return 0;
        }
        
        LocalDate currentDate = LocalDate.now();
        if (currentDate.isAfter(dueDate)) {
            return (int) java.time.temporal.ChronoUnit.DAYS.between(dueDate, currentDate);
        }
        
        return 0;
    }
    
    /**
     * Checks if this installment is overdue.
     */
    public boolean isOverdue() {
        return calculateDaysPastDue() > 0;
    }
    
    /**
     * Calculates the payment completion percentage.
     */
    public BigDecimal calculatePaymentCompletionPercentage() {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal actualPaidAmount = paidAmount != null ? paidAmount : BigDecimal.ZERO;
        
        return actualPaidAmount.divide(totalAmount, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Processes a payment against this installment.
     */
    public ScheduledInstallment processPayment(BigDecimal paymentAmount, LocalDate paymentDate, String paymentRef) {
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return this;
        }
        
        BigDecimal currentPaidAmount = paidAmount != null ? paidAmount : BigDecimal.ZERO;
        BigDecimal newPaidAmount = currentPaidAmount.add(paymentAmount);
        
        boolean isFullyPaid = newPaidAmount.compareTo(totalAmount) >= 0;
        boolean isPartial = newPaidAmount.compareTo(totalAmount) < 0;
        
        return this.withPaidAmount(newPaidAmount)
                   .withPaidDate(paymentDate)
                   .withPaymentReference(paymentRef)
                   .withPaid(isFullyPaid)
                   .withPartialPayment(isPartial)
                   .withOutstandingAmount(calculateOutstandingAmount())
                   .withLastModifiedDate(LocalDate.now());
    }
    
    /**
     * Applies late fee to this installment.
     */
    public ScheduledInstallment applyLateFee(BigDecimal lateFee) {
        if (lateFee == null || lateFee.compareTo(BigDecimal.ZERO) <= 0) {
            return this;
        }
        
        BigDecimal currentLateFee = lateFeeAmount != null ? lateFeeAmount : BigDecimal.ZERO;
        BigDecimal newLateFee = currentLateFee.add(lateFee);
        BigDecimal newTotalAmount = totalAmount.add(lateFee);
        
        return this.withLateFeeAmount(newLateFee)
                   .withTotalAmount(newTotalAmount)
                   .withLastModifiedDate(LocalDate.now());
    }
    
    /**
     * Marks this installment as overdue.
     */
    public ScheduledInstallment markAsOverdue() {
        return this.withOverdue(true)
                   .withDaysPastDue(calculateDaysPastDue())
                   .withLastModifiedDate(LocalDate.now());
    }
    
    /**
     * Gets the effective due date considering grace periods.
     */
    public LocalDate getEffectiveDueDate(Integer gracePeriodDays) {
        if (gracePeriodDays == null || gracePeriodDays <= 0) {
            return dueDate;
        }
        
        return dueDate.plusDays(gracePeriodDays);
    }
    
    /**
     * Checks if this installment is due within specified days.
     */
    public boolean isDueWithinDays(Integer days) {
        if (days == null || days <= 0) {
            return false;
        }
        
        LocalDate currentDate = LocalDate.now();
        LocalDate targetDate = currentDate.plusDays(days);
        
        return !dueDate.isAfter(targetDate) && !dueDate.isBefore(currentDate);
    }
    
    /**
     * Validates the scheduled installment.
     */
    public boolean isValid() {
        if (loanId == null || installmentId == null || installmentNumber == null) {
            return false;
        }
        
        if (installmentNumber <= 0) {
            return false;
        }
        
        if (dueDate == null) {
            return false;
        }
        
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        
        // Validate that total amount equals sum of components
        BigDecimal calculatedTotal = BigDecimal.ZERO;
        
        if (principalAmount != null) {
            calculatedTotal = calculatedTotal.add(principalAmount);
        }
        
        if (interestAmount != null) {
            calculatedTotal = calculatedTotal.add(interestAmount);
        }
        
        if (feesAmount != null) {
            calculatedTotal = calculatedTotal.add(feesAmount);
        }
        
        if (escrowAmount != null) {
            calculatedTotal = calculatedTotal.add(escrowAmount);
        }
        
        return calculatedTotal.compareTo(totalAmount) == 0;
    }
}