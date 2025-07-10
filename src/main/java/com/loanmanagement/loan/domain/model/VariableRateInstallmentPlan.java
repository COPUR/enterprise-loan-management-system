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
 * Variable rate installment plan with adjustable interest rates over time.
 */
@Data
@Builder
@With
public class VariableRateInstallmentPlan {
    
    @NotNull
    private final InstallmentPlan basePlan;
    
    @NotNull
    private final BigDecimal initialRate;
    
    @NotNull
    private final String rateIndex;
    
    @NotNull
    private final BigDecimal margin;
    
    @NotNull
    private final List<RatePeriod> ratePeriods;
    
    @NotNull
    private final List<RateAdjustment> rateAdjustments;
    
    private final BigDecimal rateCap;
    
    private final BigDecimal rateFloor;
    
    private final BigDecimal periodicRateCap;
    
    private final BigDecimal lifetimeRateCap;
    
    @NotNull
    private final Integer adjustmentFrequencyMonths;
    
    @NotNull
    private final LocalDate firstAdjustmentDate;
    
    private final LocalDate nextAdjustmentDate;
    
    private final String rateAdjustmentRule;
    
    private final boolean hasTeaser;
    
    private final BigDecimal teaserRate;
    
    private final Integer teaserPeriodMonths;
    
    /**
     * Calculates the current effective rate based on the current period.
     */
    public BigDecimal getCurrentEffectiveRate() {
        LocalDate currentDate = LocalDate.now();
        
        return ratePeriods.stream()
                .filter(period -> !currentDate.isBefore(period.getStartDate()) && 
                                !currentDate.isAfter(period.getEndDate()))
                .map(RatePeriod::getRate)
                .findFirst()
                .orElse(initialRate);
    }
    
    /**
     * Calculates the next adjustment date based on frequency.
     */
    public LocalDate calculateNextAdjustmentDate() {
        if (nextAdjustmentDate != null) {
            return nextAdjustmentDate;
        }
        
        return firstAdjustmentDate.plusMonths(adjustmentFrequencyMonths);
    }
    
    /**
     * Applies rate caps and floors to a proposed rate.
     */
    public BigDecimal applyRateLimits(BigDecimal proposedRate, BigDecimal currentRate) {
        BigDecimal adjustedRate = proposedRate;
        
        // Apply periodic rate cap
        if (periodicRateCap != null) {
            BigDecimal maxIncrease = currentRate.add(periodicRateCap);
            BigDecimal maxDecrease = currentRate.subtract(periodicRateCap);
            
            if (adjustedRate.compareTo(maxIncrease) > 0) {
                adjustedRate = maxIncrease;
            } else if (adjustedRate.compareTo(maxDecrease) < 0) {
                adjustedRate = maxDecrease;
            }
        }
        
        // Apply lifetime rate cap
        if (lifetimeRateCap != null && adjustedRate.compareTo(lifetimeRateCap) > 0) {
            adjustedRate = lifetimeRateCap;
        }
        
        // Apply rate floor
        if (rateFloor != null && adjustedRate.compareTo(rateFloor) < 0) {
            adjustedRate = rateFloor;
        }
        
        return adjustedRate;
    }
    
    /**
     * Checks if a rate adjustment is due.
     */
    public boolean isAdjustmentDue() {
        LocalDate currentDate = LocalDate.now();
        LocalDate nextAdjustment = calculateNextAdjustmentDate();
        
        return !currentDate.isBefore(nextAdjustment);
    }
    
    /**
     * Calculates the payment amount for a specific period with variable rate.
     */
    public BigDecimal calculatePaymentForPeriod(LocalDate periodDate, BigDecimal remainingBalance) {
        BigDecimal effectiveRate = ratePeriods.stream()
                .filter(period -> !periodDate.isBefore(period.getStartDate()) && 
                                !periodDate.isAfter(period.getEndDate()))
                .map(RatePeriod::getRate)
                .findFirst()
                .orElse(initialRate);
        
        // Monthly rate
        BigDecimal monthlyRate = effectiveRate.divide(BigDecimal.valueOf(12), 10, java.math.RoundingMode.HALF_UP);
        
        // Remaining term calculation would depend on the specific period
        // This is a simplified calculation
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return remainingBalance.divide(BigDecimal.valueOf(basePlan.getTermInMonths()), 2, java.math.RoundingMode.HALF_UP);
        }
        
        // Standard amortization formula with variable rate
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal powered = onePlusRate.pow(basePlan.getTermInMonths());
        
        return remainingBalance.multiply(monthlyRate).multiply(powered)
                .divide(powered.subtract(BigDecimal.ONE), 2, java.math.RoundingMode.HALF_UP);
    }
}