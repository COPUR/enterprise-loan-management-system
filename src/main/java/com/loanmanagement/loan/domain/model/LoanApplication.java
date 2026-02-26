package com.loanmanagement.loan.domain.model;

import com.loanmanagement.shared.domain.Money;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Loan Application Value Object
 * Contains the details of a loan application request
 */
@Value
@Builder(toBuilder = true)
public class LoanApplication {
    
    CustomerId customerId;
    Money requestedAmount;
    LoanPurpose loanPurpose;
    LoanTerms requestedTerms;
    Money collateralValue;
    Money downPayment;
    
    // Additional application details
    String applicationId;
    LocalDate applicationDate;
    String propertyAddress;
    String vehicleVin;
    String collateralDescription;
    boolean isPrimaryResidence;
    
    public LoanApplication(CustomerId customerId, Money requestedAmount, LoanPurpose loanPurpose,
                          LoanTerms requestedTerms, Money collateralValue, Money downPayment,
                          String applicationId, LocalDate applicationDate, String propertyAddress,
                          String vehicleVin, String collateralDescription, boolean isPrimaryResidence) {
        
        // Validation
        Objects.requireNonNull(customerId, "Customer ID cannot be null");
        Objects.requireNonNull(requestedAmount, "Requested amount cannot be null");
        Objects.requireNonNull(loanPurpose, "Loan purpose cannot be null");
        Objects.requireNonNull(requestedTerms, "Requested terms cannot be null");
        
        if (requestedAmount.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Requested amount must be positive");
        }
        
        if (downPayment != null && downPayment.getAmount().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Down payment cannot be negative");
        }
        
        if (collateralValue != null && collateralValue.getAmount().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Collateral value cannot be negative");
        }
        
        this.customerId = customerId;
        this.requestedAmount = requestedAmount;
        this.loanPurpose = loanPurpose;
        this.requestedTerms = requestedTerms;
        this.collateralValue = collateralValue;
        this.downPayment = downPayment != null ? downPayment : Money.of("USD", java.math.BigDecimal.ZERO);
        this.applicationId = applicationId;
        this.applicationDate = applicationDate != null ? applicationDate : LocalDate.now();
        this.propertyAddress = propertyAddress;
        this.vehicleVin = vehicleVin;
        this.collateralDescription = collateralDescription;
        this.isPrimaryResidence = isPrimaryResidence;
    }
    
    /**
     * Create a standard loan application
     */
    public static LoanApplication create(CustomerId customerId, Money requestedAmount, 
                                       LoanPurpose loanPurpose, LoanTerms requestedTerms) {
        return LoanApplication.builder()
                .customerId(customerId)
                .requestedAmount(requestedAmount)
                .loanPurpose(loanPurpose)
                .requestedTerms(requestedTerms)
                .applicationDate(LocalDate.now())
                .build();
    }
    
    /**
     * Create a secured loan application
     */
    public static LoanApplication secured(CustomerId customerId, Money requestedAmount,
                                        LoanPurpose loanPurpose, LoanTerms requestedTerms,
                                        Money collateralValue, Money downPayment) {
        return LoanApplication.builder()
                .customerId(customerId)
                .requestedAmount(requestedAmount)
                .loanPurpose(loanPurpose)
                .requestedTerms(requestedTerms)
                .collateralValue(collateralValue)
                .downPayment(downPayment)
                .applicationDate(LocalDate.now())
                .build();
    }
    
    /**
     * Check if this is a secured loan application
     */
    public boolean isSecured() {
        return collateralValue != null && 
               collateralValue.getAmount().compareTo(java.math.BigDecimal.ZERO) > 0;
    }
    
    /**
     * Check if this is an unsecured loan application
     */
    public boolean isUnsecured() {
        return !isSecured();
    }
    
    /**
     * Calculate loan-to-value ratio
     */
    public java.math.BigDecimal getLoanToValueRatio() {
        if (!isSecured()) {
            return java.math.BigDecimal.ZERO;
        }
        
        Money netLoanAmount = requestedAmount;
        if (downPayment != null) {
            netLoanAmount = requestedAmount.subtract(downPayment);
        }
        
        return netLoanAmount.getAmount()
                .divide(collateralValue.getAmount(), 4, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate down payment percentage
     */
    public java.math.BigDecimal getDownPaymentPercentage() {
        if (!isSecured() || downPayment == null) {
            return java.math.BigDecimal.ZERO;
        }
        
        Money totalValue = collateralValue != null ? collateralValue : requestedAmount;
        return downPayment.getAmount()
                .divide(totalValue.getAmount(), 4, java.math.RoundingMode.HALF_UP)
                .multiply(new java.math.BigDecimal("100"));
    }
    
    /**
     * Check if application requires collateral evaluation
     */
    public boolean requiresCollateralEvaluation() {
        return isSecured() && (loanPurpose == LoanPurpose.HOME || loanPurpose == LoanPurpose.AUTO);
    }
    
    /**
     * Get estimated monthly payment (simplified calculation)
     */
    public Money getEstimatedMonthlyPayment() {
        java.math.BigDecimal monthlyRate = requestedTerms.getInterestRate()
                .divide(new java.math.BigDecimal("1200"), 10, java.math.RoundingMode.HALF_UP);
        
        int totalPayments = requestedTerms.getTotalPayments();
        java.math.BigDecimal principal = requestedAmount.getAmount();
        
        if (monthlyRate.compareTo(java.math.BigDecimal.ZERO) == 0) {
            return Money.of(requestedAmount.getCurrency(), 
                    principal.divide(new java.math.BigDecimal(totalPayments), 2, java.math.RoundingMode.HALF_UP));
        }
        
        java.math.BigDecimal onePlusR = java.math.BigDecimal.ONE.add(monthlyRate);
        java.math.BigDecimal onePlusRPowN = onePlusR.pow(totalPayments);
        java.math.BigDecimal numerator = principal.multiply(monthlyRate).multiply(onePlusRPowN);
        java.math.BigDecimal denominator = onePlusRPowN.subtract(java.math.BigDecimal.ONE);
        
        java.math.BigDecimal payment = numerator.divide(denominator, 2, java.math.RoundingMode.HALF_UP);
        return Money.of(requestedAmount.getCurrency(), payment);
    }
}