package com.loanmanagement.loan.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.With;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Deferred payment record for loans.
 */
@Data
@Builder
@With
public class DeferredPayment {
    
    @NotNull
    private final String deferralId;
    
    @NotNull
    private final LoanId loanId;
    
    @NotNull
    private final LocalDate originalDueDate;
    
    @NotNull
    private final LocalDate deferredToDate;
    
    @NotNull
    @Positive
    private final BigDecimal deferredAmount;
    
    @NotNull
    private final String deferralType;
    
    @NotNull
    private final String deferralReason;
    
    private final Integer installmentNumber;
    
    private final BigDecimal principalAmount;
    
    private final BigDecimal interestAmount;
    
    private final BigDecimal feesAmount;
    
    private final BigDecimal escrowAmount;
    
    private final BigDecimal deferralFee;
    
    private final BigDecimal additionalInterest;
    
    private final BigDecimal totalDeferralCost;
    
    private final boolean isCapitalized;
    
    private final boolean accruingInterest;
    
    private final BigDecimal deferralInterestRate;
    
    private final String approvedBy;
    
    private final LocalDate approvalDate;
    
    private final boolean requiresCustomerAgreement;
    
    private final boolean customerAgreementSigned;
    
    private final LocalDate customerAgreementDate;
    
    private final boolean isTemporary;
    
    private final LocalDate revertDate;
    
    private final String legalBasis;
    
    private final String notes;
    
    private final LocalDate createdDate;
    
    private final LocalDate lastModifiedDate;
    
    /**
     * Calculates the deferral period in days.
     */
    public Long calculateDeferralDays() {
        return java.time.temporal.ChronoUnit.DAYS.between(originalDueDate, deferredToDate);
    }
    
    /**
     * Calculates the deferral period in months.
     */
    public Long calculateDeferralMonths() {
        return java.time.temporal.ChronoUnit.MONTHS.between(originalDueDate, deferredToDate);
    }
    
    /**
     * Calculates the accrued interest on the deferred amount.
     */
    public BigDecimal calculateAccruedInterest() {
        if (!accruingInterest || deferralInterestRate == null) {
            return BigDecimal.ZERO;
        }
        
        Long deferralDays = calculateDeferralDays();
        BigDecimal dailyRate = deferralInterestRate
                .divide(BigDecimal.valueOf(365), 10, java.math.RoundingMode.HALF_UP);
        
        return deferredAmount.multiply(dailyRate).multiply(BigDecimal.valueOf(deferralDays));
    }
    
    /**
     * Calculates the total cost of deferral including fees and interest.
     */
    public BigDecimal calculateTotalDeferralCost() {
        BigDecimal totalCost = BigDecimal.ZERO;
        
        if (deferralFee != null) {
            totalCost = totalCost.add(deferralFee);
        }
        
        if (additionalInterest != null) {
            totalCost = totalCost.add(additionalInterest);
        }
        
        totalCost = totalCost.add(calculateAccruedInterest());
        
        return totalCost;
    }
    
    /**
     * Calculates the total amount due when deferral ends.
     */
    public BigDecimal calculateTotalAmountDue() {
        return deferredAmount.add(calculateTotalDeferralCost());
    }
    
    /**
     * Checks if the deferral is currently active.
     */
    public boolean isCurrentlyActive() {
        LocalDate currentDate = LocalDate.now();
        return !currentDate.isBefore(originalDueDate) && !currentDate.isAfter(deferredToDate);
    }
    
    /**
     * Checks if the deferral has expired.
     */
    public boolean isExpired() {
        LocalDate currentDate = LocalDate.now();
        return currentDate.isAfter(deferredToDate);
    }
    
    /**
     * Checks if the deferral is pending (future).
     */
    public boolean isPending() {
        LocalDate currentDate = LocalDate.now();
        return currentDate.isBefore(originalDueDate);
    }
    
    /**
     * Gets the remaining days until deferral expires.
     */
    public Long getRemainingDays() {
        LocalDate currentDate = LocalDate.now();
        
        if (currentDate.isAfter(deferredToDate)) {
            return 0L;
        }
        
        return java.time.temporal.ChronoUnit.DAYS.between(currentDate, deferredToDate);
    }
    
    /**
     * Gets the elapsed days since deferral started.
     */
    public Long getElapsedDays() {
        LocalDate currentDate = LocalDate.now();
        
        if (currentDate.isBefore(originalDueDate)) {
            return 0L;
        }
        
        if (currentDate.isAfter(deferredToDate)) {
            return calculateDeferralDays();
        }
        
        return java.time.temporal.ChronoUnit.DAYS.between(originalDueDate, currentDate);
    }
    
    /**
     * Calculates the deferral progress as a percentage.
     */
    public BigDecimal calculateDeferralProgress() {
        Long totalDays = calculateDeferralDays();
        Long elapsedDays = getElapsedDays();
        
        if (totalDays == 0) {
            return BigDecimal.ZERO;
        }
        
        return BigDecimal.valueOf(elapsedDays)
                .divide(BigDecimal.valueOf(totalDays), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Checks if customer agreement is required and obtained.
     */
    public boolean isCustomerAgreementComplete() {
        if (!requiresCustomerAgreement) {
            return true;
        }
        
        return customerAgreementSigned && customerAgreementDate != null;
    }
    
    /**
     * Capitalizes the deferred payment (adds to principal balance).
     */
    public DeferredPayment capitalize() {
        return this.withCapitalized(true)
                   .withLastModifiedDate(LocalDate.now());
    }
    
    /**
     * Extends the deferral to a new date.
     */
    public DeferredPayment extendDeferral(LocalDate newDeferredDate, String reason) {
        return this.withDeferredToDate(newDeferredDate)
                   .withDeferralReason(deferralReason + " | Extended: " + reason)
                   .withLastModifiedDate(LocalDate.now());
    }
    
    /**
     * Gets the deferral impact summary.
     */
    public String getDeferralSummary() {
        StringBuilder summary = new StringBuilder();
        
        summary.append("Deferral Summary:\n");
        summary.append("Type: ").append(deferralType).append("\n");
        summary.append("Reason: ").append(deferralReason).append("\n");
        summary.append("Original Due Date: ").append(originalDueDate).append("\n");
        summary.append("Deferred To Date: ").append(deferredToDate).append("\n");
        summary.append("Deferral Period: ").append(calculateDeferralDays()).append(" days\n");
        summary.append("Deferred Amount: $").append(deferredAmount).append("\n");
        
        if (deferralFee != null && deferralFee.compareTo(BigDecimal.ZERO) > 0) {
            summary.append("Deferral Fee: $").append(deferralFee).append("\n");
        }
        
        if (accruingInterest) {
            summary.append("Accrued Interest: $").append(calculateAccruedInterest()).append("\n");
        }
        
        summary.append("Total Deferral Cost: $").append(calculateTotalDeferralCost()).append("\n");
        summary.append("Total Amount Due: $").append(calculateTotalAmountDue()).append("\n");
        
        if (isCapitalized) {
            summary.append("Status: Capitalized\n");
        } else if (isCurrentlyActive()) {
            summary.append("Status: Active (").append(getRemainingDays()).append(" days remaining)\n");
        } else if (isExpired()) {
            summary.append("Status: Expired\n");
        } else {
            summary.append("Status: Pending\n");
        }
        
        if (isTemporary && revertDate != null) {
            summary.append("Temporary deferral - reverts on: ").append(revertDate).append("\n");
        }
        
        return summary.toString();
    }
    
    /**
     * Validates the deferred payment.
     */
    public boolean isValid() {
        if (deferralId == null || loanId == null) {
            return false;
        }
        
        if (originalDueDate == null || deferredToDate == null) {
            return false;
        }
        
        if (deferredToDate.isBefore(originalDueDate)) {
            return false;
        }
        
        if (deferredAmount == null || deferredAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        if (deferralType == null || deferralReason == null) {
            return false;
        }
        
        if (requiresCustomerAgreement && !isCustomerAgreementComplete()) {
            return false;
        }
        
        if (accruingInterest && deferralInterestRate == null) {
            return false;
        }
        
        if (isTemporary && revertDate == null) {
            return false;
        }
        
        return true;
    }
}