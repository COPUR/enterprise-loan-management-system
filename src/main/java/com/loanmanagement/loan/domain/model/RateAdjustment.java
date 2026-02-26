package com.loanmanagement.loan.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.With;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Rate adjustment record for variable rate loans.
 */
@Data
@Builder
@With
public class RateAdjustment {
    
    @NotNull
    private final String adjustmentId;
    
    @NotNull
    private final LoanId loanId;
    
    @NotNull
    private final LocalDate adjustmentDate;
    
    @NotNull
    private final LocalDate effectiveDate;
    
    @NotNull
    private final BigDecimal previousRate;
    
    @NotNull
    private final BigDecimal newRate;
    
    @NotNull
    private final BigDecimal rateChange;
    
    @NotNull
    private final String indexValue;
    
    @NotNull
    private final BigDecimal margin;
    
    @NotNull
    private final BigDecimal fullyIndexedRate;
    
    private final BigDecimal appliedRate;
    
    private final String adjustmentReason;
    
    private final String adjustmentType;
    
    private final boolean isCapApplied;
    
    private final BigDecimal capAmount;
    
    private final boolean isFloorApplied;
    
    private final BigDecimal floorAmount;
    
    private final String rateSource;
    
    private final LocalDate rateSourceDate;
    
    private final String calculationMethod;
    
    private final String notificationSent;
    
    private final LocalDate notificationDate;
    
    private final BigDecimal previousPayment;
    
    private final BigDecimal newPayment;
    
    private final BigDecimal paymentChange;
    
    private final String approvedBy;
    
    private final LocalDate approvalDate;
    
    /**
     * Calculates the rate change as a percentage.
     */
    public BigDecimal calculateRateChangePercentage() {
        if (previousRate == null || previousRate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return rateChange.divide(previousRate, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Calculates the payment change as a percentage.
     */
    public BigDecimal calculatePaymentChangePercentage() {
        if (previousPayment == null || previousPayment.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return paymentChange.divide(previousPayment, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Checks if this adjustment increases the rate.
     */
    public boolean isRateIncrease() {
        return rateChange != null && rateChange.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Checks if this adjustment decreases the rate.
     */
    public boolean isRateDecrease() {
        return rateChange != null && rateChange.compareTo(BigDecimal.ZERO) < 0;
    }
    
    /**
     * Checks if this adjustment is currently effective.
     */
    public boolean isCurrentlyEffective() {
        LocalDate currentDate = LocalDate.now();
        return !currentDate.isBefore(effectiveDate);
    }
    
    /**
     * Checks if notification was sent within required timeframe.
     */
    public boolean isNotificationTimely(Integer requiredDaysNotice) {
        if (notificationDate == null || requiredDaysNotice == null) {
            return false;
        }
        
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(notificationDate, effectiveDate);
        return daysBetween >= requiredDaysNotice;
    }
    
    /**
     * Gets the adjustment impact summary.
     */
    public String getAdjustmentSummary() {
        StringBuilder summary = new StringBuilder();
        
        summary.append("Rate Adjustment Summary:\n");
        summary.append("Previous Rate: ").append(previousRate).append("%\n");
        summary.append("New Rate: ").append(newRate).append("%\n");
        summary.append("Rate Change: ").append(rateChange).append("%\n");
        summary.append("Rate Change Percentage: ").append(calculateRateChangePercentage()).append("%\n");
        
        if (previousPayment != null && newPayment != null) {
            summary.append("Previous Payment: $").append(previousPayment).append("\n");
            summary.append("New Payment: $").append(newPayment).append("\n");
            summary.append("Payment Change: $").append(paymentChange).append("\n");
            summary.append("Payment Change Percentage: ").append(calculatePaymentChangePercentage()).append("%\n");
        }
        
        if (isCapApplied) {
            summary.append("Rate Cap Applied: ").append(capAmount).append("%\n");
        }
        
        if (isFloorApplied) {
            summary.append("Rate Floor Applied: ").append(floorAmount).append("%\n");
        }
        
        summary.append("Effective Date: ").append(effectiveDate).append("\n");
        
        return summary.toString();
    }
    
    /**
     * Validates the rate adjustment.
     */
    public boolean isValid() {
        if (adjustmentId == null || loanId == null) {
            return false;
        }
        
        if (adjustmentDate == null || effectiveDate == null) {
            return false;
        }
        
        if (effectiveDate.isBefore(adjustmentDate)) {
            return false;
        }
        
        if (previousRate == null || newRate == null) {
            return false;
        }
        
        if (previousRate.compareTo(BigDecimal.ZERO) < 0 || newRate.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        
        // Validate rate change calculation
        BigDecimal calculatedChange = newRate.subtract(previousRate);
        if (rateChange.compareTo(calculatedChange) != 0) {
            return false;
        }
        
        return true;
    }
}