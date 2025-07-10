package com.loanmanagement.loan.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.With;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payment period for graduated payment plans.
 */
@Data
@Builder
@With
public class PaymentPeriod {
    
    @NotNull
    private final String periodId;
    
    @NotNull
    private final LoanId loanId;
    
    @NotNull
    @Positive
    private final Integer periodNumber;
    
    @NotNull
    @Positive
    private final Integer startMonth;
    
    @NotNull
    @Positive
    private final Integer endMonth;
    
    @NotNull
    private final LocalDate startDate;
    
    @NotNull
    private final LocalDate endDate;
    
    @NotNull
    private final BigDecimal paymentAmount;
    
    @NotNull
    private final BigDecimal interestRate;
    
    private final BigDecimal principalAmount;
    
    private final BigDecimal interestAmount;
    
    private final BigDecimal feesAmount;
    
    private final BigDecimal escrowAmount;
    
    private final String periodType;
    
    private final String periodDescription;
    
    private final boolean isGraduated;
    
    private final boolean isFixed;
    
    private final BigDecimal graduationRate;
    
    private final String graduationTrigger;
    
    private final Integer paymentCount;
    
    private final BigDecimal totalPeriodAmount;
    
    private final BigDecimal remainingBalanceStart;
    
    private final BigDecimal remainingBalanceEnd;
    
    /**
     * Calculates the duration of this payment period in months.
     */
    public Integer calculatePeriodMonths() {
        return endMonth - startMonth + 1;
    }
    
    /**
     * Calculates the duration of this payment period in days.
     */
    public Long calculatePeriodDays() {
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }
    
    /**
     * Calculates the total payments for this period.
     */
    public BigDecimal calculateTotalPeriodPayments() {
        if (paymentCount != null && paymentCount > 0) {
            return paymentAmount.multiply(BigDecimal.valueOf(paymentCount));
        }
        
        return paymentAmount.multiply(BigDecimal.valueOf(calculatePeriodMonths()));
    }
    
    /**
     * Calculates the average monthly payment for this period.
     */
    public BigDecimal calculateAverageMonthlyPayment() {
        Integer months = calculatePeriodMonths();
        if (months == 0) {
            return BigDecimal.ZERO;
        }
        
        return calculateTotalPeriodPayments().divide(BigDecimal.valueOf(months), 2, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Calculates the principal reduction for this period.
     */
    public BigDecimal calculatePrincipalReduction() {
        if (remainingBalanceStart == null || remainingBalanceEnd == null) {
            return BigDecimal.ZERO;
        }
        
        return remainingBalanceStart.subtract(remainingBalanceEnd);
    }
    
    /**
     * Calculates the interest paid for this period.
     */
    public BigDecimal calculateInterestPaid() {
        if (interestAmount != null) {
            return interestAmount.multiply(BigDecimal.valueOf(calculatePeriodMonths()));
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * Checks if a given month falls within this payment period.
     */
    public boolean containsMonth(Integer month) {
        return month != null && month >= startMonth && month <= endMonth;
    }
    
    /**
     * Checks if a given date falls within this payment period.
     */
    public boolean containsDate(LocalDate date) {
        return date != null && 
               !date.isBefore(startDate) && 
               !date.isAfter(endDate);
    }
    
    /**
     * Checks if this payment period is currently active.
     */
    public boolean isCurrentlyActive() {
        LocalDate currentDate = LocalDate.now();
        return containsDate(currentDate);
    }
    
    /**
     * Checks if this payment period is in the future.
     */
    public boolean isFuturePeriod() {
        LocalDate currentDate = LocalDate.now();
        return startDate.isAfter(currentDate);
    }
    
    /**
     * Checks if this payment period is in the past.
     */
    public boolean isPastPeriod() {
        LocalDate currentDate = LocalDate.now();
        return endDate.isBefore(currentDate);
    }
    
    /**
     * Gets the remaining months in this payment period.
     */
    public Integer getRemainingMonths() {
        LocalDate currentDate = LocalDate.now();
        
        if (currentDate.isAfter(endDate)) {
            return 0;
        }
        
        if (currentDate.isBefore(startDate)) {
            return calculatePeriodMonths();
        }
        
        // Calculate remaining months from current date to end date
        return Math.max(0, (int) java.time.temporal.ChronoUnit.MONTHS.between(currentDate, endDate));
    }
    
    /**
     * Gets the elapsed months in this payment period.
     */
    public Integer getElapsedMonths() {
        LocalDate currentDate = LocalDate.now();
        
        if (currentDate.isBefore(startDate)) {
            return 0;
        }
        
        if (currentDate.isAfter(endDate)) {
            return calculatePeriodMonths();
        }
        
        return Math.max(0, (int) java.time.temporal.ChronoUnit.MONTHS.between(startDate, currentDate));
    }
    
    /**
     * Calculates the progress through this payment period as a percentage.
     */
    public BigDecimal calculatePeriodProgress() {
        Integer totalMonths = calculatePeriodMonths();
        Integer elapsedMonths = getElapsedMonths();
        
        if (totalMonths == 0) {
            return BigDecimal.ZERO;
        }
        
        return BigDecimal.valueOf(elapsedMonths)
                .divide(BigDecimal.valueOf(totalMonths), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Validates the payment period.
     */
    public boolean isValid() {
        if (periodId == null || loanId == null || periodNumber == null) {
            return false;
        }
        
        if (startMonth == null || endMonth == null || startMonth > endMonth) {
            return false;
        }
        
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return false;
        }
        
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        
        if (interestRate == null || interestRate.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        
        return true;
    }
}