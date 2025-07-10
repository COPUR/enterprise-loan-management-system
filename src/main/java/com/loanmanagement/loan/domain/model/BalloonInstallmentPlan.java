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
 * Balloon installment plan with a large final payment.
 */
@Data
@Builder
@With
public class BalloonInstallmentPlan {
    
    @NotNull
    private final InstallmentPlan basePlan;
    
    @NotNull
    private final BalloonPaymentTerms balloonTerms;
    
    @NotNull
    @Positive
    private final BigDecimal balloonAmount;
    
    @NotNull
    private final LocalDate balloonDueDate;
    
    @NotNull
    private final BigDecimal regularPaymentAmount;
    
    @NotNull
    private final Integer regularPaymentCount;
    
    @NotNull
    private final BigDecimal amortizationAmount;
    
    private final BigDecimal balloonPaymentPercentage;
    
    private final boolean isInterestOnly;
    
    private final String balloonPaymentOption;
    
    private final List<String> refinanceOptions;
    
    private final BigDecimal estimatedRefinanceRate;
    
    private final boolean allowsExtension;
    
    private final Integer maxExtensionMonths;
    
    private final BigDecimal extensionFee;
    
    private final String balloonDisclosureText;
    
    /**
     * Calculates the regular payment amount before balloon payment.
     */
    public BigDecimal calculateRegularPayment() {
        if (isInterestOnly) {
            // Interest-only payments until balloon
            BigDecimal monthlyRate = basePlan.getInterestRate()
                    .divide(BigDecimal.valueOf(12), 10, java.math.RoundingMode.HALF_UP);
            return amortizationAmount.multiply(monthlyRate);
        } else {
            // Amortizing payments based on amortization amount
            BigDecimal monthlyRate = basePlan.getInterestRate()
                    .divide(BigDecimal.valueOf(12), 10, java.math.RoundingMode.HALF_UP);
            
            if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
                return amortizationAmount.divide(BigDecimal.valueOf(regularPaymentCount), 2, java.math.RoundingMode.HALF_UP);
            }
            
            BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
            BigDecimal powered = onePlusRate.pow(regularPaymentCount);
            
            return amortizationAmount.multiply(monthlyRate).multiply(powered)
                    .divide(powered.subtract(BigDecimal.ONE), 2, java.math.RoundingMode.HALF_UP);
        }
    }
    
    /**
     * Calculates the remaining balance at balloon payment date.
     */
    public BigDecimal calculateRemainingBalanceAtBalloon() {
        if (isInterestOnly) {
            return amortizationAmount;
        }
        
        BigDecimal monthlyRate = basePlan.getInterestRate()
                .divide(BigDecimal.valueOf(12), 10, java.math.RoundingMode.HALF_UP);
        
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return amortizationAmount.subtract(
                    regularPaymentAmount.multiply(BigDecimal.valueOf(regularPaymentCount))
            );
        }
        
        // Standard amortization calculation
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal powered = onePlusRate.pow(regularPaymentCount);
        
        return amortizationAmount.multiply(powered)
                .subtract(regularPaymentAmount.multiply(powered.subtract(BigDecimal.ONE).divide(monthlyRate, 10, java.math.RoundingMode.HALF_UP)));
    }
    
    /**
     * Calculates total payments including balloon.
     */
    public BigDecimal calculateTotalPayments() {
        BigDecimal regularPayments = regularPaymentAmount.multiply(BigDecimal.valueOf(regularPaymentCount));
        return regularPayments.add(balloonAmount);
    }
    
    /**
     * Calculates total interest paid.
     */
    public BigDecimal calculateTotalInterest() {
        return calculateTotalPayments().subtract(basePlan.getPrincipalAmount());
    }
    
    /**
     * Calculates balloon payment as percentage of original loan amount.
     */
    public BigDecimal calculateBalloonPercentage() {
        return balloonAmount.divide(basePlan.getPrincipalAmount(), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Estimates refinance payment if balloon is refinanced.
     */
    public BigDecimal estimateRefinancePayment(Integer newTermMonths) {
        if (estimatedRefinanceRate == null || newTermMonths == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal monthlyRate = estimatedRefinanceRate
                .divide(BigDecimal.valueOf(12), 10, java.math.RoundingMode.HALF_UP);
        
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return balloonAmount.divide(BigDecimal.valueOf(newTermMonths), 2, java.math.RoundingMode.HALF_UP);
        }
        
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal powered = onePlusRate.pow(newTermMonths);
        
        return balloonAmount.multiply(monthlyRate).multiply(powered)
                .divide(powered.subtract(BigDecimal.ONE), 2, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Checks if balloon payment is due within a specified number of months.
     */
    public boolean isBalloonDueWithinMonths(Integer months) {
        LocalDate currentDate = LocalDate.now();
        LocalDate warningDate = currentDate.plusMonths(months);
        
        return !balloonDueDate.isAfter(warningDate);
    }
    
    /**
     * Validates the balloon installment plan.
     */
    public boolean isValid() {
        if (basePlan == null || balloonTerms == null) {
            return false;
        }
        
        if (balloonAmount == null || balloonAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        if (balloonDueDate == null || balloonDueDate.isBefore(basePlan.getStartDate())) {
            return false;
        }
        
        return regularPaymentAmount != null && 
               regularPaymentAmount.compareTo(BigDecimal.ZERO) > 0 &&
               regularPaymentCount != null && 
               regularPaymentCount > 0;
    }
}