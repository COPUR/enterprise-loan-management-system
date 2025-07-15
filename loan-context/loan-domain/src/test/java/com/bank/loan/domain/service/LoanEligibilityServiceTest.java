package com.bank.loan.domain.service;

import com.bank.loan.domain.Customer;
import com.bank.loan.domain.TestCustomer;
import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import com.bank.loan.domain.LoanEligibilityResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for LoanEligibilityService
 * 
 * Tests verify that the extracted domain service correctly implements
 * business rules for loan eligibility assessment following SOLID and GRASP principles.
 */
@DisplayName("Loan Eligibility Service Tests")
class LoanEligibilityServiceTest {

    private LoanEligibilityService eligibilityService;
    private Customer qualifiedCustomer;
    private Money standardLoanAmount;

    @BeforeEach
    void setUp() {
        eligibilityService = new LoanEligibilityService();
        standardLoanAmount = Money.aed(new BigDecimal("100000"));
        
        qualifiedCustomer = createQualifiedCustomer();
    }

    @Nested
    @DisplayName("Basic Eligibility Assessment Tests")
    class BasicEligibilityTests {

        @Test
        @DisplayName("Should approve qualified customer for reasonable loan amount")
        void shouldApproveQualifiedCustomerForReasonableLoanAmount() {
            LoanEligibilityResult result = eligibilityService.assessEligibility(qualifiedCustomer, standardLoanAmount);
            
            assertTrue(result.isApproved());
            assertFalse(result.getFailedChecks().isEmpty() == false); // Should have passed checks
            assertTrue(result.getPassedCheckCount() > 0);
        }

        @Test
        @DisplayName("Should reject null customer")
        void shouldRejectNullCustomer() {
            LoanEligibilityResult result = eligibilityService.assessEligibility(null, standardLoanAmount);
            
            assertTrue(result.isRejected());
            assertEquals("Customer cannot be null", result.getPrimaryReason());
        }

        @Test
        @DisplayName("Should reject null loan amount")
        void shouldRejectNullLoanAmount() {
            LoanEligibilityResult result = eligibilityService.assessEligibility(qualifiedCustomer, null);
            
            assertTrue(result.isRejected());
            assertEquals("Requested amount must be positive", result.getPrimaryReason());
        }

        @Test
        @DisplayName("Should reject zero loan amount")
        void shouldRejectZeroLoanAmount() {
            Money zeroAmount = Money.aed(BigDecimal.ZERO);
            LoanEligibilityResult result = eligibilityService.assessEligibility(qualifiedCustomer, zeroAmount);
            
            assertTrue(result.isRejected());
            assertEquals("Requested amount must be positive", result.getPrimaryReason());
        }

        @Test
        @DisplayName("Should reject negative loan amount")
        void shouldRejectNegativeLoanAmount() {
            Money negativeAmount = Money.aed(new BigDecimal("-10000"));
            LoanEligibilityResult result = eligibilityService.assessEligibility(qualifiedCustomer, negativeAmount);
            
            assertTrue(result.isRejected());
            assertEquals("Requested amount must be positive", result.getPrimaryReason());
        }
    }

    @Nested
    @DisplayName("Credit Score Requirements Tests")
    class CreditScoreTests {

        @Test
        @DisplayName("Should reject customer with low credit score")
        void shouldRejectCustomerWithLowCreditScore() {
            Customer lowCreditCustomer = createCustomerWithCreditScore(550);
            
            LoanEligibilityResult result = eligibilityService.assessEligibility(lowCreditCustomer, standardLoanAmount);
            
            assertTrue(result.isRejected());
            assertTrue(result.getPrimaryReason().contains("Credit score"));
        }

        @Test
        @DisplayName("Should approve customer with minimum credit score")
        void shouldApproveCustomerWithMinimumCreditScore() {
            Customer minCreditCustomer = createCustomerWithCreditScore(600);
            
            LoanEligibilityResult result = eligibilityService.assessEligibility(minCreditCustomer, standardLoanAmount);
            
            // Should not fail due to credit score (might fail for other reasons)
            assertFalse(result.getFailedChecks().stream()
                .anyMatch(check -> check.contains("Credit score")));
        }

        @Test
        @DisplayName("Should approve customer with excellent credit score")
        void shouldApproveCustomerWithExcellentCreditScore() {
            Customer excellentCreditCustomer = createCustomerWithCreditScore(800);
            
            LoanEligibilityResult result = eligibilityService.assessEligibility(excellentCreditCustomer, standardLoanAmount);
            
            assertTrue(result.getPassedChecks().stream()
                .anyMatch(check -> check.contains("Credit score")));
        }
    }

    @Nested
    @DisplayName("Age Requirements Tests")
    class AgeRequirementTests {

        @Test
        @DisplayName("Should reject customer under minimum age")
        void shouldRejectCustomerUnderMinimumAge() {
            Customer youngCustomer = createCustomerWithAge(17);
            
            LoanEligibilityResult result = eligibilityService.assessEligibility(youngCustomer, standardLoanAmount);
            
            assertTrue(result.isRejected());
            assertTrue(result.getPrimaryReason().contains("age"));
        }

        @Test
        @DisplayName("Should reject customer over maximum age")
        void shouldRejectCustomerOverMaximumAge() {
            Customer oldCustomer = createCustomerWithAge(71);
            
            LoanEligibilityResult result = eligibilityService.assessEligibility(oldCustomer, standardLoanAmount);
            
            assertTrue(result.isRejected());
            assertTrue(result.getPrimaryReason().contains("age"));
        }

        @Test
        @DisplayName("Should approve customer at minimum age")
        void shouldApproveCustomerAtMinimumAge() {
            Customer minAgeCustomer = createCustomerWithAge(18);
            
            LoanEligibilityResult result = eligibilityService.assessEligibility(minAgeCustomer, standardLoanAmount);
            
            assertFalse(result.getFailedChecks().stream()
                .anyMatch(check -> check.contains("age")));
        }

        @Test
        @DisplayName("Should approve customer at maximum age")
        void shouldApproveCustomerAtMaximumAge() {
            Customer maxAgeCustomer = createCustomerWithAge(70);
            
            LoanEligibilityResult result = eligibilityService.assessEligibility(maxAgeCustomer, standardLoanAmount);
            
            assertFalse(result.getFailedChecks().stream()
                .anyMatch(check -> check.contains("age")));
        }
    }

    @Nested
    @DisplayName("Income Requirements Tests")
    class IncomeRequirementTests {

        @Test
        @DisplayName("Should reject customer with insufficient income")
        void shouldRejectCustomerWithInsufficientIncome() {
            Customer lowIncomeCustomer = createCustomerWithIncome(new BigDecimal("3000"));
            
            LoanEligibilityResult result = eligibilityService.assessEligibility(lowIncomeCustomer, standardLoanAmount);
            
            assertTrue(result.isRejected());
            assertTrue(result.getPrimaryReason().contains("income"));
        }

        @Test
        @DisplayName("Should approve customer with sufficient income")
        void shouldApproveCustomerWithSufficientIncome() {
            Customer highIncomeCustomer = createCustomerWithIncome(new BigDecimal("15000"));
            
            LoanEligibilityResult result = eligibilityService.assessEligibility(highIncomeCustomer, standardLoanAmount);
            
            assertFalse(result.getFailedChecks().stream()
                .anyMatch(check -> check.contains("income") && !check.contains("debt-to-income")));
        }
    }

    @Nested
    @DisplayName("Debt-to-Income Ratio Tests")
    class DebtToIncomeTests {

        @Test
        @DisplayName("Should reject customer with high debt-to-income ratio")
        void shouldRejectCustomerWithHighDebtToIncomeRatio() {
            Customer highDebtCustomer = createCustomerWithExistingDebt(new BigDecimal("8000")); // High existing debt
            
            LoanEligibilityResult result = eligibilityService.assessEligibility(highDebtCustomer, standardLoanAmount);
            
            assertTrue(result.getFailedChecks().stream()
                .anyMatch(check -> check.toLowerCase().contains("debt-to-income")));
        }

        @Test
        @DisplayName("Should approve customer with acceptable debt-to-income ratio")
        void shouldApproveCustomerWithAcceptableDebtToIncomeRatio() {
            Customer lowDebtCustomer = createCustomerWithExistingDebt(new BigDecimal("1000")); // Low existing debt
            
            LoanEligibilityResult result = eligibilityService.assessEligibility(lowDebtCustomer, standardLoanAmount);
            
            assertFalse(result.getFailedChecks().stream()
                .anyMatch(check -> check.contains("debt-to-income")));
        }
    }

    @Nested
    @DisplayName("Maximum Loan Amount Tests")
    class MaximumLoanAmountTests {

        @Test
        @DisplayName("Should calculate maximum loan amount based on customer profile")
        void shouldCalculateMaximumLoanAmountBasedOnCustomerProfile() {
            Money maxAmount = eligibilityService.calculateMaximumLoanAmount(qualifiedCustomer);
            
            assertTrue(maxAmount.isPositive());
            assertTrue(maxAmount.compareTo(Money.aed(new BigDecimal("10000"))) >= 0);
        }

        @Test
        @DisplayName("Should return zero for null customer")
        void shouldReturnZeroForNullCustomer() {
            Money maxAmount = eligibilityService.calculateMaximumLoanAmount(null);
            
            assertEquals(Money.aed(BigDecimal.ZERO), maxAmount);
        }

        @Test
        @DisplayName("Should calculate higher amounts for customers with better credit scores")
        void shouldCalculateHigherAmountsForCustomersWithBetterCreditScores() {
            Customer goodCreditCustomer = createCustomerWithCreditScore(700);
            Customer excellentCreditCustomer = createCustomerWithCreditScore(800);
            
            Money goodCreditAmount = eligibilityService.calculateMaximumLoanAmount(goodCreditCustomer);
            Money excellentCreditAmount = eligibilityService.calculateMaximumLoanAmount(excellentCreditCustomer);
            
            assertTrue(excellentCreditAmount.compareTo(goodCreditAmount) >= 0);
        }
    }

    @Nested
    @DisplayName("Pre-qualification Tests")
    class PrequalificationTests {

        @Test
        @DisplayName("Should pre-qualify eligible customer")
        void shouldPreQualifyEligibleCustomer() {
            boolean preQualified = eligibilityService.isPreQualified(qualifiedCustomer);
            
            assertTrue(preQualified);
        }

        @Test
        @DisplayName("Should not pre-qualify customer with low credit score")
        void shouldNotPreQualifyCustomerWithLowCreditScore() {
            Customer lowCreditCustomer = createCustomerWithCreditScore(500);
            
            boolean preQualified = eligibilityService.isPreQualified(lowCreditCustomer);
            
            assertFalse(preQualified);
        }

        @Test
        @DisplayName("Should not pre-qualify null customer")
        void shouldNotPreQualifyNullCustomer() {
            boolean preQualified = eligibilityService.isPreQualified(null);
            
            assertFalse(preQualified);
        }
    }

    // Helper methods for creating test customers

    private Customer createQualifiedCustomer() {
        return TestCustomer.builder()
            .customerId(CustomerId.generate())
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .dateOfBirth(LocalDate.now().minusYears(35)) // 35 years old
            .creditScore(750)
            .monthlyIncome(new BigDecimal("12000"))
            .existingMonthlyObligations(new BigDecimal("2000"))
            .active(true)
            .build();
    }

    private Customer createCustomerWithCreditScore(int creditScore) {
        return TestCustomer.builder()
            .customerId(CustomerId.generate())
            .firstName("Test")
            .lastName("Customer")
            .email("test@example.com")
            .dateOfBirth(LocalDate.now().minusYears(35))
            .creditScore(creditScore)
            .monthlyIncome(new BigDecimal("12000"))
            .existingMonthlyObligations(new BigDecimal("2000"))
            .active(true)
            .build();
    }

    private Customer createCustomerWithAge(int age) {
        return TestCustomer.builder()
            .customerId(CustomerId.generate())
            .firstName("Test")
            .lastName("Customer")
            .email("test@example.com")
            .dateOfBirth(LocalDate.now().minusYears(age))
            .creditScore(750)
            .monthlyIncome(new BigDecimal("12000"))
            .existingMonthlyObligations(new BigDecimal("2000"))
            .active(true)
            .build();
    }

    private Customer createCustomerWithIncome(BigDecimal monthlyIncome) {
        return TestCustomer.builder()
            .customerId(CustomerId.generate())
            .firstName("Test")
            .lastName("Customer")
            .email("test@example.com")
            .dateOfBirth(LocalDate.now().minusYears(35))
            .creditScore(750)
            .monthlyIncome(monthlyIncome)
            .existingMonthlyObligations(new BigDecimal("1000"))
            .active(true)
            .build();
    }

    private Customer createCustomerWithExistingDebt(BigDecimal existingObligations) {
        return TestCustomer.builder()
            .customerId(CustomerId.generate())
            .firstName("Test")
            .lastName("Customer")
            .email("test@example.com")
            .dateOfBirth(LocalDate.now().minusYears(35))
            .creditScore(750)
            .monthlyIncome(new BigDecimal("12000"))
            .existingMonthlyObligations(existingObligations)
            .active(true)
            .build();
    }
}