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
 * Balloon payment terms and conditions.
 */
@Data
@Builder
@With
public class BalloonPaymentTerms {
    
    @NotNull
    private final String termsId;
    
    @NotNull
    private final LoanId loanId;
    
    @NotNull
    @Positive
    private final BigDecimal balloonAmount;
    
    @NotNull
    private final LocalDate balloonDueDate;
    
    @NotNull
    private final BigDecimal balloonPercentage;
    
    private final BigDecimal minimumBalloonAmount;
    
    private final BigDecimal maximumBalloonAmount;
    
    private final String balloonCalculationMethod;
    
    private final boolean isAmortizing;
    
    private final boolean isInterestOnly;
    
    private final Integer regularPaymentMonths;
    
    private final BigDecimal regularPaymentAmount;
    
    private final List<String> paymentOptions;
    
    private final boolean allowsRefinancing;
    
    private final boolean allowsExtension;
    
    private final Integer maxExtensionMonths;
    
    private final BigDecimal extensionRate;
    
    private final BigDecimal extensionFee;
    
    private final String defaultAction;
    
    private final List<String> qualificationRequirements;
    
    private final String disclosureText;
    
    private final boolean requiresPrePaymentNotice;
    
    private final Integer prePaymentNoticeDays;
    
    private final String riskDisclosure;
    
    private final String refinanceDisclosure;
    
    private final LocalDate termsEffectiveDate;
    
    private final LocalDate termsExpirationDate;
    
    /**
     * Calculates the balloon payment as a percentage of the original loan amount.
     */
    public BigDecimal calculateBalloonPercentage(BigDecimal originalLoanAmount) {
        if (originalLoanAmount == null || originalLoanAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return balloonAmount.divide(originalLoanAmount, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Calculates the regular payment amount for the amortizing portion.
     */
    public BigDecimal calculateRegularPaymentAmount(BigDecimal loanAmount, BigDecimal interestRate) {
        if (isInterestOnly) {
            // Interest-only payment
            BigDecimal monthlyRate = interestRate.divide(BigDecimal.valueOf(12), 10, java.math.RoundingMode.HALF_UP);
            return loanAmount.multiply(monthlyRate);
        }
        
        if (regularPaymentMonths == null || regularPaymentMonths <= 0) {
            return BigDecimal.ZERO;
        }
        
        // Calculate amortizing amount (loan amount minus balloon)
        BigDecimal amortizingAmount = loanAmount.subtract(balloonAmount);
        
        if (amortizingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal monthlyRate = interestRate.divide(BigDecimal.valueOf(12), 10, java.math.RoundingMode.HALF_UP);
        
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return amortizingAmount.divide(BigDecimal.valueOf(regularPaymentMonths), 2, java.math.RoundingMode.HALF_UP);
        }
        
        // Standard amortization formula
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal powered = onePlusRate.pow(regularPaymentMonths);
        
        return amortizingAmount.multiply(monthlyRate).multiply(powered)
                .divide(powered.subtract(BigDecimal.ONE), 2, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Calculates the total payments including balloon.
     */
    public BigDecimal calculateTotalPayments(BigDecimal loanAmount, BigDecimal interestRate) {
        BigDecimal regularPayments = calculateRegularPaymentAmount(loanAmount, interestRate)
                .multiply(BigDecimal.valueOf(regularPaymentMonths != null ? regularPaymentMonths : 0));
        
        return regularPayments.add(balloonAmount);
    }
    
    /**
     * Calculates the total interest paid.
     */
    public BigDecimal calculateTotalInterest(BigDecimal loanAmount, BigDecimal interestRate) {
        BigDecimal totalPayments = calculateTotalPayments(loanAmount, interestRate);
        return totalPayments.subtract(loanAmount);
    }
    
    /**
     * Checks if balloon payment is due within specified days.
     */
    public boolean isBalloonDueWithinDays(Integer days) {
        if (days == null || days <= 0) {
            return false;
        }
        
        LocalDate currentDate = LocalDate.now();
        LocalDate warningDate = currentDate.plusDays(days);
        
        return !balloonDueDate.isAfter(warningDate);
    }
    
    /**
     * Checks if balloon payment terms are currently valid.
     */
    public boolean areTermsCurrentlyValid() {
        LocalDate currentDate = LocalDate.now();
        
        if (termsEffectiveDate != null && currentDate.isBefore(termsEffectiveDate)) {
            return false;
        }
        
        if (termsExpirationDate != null && currentDate.isAfter(termsExpirationDate)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Checks if extension is available and valid.
     */
    public boolean isExtensionAvailable() {
        if (!allowsExtension || maxExtensionMonths == null || maxExtensionMonths <= 0) {
            return false;
        }
        
        return areTermsCurrentlyValid();
    }
    
    /**
     * Calculates the extended balloon due date.
     */
    public LocalDate calculateExtendedBalloonDate(Integer extensionMonths) {
        if (extensionMonths == null || extensionMonths <= 0) {
            return balloonDueDate;
        }
        
        if (maxExtensionMonths != null && extensionMonths > maxExtensionMonths) {
            extensionMonths = maxExtensionMonths;
        }
        
        return balloonDueDate.plusMonths(extensionMonths);
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
        
        // Extension interest
        if (extensionRate != null) {
            BigDecimal monthlyExtensionRate = extensionRate
                    .divide(BigDecimal.valueOf(12), 10, java.math.RoundingMode.HALF_UP);
            BigDecimal extensionInterest = balloonAmount.multiply(monthlyExtensionRate)
                    .multiply(BigDecimal.valueOf(extensionMonths));
            totalCost = totalCost.add(extensionInterest);
        }
        
        return totalCost;
    }
    
    /**
     * Gets the available payment options for balloon payment.
     */
    public List<String> getAvailablePaymentOptions() {
        return paymentOptions != null ? paymentOptions : List.of();
    }
    
    /**
     * Checks if pre-payment notice is required.
     */
    public boolean isPrePaymentNoticeRequired() {
        return requiresPrePaymentNotice && prePaymentNoticeDays != null && prePaymentNoticeDays > 0;
    }
    
    /**
     * Calculates the pre-payment notice deadline.
     */
    public LocalDate calculatePrePaymentNoticeDeadline() {
        if (!isPrePaymentNoticeRequired()) {
            return balloonDueDate;
        }
        
        return balloonDueDate.minusDays(prePaymentNoticeDays);
    }
    
    /**
     * Validates the balloon payment terms.
     */
    public boolean isValid() {
        if (termsId == null || loanId == null) {
            return false;
        }
        
        if (balloonAmount == null || balloonAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        if (balloonDueDate == null) {
            return false;
        }
        
        if (balloonPercentage == null || balloonPercentage.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        if (minimumBalloonAmount != null && balloonAmount.compareTo(minimumBalloonAmount) < 0) {
            return false;
        }
        
        if (maximumBalloonAmount != null && balloonAmount.compareTo(maximumBalloonAmount) > 0) {
            return false;
        }
        
        if (allowsExtension && (maxExtensionMonths == null || maxExtensionMonths <= 0)) {
            return false;
        }
        
        return true;
    }
}