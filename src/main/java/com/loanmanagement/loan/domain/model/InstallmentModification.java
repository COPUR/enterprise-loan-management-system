package com.loanmanagement.loan.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.With;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Individual installment modification record.
 */
@Data
@Builder
@With
public class InstallmentModification {
    
    @NotNull
    private final String modificationId;
    
    @NotNull
    private final LoanId loanId;
    
    @NotNull
    private final InstallmentModificationType modificationType;
    
    @NotNull
    private final LocalDate modificationDate;
    
    @NotNull
    private final LocalDate effectiveDate;
    
    @NotNull
    private final String fieldModified;
    
    @NotNull
    private final String previousValue;
    
    @NotNull
    private final String newValue;
    
    private final BigDecimal previousAmount;
    
    private final BigDecimal newAmount;
    
    private final BigDecimal amountChange;
    
    private final Integer installmentNumber;
    
    private final LocalDate installmentDueDate;
    
    private final String modificationReason;
    
    private final String modificationDescription;
    
    private final String approvedBy;
    
    private final LocalDate approvalDate;
    
    private final boolean requiresCustomerNotification;
    
    private final boolean customerNotified;
    
    private final LocalDate customerNotificationDate;
    
    private final boolean isReversible;
    
    private final boolean isReversed;
    
    private final LocalDate reversalDate;
    
    private final String reversalReason;
    
    private final String legalBasis;
    
    private final String documentationReference;
    
    /**
     * Calculates the percentage change in amount.
     */
    public BigDecimal calculateAmountChangePercentage() {
        if (previousAmount == null || previousAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        if (amountChange == null) {
            return BigDecimal.ZERO;
        }
        
        return amountChange.divide(previousAmount, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Checks if this modification increases the amount.
     */
    public boolean isAmountIncrease() {
        return amountChange != null && amountChange.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Checks if this modification decreases the amount.
     */
    public boolean isAmountDecrease() {
        return amountChange != null && amountChange.compareTo(BigDecimal.ZERO) < 0;
    }
    
    /**
     * Checks if this modification is currently effective.
     */
    public boolean isCurrentlyEffective() {
        if (isReversed) {
            return false;
        }
        
        LocalDate currentDate = LocalDate.now();
        return !currentDate.isBefore(effectiveDate);
    }
    
    /**
     * Checks if customer notification is required and has been sent.
     */
    public boolean isCustomerNotificationComplete() {
        if (!requiresCustomerNotification) {
            return true;
        }
        
        return customerNotified && customerNotificationDate != null;
    }
    
    /**
     * Calculates days since modification was made.
     */
    public Long getDaysSinceModification() {
        LocalDate currentDate = LocalDate.now();
        return java.time.temporal.ChronoUnit.DAYS.between(modificationDate, currentDate);
    }
    
    /**
     * Calculates days until modification becomes effective.
     */
    public Long getDaysUntilEffective() {
        LocalDate currentDate = LocalDate.now();
        
        if (effectiveDate.isBefore(currentDate) || effectiveDate.isEqual(currentDate)) {
            return 0L;
        }
        
        return java.time.temporal.ChronoUnit.DAYS.between(currentDate, effectiveDate);
    }
    
    /**
     * Reverses this modification.
     */
    public InstallmentModification reverse(String reason) {
        if (!isReversible || isReversed) {
            return this;
        }
        
        return this.withReversed(true)
                   .withReversalDate(LocalDate.now())
                   .withReversalReason(reason);
    }
    
    /**
     * Gets the modification impact summary.
     */
    public String getModificationSummary() {
        StringBuilder summary = new StringBuilder();
        
        summary.append("Modification Summary:\n");
        summary.append("Type: ").append(modificationType).append("\n");
        summary.append("Field Modified: ").append(fieldModified).append("\n");
        summary.append("Previous Value: ").append(previousValue).append("\n");
        summary.append("New Value: ").append(newValue).append("\n");
        
        if (previousAmount != null && newAmount != null) {
            summary.append("Previous Amount: $").append(previousAmount).append("\n");
            summary.append("New Amount: $").append(newAmount).append("\n");
            summary.append("Amount Change: $").append(amountChange).append("\n");
            summary.append("Percentage Change: ").append(calculateAmountChangePercentage()).append("%\n");
        }
        
        if (installmentNumber != null) {
            summary.append("Installment Number: ").append(installmentNumber).append("\n");
        }
        
        if (installmentDueDate != null) {
            summary.append("Installment Due Date: ").append(installmentDueDate).append("\n");
        }
        
        summary.append("Effective Date: ").append(effectiveDate).append("\n");
        summary.append("Modification Reason: ").append(modificationReason).append("\n");
        
        if (isReversed) {
            summary.append("REVERSED on ").append(reversalDate).append(": ").append(reversalReason).append("\n");
        }
        
        return summary.toString();
    }
    
    /**
     * Validates the installment modification.
     */
    public boolean isValid() {
        if (modificationId == null || loanId == null || modificationType == null) {
            return false;
        }
        
        if (modificationDate == null || effectiveDate == null) {
            return false;
        }
        
        if (effectiveDate.isBefore(modificationDate)) {
            return false;
        }
        
        if (fieldModified == null || previousValue == null || newValue == null) {
            return false;
        }
        
        if (previousValue.equals(newValue)) {
            return false;
        }
        
        if (requiresCustomerNotification && !isCustomerNotificationComplete()) {
            return false;
        }
        
        if (isReversed && (reversalDate == null || reversalReason == null)) {
            return false;
        }
        
        return true;
    }
}