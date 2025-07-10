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
 * Graduated installment plan with payments that increase over time.
 */
@Data
@Builder
@With
public class GraduatedInstallmentPlan {
    
    @NotNull
    private final InstallmentPlan basePlan;
    
    @NotNull
    private final GraduatedPaymentSchedule graduatedSchedule;
    
    @NotNull
    @Positive
    private final BigDecimal initialPaymentAmount;
    
    @NotNull
    private final BigDecimal graduationRate;
    
    @NotNull
    private final Integer graduationFrequencyMonths;
    
    @NotNull
    private final Integer totalGraduations;
    
    @NotNull
    private final BigDecimal finalPaymentAmount;
    
    @NotNull
    private final List<PaymentPeriod> paymentPeriods;
    
    private final BigDecimal maximumPaymentIncrease;
    
    private final BigDecimal minimumPaymentIncrease;
    
    private final boolean isPercentageIncrease;
    
    private final BigDecimal negativeAmortizationLimit;
    
    private final BigDecimal accruedInterestLimit;
    
    private final String graduationTrigger;
    
    private final boolean allowsSkippedGraduation;
    
    private final String qualificationCriteria;
    
    /**
     * Calculates the payment amount for a specific period.
     */
    public BigDecimal calculatePaymentForPeriod(Integer periodNumber) {
        if (periodNumber == null || periodNumber <= 0) {
            return initialPaymentAmount;
        }
        
        // Find the appropriate payment period
        PaymentPeriod period = paymentPeriods.stream()
                .filter(p -> periodNumber >= p.getStartMonth() && periodNumber <= p.getEndMonth())
                .findFirst()
                .orElse(null);
        
        if (period != null) {
            return period.getPaymentAmount();
        }
        
        // Calculate graduated payment
        Integer graduationsApplied = Math.min(
            (periodNumber - 1) / graduationFrequencyMonths,
            totalGraduations
        );
        
        BigDecimal payment = initialPaymentAmount;
        
        for (int i = 0; i < graduationsApplied; i++) {
            if (isPercentageIncrease) {
                payment = payment.multiply(BigDecimal.ONE.add(graduationRate));
            } else {
                payment = payment.add(graduationRate);
            }
        }
        
        // Apply maximum/minimum limits
        if (maximumPaymentIncrease != null) {
            BigDecimal maxPayment = initialPaymentAmount.add(maximumPaymentIncrease);
            if (payment.compareTo(maxPayment) > 0) {
                payment = maxPayment;
            }
        }
        
        if (minimumPaymentIncrease != null) {
            BigDecimal minPayment = initialPaymentAmount.add(minimumPaymentIncrease);
            if (payment.compareTo(minPayment) < 0) {
                payment = minPayment;
            }
        }
        
        return payment;
    }
    
    /**
     * Calculates the total payments over the life of the loan.
     */
    public BigDecimal calculateTotalPayments() {
        BigDecimal totalPayments = BigDecimal.ZERO;
        
        for (PaymentPeriod period : paymentPeriods) {
            Integer monthsInPeriod = period.getEndMonth() - period.getStartMonth() + 1;
            BigDecimal periodPayments = period.getPaymentAmount().multiply(BigDecimal.valueOf(monthsInPeriod));
            totalPayments = totalPayments.add(periodPayments);
        }
        
        return totalPayments;
    }
    
    /**
     * Calculates negative amortization amount (if any).
     */
    public BigDecimal calculateNegativeAmortization() {
        BigDecimal totalInterest = BigDecimal.ZERO;
        BigDecimal totalPayments = calculateTotalPayments();
        BigDecimal principalBalance = basePlan.getPrincipalAmount();
        
        // Simplified calculation - in practice this would need detailed period-by-period analysis
        BigDecimal monthlyRate = basePlan.getInterestRate()
                .divide(BigDecimal.valueOf(12), 10, java.math.RoundingMode.HALF_UP);
        
        for (int month = 1; month <= basePlan.getTermInMonths(); month++) {
            BigDecimal interestForMonth = principalBalance.multiply(monthlyRate);
            BigDecimal paymentForMonth = calculatePaymentForPeriod(month);
            
            totalInterest = totalInterest.add(interestForMonth);
            
            if (paymentForMonth.compareTo(interestForMonth) < 0) {
                // Negative amortization occurs
                BigDecimal unpaidInterest = interestForMonth.subtract(paymentForMonth);
                principalBalance = principalBalance.add(unpaidInterest);
            } else {
                BigDecimal principalPayment = paymentForMonth.subtract(interestForMonth);
                principalBalance = principalBalance.subtract(principalPayment);
            }
        }
        
        return principalBalance.max(BigDecimal.ZERO);
    }
    
    /**
     * Gets the payment schedule breakdown by periods.
     */
    public List<PaymentPeriod> getPaymentScheduleBreakdown() {
        return paymentPeriods;
    }
    
    /**
     * Calculates the payment increase amount for a specific graduation.
     */
    public BigDecimal calculatePaymentIncrease(Integer graduationNumber) {
        if (graduationNumber == null || graduationNumber <= 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal currentPayment = calculatePaymentForPeriod(
            (graduationNumber - 1) * graduationFrequencyMonths + 1
        );
        BigDecimal nextPayment = calculatePaymentForPeriod(
            graduationNumber * graduationFrequencyMonths + 1
        );
        
        return nextPayment.subtract(currentPayment);
    }
    
    /**
     * Checks if negative amortization limit is exceeded.
     */
    public boolean isNegativeAmortizationLimitExceeded() {
        if (negativeAmortizationLimit == null) {
            return false;
        }
        
        BigDecimal negativeAmortization = calculateNegativeAmortization();
        return negativeAmortization.compareTo(negativeAmortizationLimit) > 0;
    }
    
    /**
     * Validates the graduated installment plan.
     */
    public boolean isValid() {
        if (basePlan == null || graduatedSchedule == null) {
            return false;
        }
        
        if (initialPaymentAmount == null || initialPaymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        if (graduationRate == null || graduationRate.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        if (graduationFrequencyMonths == null || graduationFrequencyMonths <= 0) {
            return false;
        }
        
        return paymentPeriods != null && !paymentPeriods.isEmpty();
    }
}