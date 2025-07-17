package com.bank.infrastructure.financial;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Loan Calculation Service - Enterprise Banking Financial Calculations
 * 
 * Implements critical business logic discovered in archive code review:
 * - Standard loan payment calculations (M = P * [r(1+r)^n] / [(1+r)^n - 1])
 * - Interest rate determination based on credit scores
 * - Payment amount distributions (principal vs interest)
 * - Late payment penalty calculations
 * - Outstanding balance calculations
 * 
 * Business Rules from Archive Analysis:
 * - Loan Amount Limits: MIN: $1,000, MAX: $500,000
 * - Interest Rate Range: 0.05% - 0.30% (5% - 30% annually)
 * - Installment Range: 6-60 months
 * - Credit Score Based Rates: 750+ = 8%, 700+ = 12%, 650+ = 16%, <650 = 20%
 */
@Service
public class LoanCalculationService {
    
    private static final Logger logger = LoggerFactory.getLogger(LoanCalculationService.class);
    
    // Business rule constants from archive analysis
    private static final BigDecimal MIN_LOAN_AMOUNT = new BigDecimal("1000.00");
    private static final BigDecimal MAX_LOAN_AMOUNT = new BigDecimal("500000.00");
    private static final BigDecimal MIN_INTEREST_RATE = new BigDecimal("0.05");
    private static final BigDecimal MAX_INTEREST_RATE = new BigDecimal("0.30");
    private static final int MIN_INSTALLMENTS = 6;
    private static final int MAX_INSTALLMENTS = 60;
    private static final BigDecimal LATE_PENALTY_RATE = new BigDecimal("0.05"); // 5% penalty
    
    /**
     * Calculate monthly payment using standard loan payment formula
     * M = P * [r(1+r)^n] / [(1+r)^n - 1]
     * 
     * @param loanAmount the principal loan amount
     * @param annualInterestRate the annual interest rate (as decimal)
     * @param numberOfInstallments the number of monthly installments
     * @return the monthly payment amount
     */
    public BigDecimal calculateMonthlyPayment(BigDecimal loanAmount, BigDecimal annualInterestRate, int numberOfInstallments) {
        logger.debug("Calculating monthly payment for loan amount: {}, rate: {}, installments: {}", 
                     loanAmount, annualInterestRate, numberOfInstallments);
        
        validateLoanParameters(loanAmount, annualInterestRate, numberOfInstallments);
        
        // Convert annual rate to monthly rate
        BigDecimal monthlyRate = annualInterestRate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        
        // Handle zero interest rate case
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return loanAmount.divide(BigDecimal.valueOf(numberOfInstallments), 2, RoundingMode.HALF_UP);
        }
        
        // Calculate (1 + r)^n
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRatePowerN = onePlusRate.pow(numberOfInstallments);
        
        // Calculate numerator: r * (1 + r)^n
        BigDecimal numerator = monthlyRate.multiply(onePlusRatePowerN);
        
        // Calculate denominator: (1 + r)^n - 1
        BigDecimal denominator = onePlusRatePowerN.subtract(BigDecimal.ONE);
        
        // Calculate monthly payment
        BigDecimal monthlyPayment = loanAmount.multiply(numerator.divide(denominator, 10, RoundingMode.HALF_UP));
        
        logger.debug("Calculated monthly payment: {}", monthlyPayment);
        return monthlyPayment.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate personalized interest rate based on credit score
     * Business rules from archive analysis:
     * - Credit Score >= 750: 8% annual rate
     * - Credit Score >= 700: 12% annual rate
     * - Credit Score >= 650: 16% annual rate
     * - Credit Score < 650: 20% annual rate
     * 
     * @param creditScore the customer's credit score
     * @return the personalized annual interest rate
     */
    public BigDecimal calculatePersonalizedRate(int creditScore) {
        logger.debug("Calculating personalized rate for credit score: {}", creditScore);
        
        if (creditScore < 300 || creditScore > 850) {
            throw new IllegalArgumentException("Credit score must be between 300 and 850");
        }
        
        BigDecimal rate;
        if (creditScore >= 750) {
            rate = new BigDecimal("0.08"); // 8%
        } else if (creditScore >= 700) {
            rate = new BigDecimal("0.12"); // 12%
        } else if (creditScore >= 650) {
            rate = new BigDecimal("0.16"); // 16%
        } else {
            rate = new BigDecimal("0.20"); // 20%
        }
        
        logger.debug("Personalized rate for credit score {}: {}", creditScore, rate);
        return rate;
    }
    
    /**
     * Calculate payment amount distribution between principal and interest
     * 
     * @param outstandingBalance the current outstanding balance
     * @param monthlyPayment the scheduled monthly payment
     * @param monthlyInterestRate the monthly interest rate
     * @return PaymentDistribution object with principal and interest amounts
     */
    public PaymentDistribution calculatePaymentDistribution(BigDecimal outstandingBalance, 
                                                           BigDecimal monthlyPayment, 
                                                           BigDecimal monthlyInterestRate) {
        logger.debug("Calculating payment distribution for balance: {}, payment: {}, rate: {}", 
                     outstandingBalance, monthlyPayment, monthlyInterestRate);
        
        // Calculate interest portion
        BigDecimal interestAmount = outstandingBalance.multiply(monthlyInterestRate)
            .setScale(2, RoundingMode.HALF_UP);
        
        // Calculate principal portion
        BigDecimal principalAmount = monthlyPayment.subtract(interestAmount);
        
        // Ensure principal doesn't exceed outstanding balance
        if (principalAmount.compareTo(outstandingBalance) > 0) {
            principalAmount = outstandingBalance;
        }
        
        logger.debug("Payment distribution - Interest: {}, Principal: {}", interestAmount, principalAmount);
        return new PaymentDistribution(principalAmount, interestAmount);
    }
    
    /**
     * Calculate late payment penalty
     * 
     * @param scheduledAmount the original scheduled payment amount
     * @return the penalty amount (5% of scheduled amount)
     */
    public BigDecimal calculateLatePenalty(BigDecimal scheduledAmount) {
        logger.debug("Calculating late penalty for scheduled amount: {}", scheduledAmount);
        
        if (scheduledAmount == null || scheduledAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal penaltyAmount = scheduledAmount.multiply(LATE_PENALTY_RATE)
            .setScale(2, RoundingMode.HALF_UP);
        
        logger.debug("Late penalty amount: {}", penaltyAmount);
        return penaltyAmount;
    }
    
    /**
     * Calculate outstanding balance after payment
     * 
     * @param currentBalance the current outstanding balance
     * @param principalPayment the principal portion of the payment
     * @return the new outstanding balance
     */
    public BigDecimal calculateOutstandingBalance(BigDecimal currentBalance, BigDecimal principalPayment) {
        logger.debug("Calculating outstanding balance: {} - {}", currentBalance, principalPayment);
        
        BigDecimal newBalance = currentBalance.subtract(principalPayment);
        
        // Ensure balance doesn't go negative
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            newBalance = BigDecimal.ZERO;
        }
        
        logger.debug("New outstanding balance: {}", newBalance);
        return newBalance.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate total interest that will be paid over the life of the loan
     * 
     * @param monthlyPayment the monthly payment amount
     * @param numberOfInstallments the number of installments
     * @param loanAmount the original loan amount
     * @return the total interest amount
     */
    public BigDecimal calculateTotalInterest(BigDecimal monthlyPayment, int numberOfInstallments, BigDecimal loanAmount) {
        logger.debug("Calculating total interest for loan: {}, payment: {}, installments: {}", 
                     loanAmount, monthlyPayment, numberOfInstallments);
        
        BigDecimal totalPayments = monthlyPayment.multiply(BigDecimal.valueOf(numberOfInstallments));
        BigDecimal totalInterest = totalPayments.subtract(loanAmount);
        
        logger.debug("Total interest over loan life: {}", totalInterest);
        return totalInterest.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Validate loan parameters against business rules
     * 
     * @param loanAmount the loan amount to validate
     * @param annualInterestRate the interest rate to validate
     * @param numberOfInstallments the number of installments to validate
     */
    private void validateLoanParameters(BigDecimal loanAmount, BigDecimal annualInterestRate, int numberOfInstallments) {
        if (loanAmount == null || loanAmount.compareTo(MIN_LOAN_AMOUNT) < 0 || loanAmount.compareTo(MAX_LOAN_AMOUNT) > 0) {
            throw new IllegalArgumentException("Loan amount must be between $1,000 and $500,000");
        }
        
        if (annualInterestRate == null || annualInterestRate.compareTo(MIN_INTEREST_RATE) < 0 || annualInterestRate.compareTo(MAX_INTEREST_RATE) > 0) {
            throw new IllegalArgumentException("Interest rate must be between 5% and 30%");
        }
        
        if (numberOfInstallments < MIN_INSTALLMENTS || numberOfInstallments > MAX_INSTALLMENTS) {
            throw new IllegalArgumentException("Number of installments must be between 6 and 60 months");
        }
    }
    
    /**
     * Value object for payment distribution
     */
    public static class PaymentDistribution {
        private final BigDecimal principalAmount;
        private final BigDecimal interestAmount;
        
        public PaymentDistribution(BigDecimal principalAmount, BigDecimal interestAmount) {
            this.principalAmount = principalAmount;
            this.interestAmount = interestAmount;
        }
        
        public BigDecimal getPrincipalAmount() {
            return principalAmount;
        }
        
        public BigDecimal getInterestAmount() {
            return interestAmount;
        }
        
        @Override
        public String toString() {
            return String.format("PaymentDistribution{principal=%s, interest=%s}", 
                               principalAmount, interestAmount);
        }
    }
}