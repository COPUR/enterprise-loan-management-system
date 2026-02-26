package com.loanmanagement.loan.domain.service;

import com.loanmanagement.loan.domain.model.*;
import com.loanmanagement.shared.domain.Money;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loan Business Rules Service
 * Implements loan eligibility checks, validation rules, and business constraints
 */
@Slf4j
@Service
public class LoanBusinessRulesService {

    // Credit score requirements by loan type
    private static final Map<LoanPurpose, Integer> MINIMUM_CREDIT_SCORES = Map.of(
            LoanPurpose.HOME, 620,
            LoanPurpose.AUTO, 580,
            LoanPurpose.PERSONAL, 600,
            LoanPurpose.BUSINESS, 650,
            LoanPurpose.EDUCATION, 550,
            LoanPurpose.DEBT_CONSOLIDATION, 580
    );

    // LTV limits by loan type
    private static final Map<LoanPurpose, BigDecimal> LTV_LIMITS = Map.of(
            LoanPurpose.HOME, new BigDecimal("0.80"), // 80%
            LoanPurpose.AUTO, new BigDecimal("0.90"), // 90%
            LoanPurpose.PERSONAL, new BigDecimal("1.00"), // 100% (unsecured)
            LoanPurpose.BUSINESS, new BigDecimal("0.75"), // 75%
            LoanPurpose.EDUCATION, new BigDecimal("1.00"), // 100% (unsecured)
            LoanPurpose.DEBT_CONSOLIDATION, new BigDecimal("0.85") // 85%
    );

    // DTI limits by loan type
    private static final Map<LoanPurpose, BigDecimal> DTI_LIMITS = Map.of(
            LoanPurpose.HOME, new BigDecimal("0.43"), // 43%
            LoanPurpose.AUTO, new BigDecimal("0.40"), // 40%
            LoanPurpose.PERSONAL, new BigDecimal("0.36"), // 36%
            LoanPurpose.BUSINESS, new BigDecimal("0.50"), // 50%
            LoanPurpose.EDUCATION, new BigDecimal("0.45"), // 45%
            LoanPurpose.DEBT_CONSOLIDATION, new BigDecimal("0.45") // 45%
    );

    // Minimum loan amounts by purpose
    private static final Map<LoanPurpose, Money> MINIMUM_AMOUNTS = Map.of(
            LoanPurpose.HOME, Money.of("USD", new BigDecimal("50000")),
            LoanPurpose.AUTO, Money.of("USD", new BigDecimal("5000")),
            LoanPurpose.PERSONAL, Money.of("USD", new BigDecimal("1000")),
            LoanPurpose.BUSINESS, Money.of("USD", new BigDecimal("10000")),
            LoanPurpose.EDUCATION, Money.of("USD", new BigDecimal("1000")),
            LoanPurpose.DEBT_CONSOLIDATION, Money.of("USD", new BigDecimal("2000"))
    );

    // Maximum loan amounts by purpose
    private static final Map<LoanPurpose, Money> MAXIMUM_AMOUNTS = Map.of(
            LoanPurpose.HOME, Money.of("USD", new BigDecimal("2000000")),
            LoanPurpose.AUTO, Money.of("USD", new BigDecimal("150000")),
            LoanPurpose.PERSONAL, Money.of("USD", new BigDecimal("100000")),
            LoanPurpose.BUSINESS, Money.of("USD", new BigDecimal("1000000")),
            LoanPurpose.EDUCATION, Money.of("USD", new BigDecimal("200000")),
            LoanPurpose.DEBT_CONSOLIDATION, Money.of("USD", new BigDecimal("200000"))
    );

    /**
     * Check loan eligibility based on customer profile and application
     */
    public LoanEligibilityResult checkLoanEligibility(CustomerProfile customer, LoanApplication application) {
        log.info("Checking loan eligibility for customer: {} and application: {}", 
                customer.getCustomerId(), application.getRequestedAmount());
        
        List<BusinessRuleViolation> violations = new ArrayList<>();
        
        // Check credit score
        CreditScoreCheck creditScoreCheck = checkCreditScore(customer, application, violations);
        
        // Check debt-to-income ratio
        DebtToIncomeCheck dtiCheck = checkDebtToIncomeRatio(customer, application, violations);
        
        // Check loan-to-value ratio
        LoanToValueCheck ltvCheck = checkLoanToValueRatio(application, violations);
        
        // Check employment requirements
        EmploymentCheck employmentCheck = checkEmploymentRequirements(customer, violations);
        
        // Check banking history
        BankingHistoryCheck bankingHistoryCheck = checkBankingHistory(customer, violations);
        
        // Check loan amount limits
        checkLoanAmountLimits(application, violations);
        
        // Check age and residency requirements
        checkAgeAndResidencyRequirements(customer, violations);
        
        // Generate additional requirements
        List<String> additionalRequirements = generateAdditionalRequirements(customer, application);
        
        boolean isEligible = violations.isEmpty() || 
                violations.stream().allMatch(v -> v.getSeverity() == ViolationSeverity.WARNING);
        
        return LoanEligibilityResult.builder()
                .eligible(isEligible)
                .creditScoreCheck(creditScoreCheck)
                .debtToIncomeCheck(dtiCheck)
                .loanToValueCheck(ltvCheck)
                .employmentCheck(employmentCheck)
                .bankingHistoryCheck(bankingHistoryCheck)
                .violations(violations)
                .additionalRequirements(additionalRequirements)
                .build();
    }

    /**
     * Calculate projected debt-to-income ratio including new loan payment
     */
    public BigDecimal calculateProjectedDTI(CustomerProfile customer, Money monthlyPayment) {
        Money totalMonthlyDebt = customer.getMonthlyDebtObligations().add(monthlyPayment);
        return totalMonthlyDebt.getAmount().divide(customer.getMonthlyIncome().getAmount(), 4, RoundingMode.HALF_UP);
    }

    /**
     * Get LTV limit for specific loan purpose
     */
    public BigDecimal getLTVLimit(LoanPurpose purpose) {
        return LTV_LIMITS.getOrDefault(purpose, new BigDecimal("0.80"));
    }

    /**
     * Calculate maximum loan amount based on customer profile
     */
    public Money calculateMaximumLoanAmount(CustomerProfile customer) {
        // Base calculation on income and credit score
        BigDecimal incomeMultiplier = determineIncomeMultiplier(customer.getCreditScore());
        Money incomeBasedLimit = Money.of(customer.getMonthlyIncome().getCurrency(),
                customer.getMonthlyIncome().getAmount().multiply(incomeMultiplier));
        
        // Apply additional factors
        if (customer.getBankingHistory() > 36) {
            incomeBasedLimit = incomeBasedLimit.multiply(new BigDecimal("1.1")); // 10% bonus
        }
        
        if (customer.getEmploymentType() == EmploymentType.FULL_TIME && 
            customer.getEmploymentDuration() > 24) {
            incomeBasedLimit = incomeBasedLimit.multiply(new BigDecimal("1.05")); // 5% bonus
        }
        
        return incomeBasedLimit;
    }

    /**
     * Calculate overall risk score
     */
    public RiskScore calculateRiskScore(CustomerProfile customer, LoanApplication application) {
        List<RiskFactor> factors = new ArrayList<>();
        double totalScore = 0.0;
        
        // Credit score factor (40% weight)
        double creditScoreFactor = calculateCreditScoreFactor(customer.getCreditScore());
        totalScore += creditScoreFactor * 0.4;
        factors.add(RiskFactor.builder()
                .factor("CREDIT_SCORE")
                .value(customer.getCreditScore())
                .weight(0.4)
                .contribution(creditScoreFactor * 0.4)
                .build());
        
        // DTI factor (25% weight)
        Money monthlyPayment = calculateEstimatedMonthlyPayment(application);
        BigDecimal projectedDTI = calculateProjectedDTI(customer, monthlyPayment);
        double dtiFactor = calculateDTIFactor(projectedDTI);
        totalScore += dtiFactor * 0.25;
        factors.add(RiskFactor.builder()
                .factor("DEBT_TO_INCOME")
                .value(projectedDTI.doubleValue())
                .weight(0.25)
                .contribution(dtiFactor * 0.25)
                .build());
        
        // Employment factor (20% weight)
        double employmentFactor = calculateEmploymentFactor(customer);
        totalScore += employmentFactor * 0.20;
        factors.add(RiskFactor.builder()
                .factor("EMPLOYMENT_STABILITY")
                .value(customer.getEmploymentDuration())
                .weight(0.20)
                .contribution(employmentFactor * 0.20)
                .build());
        
        // Banking history factor (10% weight)
        double bankingFactor = calculateBankingHistoryFactor(customer.getBankingHistory());
        totalScore += bankingFactor * 0.10;
        factors.add(RiskFactor.builder()
                .factor("BANKING_HISTORY")
                .value(customer.getBankingHistory())
                .weight(0.10)
                .contribution(bankingFactor * 0.10)
                .build());
        
        // LTV factor (5% weight)
        double ltvFactor = calculateLTVFactor(application);
        totalScore += ltvFactor * 0.05;
        factors.add(RiskFactor.builder()
                .factor("LOAN_TO_VALUE")
                .value(calculateLTV(application).doubleValue())
                .weight(0.05)
                .contribution(ltvFactor * 0.05)
                .build());
        
        // Convert to 0-1000 scale
        int riskScore = (int) (totalScore * 1000);
        String riskCategory = determineRiskCategory(riskScore);
        
        return RiskScore.builder()
                .score(riskScore)
                .riskCategory(riskCategory)
                .factors(factors)
                .build();
    }

    // Private helper methods
    
    private CreditScoreCheck checkCreditScore(CustomerProfile customer, LoanApplication application, 
                                            List<BusinessRuleViolation> violations) {
        int requiredScore = MINIMUM_CREDIT_SCORES.get(application.getLoanPurpose());
        boolean passed = customer.getCreditScore() >= requiredScore;
        
        if (!passed) {
            violations.add(BusinessRuleViolation.builder()
                    .ruleType(BusinessRuleType.CREDIT_SCORE_MINIMUM)
                    .description(String.format("Credit score %d is below minimum required %d for %s loans",
                            customer.getCreditScore(), requiredScore, application.getLoanPurpose()))
                    .severity(ViolationSeverity.ERROR)
                    .actualValue(customer.getCreditScore())
                    .requiredValue(requiredScore)
                    .build());
        }
        
        return CreditScoreCheck.builder()
                .passed(passed)
                .actualScore(customer.getCreditScore())
                .requiredScore(requiredScore)
                .build();
    }
    
    private DebtToIncomeCheck checkDebtToIncomeRatio(CustomerProfile customer, LoanApplication application,
                                                   List<BusinessRuleViolation> violations) {
        Money estimatedPayment = calculateEstimatedMonthlyPayment(application);
        BigDecimal projectedDTI = calculateProjectedDTI(customer, estimatedPayment);
        BigDecimal maxDTI = DTI_LIMITS.get(application.getLoanPurpose());
        
        boolean passed = projectedDTI.compareTo(maxDTI) <= 0;
        
        if (!passed) {
            violations.add(BusinessRuleViolation.builder()
                    .ruleType(BusinessRuleType.DEBT_TO_INCOME_RATIO)
                    .description(String.format("Projected DTI ratio %.2f%% exceeds maximum %.2f%% for %s loans",
                            projectedDTI.multiply(new BigDecimal("100")), 
                            maxDTI.multiply(new BigDecimal("100")), 
                            application.getLoanPurpose()))
                    .severity(ViolationSeverity.ERROR)
                    .actualValue(projectedDTI.doubleValue())
                    .requiredValue(maxDTI.doubleValue())
                    .build());
        }
        
        return DebtToIncomeCheck.builder()
                .passed(passed)
                .currentDTI(customer.getMonthlyDebtObligations().getAmount()
                        .divide(customer.getMonthlyIncome().getAmount(), 4, RoundingMode.HALF_UP))
                .projectedDTI(projectedDTI)
                .maxAllowedDTI(maxDTI)
                .build();
    }
    
    private LoanToValueCheck checkLoanToValueRatio(LoanApplication application, 
                                                  List<BusinessRuleViolation> violations) {
        if (application.getCollateralValue() == null || 
            application.getCollateralValue().getAmount().compareTo(BigDecimal.ZERO) == 0) {
            // Unsecured loan - no LTV check needed
            return LoanToValueCheck.builder()
                    .passed(true)
                    .ltvRatio(BigDecimal.ZERO)
                    .maxAllowedLTV(new BigDecimal("1.00"))
                    .unsecuredLoan(true)
                    .build();
        }
        
        BigDecimal ltvRatio = calculateLTV(application);
        BigDecimal maxLTV = getLTVLimit(application.getLoanPurpose());
        boolean passed = ltvRatio.compareTo(maxLTV) <= 0;
        
        if (!passed) {
            violations.add(BusinessRuleViolation.builder()
                    .ruleType(BusinessRuleType.LOAN_TO_VALUE_RATIO)
                    .description(String.format("LTV ratio %.2f%% exceeds maximum %.2f%% for %s loans",
                            ltvRatio.multiply(new BigDecimal("100")),
                            maxLTV.multiply(new BigDecimal("100")),
                            application.getLoanPurpose()))
                    .severity(ViolationSeverity.ERROR)
                    .actualValue(ltvRatio.doubleValue())
                    .requiredValue(maxLTV.doubleValue())
                    .build());
        }
        
        return LoanToValueCheck.builder()
                .passed(passed)
                .ltvRatio(ltvRatio)
                .maxAllowedLTV(maxLTV)
                .unsecuredLoan(false)
                .build();
    }
    
    private EmploymentCheck checkEmploymentRequirements(CustomerProfile customer, 
                                                      List<BusinessRuleViolation> violations) {
        boolean passed = true;
        List<String> issues = new ArrayList<>();
        
        // Check employment status
        if (customer.getEmploymentType() == EmploymentType.UNEMPLOYED) {
            passed = false;
            issues.add("Customer is unemployed");
            violations.add(BusinessRuleViolation.builder()
                    .ruleType(BusinessRuleType.EMPLOYMENT_STABILITY)
                    .description("Unemployed customers are not eligible for loans")
                    .severity(ViolationSeverity.ERROR)
                    .build());
        }
        
        // Check employment duration
        int requiredDuration = customer.getEmploymentType() == EmploymentType.SELF_EMPLOYED ? 24 : 12;
        if (customer.getEmploymentDuration() < requiredDuration) {
            passed = false;
            issues.add(String.format("Employment duration %d months is below required %d months",
                    customer.getEmploymentDuration(), requiredDuration));
            violations.add(BusinessRuleViolation.builder()
                    .ruleType(BusinessRuleType.EMPLOYMENT_STABILITY)
                    .description(String.format("Employment duration must be at least %d months for %s",
                            requiredDuration, customer.getEmploymentType()))
                    .severity(ViolationSeverity.ERROR)
                    .actualValue(customer.getEmploymentDuration())
                    .requiredValue(requiredDuration)
                    .build());
        }
        
        return EmploymentCheck.builder()
                .passed(passed)
                .employmentType(customer.getEmploymentType())
                .employmentDuration(customer.getEmploymentDuration())
                .issues(issues)
                .build();
    }
    
    private BankingHistoryCheck checkBankingHistory(CustomerProfile customer, 
                                                  List<BusinessRuleViolation> violations) {
        boolean passed = customer.getBankingHistory() >= 12; // Minimum 1 year
        
        if (!passed) {
            violations.add(BusinessRuleViolation.builder()
                    .ruleType(BusinessRuleType.BANKING_HISTORY)
                    .description(String.format("Banking history %d months is below minimum 12 months",
                            customer.getBankingHistory()))
                    .severity(ViolationSeverity.WARNING) // Warning, not error
                    .actualValue(customer.getBankingHistory())
                    .requiredValue(12)
                    .build());
        }
        
        return BankingHistoryCheck.builder()
                .passed(passed)
                .bankingHistoryMonths(customer.getBankingHistory())
                .minimumRequired(12)
                .build();
    }
    
    private void checkLoanAmountLimits(LoanApplication application, List<BusinessRuleViolation> violations) {
        Money minAmount = MINIMUM_AMOUNTS.get(application.getLoanPurpose());
        Money maxAmount = MAXIMUM_AMOUNTS.get(application.getLoanPurpose());
        
        if (application.getRequestedAmount().getAmount().compareTo(minAmount.getAmount()) < 0) {
            violations.add(BusinessRuleViolation.builder()
                    .ruleType(BusinessRuleType.MINIMUM_LOAN_AMOUNT)
                    .description(String.format("Requested amount %s is below minimum %s for %s loans",
                            application.getRequestedAmount(), minAmount, application.getLoanPurpose()))
                    .severity(ViolationSeverity.ERROR)
                    .build());
        }
        
        if (application.getRequestedAmount().getAmount().compareTo(maxAmount.getAmount()) > 0) {
            violations.add(BusinessRuleViolation.builder()
                    .ruleType(BusinessRuleType.MAXIMUM_LOAN_AMOUNT)
                    .description(String.format("Requested amount %s exceeds maximum %s for %s loans",
                            application.getRequestedAmount(), maxAmount, application.getLoanPurpose()))
                    .severity(ViolationSeverity.ERROR)
                    .build());
        }
    }
    
    private void checkAgeAndResidencyRequirements(CustomerProfile customer, 
                                                 List<BusinessRuleViolation> violations) {
        // Check minimum age
        if (customer.getAge() < 18) {
            violations.add(BusinessRuleViolation.builder()
                    .ruleType(BusinessRuleType.MINIMUM_AGE)
                    .description("Customer must be at least 18 years old")
                    .severity(ViolationSeverity.ERROR)
                    .actualValue(customer.getAge())
                    .requiredValue(18)
                    .build());
        }
        
        // Check residency status
        if (customer.getResidencyStatus() == ResidencyStatus.TOURIST) {
            violations.add(BusinessRuleViolation.builder()
                    .ruleType(BusinessRuleType.RESIDENCY_REQUIREMENT)
                    .description("Tourists are not eligible for loans")
                    .severity(ViolationSeverity.ERROR)
                    .build());
        }
    }
    
    private List<String> generateAdditionalRequirements(CustomerProfile customer, LoanApplication application) {
        List<String> requirements = new ArrayList<>();
        
        if (customer.getBankingHistory() < 12) {
            requirements.add("Additional income verification required for new customers");
        }
        
        if (customer.getEmploymentType() == EmploymentType.SELF_EMPLOYED) {
            requirements.add("Tax returns for the last 2 years required");
            requirements.add("Bank statements for the last 6 months required");
        }
        
        if (customer.getResidencyStatus() == ResidencyStatus.PERMANENT_RESIDENT) {
            requirements.add("Green card or permanent resident documentation required");
        }
        
        if (application.getRequestedAmount().getAmount().compareTo(new BigDecimal("100000")) > 0) {
            requirements.add("Additional financial documentation required for large loans");
        }
        
        return requirements;
    }
    
    private Money calculateEstimatedMonthlyPayment(LoanApplication application) {
        // Simple estimation using standard terms
        BigDecimal estimatedRate = new BigDecimal("0.06"); // 6% estimated rate
        int estimatedTerm = 60; // 5 years
        
        BigDecimal monthlyRate = estimatedRate.divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);
        BigDecimal principal = application.getRequestedAmount().getAmount();
        
        // Simple payment calculation
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRPowN = onePlusR.pow(estimatedTerm);
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(onePlusRPowN);
        BigDecimal denominator = onePlusRPowN.subtract(BigDecimal.ONE);
        
        BigDecimal payment = numerator.divide(denominator, 2, RoundingMode.HALF_UP);
        return Money.of(application.getRequestedAmount().getCurrency(), payment);
    }
    
    private BigDecimal calculateLTV(LoanApplication application) {
        if (application.getCollateralValue() == null || 
            application.getCollateralValue().getAmount().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return application.getRequestedAmount().getAmount()
                .divide(application.getCollateralValue().getAmount(), 4, RoundingMode.HALF_UP);
    }
    
    private BigDecimal determineIncomeMultiplier(int creditScore) {
        if (creditScore >= 800) return new BigDecimal("60"); // 5x annual income
        if (creditScore >= 750) return new BigDecimal("48"); // 4x annual income
        if (creditScore >= 700) return new BigDecimal("36"); // 3x annual income
        if (creditScore >= 650) return new BigDecimal("24"); // 2x annual income
        return new BigDecimal("12"); // 1x annual income
    }
    
    private double calculateCreditScoreFactor(int creditScore) {
        // Convert credit score to 0-1 scale (inverted - lower score = higher risk)
        return Math.max(0, (850 - creditScore) / 550.0);
    }
    
    private double calculateDTIFactor(BigDecimal dtiRatio) {
        // Higher DTI = higher risk
        return Math.min(1.0, dtiRatio.doubleValue() * 2);
    }
    
    private double calculateEmploymentFactor(CustomerProfile customer) {
        double factor = 0.0;
        
        switch (customer.getEmploymentType()) {
            case UNEMPLOYED -> factor = 1.0;
            case PART_TIME -> factor = 0.7;
            case CONTRACT -> factor = 0.6;
            case SELF_EMPLOYED -> factor = 0.5;
            case FULL_TIME -> factor = 0.2;
        }
        
        // Adjust for employment duration
        if (customer.getEmploymentDuration() < 12) {
            factor += 0.2;
        } else if (customer.getEmploymentDuration() > 60) {
            factor -= 0.1;
        }
        
        return Math.max(0, Math.min(1.0, factor));
    }
    
    private double calculateBankingHistoryFactor(int bankingHistory) {
        // Shorter banking history = higher risk
        if (bankingHistory >= 60) return 0.0; // 5+ years = no risk
        if (bankingHistory >= 36) return 0.1; // 3+ years = low risk
        if (bankingHistory >= 24) return 0.2; // 2+ years = moderate risk
        if (bankingHistory >= 12) return 0.4; // 1+ year = higher risk
        return 0.6; // Less than 1 year = high risk
    }
    
    private double calculateLTVFactor(LoanApplication application) {
        BigDecimal ltvRatio = calculateLTV(application);
        // Higher LTV = higher risk
        return Math.min(1.0, ltvRatio.doubleValue());
    }
    
    private String determineRiskCategory(int riskScore) {
        if (riskScore <= 200) return "VERY_LOW";
        if (riskScore <= 400) return "LOW";
        if (riskScore <= 600) return "MODERATE";
        if (riskScore <= 800) return "HIGH";
        return "VERY_HIGH";
    }
}