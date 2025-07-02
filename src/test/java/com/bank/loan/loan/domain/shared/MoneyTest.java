package com.bank.loanmanagement.loan.domain.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive TDD tests for Money value object
 * Following banking precision requirements and business rules
 */
@DisplayName("💰 Money Value Object Tests")
class MoneyTest {

    @Nested
    @DisplayName("Creation and Validation")
    class CreationTests {

        @Test
        @DisplayName("Should create money with BigDecimal amount and currency")
        void shouldCreateMoneyWithBigDecimalAndCurrency() {
            // Given
            BigDecimal amount = new BigDecimal("100.50");
            String currency = "USD";

            // When
            Money money = Money.of(amount, currency);

            // Then
            assertThat(money.getAmount()).isEqualTo(amount.setScale(2, java.math.RoundingMode.HALF_UP));
            assertThat(money.getCurrency()).isEqualTo(currency);
        }

        @Test
        @DisplayName("Should create money with string amount and currency")
        void shouldCreateMoneyWithStringAmountAndCurrency() {
            // Given
            String amount = "250.75";
            String currency = "EUR";

            // When
            Money money = new Money(amount, currency);

            // Then
            assertThat(money.getAmount()).isEqualTo(new BigDecimal("250.75"));
            assertThat(money.getCurrency()).isEqualTo(currency);
        }

        @Test
        @DisplayName("Should create zero money with specified currency")
        void shouldCreateZeroMoney() {
            // Given
            String currency = "GBP";

            // When
            Money money = Money.zero(currency);

            // Then
            assertThat(money.getAmount()).isEqualTo(BigDecimal.ZERO);
            assertThat(money.getCurrency()).isEqualTo(currency);
            assertThat(money.isZero()).isTrue();
        }

        @Test
        @DisplayName("Should automatically round to 2 decimal places")
        void shouldRoundToTwoDecimalPlaces() {
            // Given
            BigDecimal amount = new BigDecimal("100.999");

            // When
            Money money = Money.of(amount, "USD");

            // Then
            assertThat(money.getAmount()).isEqualTo(new BigDecimal("101.00"));
        }
    }

    @Nested
    @DisplayName("Arithmetic Operations")
    class ArithmeticTests {

        @Test
        @DisplayName("Should add two money amounts with same currency")
        void shouldAddSameCurrencyAmounts() {
            // Given
            Money money1 = Money.of(new BigDecimal("100.50"), "USD");
            Money money2 = Money.of(new BigDecimal("50.25"), "USD");

            // When
            Money result = money1.add(money2);

            // Then
            assertThat(result.getAmount()).isEqualTo(new BigDecimal("150.75"));
            assertThat(result.getCurrency()).isEqualTo("USD");
        }

        @Test
        @DisplayName("Should subtract two money amounts with same currency")
        void shouldSubtractSameCurrencyAmounts() {
            // Given
            Money money1 = Money.of(new BigDecimal("100.50"), "USD");
            Money money2 = Money.of(new BigDecimal("50.25"), "USD");

            // When
            Money result = money1.subtract(money2);

            // Then
            assertThat(result.getAmount()).isEqualTo(new BigDecimal("50.25"));
            assertThat(result.getCurrency()).isEqualTo("USD");
        }

        @Test
        @DisplayName("Should multiply money by a factor")
        void shouldMultiplyByFactor() {
            // Given
            Money money = Money.of(new BigDecimal("100.00"), "USD");
            BigDecimal multiplier = new BigDecimal("1.5");

            // When
            Money result = money.multiply(multiplier);

            // Then
            assertThat(result.getAmount()).isEqualTo(new BigDecimal("150.00"));
            assertThat(result.getCurrency()).isEqualTo("USD");
        }

        @Test
        @DisplayName("Should divide money by a divisor")
        void shouldDivideByDivisor() {
            // Given
            Money money = Money.of(new BigDecimal("100.00"), "USD");
            BigDecimal divisor = new BigDecimal("4");

            // When
            Money result = money.divide(divisor);

            // Then
            assertThat(result.getAmount()).isEqualTo(new BigDecimal("25.00"));
            assertThat(result.getCurrency()).isEqualTo("USD");
        }

        @Test
        @DisplayName("Should throw exception when adding different currencies")
        void shouldThrowExceptionWhenAddingDifferentCurrencies() {
            // Given
            Money usdMoney = Money.of(new BigDecimal("100.00"), "USD");
            Money eurMoney = Money.of(new BigDecimal("50.00"), "EUR");

            // When & Then
            assertThatThrownBy(() -> usdMoney.add(eurMoney))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot operate on different currencies");
        }

        @Test
        @DisplayName("Should throw exception when subtracting different currencies")
        void shouldThrowExceptionWhenSubtractingDifferentCurrencies() {
            // Given
            Money usdMoney = Money.of(new BigDecimal("100.00"), "USD");
            Money eurMoney = Money.of(new BigDecimal("50.00"), "EUR");

            // When & Then
            assertThatThrownBy(() -> usdMoney.subtract(eurMoney))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot operate on different currencies");
        }
    }

    @Nested
    @DisplayName("Comparison Operations")
    class ComparisonTests {

        @Test
        @DisplayName("Should correctly identify when money is greater than another")
        void shouldIdentifyGreaterThan() {
            // Given
            Money larger = Money.of(new BigDecimal("100.00"), "USD");
            Money smaller = Money.of(new BigDecimal("50.00"), "USD");

            // When & Then
            assertThat(larger.isGreaterThan(smaller)).isTrue();
            assertThat(smaller.isGreaterThan(larger)).isFalse();
        }

        @Test
        @DisplayName("Should correctly identify when money is less than another")
        void shouldIdentifyLessThan() {
            // Given
            Money larger = Money.of(new BigDecimal("100.00"), "USD");
            Money smaller = Money.of(new BigDecimal("50.00"), "USD");

            // When & Then
            assertThat(smaller.isLessThan(larger)).isTrue();
            assertThat(larger.isLessThan(smaller)).isFalse();
        }

        @Test
        @DisplayName("Should identify zero amounts")
        void shouldIdentifyZeroAmounts() {
            // Given
            Money zero = Money.zero("USD");
            Money nonZero = Money.of(new BigDecimal("0.01"), "USD");

            // When & Then
            assertThat(zero.isZero()).isTrue();
            assertThat(nonZero.isZero()).isFalse();
        }

        @Test
        @DisplayName("Should identify positive amounts")
        void shouldIdentifyPositiveAmounts() {
            // Given
            Money positive = Money.of(new BigDecimal("100.00"), "USD");
            Money zero = Money.zero("USD");
            Money negative = Money.of(new BigDecimal("-50.00"), "USD");

            // When & Then
            assertThat(positive.isPositive()).isTrue();
            assertThat(zero.isPositive()).isFalse();
            assertThat(negative.isPositive()).isFalse();
        }

        @Test
        @DisplayName("Should identify negative amounts")
        void shouldIdentifyNegativeAmounts() {
            // Given
            Money positive = Money.of(new BigDecimal("100.00"), "USD");
            Money zero = Money.zero("USD");
            Money negative = Money.of(new BigDecimal("-50.00"), "USD");

            // When & Then
            assertThat(negative.isNegative()).isTrue();
            assertThat(zero.isNegative()).isFalse();
            assertThat(positive.isNegative()).isFalse();
        }

        @Test
        @DisplayName("Should throw exception when comparing different currencies")
        void shouldThrowExceptionWhenComparingDifferentCurrencies() {
            // Given
            Money usdMoney = Money.of(new BigDecimal("100.00"), "USD");
            Money eurMoney = Money.of(new BigDecimal("50.00"), "EUR");

            // When & Then
            assertThatThrownBy(() -> usdMoney.isGreaterThan(eurMoney))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot operate on different currencies");
        }
    }

    @Nested
    @DisplayName("Banking Precision Tests")
    class PrecisionTests {

        @ParameterizedTest
        @ValueSource(strings = {"123.456", "123.454", "123.455", "123.999"})
        @DisplayName("Should handle precise decimal rounding for banking calculations")
        void shouldHandlePreciseDecimalRounding(String amount) {
            // Given
            BigDecimal originalAmount = new BigDecimal(amount);

            // When
            Money money = Money.of(originalAmount, "USD");

            // Then
            assertThat(money.getAmount().scale()).isEqualTo(2);
            assertThat(money.getAmount()).isEqualTo(originalAmount.setScale(2, java.math.RoundingMode.HALF_UP));
        }

        @Test
        @DisplayName("Should maintain precision in complex calculations")
        void shouldMaintainPrecisionInComplexCalculations() {
            // Given - Loan calculation scenario
            Money loanAmount = Money.of(new BigDecimal("10000.00"), "USD");
            BigDecimal interestRate = new BigDecimal("0.0725"); // 7.25%
            BigDecimal months = new BigDecimal("12");

            // When - Monthly interest calculation
            Money monthlyInterest = loanAmount.multiply(interestRate).divide(months);

            // Then
            assertThat(monthlyInterest.getAmount().scale()).isEqualTo(2);
            assertThat(monthlyInterest.getAmount()).isEqualTo(new BigDecimal("60.42"));
        }

        @Test
        @DisplayName("Should handle very small amounts correctly")
        void shouldHandleVerySmallAmounts() {
            // Given
            Money smallAmount = Money.of(new BigDecimal("0.01"), "USD");

            // When
            Money doubled = smallAmount.multiply(new BigDecimal("2"));

            // Then
            assertThat(doubled.getAmount()).isEqualTo(new BigDecimal("0.02"));
        }
    }

    @Nested
    @DisplayName("Equality and Hash Code")
    class EqualityTests {

        @Test
        @DisplayName("Should be equal when amount and currency are same")
        void shouldBeEqualWhenAmountAndCurrencyAreSame() {
            // Given
            Money money1 = Money.of(new BigDecimal("100.00"), "USD");
            Money money2 = Money.of(new BigDecimal("100.00"), "USD");

            // When & Then
            assertThat(money1).isEqualTo(money2);
            assertThat(money1.hashCode()).isEqualTo(money2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when amounts differ")
        void shouldNotBeEqualWhenAmountsDiffer() {
            // Given
            Money money1 = Money.of(new BigDecimal("100.00"), "USD");
            Money money2 = Money.of(new BigDecimal("100.01"), "USD");

            // When & Then
            assertThat(money1).isNotEqualTo(money2);
        }

        @Test
        @DisplayName("Should not be equal when currencies differ")
        void shouldNotBeEqualWhenCurrenciesDiffer() {
            // Given
            Money money1 = Money.of(new BigDecimal("100.00"), "USD");
            Money money2 = Money.of(new BigDecimal("100.00"), "EUR");

            // When & Then
            assertThat(money1).isNotEqualTo(money2);
        }
    }

    @Nested
    @DisplayName("String Representation")
    class StringRepresentationTests {

        @Test
        @DisplayName("Should format as currency and amount")
        void shouldFormatAsCurrencyAndAmount() {
            // Given
            Money money = Money.of(new BigDecimal("1234.56"), "USD");

            // When
            String formatted = money.toString();

            // Then
            assertThat(formatted).isEqualTo("USD 1234.56");
        }

        @Test
        @DisplayName("Should format zero amount correctly")
        void shouldFormatZeroAmountCorrectly() {
            // Given
            Money money = Money.zero("EUR");

            // When
            String formatted = money.toString();

            // Then
            assertThat(formatted).isEqualTo("EUR 0");
        }
    }
}