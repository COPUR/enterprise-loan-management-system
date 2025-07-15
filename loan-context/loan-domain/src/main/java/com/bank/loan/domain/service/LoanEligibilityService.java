package com.bank.loan.domain.service;

import com.bank.shared.kernel.domain.Money;
import com.bank.loan.domain.Customer;
import com.bank.loan.domain.LoanEligibilityResult;
import com.bank.loan.domain.LoanEligibilityChecks;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

/**
 * Domain Service for Loan Eligibility Assessment
 * 
 * Implements comprehensive loan eligibility logic following SOLID principles:
 * - Single Responsibility: Only handles eligibility assessment
 * - Open/Closed: Extensible for new eligibility criteria
 * - Dependency Inversion: Depends on abstractions, not concrete classes
 * 
 * This service encapsulates complex business rules for determining
 * if a customer is eligible for a specific loan amount.
 * 
 * GRASP Principles Applied:
 * - Information Expert: Customer data expertise centralized
 * - Low Coupling: Independent of infrastructure concerns
 * - High Cohesion: All methods focused on eligibility assessment
 */
public class LoanEligibilityService {

    // Business rule constants
    private static final int MIN_CREDIT_SCORE = 600;
    private static final int MIN_AGE = 18;
    private static final int MAX_AGE = 70;
    private static final BigDecimal MAX_DEBT_TO_INCOME_RATIO = new BigDecimal("0.40");
    private static final BigDecimal INCOME_MULTIPLIER_REQUIREMENT = new BigDecimal("3");
    private static final int MIN_EMPLOYMENT_MONTHS = 24;
    private static final int MAX_ACTIVE_LOANS = 3;
    private static final BigDecimal MIN_MONTHLY_INCOME = new BigDecimal("5000");
    private static final BigDecimal DEFAULT_ANNUAL_INTEREST_RATE = new BigDecimal("0.06");
    private static final int DEFAULT_LOAN_TERM_MONTHS = 60;

    /**
     * Comprehensive loan eligibility assessment for specific amount
     * 
     * @param customer The customer requesting the loan
     * @param requestedAmount The loan amount requested
     * @return LoanEligibilityResult containing decision and reasoning
     */
    public LoanEligibilityResult assessEligibility(Customer customer, Money requestedAmount) {
        if (customer == null) {
            return LoanEligibilityResult.rejected("Customer cannot be null");
        }
        
        if (requestedAmount == null || requestedAmount.isZero() || requestedAmount.isNegative()) {
            return LoanEligibilityResult.rejected("Requested amount must be positive");
        }

        LoanEligibilityChecks checks = new LoanEligibilityChecks();

        // Execute all eligibility checks
        checkBasicEligibility(customer, checks);
        checkCreditScore(customer, checks);
        checkAgeRequirements(customer, checks);
        checkIncomeRequirements(customer, requestedAmount, checks);
        checkDebtToIncomeRatio(customer, requestedAmount, checks);
        checkMaximumLoanAmount(customer, requestedAmount, checks);
        checkEmploymentStability(customer, checks);
        checkExistingLoanLimit(customer, checks);

        return checks.buildResult();
    }

    /**
     * Calculate maximum loan amount a customer is eligible for
     * 
     * @param customer The customer to assess
     * @return Maximum loan amount based on customer profile
     */
    public Money calculateMaximumLoanAmount(Customer customer) {
        if (customer == null) {
            return Money.aed(BigDecimal.ZERO);
        }

        // Base assessment without specific amount
        LoanEligibilityResult basicEligibility = assessBasicEligibility(customer);
        if (!basicEligibility.isApproved()) {
            return Money.aed(BigDecimal.ZERO);
        }

        return calculateMaximumAmountBasedOnProfile(customer);
    }

    /**
     * Quick pre-qualification check for customer
     * 
     * @param customer The customer to pre-qualify
     * @return True if customer meets basic eligibility criteria
     */
    public boolean isPreQualified(Customer customer) {
        if (customer == null) {
            return false;
        }

        LoanEligibilityChecks checks = new LoanEligibilityChecks();
        checkBasicEligibility(customer, checks);
        checkCreditScore(customer, checks);
        checkAgeRequirements(customer, checks);
        checkEmploymentStability(customer, checks);

        return checks.getFailedChecks().isEmpty();
    }

    // Private helper methods implementing specific business rules

    private void checkBasicEligibility(Customer customer, LoanEligibilityChecks checks) {
        if (!customer.isActive()) {
            checks.addFailedCheck("Customer account is not active");
        }
    }

    private void checkCreditScore(Customer customer, LoanEligibilityChecks checks) {
        Integer creditScore = customer.getCreditScore();
        if (creditScore == null || creditScore < MIN_CREDIT_SCORE) {
            checks.addFailedCheck(String.format(
                "Credit score (%s) is below minimum requirement (%d)", 
                creditScore, MIN_CREDIT_SCORE
            ));
        } else {
            checks.addPassedCheck("Credit score meets minimum requirement");
        }
    }

    private void checkAgeRequirements(Customer customer, LoanEligibilityChecks checks) {
        int age = calculateAge(customer.getDateOfBirth());
        
        if (age < MIN_AGE) {
            checks.addFailedCheck(String.format("Customer age (%d) is below minimum (%d)", age, MIN_AGE));
        } else if (age > MAX_AGE) {
            checks.addFailedCheck(String.format("Customer age (%d) exceeds maximum (%d)", age, MAX_AGE));
        } else {
            checks.addPassedCheck("Age requirements met");
        }
    }

    private void checkIncomeRequirements(Customer customer, Money requestedAmount, LoanEligibilityChecks checks) {
        BigDecimal monthlyIncome = customer.getMonthlyIncome();
        BigDecimal requiredIncome = calculateMinimumIncomeRequirement(requestedAmount.getAmount());
        
        if (monthlyIncome == null || monthlyIncome.compareTo(requiredIncome) < 0) {
            checks.addFailedCheck(String.format(
                "Monthly income (%s AED) is below required (%s AED) for requested amount", 
                monthlyIncome, requiredIncome
            ));
        } else {
            checks.addPassedCheck("Income requirement met");
        }
    }

    private void checkDebtToIncomeRatio(Customer customer, Money requestedAmount, LoanEligibilityChecks checks) {
        BigDecimal monthlyIncome = customer.getMonthlyIncome();
        BigDecimal currentObligations = customer.getExistingMonthlyObligations();
        BigDecimal newLoanPayment = calculateMonthlyLoanPayment(requestedAmount.getAmount());
        
        BigDecimal totalObligations = currentObligations.add(newLoanPayment);
        BigDecimal debtToIncomeRatio = totalObligations.divide(monthlyIncome, 4, java.math.RoundingMode.HALF_UP);
        
        if (debtToIncomeRatio.compareTo(MAX_DEBT_TO_INCOME_RATIO) > 0) {
            checks.addFailedCheck(String.format(
                "Debt-to-income ratio (%.2f%%) exceeds maximum allowed (%.2f%%)", 
                debtToIncomeRatio.multiply(new BigDecimal("100")), 
                MAX_DEBT_TO_INCOME_RATIO.multiply(new BigDecimal("100"))
            ));
        } else {
            checks.addPassedCheck("Debt-to-income ratio within acceptable limits");
        }
    }

    private void checkMaximumLoanAmount(Customer customer, Money requestedAmount, LoanEligibilityChecks checks) {
        Money maxAmount = calculateMaximumAmountBasedOnProfile(customer);
        
        if (requestedAmount.compareTo(maxAmount) > 0) {
            checks.addFailedCheck(String.format(
                "Requested amount (%s AED) exceeds maximum eligible amount (%s AED)", 
                requestedAmount.getAmount(), maxAmount.getAmount()
            ));
        } else {
            checks.addPassedCheck("Requested amount within eligible limits");
        }
    }

    private void checkEmploymentStability(Customer customer, LoanEligibilityChecks checks) {
        if (!hasStableEmployment(customer)) {
            checks.addFailedCheck("Employment stability requirement not met");
        } else {
            checks.addPassedCheck("Employment stability verified");
        }
    }

    private void checkExistingLoanLimit(Customer customer, LoanEligibilityChecks checks) {
        int activeLoanCount = estimateActiveLoansCount(customer);
        
        if (activeLoanCount >= MAX_ACTIVE_LOANS) {
            checks.addFailedCheck(String.format(
                "Customer has too many active loans (%d), maximum allowed is %d", 
                activeLoanCount, MAX_ACTIVE_LOANS
            ));
        } else {
            checks.addPassedCheck("Active loan count within limits");
        }
    }

    // Calculation helper methods

    private BigDecimal calculateMinimumIncomeRequirement(BigDecimal loanAmount) {
        BigDecimal monthlyPayment = calculateMonthlyLoanPayment(loanAmount);
        return monthlyPayment.multiply(INCOME_MULTIPLIER_REQUIREMENT);
    }

    private BigDecimal calculateMonthlyLoanPayment(BigDecimal loanAmount) {
        BigDecimal monthlyRate = DEFAULT_ANNUAL_INTEREST_RATE.divide(new BigDecimal("12"), 6, java.math.RoundingMode.HALF_UP);
        int numberOfPayments = DEFAULT_LOAN_TERM_MONTHS;
        
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return loanAmount.divide(new BigDecimal(numberOfPayments), 2, java.math.RoundingMode.HALF_UP);
        }
        
        // PMT formula: P * [r(1+r)^n] / [(1+r)^n - 1]
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRPowerN = onePlusR.pow(numberOfPayments);
        
        BigDecimal numerator = loanAmount.multiply(monthlyRate).multiply(onePlusRPowerN);
        BigDecimal denominator = onePlusRPowerN.subtract(BigDecimal.ONE);
        
        return numerator.divide(denominator, 2, java.math.RoundingMode.HALF_UP);
    }

    private Money calculateMaximumAmountBasedOnProfile(Customer customer) {
        BigDecimal baseAmount = calculateBaseAmountFromCreditScore(customer);
        BigDecimal availableCapacity = calculateAvailableDebtCapacity(customer);
        
        // Apply business caps
        BigDecimal maxCap = new BigDecimal("1000000"); // 1M AED maximum
        BigDecimal minCap = new BigDecimal("10000");   // 10K AED minimum
        
        BigDecimal finalAmount = baseAmount
            .min(availableCapacity)
            .min(maxCap)
            .max(minCap);
            
        return Money.aed(finalAmount);
    }

    private BigDecimal calculateBaseAmountFromCreditScore(Customer customer) {
        Integer creditScore = customer.getCreditScore();
        BigDecimal monthlyIncome = customer.getMonthlyIncome();
        
        if (creditScore == null || monthlyIncome == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal multiplier;
        if (creditScore >= 800) {
            multiplier = new BigDecimal("120"); // 10 years income
        } else if (creditScore >= 750) {
            multiplier = new BigDecimal("96");  // 8 years income
        } else if (creditScore >= 700) {
            multiplier = new BigDecimal("72");  // 6 years income
        } else if (creditScore >= 650) {
            multiplier = new BigDecimal("48");  // 4 years income
        } else {
            multiplier = new BigDecimal("24");  // 2 years income
        }
        
        return monthlyIncome.multiply(multiplier);
    }

    private BigDecimal calculateAvailableDebtCapacity(Customer customer) {
        BigDecimal monthlyIncome = customer.getMonthlyIncome();
        BigDecimal currentObligations = customer.getExistingMonthlyObligations();
        
        BigDecimal maxAllowableDebt = monthlyIncome.multiply(MAX_DEBT_TO_INCOME_RATIO);
        BigDecimal availableDebtCapacity = maxAllowableDebt.subtract(currentObligations);
        
        // Convert monthly capacity to loan amount (5-year term assumption)
        return availableDebtCapacity.multiply(new BigDecimal("60"));
    }

    private boolean hasStableEmployment(Customer customer) {
        // Simplified employment stability check
        // In production, this would integrate with employment verification services
        BigDecimal monthlyIncome = customer.getMonthlyIncome();
        return monthlyIncome != null && monthlyIncome.compareTo(MIN_MONTHLY_INCOME) >= 0;
    }

    private int estimateActiveLoansCount(Customer customer) {
        // Simplified active loan estimation
        // In production, this would query the loan repository
        BigDecimal totalObligations = customer.getExistingMonthlyObligations();
        BigDecimal avgLoanPayment = new BigDecimal("2000");
        
        return totalObligations
            .divide(avgLoanPayment, 0, java.math.RoundingMode.DOWN)
            .intValue();
    }

    private int calculateAge(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return 0;
        }
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    private LoanEligibilityResult assessBasicEligibility(Customer customer) {
        LoanEligibilityChecks checks = new LoanEligibilityChecks();
        
        checkBasicEligibility(customer, checks);
        checkCreditScore(customer, checks);
        checkAgeRequirements(customer, checks);
        checkEmploymentStability(customer, checks);
        
        return checks.buildResult();
    }
}