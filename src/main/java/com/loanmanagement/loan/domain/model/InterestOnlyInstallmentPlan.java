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
 * Interest-only installment plan with principal payment deferred.
 */
@Data
@Builder
@With
public class InterestOnlyInstallmentPlan {
    
    @NotNull
    private final InstallmentPlan basePlan;
    
    @NotNull
    private final InterestOnlyPeriod interestOnlyPeriod;
    
    @NotNull
    @Positive
    private final BigDecimal interestOnlyPaymentAmount;
    
    @NotNull
    private final Integer interestOnlyMonths;
    
    @NotNull
    private final LocalDate interestOnlyEndDate;
    
    @NotNull
    private final BigDecimal principalAndInterestPayment;
    
    @NotNull
    private final Integer amortizationMonths;
    
    private final List<DeferredPayment> deferredPayments;
    
    private final boolean allowsPartialPrincipalPayments;
    
    private final BigDecimal minimumPrincipalPayment;
    
    private final BigDecimal maximumPrincipalPayment;
    
    private final String transitionRule;
    
    private final boolean requiresQualificationReview;
    
    private final LocalDate qualificationReviewDate;
    
    private final String qualificationCriteria;
    
    private final boolean allowsExtension;
    
    private final Integer maxExtensionMonths;
    
    private final BigDecimal extensionFee;
    
    /**
     * Calculates the monthly interest-only payment.
     */
    public BigDecimal calculateInterestOnlyPayment() {
        BigDecimal monthlyRate = basePlan.getInterestRate()
                .divide(BigDecimal.valueOf(12), 10, java.math.RoundingMode.HALF_UP);
        
        return basePlan.getPrincipalAmount().multiply(monthlyRate);
    }
    
    /**
     * Calculates the principal and interest payment after interest-only period.
     */
    public BigDecimal calculatePrincipalAndInterestPayment() {
        BigDecimal monthlyRate = basePlan.getInterestRate()
                .divide(BigDecimal.valueOf(12), 10, java.math.RoundingMode.HALF_UP);
        
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return basePlan.getPrincipalAmount()
                    .divide(BigDecimal.valueOf(amortizationMonths), 2, java.math.RoundingMode.HALF_UP);
        }
        
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal powered = onePlusRate.pow(amortizationMonths);
        
        return basePlan.getPrincipalAmount().multiply(monthlyRate).multiply(powered)
                .divide(powered.subtract(BigDecimal.ONE), 2, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Calculates the payment shock (increase) when transitioning to amortization.
     */
    public BigDecimal calculatePaymentShock() {
        return calculatePrincipalAndInterestPayment().subtract(calculateInterestOnlyPayment());
    }
    
    /**
     * Calculates the payment shock as a percentage.
     */
    public BigDecimal calculatePaymentShockPercentage() {
        BigDecimal interestOnlyPayment = calculateInterestOnlyPayment();
        BigDecimal paymentShock = calculatePaymentShock();
        
        if (interestOnlyPayment.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return paymentShock.divide(interestOnlyPayment, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Calculates total payments over the life of the loan.
     */
    public BigDecimal calculateTotalPayments() {
        BigDecimal interestOnlyPayments = calculateInterestOnlyPayment()
                .multiply(BigDecimal.valueOf(interestOnlyMonths));
        
        BigDecimal principalAndInterestPayments = calculatePrincipalAndInterestPayment()
                .multiply(BigDecimal.valueOf(amortizationMonths));
        
        return interestOnlyPayments.add(principalAndInterestPayments);
    }
    
    /**
     * Calculates total interest paid over the life of the loan.
     */
    public BigDecimal calculateTotalInterest() {
        return calculateTotalPayments().subtract(basePlan.getPrincipalAmount());
    }
    
    /**
     * Calculates the remaining balance at the end of interest-only period.
     */
    public BigDecimal calculateRemainingBalanceAtTransition() {
        // Principal remains unchanged during interest-only period
        return basePlan.getPrincipalAmount();
    }
    
    /**
     * Calculates payment for a specific month.
     */
    public BigDecimal calculatePaymentForMonth(Integer month) {
        if (month == null || month <= 0) {
            return BigDecimal.ZERO;
        }
        
        if (month <= interestOnlyMonths) {
            return calculateInterestOnlyPayment();
        } else {
            return calculatePrincipalAndInterestPayment();
        }
    }
    
    /**
     * Calculates the impact of optional principal payments.
     */
    public BigDecimal calculateOptionalPrincipalImpact(BigDecimal principalPayment) {
        if (principalPayment == null || principalPayment.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal newPrincipal = basePlan.getPrincipalAmount().subtract(principalPayment);
        BigDecimal newMonthlyPayment = calculateAmortizedPayment(newPrincipal, amortizationMonths);
        
        return calculatePrincipalAndInterestPayment().subtract(newMonthlyPayment);
    }
    
    /**
     * Helper method to calculate amortized payment for a given principal and term.
     */
    private BigDecimal calculateAmortizedPayment(BigDecimal principal, Integer termMonths) {
        BigDecimal monthlyRate = basePlan.getInterestRate()
                .divide(BigDecimal.valueOf(12), 10, java.math.RoundingMode.HALF_UP);
        
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(termMonths), 2, java.math.RoundingMode.HALF_UP);
        }
        
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal powered = onePlusRate.pow(termMonths);
        
        return principal.multiply(monthlyRate).multiply(powered)
                .divide(powered.subtract(BigDecimal.ONE), 2, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Checks if the borrower qualifies for the interest-only period.
     */
    public boolean isQualified() {
        if (!requiresQualificationReview) {
            return true;
        }
        
        // This would typically involve complex qualification logic
        // For now, return true if qualification criteria is met
        return qualificationCriteria != null && !qualificationCriteria.isEmpty();
    }
    
    /**
     * Validates the interest-only installment plan.
     */
    public boolean isValid() {
        if (basePlan == null || interestOnlyPeriod == null) {
            return false;
        }
        
        if (interestOnlyMonths == null || interestOnlyMonths <= 0) {
            return false;
        }
        
        if (amortizationMonths == null || amortizationMonths <= 0) {
            return false;
        }
        
        if (interestOnlyEndDate == null || interestOnlyEndDate.isBefore(basePlan.getStartDate())) {
            return false;
        }
        
        return interestOnlyPaymentAmount != null && 
               interestOnlyPaymentAmount.compareTo(BigDecimal.ZERO) > 0;
    }
}