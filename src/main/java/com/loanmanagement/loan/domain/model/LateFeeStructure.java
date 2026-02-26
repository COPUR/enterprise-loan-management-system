package com.loanmanagement.loan.domain.model;

import com.loanmanagement.shared.domain.Money;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Late Fee Structure Value Object
 * Defines how late fees are calculated for missed payments
 */
@Value
@Builder(toBuilder = true)
public class LateFeeStructure {
    
    int gracePeriodDays;
    Money flatFee;
    BigDecimal percentageFee; // As percentage (e.g., 5.0 for 5%)
    Money maxFee;
    
    // Additional fee structure details
    Money minFee;
    boolean compoundDaily;
    int maxGracePeriodExtensions;
    String feeCalculationMethod;
    
    public LateFeeStructure(int gracePeriodDays, Money flatFee, BigDecimal percentageFee,
                           Money maxFee, Money minFee, boolean compoundDaily,
                           int maxGracePeriodExtensions, String feeCalculationMethod) {
        
        // Validation
        if (gracePeriodDays < 0) {
            throw new IllegalArgumentException("Grace period days cannot be negative");
        }
        
        Objects.requireNonNull(flatFee, "Flat fee cannot be null");
        Objects.requireNonNull(percentageFee, "Percentage fee cannot be null");
        Objects.requireNonNull(maxFee, "Maximum fee cannot be null");
        
        if (percentageFee.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Percentage fee cannot be negative");
        }
        
        if (percentageFee.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Percentage fee cannot exceed 100%");
        }
        
        if (flatFee.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Flat fee cannot be negative");
        }
        
        if (maxFee.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Maximum fee cannot be negative");
        }
        
        this.gracePeriodDays = gracePeriodDays;
        this.flatFee = flatFee;
        this.percentageFee = percentageFee;
        this.maxFee = maxFee;
        this.minFee = minFee != null ? minFee : Money.zero(flatFee.getCurrency());
        this.compoundDaily = compoundDaily;
        this.maxGracePeriodExtensions = Math.max(0, maxGracePeriodExtensions);
        this.feeCalculationMethod = feeCalculationMethod != null ? feeCalculationMethod : "HIGHER_OF_FLAT_OR_PERCENTAGE";
    }
    
    /**
     * Create standard late fee structure
     */
    public static LateFeeStructure standard(String currency) {
        return LateFeeStructure.builder()
                .gracePeriodDays(15)
                .flatFee(Money.of(currency, new BigDecimal("25.00")))
                .percentageFee(new BigDecimal("5.0"))
                .maxFee(Money.of(currency, new BigDecimal("100.00")))
                .compoundDaily(false)
                .build();
    }
    
    /**
     * Create percentage-only late fee structure
     */
    public static LateFeeStructure percentageOnly(String currency, BigDecimal percentage, Money maxFee) {
        return LateFeeStructure.builder()
                .gracePeriodDays(10)
                .flatFee(Money.zero(currency))
                .percentageFee(percentage)
                .maxFee(maxFee)
                .feeCalculationMethod("PERCENTAGE_ONLY")
                .build();
    }
    
    /**
     * Create flat-fee-only late fee structure
     */
    public static LateFeeStructure flatFeeOnly(String currency, Money flatFee) {
        return LateFeeStructure.builder()
                .gracePeriodDays(15)
                .flatFee(flatFee)
                .percentageFee(BigDecimal.ZERO)
                .maxFee(flatFee)
                .feeCalculationMethod("FLAT_FEE_ONLY")
                .build();
    }
    
    /**
     * Create residential mortgage late fee structure (common industry standard)
     */
    public static LateFeeStructure residentialMortgage(String currency) {
        return LateFeeStructure.builder()
                .gracePeriodDays(15)
                .flatFee(Money.zero(currency))
                .percentageFee(new BigDecimal("4.0")) // 4% of payment
                .maxFee(Money.of(currency, new BigDecimal("500.00")))
                .feeCalculationMethod("PERCENTAGE_ONLY")
                .compoundDaily(false)
                .build();
    }
    
    /**
     * Create commercial loan late fee structure
     */
    public static LateFeeStructure commercial(String currency) {
        return LateFeeStructure.builder()
                .gracePeriodDays(10)
                .flatFee(Money.of(currency, new BigDecimal("100.00")))
                .percentageFee(new BigDecimal("5.0"))
                .maxFee(Money.of(currency, new BigDecimal("1000.00")))
                .feeCalculationMethod("HIGHER_OF_FLAT_OR_PERCENTAGE")
                .compoundDaily(true)
                .build();
    }
    
    /**
     * Check if grace period applies for given days late
     */
    public boolean isWithinGracePeriod(int daysLate) {
        return daysLate <= gracePeriodDays;
    }
    
    /**
     * Calculate late fee for given payment amount and days late
     */
    public Money calculateLateFee(Money paymentAmount, int daysLate) {
        if (isWithinGracePeriod(daysLate)) {
            return Money.zero(flatFee.getCurrency());
        }
        
        Money calculatedFee = switch (feeCalculationMethod) {
            case "FLAT_FEE_ONLY" -> flatFee;
            case "PERCENTAGE_ONLY" -> calculatePercentageFee(paymentAmount);
            case "HIGHER_OF_FLAT_OR_PERCENTAGE" -> {
                Money percentageFeeAmount = calculatePercentageFee(paymentAmount);
                yield flatFee.getAmount().compareTo(percentageFeeAmount.getAmount()) > 0 
                    ? flatFee : percentageFeeAmount;
            }
            case "FLAT_PLUS_PERCENTAGE" -> flatFee.add(calculatePercentageFee(paymentAmount));
            default -> {
                Money percentageFeeAmount = calculatePercentageFee(paymentAmount);
                yield flatFee.getAmount().compareTo(percentageFeeAmount.getAmount()) > 0 
                    ? flatFee : percentageFeeAmount;
            }
        };
        
        // Apply minimum fee
        if (minFee.getAmount().compareTo(calculatedFee.getAmount()) > 0) {
            calculatedFee = minFee;
        }
        
        // Apply maximum fee cap
        if (calculatedFee.getAmount().compareTo(maxFee.getAmount()) > 0) {
            calculatedFee = maxFee;
        }
        
        // Apply daily compounding if enabled
        if (compoundDaily && daysLate > gracePeriodDays) {
            int compoundingDays = daysLate - gracePeriodDays;
            BigDecimal compoundingFactor = BigDecimal.ONE.add(new BigDecimal("0.01")) // 1% daily
                    .pow(compoundingDays);
            calculatedFee = calculatedFee.multiply(compoundingFactor);
            
            // Reapply maximum fee cap after compounding
            if (calculatedFee.getAmount().compareTo(maxFee.getAmount()) > 0) {
                calculatedFee = maxFee;
            }
        }
        
        return calculatedFee;
    }
    
    /**
     * Get effective grace period with extensions
     */
    public int getEffectiveGracePeriod(int extensionsUsed) {
        int additionalDays = Math.min(extensionsUsed, maxGracePeriodExtensions) * 5; // 5 days per extension
        return gracePeriodDays + additionalDays;
    }
    
    /**
     * Check if fee structure uses percentage calculation
     */
    public boolean usesPercentageCalculation() {
        return percentageFee.compareTo(BigDecimal.ZERO) > 0 &&
               !feeCalculationMethod.equals("FLAT_FEE_ONLY");
    }
    
    /**
     * Check if fee structure uses flat fee calculation
     */
    public boolean usesFlatFeeCalculation() {
        return flatFee.getAmount().compareTo(BigDecimal.ZERO) > 0 &&
               !feeCalculationMethod.equals("PERCENTAGE_ONLY");
    }
    
    private Money calculatePercentageFee(Money paymentAmount) {
        BigDecimal feeAmount = paymentAmount.getAmount()
                .multiply(percentageFee)
                .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        return Money.of(paymentAmount.getCurrency(), feeAmount);
    }
}