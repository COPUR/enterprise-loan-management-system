package com.bank.loan.domain;

import com.bank.shared.kernel.domain.Money;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Value Object representing the distribution of a loan payment
 * 
 * This immutable value object encapsulates how a payment is allocated
 * across different components (principal, interest, fees, etc.).
 * 
 * GRASP Principles Applied:
 * - Information Expert: Knows how to calculate payment breakdowns
 * - Low Coupling: Self-contained with minimal dependencies
 * - High Cohesion: All data related to payment distribution
 */
@Value
@Builder
public class PaymentDistribution {
    
    /**
     * Total amount of the payment
     */
    Money totalPayment;
    
    /**
     * Portion of payment applied to principal
     */
    Money principalPayment;
    
    /**
     * Portion of payment applied to interest
     */
    Money interestPayment;
    
    /**
     * Portion of payment applied to fees (if any)
     */
    @Builder.Default
    Money feePayment = Money.aed(java.math.BigDecimal.ZERO);
    
    /**
     * Loan balance before this payment
     */
    Money previousBalance;
    
    /**
     * Date when payment was processed
     */
    LocalDate paymentDate;
    
    /**
     * Additional payment details or notes
     */
    String paymentNotes;

    /**
     * Validate the payment distribution is mathematically correct
     */
    public boolean isValid() {
        if (totalPayment == null || principalPayment == null || 
            interestPayment == null || feePayment == null) {
            return false;
        }
        
        Money calculatedTotal = principalPayment.add(interestPayment).add(feePayment);
        return totalPayment.equals(calculatedTotal);
    }

    /**
     * Get the effective payment amount (excluding fees)
     */
    public Money getEffectivePayment() {
        return principalPayment.add(interestPayment);
    }

    /**
     * Check if this payment includes any interest
     */
    public boolean hasInterestComponent() {
        return interestPayment != null && interestPayment.isPositive();
    }

    /**
     * Check if this payment includes any fees
     */
    public boolean hasFeeComponent() {
        return feePayment != null && feePayment.isPositive();
    }

    /**
     * Get percentage of payment that went to principal
     */
    public java.math.BigDecimal getPrincipalPercentage() {
        if (totalPayment.isZero()) {
            return java.math.BigDecimal.ZERO;
        }
        
        return principalPayment.getAmount()
            .divide(totalPayment.getAmount(), 4, java.math.RoundingMode.HALF_UP)
            .multiply(new java.math.BigDecimal("100"));
    }

    /**
     * Get percentage of payment that went to interest
     */
    public java.math.BigDecimal getInterestPercentage() {
        if (totalPayment.isZero()) {
            return java.math.BigDecimal.ZERO;
        }
        
        return interestPayment.getAmount()
            .divide(totalPayment.getAmount(), 4, java.math.RoundingMode.HALF_UP)
            .multiply(new java.math.BigDecimal("100"));
    }

    /**
     * Create a summary string of the payment distribution
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Payment Distribution (%s):\n", paymentDate));
        summary.append(String.format("  Total Payment: %s AED\n", totalPayment.getAmount()));
        summary.append(String.format("  Principal: %s AED (%.2f%%)\n", 
                                    principalPayment.getAmount(), getPrincipalPercentage()));
        
        if (hasInterestComponent()) {
            summary.append(String.format("  Interest: %s AED (%.2f%%)\n", 
                                        interestPayment.getAmount(), getInterestPercentage()));
        }
        
        if (hasFeeComponent()) {
            summary.append(String.format("  Fees: %s AED\n", feePayment.getAmount()));
        }
        
        if (paymentNotes != null && !paymentNotes.trim().isEmpty()) {
            summary.append(String.format("  Notes: %s\n", paymentNotes));
        }
        
        return summary.toString();
    }

    /**
     * Validate all required fields are present and consistent
     */
    private void validateDistribution() {
        Objects.requireNonNull(totalPayment, "Total payment cannot be null");
        Objects.requireNonNull(principalPayment, "Principal payment cannot be null");
        Objects.requireNonNull(interestPayment, "Interest payment cannot be null");
        Objects.requireNonNull(previousBalance, "Previous balance cannot be null");
        Objects.requireNonNull(paymentDate, "Payment date cannot be null");
        
        if (!totalPayment.isPositive()) {
            throw new IllegalArgumentException("Total payment must be positive");
        }
        
        if (principalPayment.isNegative() || interestPayment.isNegative() || feePayment.isNegative()) {
            throw new IllegalArgumentException("Payment components cannot be negative");
        }
        
        if (!isValid()) {
            throw new IllegalArgumentException("Payment distribution is mathematically incorrect");
        }
    }

    /**
     * Validate the payment distribution after construction
     */
    public void validate() {
        validateDistribution();
    }
}