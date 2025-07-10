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
 * Graduated payment schedule with increasing payments over time.
 */
@Data
@Builder
@With
public class GraduatedPaymentSchedule {
    
    @NotNull
    private final String scheduleId;
    
    @NotNull
    private final LoanId loanId;
    
    @NotNull
    private final LocalDate scheduleDate;
    
    @NotNull
    private final List<PaymentPeriod> paymentPeriods;
    
    @NotNull
    @Positive
    private final BigDecimal initialPayment;
    
    @NotNull
    @Positive
    private final BigDecimal finalPayment;
    
    @NotNull
    private final BigDecimal graduationRate;
    
    @NotNull
    private final Integer graduationFrequency;
    
    @NotNull
    private final Integer totalGraduations;
    
    private final BigDecimal maximumPaymentIncrease;
    
    private final BigDecimal minimumPaymentIncrease;
    
    private final boolean isPercentageIncrease;
    
    private final String graduationTrigger;
    
    private final String qualificationCriteria;
    
    private final BigDecimal negativeAmortizationLimit;
    
    private final BigDecimal totalScheduledPayments;
    
    private final BigDecimal totalInterestAmount;
    
    private final BigDecimal totalNegativeAmortization;
    
    private final boolean allowsSkippedGraduation;
    
    private final Integer maxSkippedGraduations;
    
    private final String scheduleType;
    
    private final LocalDate lastModifiedDate;
    
    private final String modifiedBy;
    
    /**
     * Calculates the payment amount for a specific period number.
     */
    public BigDecimal calculatePaymentForPeriod(Integer periodNumber) {
        if (periodNumber == null || periodNumber <= 0) {
            return initialPayment;
        }
        
        // Find the payment period that contains this period number
        PaymentPeriod period = paymentPeriods.stream()
                .filter(p -> p.containsMonth(periodNumber))
                .findFirst()
                .orElse(null);
        
        if (period != null) {
            return period.getPaymentAmount();
        }
        
        // Calculate graduated payment if not found in periods
        return calculateGraduatedPayment(periodNumber);
    }
    
    /**
     * Calculates the graduated payment amount for a specific period.
     */
    private BigDecimal calculateGraduatedPayment(Integer periodNumber) {
        Integer graduationsApplied = Math.min(
            (periodNumber - 1) / graduationFrequency,
            totalGraduations
        );
        
        BigDecimal payment = initialPayment;
        
        for (int i = 0; i < graduationsApplied; i++) {
            if (isPercentageIncrease) {
                payment = payment.multiply(BigDecimal.ONE.add(graduationRate));
            } else {
                payment = payment.add(graduationRate);
            }
        }
        
        // Apply maximum/minimum limits
        if (maximumPaymentIncrease != null) {
            BigDecimal maxPayment = initialPayment.add(maximumPaymentIncrease);
            if (payment.compareTo(maxPayment) > 0) {
                payment = maxPayment;
            }
        }
        
        if (minimumPaymentIncrease != null) {
            BigDecimal minPayment = initialPayment.add(minimumPaymentIncrease);
            if (payment.compareTo(minPayment) < 0) {
                payment = minPayment;
            }
        }
        
        return payment;
    }
    
    /**
     * Calculates the total payments over the schedule.
     */
    public BigDecimal calculateTotalScheduledPayments() {
        return paymentPeriods.stream()
                .map(PaymentPeriod::calculateTotalPeriodPayments)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Calculates the average payment amount.
     */
    public BigDecimal calculateAveragePayment() {
        if (paymentPeriods.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        Integer totalMonths = paymentPeriods.stream()
                .mapToInt(PaymentPeriod::calculatePeriodMonths)
                .sum();
        
        if (totalMonths == 0) {
            return BigDecimal.ZERO;
        }
        
        return calculateTotalScheduledPayments().divide(BigDecimal.valueOf(totalMonths), 2, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Calculates the payment increase for a specific graduation.
     */
    public BigDecimal calculatePaymentIncrease(Integer graduationNumber) {
        if (graduationNumber == null || graduationNumber <= 0) {
            return BigDecimal.ZERO;
        }
        
        Integer previousPeriod = (graduationNumber - 1) * graduationFrequency;
        Integer currentPeriod = graduationNumber * graduationFrequency;
        
        BigDecimal previousPayment = calculatePaymentForPeriod(previousPeriod);
        BigDecimal currentPayment = calculatePaymentForPeriod(currentPeriod);
        
        return currentPayment.subtract(previousPayment);
    }
    
    /**
     * Calculates the cumulative payment increase from initial payment.
     */
    public BigDecimal calculateCumulativePaymentIncrease(Integer periodNumber) {
        BigDecimal currentPayment = calculatePaymentForPeriod(periodNumber);
        return currentPayment.subtract(initialPayment);
    }
    
    /**
     * Gets the payment period for a specific month.
     */
    public PaymentPeriod getPaymentPeriodForMonth(Integer month) {
        return paymentPeriods.stream()
                .filter(p -> p.containsMonth(month))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Gets the current payment period.
     */
    public PaymentPeriod getCurrentPaymentPeriod() {
        return paymentPeriods.stream()
                .filter(PaymentPeriod::isCurrentlyActive)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Gets the next payment period.
     */
    public PaymentPeriod getNextPaymentPeriod() {
        return paymentPeriods.stream()
                .filter(PaymentPeriod::isFuturePeriod)
                .min((p1, p2) -> p1.getStartDate().compareTo(p2.getStartDate()))
                .orElse(null);
    }
    
    /**
     * Calculates the negative amortization amount.
     */
    public BigDecimal calculateNegativeAmortization() {
        return paymentPeriods.stream()
                .filter(p -> p.getRemainingBalanceEnd() != null && p.getRemainingBalanceStart() != null)
                .filter(p -> p.getRemainingBalanceEnd().compareTo(p.getRemainingBalanceStart()) > 0)
                .map(p -> p.getRemainingBalanceEnd().subtract(p.getRemainingBalanceStart()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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
     * Calculates the schedule progress as a percentage.
     */
    public BigDecimal calculateScheduleProgress() {
        PaymentPeriod currentPeriod = getCurrentPaymentPeriod();
        if (currentPeriod == null) {
            return BigDecimal.ZERO;
        }
        
        Integer totalMonths = paymentPeriods.stream()
                .mapToInt(PaymentPeriod::calculatePeriodMonths)
                .sum();
        
        Integer elapsedMonths = paymentPeriods.stream()
                .filter(p -> p.isPastPeriod() || p.isCurrentlyActive())
                .mapToInt(PaymentPeriod::getElapsedMonths)
                .sum();
        
        if (totalMonths == 0) {
            return BigDecimal.ZERO;
        }
        
        return BigDecimal.valueOf(elapsedMonths)
                .divide(BigDecimal.valueOf(totalMonths), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Gets the graduation schedule summary.
     */
    public String getGraduationScheduleSummary() {
        StringBuilder summary = new StringBuilder();
        
        summary.append("Graduated Payment Schedule Summary:\n");
        summary.append("Initial Payment: $").append(initialPayment).append("\n");
        summary.append("Final Payment: $").append(finalPayment).append("\n");
        summary.append("Graduation Rate: ").append(graduationRate);
        if (isPercentageIncrease) {
            summary.append("%\n");
        } else {
            summary.append(" (fixed amount)\n");
        }
        summary.append("Graduation Frequency: Every ").append(graduationFrequency).append(" months\n");
        summary.append("Total Graduations: ").append(totalGraduations).append("\n");
        summary.append("Total Scheduled Payments: $").append(calculateTotalScheduledPayments()).append("\n");
        summary.append("Average Payment: $").append(calculateAveragePayment()).append("\n");
        
        if (totalNegativeAmortization != null && totalNegativeAmortization.compareTo(BigDecimal.ZERO) > 0) {
            summary.append("Negative Amortization: $").append(totalNegativeAmortization).append("\n");
        }
        
        summary.append("Schedule Progress: ").append(calculateScheduleProgress()).append("%\n");
        
        return summary.toString();
    }
    
    /**
     * Validates the graduated payment schedule.
     */
    public boolean isValid() {
        if (scheduleId == null || loanId == null) {
            return false;
        }
        
        if (paymentPeriods == null || paymentPeriods.isEmpty()) {
            return false;
        }
        
        if (initialPayment == null || initialPayment.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        if (finalPayment == null || finalPayment.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        if (graduationRate == null || graduationRate.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        if (graduationFrequency == null || graduationFrequency <= 0) {
            return false;
        }
        
        if (totalGraduations == null || totalGraduations <= 0) {
            return false;
        }
        
        // Validate that payment periods are sequential and non-overlapping
        for (int i = 0; i < paymentPeriods.size() - 1; i++) {
            PaymentPeriod current = paymentPeriods.get(i);
            PaymentPeriod next = paymentPeriods.get(i + 1);
            
            if (current.getEndMonth() >= next.getStartMonth()) {
                return false;
            }
        }
        
        return true;
    }
}