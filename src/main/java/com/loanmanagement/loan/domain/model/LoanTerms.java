package com.loanmanagement.loan.domain.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Loan Terms Value Object
 * Encapsulates all terms and conditions for a loan
 */
@Value
@Builder(toBuilder = true)
public class LoanTerms {
    
    int termInMonths;
    BigDecimal interestRate;
    PaymentFrequency paymentFrequency;
    
    // Optional terms
    BigDecimal originationFee;
    BigDecimal prepaymentPenalty;
    boolean allowsEarlyPayoff;
    BigDecimal latePaymentFee;
    int gracePeriodDays;
    
    public LoanTerms(int termInMonths, BigDecimal interestRate, PaymentFrequency paymentFrequency,
                     BigDecimal originationFee, BigDecimal prepaymentPenalty, boolean allowsEarlyPayoff,
                     BigDecimal latePaymentFee, int gracePeriodDays) {
        
        // Validation
        if (termInMonths <= 0) {
            throw new IllegalArgumentException("Term in months must be positive");
        }
        if (interestRate == null || interestRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Interest rate cannot be null or negative");
        }
        if (paymentFrequency == null) {
            throw new IllegalArgumentException("Payment frequency cannot be null");
        }
        if (gracePeriodDays < 0) {
            throw new IllegalArgumentException("Grace period days cannot be negative");
        }
        
        this.termInMonths = termInMonths;
        this.interestRate = interestRate;
        this.paymentFrequency = paymentFrequency;
        this.originationFee = originationFee != null ? originationFee : BigDecimal.ZERO;
        this.prepaymentPenalty = prepaymentPenalty != null ? prepaymentPenalty : BigDecimal.ZERO;
        this.allowsEarlyPayoff = allowsEarlyPayoff;
        this.latePaymentFee = latePaymentFee != null ? latePaymentFee : BigDecimal.ZERO;
        this.gracePeriodDays = gracePeriodDays;
    }
    
    /**
     * Create standard loan terms
     */
    public static LoanTerms standard(int termInMonths, BigDecimal interestRate, PaymentFrequency frequency) {
        return LoanTerms.builder()
                .termInMonths(termInMonths)
                .interestRate(interestRate)
                .paymentFrequency(frequency)
                .allowsEarlyPayoff(true)
                .gracePeriodDays(15)
                .build();
    }
    
    /**
     * Calculate total number of payments
     */
    public int getTotalPayments() {
        int paymentsPerYear = switch (paymentFrequency) {
            case WEEKLY -> 52;
            case BI_WEEKLY -> 26;
            case MONTHLY -> 12;
            case QUARTERLY -> 4;
            case SEMI_ANNUALLY -> 2;
            case ANNUALLY -> 1;
        };
        
        return (termInMonths * paymentsPerYear) / 12;
    }
    
    /**
     * Get term in years
     */
    public BigDecimal getTermInYears() {
        return new BigDecimal(termInMonths).divide(new BigDecimal("12"), 2, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Check if terms are equal (for comparison during approval)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        LoanTerms loanTerms = (LoanTerms) obj;
        return termInMonths == loanTerms.termInMonths &&
               allowsEarlyPayoff == loanTerms.allowsEarlyPayoff &&
               gracePeriodDays == loanTerms.gracePeriodDays &&
               Objects.equals(interestRate, loanTerms.interestRate) &&
               paymentFrequency == loanTerms.paymentFrequency &&
               Objects.equals(originationFee, loanTerms.originationFee) &&
               Objects.equals(prepaymentPenalty, loanTerms.prepaymentPenalty) &&
               Objects.equals(latePaymentFee, loanTerms.latePaymentFee);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(termInMonths, interestRate, paymentFrequency, originationFee,
                prepaymentPenalty, allowsEarlyPayoff, latePaymentFee, gracePeriodDays);
    }
}