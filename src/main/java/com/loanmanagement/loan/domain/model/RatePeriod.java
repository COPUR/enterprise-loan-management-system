package com.loanmanagement.loan.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.With;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Rate period for variable rate loans defining rate for a specific time period.
 */
@Data
@Builder
@With
public class RatePeriod {
    
    @NotNull
    private final String periodId;
    
    @NotNull
    private final LoanId loanId;
    
    @NotNull
    private final LocalDate startDate;
    
    @NotNull
    private final LocalDate endDate;
    
    @NotNull
    private final BigDecimal rate;
    
    @NotNull
    private final String rateType;
    
    @NotNull
    private final BigDecimal indexValue;
    
    @NotNull
    private final BigDecimal margin;
    
    private final BigDecimal fullyIndexedRate;
    
    private final BigDecimal appliedRate;
    
    private final String rateSource;
    
    private final LocalDate rateSourceDate;
    
    private final boolean isTeaser;
    
    private final boolean isPromotional;
    
    private final Integer periodNumber;
    
    private final Integer totalPeriods;
    
    private final String adjustmentFrequency;
    
    private final LocalDate nextAdjustmentDate;
    
    private final BigDecimal rateCap;
    
    private final BigDecimal rateFloor;
    
    private final String notes;
    
    /**
     * Calculates the duration of this rate period in days.
     */
    public Long calculatePeriodDays() {
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }
    
    /**
     * Calculates the duration of this rate period in months.
     */
    public Long calculatePeriodMonths() {
        return java.time.temporal.ChronoUnit.MONTHS.between(startDate, endDate);
    }
    
    /**
     * Checks if a given date falls within this rate period.
     */
    public boolean containsDate(LocalDate date) {
        return date != null && 
               !date.isBefore(startDate) && 
               !date.isAfter(endDate);
    }
    
    /**
     * Checks if this rate period is currently active.
     */
    public boolean isCurrentlyActive() {
        LocalDate currentDate = LocalDate.now();
        return containsDate(currentDate);
    }
    
    /**
     * Checks if this rate period is in the future.
     */
    public boolean isFuturePeriod() {
        LocalDate currentDate = LocalDate.now();
        return startDate.isAfter(currentDate);
    }
    
    /**
     * Checks if this rate period is in the past.
     */
    public boolean isPastPeriod() {
        LocalDate currentDate = LocalDate.now();
        return endDate.isBefore(currentDate);
    }
    
    /**
     * Calculates the effective rate considering caps and floors.
     */
    public BigDecimal calculateEffectiveRate() {
        BigDecimal effectiveRate = rate;
        
        // Apply rate cap if specified
        if (rateCap != null && effectiveRate.compareTo(rateCap) > 0) {
            effectiveRate = rateCap;
        }
        
        // Apply rate floor if specified
        if (rateFloor != null && effectiveRate.compareTo(rateFloor) < 0) {
            effectiveRate = rateFloor;
        }
        
        return effectiveRate;
    }
    
    /**
     * Calculates the difference between fully indexed rate and applied rate.
     */
    public BigDecimal calculateRateAdjustmentAmount() {
        if (fullyIndexedRate == null || appliedRate == null) {
            return BigDecimal.ZERO;
        }
        
        return fullyIndexedRate.subtract(appliedRate);
    }
    
    /**
     * Checks if rate caps or floors were applied.
     */
    public boolean isRateLimitApplied() {
        return calculateRateAdjustmentAmount().compareTo(BigDecimal.ZERO) != 0;
    }
    
    /**
     * Gets the remaining days in this rate period.
     */
    public Long getRemainingDays() {
        LocalDate currentDate = LocalDate.now();
        
        if (currentDate.isAfter(endDate)) {
            return 0L;
        }
        
        if (currentDate.isBefore(startDate)) {
            return calculatePeriodDays();
        }
        
        return java.time.temporal.ChronoUnit.DAYS.between(currentDate, endDate);
    }
    
    /**
     * Gets the elapsed days in this rate period.
     */
    public Long getElapsedDays() {
        LocalDate currentDate = LocalDate.now();
        
        if (currentDate.isBefore(startDate)) {
            return 0L;
        }
        
        if (currentDate.isAfter(endDate)) {
            return calculatePeriodDays();
        }
        
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, currentDate);
    }
    
    /**
     * Calculates the progress through this rate period as a percentage.
     */
    public BigDecimal calculatePeriodProgress() {
        Long totalDays = calculatePeriodDays();
        Long elapsedDays = getElapsedDays();
        
        if (totalDays == 0) {
            return BigDecimal.ZERO;
        }
        
        return BigDecimal.valueOf(elapsedDays)
                .divide(BigDecimal.valueOf(totalDays), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Validates the rate period.
     */
    public boolean isValid() {
        if (periodId == null || loanId == null) {
            return false;
        }
        
        if (startDate == null || endDate == null) {
            return false;
        }
        
        if (endDate.isBefore(startDate)) {
            return false;
        }
        
        if (rate == null || rate.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        
        if (rateCap != null && rate.compareTo(rateCap) > 0) {
            return false;
        }
        
        if (rateFloor != null && rate.compareTo(rateFloor) < 0) {
            return false;
        }
        
        return true;
    }
}