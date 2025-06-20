package com.bank.loanmanagement.domain.loan;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive TDD tests for InterestRate value object
 * Testing interest rate creation, validation, calculations, and business rules
 */
@DisplayName("💹 Interest Rate Value Object Tests")
class InterestRateTest {

    @Nested
    @DisplayName("Interest Rate Creation")
    class InterestRateCreationTests {

        @Test
        @DisplayName("Should create interest rate using of() method with BigDecimal")
        void shouldCreateInterestRateUsingOfMethodWithBigDecimal() {
            // Given
            BigDecimal rate = new BigDecimal("0.05"); // 5%

            // When
            InterestRate interestRate = InterestRate.of(rate);

            // Then
            assertThat(interestRate.getAnnualRate()).isEqualTo(rate);
        }

        @Test
        @DisplayName("Should create interest rate using ofPercentage() method")
        void shouldCreateInterestRateUsingOfPercentageMethod() {
            // Given
            double percentage = 7.5; // 7.5%

            // When
            InterestRate interestRate = InterestRate.ofPercentage(percentage);

            // Then
            assertThat(interestRate.getPercentage()).isEqualByComparingTo(BigDecimal.valueOf(7.5));
            assertThat(interestRate.getAnnualRate()).isEqualTo(new BigDecimal("0.075"));
        }

        @Test
        @DisplayName("Should create zero interest rate")
        void shouldCreateZeroInterestRate() {
            // When
            InterestRate zeroRate = InterestRate.of(BigDecimal.ZERO);

            // Then
            assertThat(zeroRate.getAnnualRate()).isEqualTo(BigDecimal.ZERO);
            assertThat(zeroRate.getPercentage()).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should create maximum allowed interest rate")
        void shouldCreateMaximumAllowedInterestRate() {
            // Given
            BigDecimal maxRate = new BigDecimal("1.0"); // 100%

            // When
            InterestRate maxInterestRate = InterestRate.of(maxRate);

            // Then
            assertThat(maxInterestRate.getAnnualRate()).isEqualTo(maxRate);
            assertThat(maxInterestRate.getPercentage()).isEqualByComparingTo(new BigDecimal("100"));
        }
    }

    @Nested
    @DisplayName("Interest Rate Validation")
    class InterestRateValidationTests {

        @Test
        @DisplayName("Should throw exception for null annual rate")
        void shouldThrowExceptionForNullAnnualRate() {
            // When & Then
            assertThatThrownBy(() -> InterestRate.of(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Annual rate cannot be null");
        }

        @Test
        @DisplayName("Should throw exception for negative annual rate")
        void shouldThrowExceptionForNegativeAnnualRate() {
            // Given
            BigDecimal negativeRate = new BigDecimal("-0.01");

            // When & Then
            assertThatThrownBy(() -> InterestRate.of(negativeRate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Interest rate cannot be negative");
        }

        @Test
        @DisplayName("Should throw exception for rate exceeding 100%")
        void shouldThrowExceptionForRateExceeding100Percent() {
            // Given
            BigDecimal excessiveRate = new BigDecimal("1.01"); // 101%

            // When & Then
            assertThatThrownBy(() -> InterestRate.of(excessiveRate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Interest rate cannot exceed 100%");
        }

        @Test
        @DisplayName("Should throw exception for negative percentage")
        void shouldThrowExceptionForNegativePercentage() {
            // When & Then
            assertThatThrownBy(() -> InterestRate.ofPercentage(-1.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Interest rate cannot be negative");
        }

        @Test
        @DisplayName("Should throw exception for percentage exceeding 100%")
        void shouldThrowExceptionForPercentageExceeding100() {
            // When & Then
            assertThatThrownBy(() -> InterestRate.ofPercentage(101.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Interest rate cannot exceed 100%");
        }
    }

    @Nested
    @DisplayName("Interest Rate Calculations")
    class InterestRateCalculationsTests {

        @Test
        @DisplayName("Should calculate percentage correctly")
        void shouldCalculatePercentageCorrectly() {
            // Given
            InterestRate rate5Percent = InterestRate.of(new BigDecimal("0.05"));
            InterestRate rate12Point5Percent = InterestRate.of(new BigDecimal("0.125"));

            // When & Then
            assertThat(rate5Percent.getPercentage()).isEqualTo(new BigDecimal("5.00"));
            assertThat(rate12Point5Percent.getPercentage()).isEqualByComparingTo(new BigDecimal("12.50"));
        }

        @Test
        @DisplayName("Should calculate monthly rate correctly")
        void shouldCalculateMonthlyRateCorrectly() {
            // Given
            InterestRate annualRate12Percent = InterestRate.ofPercentage(12.0); // 12% annually

            // When
            BigDecimal monthlyRate = annualRate12Percent.getMonthlyRate();

            // Then
            // 12% / 12 months = 1% monthly = 0.01
            assertThat(monthlyRate).isEqualByComparingTo(new BigDecimal("0.01"));
        }

        @Test
        @DisplayName("Should calculate monthly rate with high precision")
        void shouldCalculateMonthlyRateWithHighPrecision() {
            // Given
            InterestRate annualRate7Point25Percent = InterestRate.ofPercentage(7.25); // 7.25% annually

            // When
            BigDecimal monthlyRate = annualRate7Point25Percent.getMonthlyRate();

            // Then
            // 7.25% / 12 months = 0.6041666667% monthly
            BigDecimal expectedMonthlyRate = new BigDecimal("0.0060416667").setScale(10, BigDecimal.ROUND_HALF_UP);
            assertThat(monthlyRate).isEqualByComparingTo(expectedMonthlyRate);
        }

        @Test
        @DisplayName("Should handle zero rate monthly calculation")
        void shouldHandleZeroRateMonthlyCalculation() {
            // Given
            InterestRate zeroRate = InterestRate.of(BigDecimal.ZERO);

            // When
            BigDecimal monthlyRate = zeroRate.getMonthlyRate();

            // Then
            assertThat(monthlyRate).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Interest Rate Comparisons")
    class InterestRateComparisonsTests {

        @Test
        @DisplayName("Should correctly identify lower interest rate")
        void shouldCorrectlyIdentifyLowerInterestRate() {
            // Given
            InterestRate rate3Percent = InterestRate.ofPercentage(3.0);
            InterestRate rate5Percent = InterestRate.ofPercentage(5.0);

            // When & Then
            assertThat(rate3Percent.isLowerThan(rate5Percent)).isTrue();
            assertThat(rate5Percent.isLowerThan(rate3Percent)).isFalse();
        }

        @Test
        @DisplayName("Should correctly identify higher interest rate")
        void shouldCorrectlyIdentifyHigherInterestRate() {
            // Given
            InterestRate rate7Percent = InterestRate.ofPercentage(7.0);
            InterestRate rate4Percent = InterestRate.ofPercentage(4.0);

            // When & Then
            assertThat(rate7Percent.isHigherThan(rate4Percent)).isTrue();
            assertThat(rate4Percent.isHigherThan(rate7Percent)).isFalse();
        }

        @Test
        @DisplayName("Should return false for equal rates in comparison")
        void shouldReturnFalseForEqualRatesInComparison() {
            // Given
            InterestRate rate6Percent1 = InterestRate.ofPercentage(6.0);
            InterestRate rate6Percent2 = InterestRate.ofPercentage(6.0);

            // When & Then
            assertThat(rate6Percent1.isLowerThan(rate6Percent2)).isFalse();
            assertThat(rate6Percent1.isHigherThan(rate6Percent2)).isFalse();
        }

        @Test
        @DisplayName("Should handle precision in comparisons")
        void shouldHandlePrecisionInComparisons() {
            // Given
            InterestRate rate1 = InterestRate.of(new BigDecimal("0.0500")); // 5.00%
            InterestRate rate2 = InterestRate.of(new BigDecimal("0.0501")); // 5.01%

            // When & Then
            assertThat(rate1.isLowerThan(rate2)).isTrue();
            assertThat(rate2.isHigherThan(rate1)).isTrue();
        }
    }

    @Nested
    @DisplayName("Interest Rate Arithmetic")
    class InterestRateArithmeticTests {

        @Test
        @DisplayName("Should add interest rates correctly")
        void shouldAddInterestRatesCorrectly() {
            // Given
            InterestRate rate3Percent = InterestRate.ofPercentage(3.0);
            InterestRate rate2Percent = InterestRate.ofPercentage(2.0);

            // When
            InterestRate sum = rate3Percent.add(rate2Percent);

            // Then
            assertThat(sum.getPercentage()).isEqualTo(new BigDecimal("5.00"));
        }

        @Test
        @DisplayName("Should subtract interest rates correctly")
        void shouldSubtractInterestRatesCorrectly() {
            // Given
            InterestRate rate7Percent = InterestRate.ofPercentage(7.0);
            InterestRate rate3Percent = InterestRate.ofPercentage(3.0);

            // When
            InterestRate difference = rate7Percent.subtract(rate3Percent);

            // Then
            assertThat(difference.getPercentage()).isEqualTo(new BigDecimal("4.00"));
        }

        @Test
        @DisplayName("Should handle adding zero rate")
        void shouldHandleAddingZeroRate() {
            // Given
            InterestRate rate5Percent = InterestRate.ofPercentage(5.0);
            InterestRate zeroRate = InterestRate.of(BigDecimal.ZERO);

            // When
            InterestRate sum = rate5Percent.add(zeroRate);

            // Then
            assertThat(sum.getPercentage()).isEqualTo(new BigDecimal("5.00"));
        }

        @Test
        @DisplayName("Should handle subtracting equal rates")
        void shouldHandleSubtractingEqualRates() {
            // Given
            InterestRate rate4Percent1 = InterestRate.ofPercentage(4.0);
            InterestRate rate4Percent2 = InterestRate.ofPercentage(4.0);

            // When
            InterestRate difference = rate4Percent1.subtract(rate4Percent2);

            // Then
            assertThat(difference.getPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should throw exception when subtraction results in negative rate")
        void shouldThrowExceptionWhenSubtractionResultsInNegativeRate() {
            // Given
            InterestRate rate2Percent = InterestRate.ofPercentage(2.0);
            InterestRate rate5Percent = InterestRate.ofPercentage(5.0);

            // When & Then
            assertThatThrownBy(() -> rate2Percent.subtract(rate5Percent))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Result would be negative interest rate");
        }

        @Test
        @DisplayName("Should handle addition exceeding maximum rate")
        void shouldHandleAdditionExceedingMaximumRate() {
            // Given
            InterestRate rate60Percent = InterestRate.ofPercentage(60.0);
            InterestRate rate50Percent = InterestRate.ofPercentage(50.0);

            // When & Then
            assertThatThrownBy(() -> rate60Percent.add(rate50Percent))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Interest rate cannot exceed 100%");
        }
    }

    @Nested
    @DisplayName("Interest Rate Equality")
    class InterestRateEqualityTests {

        @Test
        @DisplayName("Should be equal when rates match")
        void shouldBeEqualWhenRatesMatch() {
            // Given
            InterestRate rate1 = InterestRate.ofPercentage(5.5);
            InterestRate rate2 = InterestRate.ofPercentage(5.5);

            // When & Then
            assertThat(rate1).isEqualTo(rate2);
            assertThat(rate1.hashCode()).isEqualTo(rate2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when rates differ")
        void shouldNotBeEqualWhenRatesDiffer() {
            // Given
            InterestRate rate1 = InterestRate.ofPercentage(5.0);
            InterestRate rate2 = InterestRate.ofPercentage(5.1);

            // When & Then
            assertThat(rate1).isNotEqualTo(rate2);
        }

        @Test
        @DisplayName("Should be equal regardless of creation method")
        void shouldBeEqualRegardlessOfCreationMethod() {
            // Given
            InterestRate rateFromBigDecimal = InterestRate.of(new BigDecimal("0.075"));
            InterestRate rateFromPercentage = InterestRate.ofPercentage(7.5);

            // When & Then
            assertThat(rateFromBigDecimal).isEqualTo(rateFromPercentage);
            assertThat(rateFromBigDecimal.hashCode()).isEqualTo(rateFromPercentage.hashCode());
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            // Given
            InterestRate rate = InterestRate.ofPercentage(5.0);

            // When & Then
            assertThat(rate).isNotEqualTo(null);
        }

        @Test
        @DisplayName("Should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            // Given
            InterestRate rate = InterestRate.ofPercentage(5.0);
            String notRate = "5.0%";

            // When & Then
            assertThat(rate).isNotEqualTo(notRate);
        }
    }

    @Nested
    @DisplayName("Interest Rate String Representation")
    class InterestRateStringRepresentationTests {

        @Test
        @DisplayName("Should format percentage as string correctly")
        void shouldFormatPercentageAsStringCorrectly() {
            // Given
            InterestRate rate5Percent = InterestRate.ofPercentage(5.0);
            InterestRate rate12Point75Percent = InterestRate.ofPercentage(12.75);

            // When & Then
            assertThat(rate5Percent.toString()).isEqualTo("5.00%");
            assertThat(rate12Point75Percent.toString()).isEqualTo("12.75%");
        }

        @Test
        @DisplayName("Should format zero rate as string")
        void shouldFormatZeroRateAsString() {
            // Given
            InterestRate zeroRate = InterestRate.of(BigDecimal.ZERO);

            // When & Then
            assertThat(zeroRate.toString()).isEqualTo("0.00%");
        }

        @Test
        @DisplayName("Should format high precision rates as string")
        void shouldFormatHighPrecisionRatesAsString() {
            // Given
            InterestRate preciseRate = InterestRate.of(new BigDecimal("0.03456789"));

            // When & Then
            assertThat(preciseRate.toString()).isEqualTo("3.46%"); // Rounded to 2 decimal places
        }
    }

    @Nested
    @DisplayName("Interest Rate Banking Scenarios")
    class InterestRateBankingScenariosTests {

        @Test
        @DisplayName("Should handle typical mortgage rates")
        void shouldHandleTypicalMortgageRates() {
            // Given
            double[] mortgageRates = {2.5, 3.0, 3.5, 4.0, 4.5, 5.0, 6.0, 7.0};

            // When & Then
            for (double rate : mortgageRates) {
                InterestRate mortgageRate = InterestRate.ofPercentage(rate);
                assertThat(mortgageRate.getPercentage().doubleValue()).isEqualTo(rate);
                assertThat(mortgageRate.getMonthlyRate()).isGreaterThan(BigDecimal.ZERO);
            }
        }

        @Test
        @DisplayName("Should handle credit card interest rates")
        void shouldHandleCreditCardInterestRates() {
            // Given
            InterestRate creditCardRate = InterestRate.ofPercentage(18.9); // Typical credit card APR

            // When
            BigDecimal monthlyRate = creditCardRate.getMonthlyRate();

            // Then
            assertThat(creditCardRate.getPercentage().setScale(2, java.math.RoundingMode.HALF_UP)).isEqualByComparingTo(new BigDecimal("18.90"));
            assertThat(monthlyRate).isGreaterThan(new BigDecimal("0.01")); // > 1% monthly
        }

        @Test
        @DisplayName("Should handle savings account rates")
        void shouldHandleSavingsAccountRates() {
            // Given
            InterestRate savingsRate = InterestRate.ofPercentage(0.25); // Low savings rate

            // When
            BigDecimal monthlyRate = savingsRate.getMonthlyRate();

            // Then
            assertThat(savingsRate.getPercentage()).isEqualByComparingTo(new BigDecimal("0.25"));
            assertThat(monthlyRate).isLessThan(new BigDecimal("0.01")); // < 1% monthly
        }

        @Test
        @DisplayName("Should calculate compound scenarios")
        void shouldCalculateCompoundScenarios() {
            // Given
            InterestRate baseRate = InterestRate.ofPercentage(5.0);
            InterestRate riskAdjustment = InterestRate.ofPercentage(1.5);

            // When
            InterestRate totalRate = baseRate.add(riskAdjustment);

            // Then
            assertThat(totalRate.getPercentage()).isEqualByComparingTo(new BigDecimal("6.50"));
            assertThat(totalRate.isHigherThan(baseRate)).isTrue();
        }
    }

    @Nested
    @DisplayName("Interest Rate Performance")
    class InterestRatePerformanceTests {

        @Test
        @DisplayName("Should create many rates efficiently")
        void shouldCreateManyRatesEfficiently() {
            // Given
            int rateCount = 10000;

            // When
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < rateCount; i++) {
                double percentage = (i % 100) + 0.01; // Rates from 0.01% to 99.01%
                InterestRate.ofPercentage(percentage);
            }
            long endTime = System.currentTimeMillis();

            // Then
            long duration = endTime - startTime;
            assertThat(duration).isLessThan(1000); // Should complete within 1 second
        }

        @Test
        @DisplayName("Should perform calculations efficiently")
        void shouldPerformCalculationsEfficiently() {
            // Given
            InterestRate rate = InterestRate.ofPercentage(5.75);
            int calculationCount = 100000;

            // When
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < calculationCount; i++) {
                rate.getMonthlyRate();
                rate.getPercentage();
                rate.toString();
            }
            long endTime = System.currentTimeMillis();

            // Then
            long duration = endTime - startTime;
            assertThat(duration).isLessThan(500); // Should complete within 500ms
        }
    }
}