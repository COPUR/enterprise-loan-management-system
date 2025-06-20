package com.bank.loanmanagement.domain.loan;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive TDD tests for LoanTerms value object
 * Testing loan terms creation, validation, and business rules
 */
@DisplayName("📋 Loan Terms Value Object Tests")
class LoanTermsTest {

    @Nested
    @DisplayName("Loan Terms Creation")
    class LoanTermsCreationTests {

        @Test
        @DisplayName("Should create loan terms with all parameters using constructor")
        void shouldCreateLoanTermsWithAllParametersUsingConstructor() {
            // Given
            BigDecimal penaltyRate = BigDecimal.valueOf(0.03);
            BigDecimal lateFeeAmount = BigDecimal.valueOf(50.00);
            Integer gracePeriodDays = 7;
            Boolean allowPrepayment = true;
            BigDecimal prepaymentPenaltyRate = BigDecimal.valueOf(0.01);
            String specialConditions = "Custom loan terms";

            // When
            LoanTerms loanTerms = new LoanTerms(penaltyRate, lateFeeAmount, gracePeriodDays, 
                                               allowPrepayment, prepaymentPenaltyRate, specialConditions);

            // Then
            assertThat(loanTerms.getPenaltyRate()).isEqualTo(penaltyRate);
            assertThat(loanTerms.getLateFeeAmount()).isEqualTo(lateFeeAmount);
            assertThat(loanTerms.getGracePeriodDays()).isEqualTo(gracePeriodDays);
            assertThat(loanTerms.getAllowPrepayment()).isEqualTo(allowPrepayment);
            assertThat(loanTerms.getPrepaymentPenaltyRate()).isEqualTo(prepaymentPenaltyRate);
            assertThat(loanTerms.getSpecialConditions()).isEqualTo(specialConditions);
        }

        @Test
        @DisplayName("Should create loan terms using no-args constructor")
        void shouldCreateLoanTermsUsingNoArgsConstructor() {
            // When
            LoanTerms loanTerms = new LoanTerms();

            // Then
            assertThat(loanTerms.getPenaltyRate()).isNull();
            assertThat(loanTerms.getLateFeeAmount()).isNull();
            assertThat(loanTerms.getGracePeriodDays()).isNull();
            assertThat(loanTerms.getAllowPrepayment()).isNull();
            assertThat(loanTerms.getPrepaymentPenaltyRate()).isNull();
            assertThat(loanTerms.getSpecialConditions()).isNull();
        }

        @Test
        @DisplayName("Should create standard loan terms using factory method")
        void shouldCreateStandardLoanTermsUsingFactoryMethod() {
            // When
            LoanTerms standardTerms = LoanTerms.standard();

            // Then
            assertThat(standardTerms.getPenaltyRate()).isEqualTo(BigDecimal.valueOf(0.02));
            assertThat(standardTerms.getLateFeeAmount()).isEqualTo(BigDecimal.valueOf(25.00));
            assertThat(standardTerms.getGracePeriodDays()).isEqualTo(5);
            assertThat(standardTerms.getAllowPrepayment()).isTrue();
            assertThat(standardTerms.getPrepaymentPenaltyRate()).isEqualTo(BigDecimal.ZERO);
            assertThat(standardTerms.getSpecialConditions()).isEqualTo("Standard loan terms and conditions");
        }
    }

    @Nested
    @DisplayName("Penalty Rate Validation")
    class PenaltyRateValidationTests {

        @Test
        @DisplayName("Should accept valid penalty rates")
        void shouldAcceptValidPenaltyRates() {
            // Given
            BigDecimal[] validRates = {
                BigDecimal.ZERO,
                BigDecimal.valueOf(0.01),    // 1%
                BigDecimal.valueOf(0.05),    // 5%
                BigDecimal.valueOf(0.10),    // 10%
                BigDecimal.valueOf(0.15)     // 15%
            };

            // When & Then
            for (BigDecimal rate : validRates) {
                LoanTerms terms = new LoanTerms(rate, BigDecimal.valueOf(25.00), 5, true, BigDecimal.ZERO, "Test");
                assertThat(terms.getPenaltyRate()).isEqualTo(rate);
            }
        }

        @Test
        @DisplayName("Should handle null penalty rate")
        void shouldHandleNullPenaltyRate() {
            // When
            LoanTerms terms = new LoanTerms(null, BigDecimal.valueOf(25.00), 5, true, BigDecimal.ZERO, "Test");

            // Then
            assertThat(terms.getPenaltyRate()).isNull();
        }

        @Test
        @DisplayName("Should support high precision penalty rates")
        void shouldSupportHighPrecisionPenaltyRates() {
            // Given
            BigDecimal preciseRate = new BigDecimal("0.02375"); // 2.375%

            // When
            LoanTerms terms = new LoanTerms(preciseRate, BigDecimal.valueOf(25.00), 5, true, BigDecimal.ZERO, "Test");

            // Then
            assertThat(terms.getPenaltyRate()).isEqualTo(preciseRate);
        }
    }

    @Nested
    @DisplayName("Late Fee Amount Validation")
    class LateFeeAmountValidationTests {

        @Test
        @DisplayName("Should accept various late fee amounts")
        void shouldAcceptVariousLateFeeAmounts() {
            // Given
            BigDecimal[] validFees = {
                BigDecimal.ZERO,
                BigDecimal.valueOf(15.00),
                BigDecimal.valueOf(25.00),
                BigDecimal.valueOf(50.00),
                BigDecimal.valueOf(100.00),
                new BigDecimal("37.50")
            };

            // When & Then
            for (BigDecimal fee : validFees) {
                LoanTerms terms = new LoanTerms(BigDecimal.valueOf(0.02), fee, 5, true, BigDecimal.ZERO, "Test");
                assertThat(terms.getLateFeeAmount()).isEqualTo(fee);
            }
        }

        @Test
        @DisplayName("Should handle null late fee amount")
        void shouldHandleNullLateFeeAmount() {
            // When
            LoanTerms terms = new LoanTerms(BigDecimal.valueOf(0.02), null, 5, true, BigDecimal.ZERO, "Test");

            // Then
            assertThat(terms.getLateFeeAmount()).isNull();
        }

        @Test
        @DisplayName("Should support fractional late fee amounts")
        void shouldSupportFractionalLateFeeAmounts() {
            // Given
            BigDecimal fractionalFee = new BigDecimal("12.75");

            // When
            LoanTerms terms = new LoanTerms(BigDecimal.valueOf(0.02), fractionalFee, 5, true, BigDecimal.ZERO, "Test");

            // Then
            assertThat(terms.getLateFeeAmount()).isEqualTo(fractionalFee);
        }
    }

    @Nested
    @DisplayName("Grace Period Validation")
    class GracePeriodValidationTests {

        @Test
        @DisplayName("Should accept various grace period lengths")
        void shouldAcceptVariousGracePeriodLengths() {
            // Given
            Integer[] validPeriods = {0, 1, 3, 5, 7, 10, 15, 30};

            // When & Then
            for (Integer period : validPeriods) {
                LoanTerms terms = new LoanTerms(BigDecimal.valueOf(0.02), BigDecimal.valueOf(25.00), 
                                               period, true, BigDecimal.ZERO, "Test");
                assertThat(terms.getGracePeriodDays()).isEqualTo(period);
            }
        }

        @Test
        @DisplayName("Should handle null grace period")
        void shouldHandleNullGracePeriod() {
            // When
            LoanTerms terms = new LoanTerms(BigDecimal.valueOf(0.02), BigDecimal.valueOf(25.00), 
                                           null, true, BigDecimal.ZERO, "Test");

            // Then
            assertThat(terms.getGracePeriodDays()).isNull();
        }

        @Test
        @DisplayName("Should support zero grace period")
        void shouldSupportZeroGracePeriod() {
            // When
            LoanTerms terms = new LoanTerms(BigDecimal.valueOf(0.02), BigDecimal.valueOf(25.00), 
                                           0, true, BigDecimal.ZERO, "Test");

            // Then
            assertThat(terms.getGracePeriodDays()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should support extended grace periods")
        void shouldSupportExtendedGracePeriods() {
            // Given
            Integer extendedPeriod = 60; // 60 days

            // When
            LoanTerms terms = new LoanTerms(BigDecimal.valueOf(0.02), BigDecimal.valueOf(25.00), 
                                           extendedPeriod, true, BigDecimal.ZERO, "Test");

            // Then
            assertThat(terms.getGracePeriodDays()).isEqualTo(extendedPeriod);
        }
    }

    @Nested
    @DisplayName("Prepayment Options")
    class PrepaymentOptionsTests {

        @Test
        @DisplayName("Should allow prepayment when flag is true")
        void shouldAllowPrepaymentWhenFlagIsTrue() {
            // When
            LoanTerms terms = new LoanTerms(BigDecimal.valueOf(0.02), BigDecimal.valueOf(25.00), 
                                           5, true, BigDecimal.ZERO, "Test");

            // Then
            assertThat(terms.getAllowPrepayment()).isTrue();
        }

        @Test
        @DisplayName("Should disallow prepayment when flag is false")
        void shouldDisallowPrepaymentWhenFlagIsFalse() {
            // When
            LoanTerms terms = new LoanTerms(BigDecimal.valueOf(0.02), BigDecimal.valueOf(25.00), 
                                           5, false, BigDecimal.ZERO, "Test");

            // Then
            assertThat(terms.getAllowPrepayment()).isFalse();
        }

        @Test
        @DisplayName("Should handle null prepayment flag")
        void shouldHandleNullPrepaymentFlag() {
            // When
            LoanTerms terms = new LoanTerms(BigDecimal.valueOf(0.02), BigDecimal.valueOf(25.00), 
                                           5, null, BigDecimal.ZERO, "Test");

            // Then
            assertThat(terms.getAllowPrepayment()).isNull();
        }
    }

    @Nested
    @DisplayName("Prepayment Penalty Rate")
    class PrepaymentPenaltyRateTests {

        @Test
        @DisplayName("Should accept zero prepayment penalty rate")
        void shouldAcceptZeroPrepaymentPenaltyRate() {
            // When
            LoanTerms terms = new LoanTerms(BigDecimal.valueOf(0.02), BigDecimal.valueOf(25.00), 
                                           5, true, BigDecimal.ZERO, "Test");

            // Then
            assertThat(terms.getPrepaymentPenaltyRate()).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should accept various prepayment penalty rates")
        void shouldAcceptVariousPrepaymentPenaltyRates() {
            // Given
            BigDecimal[] validRates = {
                BigDecimal.valueOf(0.005),   // 0.5%
                BigDecimal.valueOf(0.01),    // 1%
                BigDecimal.valueOf(0.02),    // 2%
                BigDecimal.valueOf(0.03)     // 3%
            };

            // When & Then
            for (BigDecimal rate : validRates) {
                LoanTerms terms = new LoanTerms(BigDecimal.valueOf(0.02), BigDecimal.valueOf(25.00), 
                                               5, true, rate, "Test");
                assertThat(terms.getPrepaymentPenaltyRate()).isEqualTo(rate);
            }
        }

        @Test
        @DisplayName("Should handle null prepayment penalty rate")
        void shouldHandleNullPrepaymentPenaltyRate() {
            // When
            LoanTerms terms = new LoanTerms(BigDecimal.valueOf(0.02), BigDecimal.valueOf(25.00), 
                                           5, true, null, "Test");

            // Then
            assertThat(terms.getPrepaymentPenaltyRate()).isNull();
        }
    }

    @Nested
    @DisplayName("Special Conditions")
    class SpecialConditionsTests {

        @Test
        @DisplayName("Should accept various special conditions")
        void shouldAcceptVariousSpecialConditions() {
            // Given
            String[] conditions = {
                "Standard terms",
                "Special corporate rate",
                "Early payment discount available",
                "Seasonal payment adjustments allowed",
                "Interest rate may be adjusted based on performance"
            };

            // When & Then
            for (String condition : conditions) {
                LoanTerms terms = new LoanTerms(BigDecimal.valueOf(0.02), BigDecimal.valueOf(25.00), 
                                               5, true, BigDecimal.ZERO, condition);
                assertThat(terms.getSpecialConditions()).isEqualTo(condition);
            }
        }

        @Test
        @DisplayName("Should handle null special conditions")
        void shouldHandleNullSpecialConditions() {
            // When
            LoanTerms terms = new LoanTerms(BigDecimal.valueOf(0.02), BigDecimal.valueOf(25.00), 
                                           5, true, BigDecimal.ZERO, null);

            // Then
            assertThat(terms.getSpecialConditions()).isNull();
        }

        @Test
        @DisplayName("Should handle empty special conditions")
        void shouldHandleEmptySpecialConditions() {
            // When
            LoanTerms terms = new LoanTerms(BigDecimal.valueOf(0.02), BigDecimal.valueOf(25.00), 
                                           5, true, BigDecimal.ZERO, "");

            // Then
            assertThat(terms.getSpecialConditions()).isEqualTo("");
        }

        @Test
        @DisplayName("Should handle long special conditions text")
        void shouldHandleLongSpecialConditionsText() {
            // Given
            String longConditions = "This is a very long special conditions text that might contain " +
                                  "multiple clauses, terms, and conditions that apply to this specific " +
                                  "loan agreement including but not limited to payment schedules, " +
                                  "interest rate adjustments, and various other banking regulations.";

            // When
            LoanTerms terms = new LoanTerms(BigDecimal.valueOf(0.02), BigDecimal.valueOf(25.00), 
                                           5, true, BigDecimal.ZERO, longConditions);

            // Then
            assertThat(terms.getSpecialConditions()).isEqualTo(longConditions);
        }
    }

    @Nested
    @DisplayName("Loan Terms Equality")
    class LoanTermsEqualityTests {

        @Test
        @DisplayName("Should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            // Given
            LoanTerms terms1 = new LoanTerms(BigDecimal.valueOf(0.02), BigDecimal.valueOf(25.00), 
                                            5, true, BigDecimal.ZERO, "Standard terms");
            LoanTerms terms2 = new LoanTerms(BigDecimal.valueOf(0.02), BigDecimal.valueOf(25.00), 
                                            5, true, BigDecimal.ZERO, "Standard terms");

            // When & Then
            assertThat(terms1).isEqualTo(terms2);
            assertThat(terms1.hashCode()).isEqualTo(terms2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when penalty rates differ")
        void shouldNotBeEqualWhenPenaltyRatesDiffer() {
            // Given
            LoanTerms terms1 = new LoanTerms(BigDecimal.valueOf(0.02), BigDecimal.valueOf(25.00), 
                                            5, true, BigDecimal.ZERO, "Standard terms");
            LoanTerms terms2 = new LoanTerms(BigDecimal.valueOf(0.03), BigDecimal.valueOf(25.00), 
                                            5, true, BigDecimal.ZERO, "Standard terms");

            // When & Then
            assertThat(terms1).isNotEqualTo(terms2);
        }

        @Test
        @DisplayName("Should not be equal when grace periods differ")
        void shouldNotBeEqualWhenGracePeriodsdiffer() {
            // Given
            LoanTerms terms1 = new LoanTerms(BigDecimal.valueOf(0.02), BigDecimal.valueOf(25.00), 
                                            5, true, BigDecimal.ZERO, "Standard terms");
            LoanTerms terms2 = new LoanTerms(BigDecimal.valueOf(0.02), BigDecimal.valueOf(25.00), 
                                            7, true, BigDecimal.ZERO, "Standard terms");

            // When & Then
            assertThat(terms1).isNotEqualTo(terms2);
        }

        @Test
        @DisplayName("Should not be equal when prepayment flags differ")
        void shouldNotBeEqualWhenPrepaymentFlagsDiffer() {
            // Given
            LoanTerms terms1 = new LoanTerms(BigDecimal.valueOf(0.02), BigDecimal.valueOf(25.00), 
                                            5, true, BigDecimal.ZERO, "Standard terms");
            LoanTerms terms2 = new LoanTerms(BigDecimal.valueOf(0.02), BigDecimal.valueOf(25.00), 
                                            5, false, BigDecimal.ZERO, "Standard terms");

            // When & Then
            assertThat(terms1).isNotEqualTo(terms2);
        }
    }

    @Nested
    @DisplayName("Standard Terms Factory")
    class StandardTermsFactoryTests {

        @Test
        @DisplayName("Should create consistent standard terms")
        void shouldCreateConsistentStandardTerms() {
            // When
            LoanTerms standard1 = LoanTerms.standard();
            LoanTerms standard2 = LoanTerms.standard();

            // Then
            assertThat(standard1).isEqualTo(standard2);
            assertThat(standard1.getPenaltyRate()).isEqualTo(standard2.getPenaltyRate());
            assertThat(standard1.getLateFeeAmount()).isEqualTo(standard2.getLateFeeAmount());
            assertThat(standard1.getGracePeriodDays()).isEqualTo(standard2.getGracePeriodDays());
            assertThat(standard1.getAllowPrepayment()).isEqualTo(standard2.getAllowPrepayment());
            assertThat(standard1.getPrepaymentPenaltyRate()).isEqualTo(standard2.getPrepaymentPenaltyRate());
            assertThat(standard1.getSpecialConditions()).isEqualTo(standard2.getSpecialConditions());
        }

        @Test
        @DisplayName("Should create standard terms with expected values")
        void shouldCreateStandardTermsWithExpectedValues() {
            // When
            LoanTerms standardTerms = LoanTerms.standard();

            // Then
            assertThat(standardTerms.getPenaltyRate()).isEqualTo(BigDecimal.valueOf(0.02));
            assertThat(standardTerms.getLateFeeAmount()).isEqualTo(BigDecimal.valueOf(25.00));
            assertThat(standardTerms.getGracePeriodDays()).isEqualTo(5);
            assertThat(standardTerms.getAllowPrepayment()).isTrue();
            assertThat(standardTerms.getPrepaymentPenaltyRate()).isEqualTo(BigDecimal.ZERO);
            assertThat(standardTerms.getSpecialConditions()).isNotEmpty();
        }
    }
}