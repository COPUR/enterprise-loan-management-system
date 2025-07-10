package com.loanmanagement.loan.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.With;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Balloon payment execution and status.
 */
@Data
@Builder
@With
public class BalloonPayment {
    
    @NotNull
    private final String paymentId;
    
    @NotNull
    private final LoanId loanId;
    
    @NotNull
    @Positive
    private final BigDecimal scheduledAmount;
    
    @NotNull
    private final LocalDate scheduledDate;
    
    private final BigDecimal actualAmount;
    
    private final LocalDate actualDate;
    
    @NotNull
    private final PaymentStatus paymentStatus;
    
    private final String paymentMethod;
    
    private final String paymentReference;
    
    private final BigDecimal remainingBalance;
    
    private final boolean isPartialPayment;
    
    private final BigDecimal shortfallAmount;
    
    private final String shortfallReason;
    
    private final List<String> paymentOptions;
    
    private final boolean isRefinanced;
    
    private final String refinanceReference;
    
    private final BigDecimal refinanceAmount;
    
    private final boolean isExtended;
    
    private final LocalDate extendedDate;
    
    private final Integer extensionMonths;
    
    private final BigDecimal extensionFee;
    
    private final String defaultAction;
    
    private final boolean isInDefault;
    
    private final LocalDate defaultDate;
    
    private final String defaultReason;
    
    private final String collectionStatus;
    
    private final LocalDate lastContactDate;
    
    private final String lastContactMethod;
    
    private final String notes;
    
    private final LocalDate createdDate;
    
    private final LocalDate lastModifiedDate;
    
    /**
     * Calculates the payment completion percentage.
     */
    public BigDecimal calculatePaymentCompletionPercentage() {
        if (scheduledAmount == null || scheduledAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal paidAmount = actualAmount != null ? actualAmount : BigDecimal.ZERO;
        
        return paidAmount.divide(scheduledAmount, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Calculates the outstanding balloon amount.
     */
    public BigDecimal calculateOutstandingAmount() {
        if (paymentStatus == PaymentStatus.PAID) {
            return BigDecimal.ZERO;
        }
        
        if (actualAmount == null) {
            return scheduledAmount;
        }
        
        return scheduledAmount.subtract(actualAmount).max(BigDecimal.ZERO);
    }
    
    /**
     * Calculates days since scheduled due date.
     */
    public Long calculateDaysSinceScheduled() {
        LocalDate currentDate = LocalDate.now();
        return java.time.temporal.ChronoUnit.DAYS.between(scheduledDate, currentDate);
    }
    
    /**
     * Calculates days until scheduled due date.
     */
    public Long calculateDaysUntilScheduled() {
        LocalDate currentDate = LocalDate.now();
        
        if (scheduledDate.isBefore(currentDate) || scheduledDate.isEqual(currentDate)) {
            return 0L;
        }
        
        return java.time.temporal.ChronoUnit.DAYS.between(currentDate, scheduledDate);
    }
    
    /**
     * Checks if balloon payment is overdue.
     */
    public boolean isOverdue() {
        if (paymentStatus == PaymentStatus.PAID) {
            return false;
        }
        
        LocalDate currentDate = LocalDate.now();
        return currentDate.isAfter(scheduledDate);
    }
    
    /**
     * Checks if balloon payment is due within specified days.
     */
    public boolean isDueWithinDays(Integer days) {
        if (days == null || days <= 0) {
            return false;
        }
        
        LocalDate currentDate = LocalDate.now();
        LocalDate warningDate = currentDate.plusDays(days);
        
        return !scheduledDate.isAfter(warningDate) && !scheduledDate.isBefore(currentDate);
    }
    
    /**
     * Processes a balloon payment.
     */
    public BalloonPayment processPayment(BigDecimal paymentAmount, LocalDate paymentDate, 
                                        String paymentMethod, String paymentReference) {
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return this;
        }
        
        BigDecimal currentPaid = actualAmount != null ? actualAmount : BigDecimal.ZERO;
        BigDecimal newPaidAmount = currentPaid.add(paymentAmount);
        
        PaymentStatus newStatus;
        boolean isPartial = false;
        
        if (newPaidAmount.compareTo(scheduledAmount) >= 0) {
            newStatus = PaymentStatus.PAID;
        } else {
            newStatus = PaymentStatus.PARTIAL;
            isPartial = true;
        }
        
        BigDecimal newShortfall = scheduledAmount.subtract(newPaidAmount).max(BigDecimal.ZERO);
        
        return this.withActualAmount(newPaidAmount)
                   .withActualDate(paymentDate)
                   .withPaymentStatus(newStatus)
                   .withPaymentMethod(paymentMethod)
                   .withPaymentReference(paymentReference)
                   .withPartialPayment(isPartial)
                   .withShortfallAmount(newShortfall)
                   .withLastModifiedDate(LocalDate.now());
    }
    
    /**
     * Extends the balloon payment due date.
     */
    public BalloonPayment extendPayment(Integer months, BigDecimal extensionFee) {
        if (months == null || months <= 0) {
            return this;
        }
        
        LocalDate newScheduledDate = scheduledDate.plusMonths(months);
        
        return this.withExtended(true)
                   .withExtendedDate(newScheduledDate)
                   .withExtensionMonths(months)
                   .withExtensionFee(extensionFee)
                   .withScheduledDate(newScheduledDate)
                   .withLastModifiedDate(LocalDate.now());
    }
    
    /**
     * Marks the balloon payment as refinanced.
     */
    public BalloonPayment markAsRefinanced(String refinanceRef, BigDecimal refinanceAmount) {
        return this.withRefinanced(true)
                   .withRefinanceReference(refinanceRef)
                   .withRefinanceAmount(refinanceAmount)
                   .withPaymentStatus(PaymentStatus.PAID)
                   .withActualAmount(scheduledAmount)
                   .withActualDate(LocalDate.now())
                   .withLastModifiedDate(LocalDate.now());
    }
    
    /**
     * Marks the balloon payment as in default.
     */
    public BalloonPayment markAsDefault(String reason) {
        return this.withInDefault(true)
                   .withDefaultDate(LocalDate.now())
                   .withDefaultReason(reason)
                   .withPaymentStatus(PaymentStatus.DEFAULTED)
                   .withLastModifiedDate(LocalDate.now());
    }
    
    /**
     * Updates collection status.
     */
    public BalloonPayment updateCollectionStatus(String status, String contactMethod) {
        return this.withCollectionStatus(status)
                   .withLastContactDate(LocalDate.now())
                   .withLastContactMethod(contactMethod)
                   .withLastModifiedDate(LocalDate.now());
    }
    
    /**
     * Checks if balloon payment requires immediate attention.
     */
    public boolean requiresAttention() {
        return isOverdue() || 
               isDueWithinDays(30) || 
               isInDefault || 
               paymentStatus == PaymentStatus.PARTIAL;
    }
    
    /**
     * Gets the payment status description.
     */
    public String getStatusDescription() {
        StringBuilder description = new StringBuilder();
        
        description.append("Balloon Payment Status: ").append(paymentStatus).append("\n");
        
        if (paymentStatus == PaymentStatus.PAID) {
            description.append("Paid Amount: $").append(actualAmount).append("\n");
            description.append("Paid Date: ").append(actualDate).append("\n");
        } else {
            description.append("Scheduled Amount: $").append(scheduledAmount).append("\n");
            description.append("Scheduled Date: ").append(scheduledDate).append("\n");
            description.append("Outstanding Amount: $").append(calculateOutstandingAmount()).append("\n");
            
            if (isOverdue()) {
                description.append("Days Overdue: ").append(calculateDaysSinceScheduled()).append("\n");
            } else {
                description.append("Days Until Due: ").append(calculateDaysUntilScheduled()).append("\n");
            }
        }
        
        if (isExtended) {
            description.append("Extended by ").append(extensionMonths).append(" months\n");
        }
        
        if (isRefinanced) {
            description.append("Refinanced: ").append(refinanceReference).append("\n");
        }
        
        if (isInDefault) {
            description.append("In Default since: ").append(defaultDate).append("\n");
            description.append("Default Reason: ").append(defaultReason).append("\n");
        }
        
        return description.toString();
    }
    
    /**
     * Validates the balloon payment.
     */
    public boolean isValid() {
        if (paymentId == null || loanId == null) {
            return false;
        }
        
        if (scheduledAmount == null || scheduledAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        if (scheduledDate == null) {
            return false;
        }
        
        if (paymentStatus == null) {
            return false;
        }
        
        if (actualAmount != null && actualAmount.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        
        if (isExtended && (extensionMonths == null || extensionMonths <= 0)) {
            return false;
        }
        
        if (isInDefault && (defaultDate == null || defaultReason == null)) {
            return false;
        }
        
        return true;
    }
}