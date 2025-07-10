package com.loanmanagement.loan.domain.model;

import com.loanmanagement.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to validate the loan business rules model classes
 */
class LoanBusinessRulesModelTest {

    @Test
    void testLoanEligibilityResultCreation() {
        // Given
        CreditScoreCheck creditCheck = CreditScoreCheck.builder()
                .passed(true)
                .actualScore(750)
                .requiredScore(650)
                .build();

        DebtToIncomeCheck dtiCheck = DebtToIncomeCheck.builder()
                .passed(true)
                .currentDTI(new BigDecimal("0.25"))
                .projectedDTI(new BigDecimal("0.35"))
                .maxAllowedDTI(new BigDecimal("0.43"))
                .build();

        LoanToValueCheck ltvCheck = LoanToValueCheck.builder()
                .passed(true)
                .ltvRatio(new BigDecimal("0.75"))
                .maxAllowedLTV(new BigDecimal("0.80"))
                .unsecuredLoan(false)
                .build();

        EmploymentCheck employmentCheck = EmploymentCheck.builder()
                .passed(true)
                .employmentType(EmploymentType.FULL_TIME)
                .employmentDuration(36)
                .issues(List.of())
                .build();

        BankingHistoryCheck bankingCheck = BankingHistoryCheck.builder()
                .passed(true)
                .bankingHistoryMonths(48)
                .minimumRequired(12)
                .build();

        // When
        LoanEligibilityResult result = LoanEligibilityResult.builder()
                .eligible(true)
                .creditScoreCheck(creditCheck)
                .debtToIncomeCheck(dtiCheck)
                .loanToValueCheck(ltvCheck)
                .employmentCheck(employmentCheck)
                .bankingHistoryCheck(bankingCheck)
                .violations(List.of())
                .additionalRequirements(List.of())
                .build();

        // Then
        assertTrue(result.isEligible());
        assertTrue(result.allMajorChecksPassed());
        assertFalse(result.hasErrorViolations());
        assertEquals("Fully eligible", result.getEligibilityStatusMessage());
    }

    @Test
    void testBusinessRuleViolationCreation() {
        // Given
        BusinessRuleType ruleType = BusinessRuleType.CREDIT_SCORE_MINIMUM;
        String description = "Credit score too low";
        ViolationSeverity severity = ViolationSeverity.ERROR;

        // When
        BusinessRuleViolation violation = BusinessRuleViolation.builder()
                .ruleType(ruleType)
                .description(description)
                .severity(severity)
                .actualValue(550)
                .requiredValue(650)
                .build();

        // Then
        assertTrue(violation.isCritical());
        assertTrue(violation.hasComparisonValues());
        assertEquals(ruleType, violation.getRuleType());
        assertEquals(severity, violation.getSeverity());
    }

    @Test
    void testRiskScoreCalculation() {
        // Given
        List<RiskFactor> factors = List.of(
                RiskFactor.creditScore(750, 0.4, 0.1),
                RiskFactor.debtToIncome(0.35, 0.25, 0.15),
                RiskFactor.employmentStability(36, 0.2, 0.05)
        );

        // When
        RiskScore riskScore = RiskScore.builder()
                .score(300)
                .riskCategory("LOW")
                .factors(factors)
                .build();

        // Then
        assertTrue(riskScore.isLowRisk());
        assertTrue(riskScore.isAcceptableRisk());
        assertTrue(riskScore.qualifiesForPremiumRates());
        assertEquals(3, riskScore.getFactors().size());
    }

    @Test
    void testCreditScoreCheckValidation() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
                CreditScoreCheck.builder()
                        .passed(true)
                        .actualScore(250) // Below valid range
                        .requiredScore(650)
                        .build());

        assertThrows(IllegalArgumentException.class, () -> 
                CreditScoreCheck.builder()
                        .passed(true)
                        .actualScore(750)
                        .requiredScore(900) // Above valid range
                        .build());
    }

    @Test
    void testDebtToIncomeCheckCalculations() {
        // Given
        DebtToIncomeCheck dtiCheck = DebtToIncomeCheck.builder()
                .passed(false)
                .currentDTI(new BigDecimal("0.30"))
                .projectedDTI(new BigDecimal("0.45"))
                .maxAllowedDTI(new BigDecimal("0.43"))
                .build();

        // When & Then
        assertFalse(dtiCheck.isPassed());
        assertTrue(dtiCheck.isConcerningDTI());
        assertEquals(new BigDecimal("45.00"), dtiCheck.getProjectedDTIPercentage());
        assertEquals(new BigDecimal("43.00"), dtiCheck.getMaxAllowedDTIPercentage());
    }

    @Test
    void testLoanToValueCheckUnsecured() {
        // When
        LoanToValueCheck unsecuredCheck = LoanToValueCheck.unsecured();

        // Then
        assertTrue(unsecuredCheck.isPassed());
        assertTrue(unsecuredCheck.isUnsecuredLoan());
        assertEquals("Unsecured loan - No LTV applicable", unsecuredCheck.getLTVDisplay());
    }

    @Test
    void testEmploymentCheckStability() {
        // Given
        EmploymentCheck stableEmployment = EmploymentCheck.builder()
                .passed(true)
                .employmentType(EmploymentType.FULL_TIME)
                .employmentDuration(36) // 3 years
                .issues(List.of())
                .build();

        // When & Then
        assertTrue(stableEmployment.isPassed());
        assertTrue(stableEmployment.isStableEmployment());
        assertTrue(stableEmployment.qualifiesForPremiumRates());
        assertEquals(3.0, stableEmployment.getEmploymentDurationYears());
    }

    @Test
    void testBankingHistoryCheckExperience() {
        // Given
        BankingHistoryCheck experiencedCustomer = BankingHistoryCheck.builder()
                .passed(true)
                .bankingHistoryMonths(72) // 6 years
                .minimumRequired(12)
                .build();

        // When & Then
        assertTrue(experiencedCustomer.isPassed());
        assertTrue(experiencedCustomer.hasExtensiveHistory());
        assertTrue(experiencedCustomer.qualifiesForPremiumRates());
        assertEquals("Extensive", experiencedCustomer.getExperienceLevel());
    }

    @Test
    void testViolationSeverityComparison() {
        // When & Then
        assertTrue(ViolationSeverity.ERROR.isMoreSevereThan(ViolationSeverity.WARNING));
        assertTrue(ViolationSeverity.WARNING.isMoreSevereThan(ViolationSeverity.INFO));
        assertFalse(ViolationSeverity.INFO.isMoreSevereThan(ViolationSeverity.ERROR));
        
        assertTrue(ViolationSeverity.ERROR.preventsApproval());
        assertFalse(ViolationSeverity.WARNING.preventsApproval());
        assertFalse(ViolationSeverity.INFO.preventsApproval());
    }

    @Test
    void testBusinessRuleTypeClassification() {
        // When & Then
        assertTrue(BusinessRuleType.CREDIT_SCORE_MINIMUM.isFinancialAssessment());
        assertTrue(BusinessRuleType.DEBT_TO_INCOME_RATIO.isFinancialAssessment());
        assertTrue(BusinessRuleType.AML_CHECK.isComplianceRule());
        assertTrue(BusinessRuleType.FRAUD_INDICATOR.isRiskAssessment());
        assertTrue(BusinessRuleType.DOCUMENTATION_INCOMPLETE.isDocumentationRule());
        
        assertTrue(BusinessRuleType.CREDIT_SCORE_MINIMUM.isTypicallyBlocking());
        assertEquals(ViolationSeverity.ERROR, BusinessRuleType.CREDIT_SCORE_MINIMUM.getDefaultSeverity());
    }

    @Test
    void testRiskFactorContributions() {
        // Given
        RiskFactor majorFactor = RiskFactor.builder()
                .factor("CREDIT_SCORE")
                .value(550)
                .weight(0.4)
                .contribution(0.25) // 25%
                .build();

        RiskFactor minorFactor = RiskFactor.builder()
                .factor("BANKING_HISTORY")
                .value(18)
                .weight(0.1)
                .contribution(0.03) // 3%
                .build();

        // When & Then
        assertTrue(majorFactor.isMajorContributor());
        assertFalse(majorFactor.isCriticalContributor());
        assertEquals("Major", majorFactor.getRiskLevel());
        
        assertFalse(minorFactor.isSignificantContributor());
        assertEquals("Minor", minorFactor.getRiskLevel());
        
        assertTrue(majorFactor.isCreditFactor());
        assertFalse(minorFactor.isCreditFactor());
    }
}