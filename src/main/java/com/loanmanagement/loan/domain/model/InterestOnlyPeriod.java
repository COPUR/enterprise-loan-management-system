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
 * Interest-only payment period definition.
 */
@Data
@Builder
@With
public class InterestOnlyPeriod {
    
    @NotNull
    private final String periodId;
    
    @NotNull
    private final LoanId loanId;
    
    @NotNull
    private final LocalDate startDate;
    
    @NotNull
    private final LocalDate endDate;
    
    @NotNull
    @Positive
    private final Integer durationMonths;
    
    @NotNull
    private final BigDecimal interestRate;
    
    @NotNull
    private final BigDecimal monthlyInterestPayment;
    
    @NotNull
    private final BigDecimal principalBalance;
    
    private final BigDecimal totalInterestPayments;
    
    private final boolean allowsPartialPrincipalPayments;
    
    private final BigDecimal minimumPrincipalPayment;
    
    private final BigDecimal maximumPrincipalPayment;
    
    private final List<DeferredPayment> deferredPayments;
    
    private final String transitionRule;
    
    private final boolean requiresQualificationReview;
    
    private final LocalDate qualificationReviewDate;
    
    private final String qualificationCriteria;
    
    private final boolean allowsExtension;
    
    private final Integer maxExtensionMonths;
    
    private final BigDecimal extensionFee;
    
    private final String rateAdjustmentRule;
    
    private final boolean isRateFixed;
    
    private final String periodType;
    
    private final String periodDescription;
    
    private final LocalDate lastModifiedDate;
    
    /**
     * Calculates the duration of this interest-only period in days.
     */
    public Long calculatePeriodDays() {
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }
    
    /**
     * Calculates the total interest to be paid during this period.
     */
    public BigDecimal calculateTotalInterestPayments() {
        return monthlyInterestPayment.multiply(BigDecimal.valueOf(durationMonths));
    }
    
    /**
     * Calculates the monthly interest payment based on principal and rate.
     */
    public BigDecimal calculateMonthlyInterestPayment() {
        BigDecimal monthlyRate = interestRate.divide(BigDecimal.valueOf(12), 10, java.math.RoundingMode.HALF_UP);
        return principalBalance.multiply(monthlyRate);
    }
    
    /**
     * Checks if this interest-only period is currently active.
     */
    public boolean isCurrentlyActive() {
        LocalDate currentDate = LocalDate.now();
        return !currentDate.isBefore(startDate) && !currentDate.isAfter(endDate);
    }
    
    /**
     * Checks if this interest-only period is in the future.
     */
    public boolean isFuturePeriod() {
        LocalDate currentDate = LocalDate.now();
        return startDate.isAfter(currentDate);
    }
    
    /**
     * Checks if this interest-only period is in the past.
     */
    public boolean isPastPeriod() {
        LocalDate currentDate = LocalDate.now();
        return endDate.isBefore(currentDate);
    }
    
    /**
     * Gets the remaining months in this interest-only period.
     */
    public Integer getRemainingMonths() {
        LocalDate currentDate = LocalDate.now();
        
        if (currentDate.isAfter(endDate)) {
            return 0;
        }
        
        if (currentDate.isBefore(startDate)) {
            return durationMonths;
        }
        
        return Math.max(0, (int) java.time.temporal.ChronoUnit.MONTHS.between(currentDate, endDate));
    }
    
    /**
     * Gets the elapsed months in this interest-only period.
     */
    public Integer getElapsedMonths() {
        LocalDate currentDate = LocalDate.now();
        
        if (currentDate.isBefore(startDate)) {
            return 0;
        }
        
        if (currentDate.isAfter(endDate)) {
            return durationMonths;
        }
        
        return Math.max(0, (int) java.time.temporal.ChronoUnit.MONTHS.between(startDate, currentDate));
    }
    
    /**
     * Calculates the progress through this interest-only period as a percentage.
     */
    public BigDecimal calculatePeriodProgress() {
        Integer elapsedMonths = getElapsedMonths();
        
        if (durationMonths == 0) {
            return BigDecimal.ZERO;
        }
        
        return BigDecimal.valueOf(elapsedMonths)
                .divide(BigDecimal.valueOf(durationMonths), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Calculates the impact of optional principal payments on future payments.
     */
    public BigDecimal calculateOptionalPrincipalImpact(BigDecimal principalPayment, 
                                                      Integer futureAmortizationMonths, 
                                                      BigDecimal futureInterestRate) {
        if (principalPayment == null || principalPayment.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal newPrincipal = principalBalance.subtract(principalPayment);
        
        // Calculate original future payment
        BigDecimal originalFuturePayment = calculateAmortizedPayment(
            principalBalance, futureAmortizationMonths, futureInterestRate);
        
        // Calculate new future payment with reduced principal
        BigDecimal newFuturePayment = calculateAmortizedPayment(
            newPrincipal, futureAmortizationMonths, futureInterestRate);
        
        return originalFuturePayment.subtract(newFuturePayment);
    }
    
    /**
     * Helper method to calculate amortized payment.
     */
    private BigDecimal calculateAmortizedPayment(BigDecimal principal, Integer months, BigDecimal rate) {
        if (months == null || months <= 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal monthlyRate = rate.divide(BigDecimal.valueOf(12), 10, java.math.RoundingMode.HALF_UP);
        
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(months), 2, java.math.RoundingMode.HALF_UP);
        }
        
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal powered = onePlusRate.pow(months);
        
        return principal.multiply(monthlyRate).multiply(powered)
                .divide(powered.subtract(BigDecimal.ONE), 2, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Checks if qualification review is due.
     */
    public boolean isQualificationReviewDue() {
        if (!requiresQualificationReview || qualificationReviewDate == null) {
            return false;
        }
        
        LocalDate currentDate = LocalDate.now();
        return !currentDate.isBefore(qualificationReviewDate);
    }
    
    /**
     * Checks if extension is available.
     */
    public boolean isExtensionAvailable() {
        return allowsExtension && maxExtensionMonths != null && maxExtensionMonths > 0;
    }
    
    /**
     * Calculates the extended end date.
     */
    public LocalDate calculateExtendedEndDate(Integer extensionMonths) {
        if (extensionMonths == null || extensionMonths <= 0) {
            return endDate;
        }
        
        if (maxExtensionMonths != null && extensionMonths > maxExtensionMonths) {
            extensionMonths = maxExtensionMonths;
        }
        
        return endDate.plusMonths(extensionMonths);
    }
    
    /**
     * Calculates the extension cost.
     */
    public BigDecimal calculateExtensionCost(Integer extensionMonths) {
        if (extensionMonths == null || extensionMonths <= 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal totalCost = BigDecimal.ZERO;
        
        // Extension fee
        if (extensionFee != null) {
            totalCost = totalCost.add(extensionFee);
        }
        
        // Additional interest payments
        BigDecimal additionalInterest = monthlyInterestPayment.multiply(BigDecimal.valueOf(extensionMonths));
        totalCost = totalCost.add(additionalInterest);
        
        return totalCost;
    }
    
    /**
     * Gets the deferred payment total.
     */
    public BigDecimal getTotalDeferredPayments() {
        if (deferredPayments == null) {
            return BigDecimal.ZERO;
        }
        
        return deferredPayments.stream()
                .map(DeferredPayment::getDeferredAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Gets the interest-only period summary.
     */
    public String getPeriodSummary() {
        StringBuilder summary = new StringBuilder();
        
        summary.append("Interest-Only Period Summary:\n");
        summary.append("Period: ").append(startDate).append(" to ").append(endDate).append("\n");
        summary.append("Duration: ").append(durationMonths).append(" months\n");
        summary.append("Interest Rate: ").append(interestRate).append("%\n");
        summary.append("Monthly Interest Payment: $").append(monthlyInterestPayment).append("\n");
        summary.append("Principal Balance: $").append(principalBalance).append("\n");
        summary.append("Total Interest Payments: $").append(calculateTotalInterestPayments()).append("\n");
        summary.append("Period Progress: ").append(calculatePeriodProgress()).append("%\n");
        
        if (allowsPartialPrincipalPayments) {
            summary.append("Allows Optional Principal Payments\n");
            if (minimumPrincipalPayment != null) {
                summary.append("Minimum Principal Payment: $").append(minimumPrincipalPayment).append("\n");
            }
            if (maximumPrincipalPayment != null) {
                summary.append("Maximum Principal Payment: $").append(maximumPrincipalPayment).append("\n");
            }
        }
        
        if (getTotalDeferredPayments().compareTo(BigDecimal.ZERO) > 0) {
            summary.append("Total Deferred Payments: $").append(getTotalDeferredPayments()).append("\n");
        }
        
        return summary.toString();
    }
    
    /**
     * Validates the interest-only period.
     */
    public boolean isValid() {
        if (periodId == null || loanId == null) {
            return false;
        }
        
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return false;
        }
        
        if (durationMonths == null || durationMonths <= 0) {
            return false;
        }
        
        if (interestRate == null || interestRate.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        
        if (monthlyInterestPayment == null || monthlyInterestPayment.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        if (principalBalance == null || principalBalance.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        // Validate calculated vs provided monthly payment
        BigDecimal calculatedPayment = calculateMonthlyInterestPayment();
        if (monthlyInterestPayment.subtract(calculatedPayment).abs()
                .compareTo(BigDecimal.valueOf(0.01)) > 0) {
            return false;
        }
        
        if (allowsExtension && (maxExtensionMonths == null || maxExtensionMonths <= 0)) {
            return false;
        }
        
        return true;
    }
}