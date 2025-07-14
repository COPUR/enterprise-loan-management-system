package com.amanahfi.platform.shared.domain;

import com.amanahfi.shared.domain.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD Tests for Money value object
 * 
 * This test class ensures the Money value object behaves correctly
 * for all Islamic finance and MENAT region monetary operations.
 * 
 * Test Coverage:
 * - Construction and validation
 * - Arithmetic operations
 * - Currency conversions
 * - MENAT region currencies
 * - Islamic finance precision requirements
 * - Error handling and edge cases
 * 
 * @author AmanahFi Development Team
 * @version 1.0.0
 * @since 2024
 */
@DisplayName("Money Value Object Tests")
class MoneyTest {

    @Nested
    @DisplayName("Construction and Validation")
    class ConstructionAndValidation {

        @Test
        @DisplayName("Should create Money with valid amount and currency")
        void shouldCreateMoneyWithValidAmountAndCurrency() {
            // Given
            BigDecimal amount = new BigDecimal("100.50");
            Currency currency = Currency.getInstance("AED");

            // When
            Money money = new Money(amount, currency);

            // Then
            assertNotNull(money);
            assertEquals(amount.setScale(2), money.getAmount());
            assertEquals(currency, money.getCurrency());
            assertEquals("AED", money.getCurrencyCode());
        }

        @Test
        @DisplayName("Should throw exception when amount is null")
        void shouldThrowExceptionWhenAmountIsNull() {
            // Given
            Currency currency = Currency.getInstance("AED");

            // When & Then
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Money(null, currency)
            );

            assertEquals("Amount cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when currency is null")
        void shouldThrowExceptionWhenCurrencyIsNull() {
            // Given
            BigDecimal amount = new BigDecimal("100.00");

            // When & Then
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Money(amount, (Currency) null)
            );

            assertEquals("Currency cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should create Money with string currency code")
        void shouldCreateMoneyWithStringCurrencyCode() {
            // Given
            BigDecimal amount = new BigDecimal("250.75");
            String currencyCode = "SAR";

            // When
            Money money = new Money(amount, currencyCode);

            // Then
            assertEquals(amount.setScale(2), money.getAmount());
            assertEquals("SAR", money.getCurrencyCode());
        }

        @Test
        @DisplayName("Should create Money with string amount")
        void shouldCreateMoneyWithStringAmount() {
            // Given
            String amount = "999.99";
            Currency currency = Currency.getInstance("USD");

            // When
            Money money = new Money(amount, currency);

            // Then
            assertEquals(new BigDecimal("999.99"), money.getAmount());
            assertEquals(currency, money.getCurrency());
        }
    }

    @Nested
    @DisplayName("MENAT Region Currency Factory Methods")
    class MenatRegionCurrencyFactoryMethods {

        @Test
        @DisplayName("Should create AED Money using factory method")
        void shouldCreateAedMoneyUsingFactoryMethod() {
            // Given
            BigDecimal amount = new BigDecimal("1000.00");

            // When
            Money money = Money.aed(amount);

            // Then
            assertEquals(amount, money.getAmount());
            assertEquals("AED", money.getCurrencyCode());
        }

        @Test
        @DisplayName("Should create SAR Money using factory method")
        void shouldCreateSarMoneyUsingFactoryMethod() {
            // Given
            BigDecimal amount = new BigDecimal("500.00");

            // When
            Money money = Money.sar(amount);

            // Then
            assertEquals(amount, money.getAmount());
            assertEquals("SAR", money.getCurrencyCode());
        }

        @Test
        @DisplayName("Should create TRY Money using factory method")
        void shouldCreateTryMoneyUsingFactoryMethod() {
            // Given
            BigDecimal amount = new BigDecimal("2500.00");

            // When
            Money money = Money.tryLira(amount);

            // Then
            assertEquals(amount, money.getAmount());
            assertEquals("TRY", money.getCurrencyCode());
        }

        @Test
        @DisplayName("Should create PKR Money using factory method")
        void shouldCreatePkrMoneyUsingFactoryMethod() {
            // Given
            BigDecimal amount = new BigDecimal("50000.00");

            // When
            Money money = Money.pkr(amount);

            // Then
            assertEquals(amount, money.getAmount());
            assertEquals("PKR", money.getCurrencyCode());
        }

        @Test
        @DisplayName("Should create zero AED Money")
        void shouldCreateZeroAedMoney() {
            // When
            Money money = Money.zeroAed();

            // Then
            assertEquals(BigDecimal.ZERO.setScale(2), money.getAmount());
            assertEquals("AED", money.getCurrencyCode());
            assertTrue(money.isZero());
        }
    }

    @Nested
    @DisplayName("Arithmetic Operations")
    class ArithmeticOperations {

        @Test
        @DisplayName("Should add two Money amounts with same currency")
        void shouldAddTwoMoneyAmountsWithSameCurrency() {
            // Given
            Money money1 = Money.aed(new BigDecimal("100.50"));
            Money money2 = Money.aed(new BigDecimal("200.25"));

            // When
            Money result = money1.add(money2);

            // Then
            assertEquals(new BigDecimal("300.75"), result.getAmount());
            assertEquals("AED", result.getCurrencyCode());
        }

        @Test
        @DisplayName("Should subtract two Money amounts with same currency")
        void shouldSubtractTwoMoneyAmountsWithSameCurrency() {
            // Given
            Money money1 = Money.aed(new BigDecimal("300.75"));
            Money money2 = Money.aed(new BigDecimal("100.25"));

            // When
            Money result = money1.subtract(money2);

            // Then
            assertEquals(new BigDecimal("200.50"), result.getAmount());
            assertEquals("AED", result.getCurrencyCode());
        }

        @Test
        @DisplayName("Should multiply Money by BigDecimal")
        void shouldMultiplyMoneyByBigDecimal() {
            // Given
            Money money = Money.aed(new BigDecimal("100.00"));
            BigDecimal multiplier = new BigDecimal("1.15"); // 15% markup for Murabaha

            // When
            Money result = money.multiply(multiplier);

            // Then
            assertEquals(new BigDecimal("115.00"), result.getAmount());
            assertEquals("AED", result.getCurrencyCode());
        }

        @Test
        @DisplayName("Should divide Money by BigDecimal")
        void shouldDivideMoneyByBigDecimal() {
            // Given
            Money money = Money.aed(new BigDecimal("230.00"));
            BigDecimal divisor = new BigDecimal("2");

            // When
            Money result = money.divide(divisor);

            // Then
            assertEquals(new BigDecimal("115.00"), result.getAmount());
            assertEquals("AED", result.getCurrencyCode());
        }

        @Test
        @DisplayName("Should throw exception when adding different currencies")
        void shouldThrowExceptionWhenAddingDifferentCurrencies() {
            // Given
            Money aedMoney = Money.aed(new BigDecimal("100.00"));
            Money usdMoney = Money.usd(new BigDecimal("100.00"));

            // When & Then
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> aedMoney.add(usdMoney)
            );

            assertTrue(exception.getMessage().contains("Currency mismatch"));
        }

        @Test
        @DisplayName("Should throw exception when dividing by zero")
        void shouldThrowExceptionWhenDividingByZero() {
            // Given
            Money money = Money.aed(new BigDecimal("100.00"));
            BigDecimal zero = BigDecimal.ZERO;

            // When & Then
            ArithmeticException exception = assertThrows(
                ArithmeticException.class,
                () -> money.divide(zero)
            );

            assertEquals("Cannot divide by zero", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Comparison Operations")
    class ComparisonOperations {

        @ParameterizedTest
        @CsvSource({
            "100.00, 50.00, true",
            "100.00, 100.00, false",
            "50.00, 100.00, false"
        })
        @DisplayName("Should compare Money amounts correctly for isGreaterThan")
        void shouldCompareMoneyAmountsCorrectlyForIsGreaterThan(String amount1, String amount2, boolean expected) {
            // Given
            Money money1 = Money.aed(new BigDecimal(amount1));
            Money money2 = Money.aed(new BigDecimal(amount2));

            // When
            boolean result = money1.isGreaterThan(money2);

            // Then
            assertEquals(expected, result);
        }

        @ParameterizedTest
        @CsvSource({
            "50.00, 100.00, true",
            "100.00, 100.00, false",
            "100.00, 50.00, false"
        })
        @DisplayName("Should compare Money amounts correctly for isLessThan")
        void shouldCompareMoneyAmountsCorrectlyForIsLessThan(String amount1, String amount2, boolean expected) {
            // Given
            Money money1 = Money.aed(new BigDecimal(amount1));
            Money money2 = Money.aed(new BigDecimal(amount2));

            // When
            boolean result = money1.isLessThan(money2);

            // Then
            assertEquals(expected, result);
        }

        @ParameterizedTest
        @ValueSource(strings = {"0.00", "0", "0.0000"})
        @DisplayName("Should identify zero amounts correctly")
        void shouldIdentifyZeroAmountsCorrectly(String amount) {
            // Given
            Money money = Money.aed(new BigDecimal(amount));

            // When & Then
            assertTrue(money.isZero());
            assertFalse(money.isPositive());
            assertFalse(money.isNegative());
        }

        @Test
        @DisplayName("Should identify positive amounts correctly")
        void shouldIdentifyPositiveAmountsCorrectly() {
            // Given
            Money money = Money.aed(new BigDecimal("100.00"));

            // When & Then
            assertTrue(money.isPositive());
            assertFalse(money.isZero());
            assertFalse(money.isNegative());
        }

        @Test
        @DisplayName("Should identify negative amounts correctly")
        void shouldIdentifyNegativeAmountsCorrectly() {
            // Given
            Money money = Money.aed(new BigDecimal("-100.00"));

            // When & Then
            assertTrue(money.isNegative());
            assertFalse(money.isZero());
            assertFalse(money.isPositive());
        }
    }

    @Nested
    @DisplayName("Utility Operations")
    class UtilityOperations {

        @Test
        @DisplayName("Should return absolute value")
        void shouldReturnAbsoluteValue() {
            // Given
            Money negativeMoney = Money.aed(new BigDecimal("-100.00"));

            // When
            Money result = negativeMoney.abs();

            // Then
            assertEquals(new BigDecimal("100.00"), result.getAmount());
            assertTrue(result.isPositive());
        }

        @Test
        @DisplayName("Should return negated value")
        void shouldReturnNegatedValue() {
            // Given
            Money positiveMoney = Money.aed(new BigDecimal("100.00"));

            // When
            Money result = positiveMoney.negate();

            // Then
            assertEquals(new BigDecimal("-100.00"), result.getAmount());
            assertTrue(result.isNegative());
        }
    }

    @Nested
    @DisplayName("String Representation")
    class StringRepresentation {

        @Test
        @DisplayName("Should format Money to string correctly")
        void shouldFormatMoneyToStringCorrectly() {
            // Given
            Money money = Money.aed(new BigDecimal("1250.75"));

            // When
            String result = money.toString();

            // Then
            assertEquals("1250.75 AED", result);
        }

        @Test
        @DisplayName("Should format Money with currency symbol")
        void shouldFormatMoneyWithCurrencySymbol() {
            // Given
            Money money = Money.usd(new BigDecimal("1000.00"));

            // When
            String result = money.toFormattedString();

            // Then
            assertTrue(result.contains("1000.00"));
            assertTrue(result.contains("$") || result.contains("USD"));
        }
    }

    @Nested
    @DisplayName("Equality and Hashing")
    class EqualityAndHashing {

        @Test
        @DisplayName("Should be equal when amount and currency are same")
        void shouldBeEqualWhenAmountAndCurrencyAreSame() {
            // Given
            Money money1 = Money.aed(new BigDecimal("100.00"));
            Money money2 = Money.aed(new BigDecimal("100.00"));

            // When & Then
            assertEquals(money1, money2);
            assertEquals(money1.hashCode(), money2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when currencies differ")
        void shouldNotBeEqualWhenCurrenciesDiffer() {
            // Given
            Money aedMoney = Money.aed(new BigDecimal("100.00"));
            Money usdMoney = Money.usd(new BigDecimal("100.00"));

            // When & Then
            assertNotEquals(aedMoney, usdMoney);
        }

        @Test
        @DisplayName("Should not be equal when amounts differ")
        void shouldNotBeEqualWhenAmountsDiffer() {
            // Given
            Money money1 = Money.aed(new BigDecimal("100.00"));
            Money money2 = Money.aed(new BigDecimal("200.00"));

            // When & Then
            assertNotEquals(money1, money2);
        }
    }
}