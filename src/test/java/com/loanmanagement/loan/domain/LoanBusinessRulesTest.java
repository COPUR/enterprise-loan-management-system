package com.loanmanagement.loan.domain;

import com.loanmanagement.loan.domain.model.*;
import com.loanmanagement.loan.domain.service.LoanBusinessRulesService;
import com.loanmanagement.shared.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Loan Business Rules Service
 * Tests loan eligibility, validation rules, and business constraints
 */
@DisplayName("Loan Business Rules Tests")
class LoanBusinessRulesTest {

    private LoanBusinessRulesService businessRulesService;
    private CustomerProfile standardCustomer;
    private LoanApplication standardApplication;

    @BeforeEach
    void setUp() {
        businessRulesService = new LoanBusinessRulesService();
        
        standardCustomer = CustomerProfile.builder()
                .customerId(CustomerId.of("CUST-001"))
                .creditScore(750)
                .monthlyIncome(Money.of("USD", new BigDecimal("8000.00")))
                .monthlyDebtObligations(Money.of("USD", new BigDecimal("2000.00")))
                .employmentType(EmploymentType.FULL_TIME)
                .employmentDuration(36) // 3 years
                .residencyStatus(ResidencyStatus.CITIZEN)
                .age(30)
                .bankingHistory(24) // 2 years
                .build();
                
        standardApplication = LoanApplication.builder()
                .customerId(standardCustomer.getCustomerId())
                .requestedAmount(Money.of("USD", new BigDecimal("250000.00")))
                .loanPurpose(LoanPurpose.HOME)
                .requestedTerms(LoanTerms.builder()
                        .termInMonths(360)
                        .interestRate(new BigDecimal("4.5"))
                        .paymentFrequency(PaymentFrequency.MONTHLY)
                        .build())
                .collateralValue(Money.of("USD", new BigDecimal("300000.00")))
                .downPayment(Money.of("USD", new BigDecimal("50000.00")))
                .build();
    }

    @Nested
    @DisplayName("Credit Score Requirements Tests")
    class CreditScoreRequirementsTests {

        @Test
        @DisplayName("Should approve application with excellent credit score")
        void shouldApproveApplicationWithExcellentCreditScore() {
            // Given
            CustomerProfile excellentCreditCustomer = standardCustomer.toBuilder()
                    .creditScore(800)
                    .build();

            // When
            LoanEligibilityResult result = businessRulesService.checkLoanEligibility(
                    excellentCreditCustomer, standardApplication);

            // Then
            assertTrue(result.isEligible());
            assertTrue(result.getCreditScoreCheck().isPassed());
            assertFalse(result.getViolations().stream()
                    .anyMatch(v -> v.getRuleType() == BusinessRuleType.CREDIT_SCORE_MINIMUM));
        }

        @Test
        @DisplayName("Should reject application with poor credit score")
        void shouldRejectApplicationWithPoorCreditScore() {
            // Given
            CustomerProfile poorCreditCustomer = standardCustomer.toBuilder()
                    .creditScore(550)
                    .build();

            // When
            LoanEligibilityResult result = businessRulesService.checkLoanEligibility(
                    poorCreditCustomer, standardApplication);

            // Then
            assertFalse(result.isEligible());
            assertFalse(result.getCreditScoreCheck().isPassed());
            assertTrue(result.getViolations().stream()
                    .anyMatch(v -> v.getRuleType() == BusinessRuleType.CREDIT_SCORE_MINIMUM));
        }

        @Test
        @DisplayName("Should have different requirements for different loan types")
        void shouldHaveDifferentRequirementsForDifferentLoanTypes() {
            // Given
            CustomerProfile marginalCustomer = standardCustomer.toBuilder()
                    .creditScore(620)
                    .build();
            
            LoanApplication autoLoan = standardApplication.toBuilder()
                    .loanPurpose(LoanPurpose.AUTO)
                    .requestedAmount(Money.of("USD", new BigDecimal("30000.00")))
                    .build();
            
            LoanApplication homeLoan = standardApplication.toBuilder()
                    .loanPurpose(LoanPurpose.HOME)
                    .build();

            // When
            LoanEligibilityResult autoResult = businessRulesService.checkLoanEligibility(
                    marginalCustomer, autoLoan);
            LoanEligibilityResult homeResult = businessRulesService.checkLoanEligibility(
                    marginalCustomer, homeLoan);

            // Then
            // Auto loans typically have lower credit score requirements
            assertTrue(autoResult.getCreditScoreCheck().isPassed());
            // Home loans typically require higher credit scores
            assertFalse(homeResult.getCreditScoreCheck().isPassed());
        }
    }

    @Nested
    @DisplayName("Debt-to-Income Ratio Tests")
    class DebtToIncomeRatioTests {

        @Test
        @DisplayName("Should approve application with acceptable DTI ratio")
        void shouldApproveApplicationWithAcceptableDTIRatio() {
            // Given - Customer with good DTI ratio
            CustomerProfile goodDTICustomer = standardCustomer.toBuilder()
                    .monthlyIncome(Money.of("USD", new BigDecimal("10000.00")))
                    .monthlyDebtObligations(Money.of("USD", new BigDecimal("2000.00")))
                    .build();

            // When
            LoanEligibilityResult result = businessRulesService.checkLoanEligibility(
                    goodDTICustomer, standardApplication);

            // Then
            assertTrue(result.isEligible());
            assertTrue(result.getDebtToIncomeCheck().isPassed());
        }

        @Test
        @DisplayName("Should reject application with high DTI ratio")
        void shouldRejectApplicationWithHighDTIRatio() {
            // Given - Customer with high existing debt
            CustomerProfile highDTICustomer = standardCustomer.toBuilder()
                    .monthlyIncome(Money.of("USD", new BigDecimal("5000.00")))
                    .monthlyDebtObligations(Money.of("USD", new BigDecimal("4000.00")))
                    .build();

            // When
            LoanEligibilityResult result = businessRulesService.checkLoanEligibility(
                    highDTICustomer, standardApplication);

            // Then
            assertFalse(result.isEligible());
            assertFalse(result.getDebtToIncomeCheck().isPassed());
            assertTrue(result.getViolations().stream()
                    .anyMatch(v -> v.getRuleType() == BusinessRuleType.DEBT_TO_INCOME_RATIO));
        }

        @Test
        @DisplayName("Should calculate DTI including new loan payment")
        void shouldCalculateDTIIncludingNewLoanPayment() {
            // Given
            Money monthlyPayment = Money.of("USD", new BigDecimal("1500.00"));
            
            // When
            BigDecimal projectedDTI = businessRulesService.calculateProjectedDTI(
                    standardCustomer, monthlyPayment);

            // Then
            // Existing debt: $2000, New payment: $1500, Total: $3500
            // Income: $8000, DTI = $3500 / $8000 = 0.4375 = 43.75%
            BigDecimal expectedDTI = new BigDecimal("0.4375");
            assertEquals(0, expectedDTI.compareTo(projectedDTI.setScale(4, BigDecimal.ROUND_HALF_UP)));
        }
    }

    @Nested
    @DisplayName("Loan-to-Value Ratio Tests")
    class LoanToValueRatioTests {

        @Test
        @DisplayName("Should approve application with acceptable LTV ratio")
        void shouldApproveApplicationWithAcceptableLTVRatio() {
            // Given - LTV = $250,000 / $300,000 = 83.33%
            LoanApplication goodLTVApplication = standardApplication.toBuilder()
                    .requestedAmount(Money.of("USD", new BigDecimal("250000.00")))
                    .collateralValue(Money.of("USD", new BigDecimal("300000.00")))
                    .build();

            // When
            LoanEligibilityResult result = businessRulesService.checkLoanEligibility(
                    standardCustomer, goodLTVApplication);

            // Then
            assertTrue(result.getLoanToValueCheck().isPassed());
        }

        @Test
        @DisplayName("Should reject application with high LTV ratio")
        void shouldRejectApplicationWithHighLTVRatio() {
            // Given - LTV = $280,000 / $300,000 = 93.33%
            LoanApplication highLTVApplication = standardApplication.toBuilder()
                    .requestedAmount(Money.of("USD", new BigDecimal("280000.00")))
                    .collateralValue(Money.of("USD", new BigDecimal("300000.00")))
                    .build();

            // When
            LoanEligibilityResult result = businessRulesService.checkLoanEligibility(
                    standardCustomer, highLTVApplication);

            // Then
            assertFalse(result.getLoanToValueCheck().isPassed());
            assertTrue(result.getViolations().stream()
                    .anyMatch(v -> v.getRuleType() == BusinessRuleType.LOAN_TO_VALUE_RATIO));
        }

        @Test
        @DisplayName("Should have different LTV limits for different loan types")
        void shouldHaveDifferentLTVLimitsForDifferentLoanTypes() {
            // Given
            Money loanAmount = Money.of("USD", new BigDecimal("25000.00"));
            Money autoValue = Money.of("USD", new BigDecimal("30000.00"));
            Money homeValue = Money.of("USD", new BigDecimal("30000.00"));
            
            LoanApplication autoLoan = LoanApplication.builder()
                    .customerId(standardCustomer.getCustomerId())
                    .requestedAmount(loanAmount)
                    .loanPurpose(LoanPurpose.AUTO)
                    .collateralValue(autoValue)
                    .build();
            
            LoanApplication homeLoan = LoanApplication.builder()
                    .customerId(standardCustomer.getCustomerId())
                    .requestedAmount(loanAmount)
                    .loanPurpose(LoanPurpose.HOME)
                    .collateralValue(homeValue)
                    .build();

            // When
            BigDecimal autoLTVLimit = businessRulesService.getLTVLimit(LoanPurpose.AUTO);
            BigDecimal homeLTVLimit = businessRulesService.getLTVLimit(LoanPurpose.HOME);

            // Then
            // Auto loans typically allow higher LTV ratios
            assertTrue(autoLTVLimit.compareTo(homeLTVLimit) > 0);
        }
    }

    @Nested
    @DisplayName("Employment Requirements Tests")
    class EmploymentRequirementsTests {

        @Test
        @DisplayName("Should approve application with stable employment")
        void shouldApproveApplicationWithStableEmployment() {
            // Given
            CustomerProfile stableEmploymentCustomer = standardCustomer.toBuilder()
                    .employmentType(EmploymentType.FULL_TIME)
                    .employmentDuration(48) // 4 years
                    .build();

            // When
            LoanEligibilityResult result = businessRulesService.checkLoanEligibility(
                    stableEmploymentCustomer, standardApplication);

            // Then
            assertTrue(result.getEmploymentCheck().isPassed());
        }

        @Test
        @DisplayName("Should require longer employment history for self-employed")
        void shouldRequireLongerEmploymentHistoryForSelfEmployed() {
            // Given
            CustomerProfile selfEmployedCustomer = standardCustomer.toBuilder()
                    .employmentType(EmploymentType.SELF_EMPLOYED)
                    .employmentDuration(12) // 1 year
                    .build();

            // When
            LoanEligibilityResult result = businessRulesService.checkLoanEligibility(
                    selfEmployedCustomer, standardApplication);

            // Then
            assertFalse(result.getEmploymentCheck().isPassed());
            assertTrue(result.getViolations().stream()
                    .anyMatch(v -> v.getRuleType() == BusinessRuleType.EMPLOYMENT_STABILITY));
        }

        @Test
        @DisplayName("Should reject application for unemployed customers")
        void shouldRejectApplicationForUnemployedCustomers() {
            // Given
            CustomerProfile unemployedCustomer = standardCustomer.toBuilder()
                    .employmentType(EmploymentType.UNEMPLOYED)
                    .employmentDuration(0)
                    .build();

            // When
            LoanEligibilityResult result = businessRulesService.checkLoanEligibility(
                    unemployedCustomer, standardApplication);

            // Then
            assertFalse(result.isEligible());
            assertFalse(result.getEmploymentCheck().isPassed());
        }
    }

    @Nested
    @DisplayName("Loan Amount Limits Tests")
    class LoanAmountLimitsTests {

        @Test
        @DisplayName("Should reject application below minimum amount")
        void shouldRejectApplicationBelowMinimumAmount() {
            // Given
            LoanApplication smallLoanApplication = standardApplication.toBuilder()
                    .requestedAmount(Money.of("USD", new BigDecimal("500.00")))
                    .build();

            // When
            LoanEligibilityResult result = businessRulesService.checkLoanEligibility(
                    standardCustomer, smallLoanApplication);

            // Then
            assertFalse(result.isEligible());
            assertTrue(result.getViolations().stream()
                    .anyMatch(v -> v.getRuleType() == BusinessRuleType.MINIMUM_LOAN_AMOUNT));
        }

        @Test
        @DisplayName("Should reject application above maximum amount")
        void shouldRejectApplicationAboveMaximumAmount() {
            // Given
            LoanApplication largeLoanApplication = standardApplication.toBuilder()
                    .requestedAmount(Money.of("USD", new BigDecimal("10000000.00")))
                    .build();

            // When
            LoanEligibilityResult result = businessRulesService.checkLoanEligibility(
                    standardCustomer, largeLoanApplication);

            // Then
            assertFalse(result.isEligible());
            assertTrue(result.getViolations().stream()
                    .anyMatch(v -> v.getRuleType() == BusinessRuleType.MAXIMUM_LOAN_AMOUNT));
        }

        @Test
        @DisplayName("Should have different limits based on customer profile")
        void shouldHaveDifferentLimitsBasedOnCustomerProfile() {
            // Given
            CustomerProfile premiumCustomer = standardCustomer.toBuilder()
                    .creditScore(800)
                    .monthlyIncome(Money.of("USD", new BigDecimal("20000.00")))
                    .bankingHistory(60)
                    .build();

            // When
            Money standardLimit = businessRulesService.calculateMaximumLoanAmount(standardCustomer);
            Money premiumLimit = businessRulesService.calculateMaximumLoanAmount(premiumCustomer);

            // Then
            assertTrue(premiumLimit.getAmount().compareTo(standardLimit.getAmount()) > 0);
        }
    }

    @Nested
    @DisplayName("Age and Residency Requirements Tests")
    class AgeAndResidencyRequirementsTests {

        @Test
        @DisplayName("Should reject application for underage customer")
        void shouldRejectApplicationForUnderageCustomer() {
            // Given
            CustomerProfile underageCustomer = standardCustomer.toBuilder()
                    .age(17)
                    .build();

            // When
            LoanEligibilityResult result = businessRulesService.checkLoanEligibility(
                    underageCustomer, standardApplication);

            // Then
            assertFalse(result.isEligible());
            assertTrue(result.getViolations().stream()
                    .anyMatch(v -> v.getRuleType() == BusinessRuleType.MINIMUM_AGE));
        }

        @Test
        @DisplayName("Should have special requirements for non-citizens")
        void shouldHaveSpecialRequirementsForNonCitizens() {
            // Given
            CustomerProfile nonCitizenCustomer = standardCustomer.toBuilder()
                    .residencyStatus(ResidencyStatus.PERMANENT_RESIDENT)
                    .build();

            // When
            LoanEligibilityResult result = businessRulesService.checkLoanEligibility(
                    nonCitizenCustomer, standardApplication);

            // Then
            // Non-citizens might require additional documentation or have different terms
            assertNotNull(result.getAdditionalRequirements());
            assertFalse(result.getAdditionalRequirements().isEmpty());
        }

        @Test
        @DisplayName("Should reject application for tourists")
        void shouldRejectApplicationForTourists() {
            // Given
            CustomerProfile touristCustomer = standardCustomer.toBuilder()
                    .residencyStatus(ResidencyStatus.TOURIST)
                    .build();

            // When
            LoanEligibilityResult result = businessRulesService.checkLoanEligibility(
                    touristCustomer, standardApplication);

            // Then
            assertFalse(result.isEligible());
            assertTrue(result.getViolations().stream()
                    .anyMatch(v -> v.getRuleType() == BusinessRuleType.RESIDENCY_REQUIREMENT));
        }
    }

    @Nested
    @DisplayName("Banking History Requirements Tests")
    class BankingHistoryRequirementsTests {

        @Test
        @DisplayName("Should approve application with sufficient banking history")
        void shouldApproveApplicationWithSufficientBankingHistory() {
            // Given
            CustomerProfile experiencedCustomer = standardCustomer.toBuilder()
                    .bankingHistory(36) // 3 years
                    .build();

            // When
            LoanEligibilityResult result = businessRulesService.checkLoanEligibility(
                    experiencedCustomer, standardApplication);

            // Then
            assertTrue(result.getBankingHistoryCheck().isPassed());
        }

        @Test
        @DisplayName("Should require additional verification for new customers")
        void shouldRequireAdditionalVerificationForNewCustomers() {
            // Given
            CustomerProfile newCustomer = standardCustomer.toBuilder()
                    .bankingHistory(3) // 3 months
                    .build();

            // When
            LoanEligibilityResult result = businessRulesService.checkLoanEligibility(
                    newCustomer, standardApplication);

            // Then
            assertFalse(result.getBankingHistoryCheck().isPassed());
            assertNotNull(result.getAdditionalRequirements());
            assertTrue(result.getAdditionalRequirements().contains(
                    "Additional income verification required for new customers"));
        }
    }

    @Nested
    @DisplayName("Combined Business Rules Tests")
    class CombinedBusinessRulesTests {

        @Test
        @DisplayName("Should pass all checks for ideal customer")
        void shouldPassAllChecksForIdealCustomer() {
            // Given
            CustomerProfile idealCustomer = CustomerProfile.builder()
                    .customerId(CustomerId.of("IDEAL-001"))
                    .creditScore(800)
                    .monthlyIncome(Money.of("USD", new BigDecimal("15000.00")))
                    .monthlyDebtObligations(Money.of("USD", new BigDecimal("2000.00")))
                    .employmentType(EmploymentType.FULL_TIME)
                    .employmentDuration(60)
                    .residencyStatus(ResidencyStatus.CITIZEN)
                    .age(35)
                    .bankingHistory(60)
                    .build();

            // When
            LoanEligibilityResult result = businessRulesService.checkLoanEligibility(
                    idealCustomer, standardApplication);

            // Then
            assertTrue(result.isEligible());
            assertTrue(result.getCreditScoreCheck().isPassed());
            assertTrue(result.getDebtToIncomeCheck().isPassed());
            assertTrue(result.getLoanToValueCheck().isPassed());
            assertTrue(result.getEmploymentCheck().isPassed());
            assertTrue(result.getBankingHistoryCheck().isPassed());
            assertTrue(result.getViolations().isEmpty());
        }

        @Test
        @DisplayName("Should calculate overall risk score")
        void shouldCalculateOverallRiskScore() {
            // When
            RiskScore riskScore = businessRulesService.calculateRiskScore(
                    standardCustomer, standardApplication);

            // Then
            assertNotNull(riskScore);
            assertTrue(riskScore.getScore() >= 0 && riskScore.getScore() <= 1000);
            assertNotNull(riskScore.getRiskCategory());
            assertNotNull(riskScore.getFactors());
            assertFalse(riskScore.getFactors().isEmpty());
        }

        @Test
        @DisplayName("Should provide detailed violation information")
        void shouldProvideDetailedViolationInformation() {
            // Given
            CustomerProfile problematicCustomer = CustomerProfile.builder()
                    .customerId(CustomerId.of("PROB-001"))
                    .creditScore(500)
                    .monthlyIncome(Money.of("USD", new BigDecimal("3000.00")))
                    .monthlyDebtObligations(Money.of("USD", new BigDecimal("2800.00")))
                    .employmentType(EmploymentType.UNEMPLOYED)
                    .employmentDuration(0)
                    .residencyStatus(ResidencyStatus.CITIZEN)
                    .age(16)
                    .bankingHistory(1)
                    .build();

            // When
            LoanEligibilityResult result = businessRulesService.checkLoanEligibility(
                    problematicCustomer, standardApplication);

            // Then
            assertFalse(result.isEligible());
            assertFalse(result.getViolations().isEmpty());
            
            // Should have multiple violations
            assertTrue(result.getViolations().stream()
                    .anyMatch(v -> v.getRuleType() == BusinessRuleType.CREDIT_SCORE_MINIMUM));
            assertTrue(result.getViolations().stream()
                    .anyMatch(v -> v.getRuleType() == BusinessRuleType.DEBT_TO_INCOME_RATIO));
            assertTrue(result.getViolations().stream()
                    .anyMatch(v -> v.getRuleType() == BusinessRuleType.MINIMUM_AGE));
            
            // Each violation should have detailed information
            result.getViolations().forEach(violation -> {
                assertNotNull(violation.getRuleType());
                assertNotNull(violation.getDescription());
                assertNotNull(violation.getSeverity());
            });
        }
    }
}