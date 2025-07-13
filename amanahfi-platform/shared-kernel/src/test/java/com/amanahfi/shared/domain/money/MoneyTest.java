package com.amanahfi.shared.domain.money;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Test Suite for Money Value Object
 * Following Islamic finance principles - precise monetary calculations
 */
@DisplayName("Money Value Object Tests")
class MoneyTest {

    @Test
    @DisplayName("Should create Money with valid amount and currency")
    void shouldCreateMoneyWithValidAmountAndCurrency() {
        // Given
        BigDecimal amount = new BigDecimal("1000.00");
        String currency = "AED";

        // When
        Money money = Money.of(amount, currency);

        // Then
        assertThat(money.getAmount()).isEqualByComparingTo(amount);
        assertThat(money.getCurrency()).isEqualTo(currency);
        assertThat(money.toString()).isEqualTo("AED 1000.00");
    }

    @Test
    @DisplayName("Should create zero Money")
    void shouldCreateZeroMoney() {
        // When
        Money zero = Money.zero("AED");

        // Then
        assertThat(zero.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(zero.getCurrency()).isEqualTo("AED");
        assertThat(zero.isZero()).isTrue();
    }

    @Test
    @DisplayName("Should add two Money objects with same currency")
    void shouldAddTwoMoneyObjectsWithSameCurrency() {
        // Given
        Money money1 = Money.of(new BigDecimal("100.00"), "AED");
        Money money2 = Money.of(new BigDecimal("50.00"), "AED");

        // When
        Money result = money1.add(money2);

        // Then
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(result.getCurrency()).isEqualTo("AED");
    }

    @Test
    @DisplayName("Should subtract two Money objects with same currency")
    void shouldSubtractTwoMoneyObjectsWithSameCurrency() {
        // Given
        Money money1 = Money.of(new BigDecimal("100.00"), "AED");
        Money money2 = Money.of(new BigDecimal("30.00"), "AED");

        // When
        Money result = money1.subtract(money2);

        // Then
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("70.00"));
        assertThat(result.getCurrency()).isEqualTo("AED");
    }

    @Test
    @DisplayName("Should multiply Money by scalar")
    void shouldMultiplyMoneyByScalar() {
        // Given
        Money money = Money.of(new BigDecimal("100.00"), "AED");
        BigDecimal multiplier = new BigDecimal("1.5");

        // When
        Money result = money.multiply(multiplier);

        // Then
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(result.getCurrency()).isEqualTo("AED");
    }

    @Test
    @DisplayName("Should divide Money by scalar")
    void shouldDivideMoneyByScalar() {
        // Given
        Money money = Money.of(new BigDecimal("100.00"), "AED");
        BigDecimal divisor = new BigDecimal("4");

        // When
        Money result = money.divide(divisor);

        // Then
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(result.getCurrency()).isEqualTo("AED");
    }

    @Test
    @DisplayName("Should throw exception when adding Money with different currencies")
    void shouldThrowExceptionWhenAddingMoneyWithDifferentCurrencies() {
        // Given
        Money aedMoney = Money.of(new BigDecimal("100.00"), "AED");
        Money usdMoney = Money.of(new BigDecimal("50.00"), "USD");

        // When & Then
        assertThatThrownBy(() -> aedMoney.add(usdMoney))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency mismatch");
    }

    @Test
    @DisplayName("Should throw exception for null amount")
    void shouldThrowExceptionForNullAmount() {
        // When & Then
        assertThatThrownBy(() -> Money.of((BigDecimal) null, "AED"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount cannot be null");
    }

    @Test
    @DisplayName("Should throw exception for null or empty currency")
    void shouldThrowExceptionForNullOrEmptyCurrency() {
        BigDecimal amount = new BigDecimal("100.00");

        // When & Then
        assertThatThrownBy(() -> Money.of(amount, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency cannot be null or empty");

        assertThatThrownBy(() -> Money.of(amount, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency cannot be null or empty");
    }

    @Test
    @DisplayName("Should throw exception for negative amounts")
    void shouldThrowExceptionForNegativeAmounts() {
        // Given
        BigDecimal negativeAmount = new BigDecimal("-100.00");

        // When & Then
        assertThatThrownBy(() -> Money.of(negativeAmount, "AED"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount cannot be negative in Islamic finance");
    }

    @ParameterizedTest
    @ValueSource(strings = {"AED", "USD", "EUR", "SAR", "QAR", "KWD", "BHD"})
    @DisplayName("Should support major MENAT currencies")
    void shouldSupportMajorMenatCurrencies(String currency) {
        // Given
        BigDecimal amount = new BigDecimal("1000.00");

        // When
        Money money = Money.of(amount, currency);

        // Then
        assertThat(money.getCurrency()).isEqualTo(currency);
        assertThat(money.getAmount()).isEqualByComparingTo(amount);
    }

    @Test
    @DisplayName("Should compare Money objects correctly")
    void shouldCompareMoneyObjectsCorrectly() {
        // Given
        Money money1 = Money.of(new BigDecimal("100.00"), "AED");
        Money money2 = Money.of(new BigDecimal("50.00"), "AED");
        Money money3 = Money.of(new BigDecimal("100.00"), "AED");

        // Then
        assertThat(money1.isGreaterThan(money2)).isTrue();
        assertThat(money2.isLessThan(money1)).isTrue();
        assertThat(money1.equals(money3)).isTrue();
        assertThat(money1.hashCode()).isEqualTo(money3.hashCode());
    }

    @Test
    @DisplayName("Should calculate percentage for Islamic profit sharing")
    void shouldCalculatePercentageForIslamicProfitSharing() {
        // Given
        Money principalAmount = Money.of(new BigDecimal("10000.00"), "AED");
        BigDecimal profitRate = new BigDecimal("0.05"); // 5% profit rate

        // When
        Money profit = principalAmount.multiply(profitRate);

        // Then
        assertThat(profit.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(profit.getCurrency()).isEqualTo("AED");
    }

    @Test
    @DisplayName("Should handle precise decimal calculations for Islamic compliance")
    void shouldHandlePreciseDecimalCalculationsForIslamicCompliance() {
        // Given - Test precision for Islamic finance calculations
        Money amount = Money.of(new BigDecimal("1000.333"), "AED");
        BigDecimal rate = new BigDecimal("0.033333");

        // When
        Money result = amount.multiply(rate);

        // Then - Should maintain precision for compliance
        assertThat(result.getAmount().scale()).isEqualTo(2); // Standard currency scale
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("33.34"));
    }

    @Test
    @DisplayName("Should create Money from string representation")
    void shouldCreateMoneyFromStringRepresentation() {
        // When
        Money money = Money.parse("AED 1500.75");

        // Then
        assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("1500.75"));
        assertThat(money.getCurrency()).isEqualTo("AED");
    }

    @Test
    @DisplayName("Should validate Islamic finance business rules")
    void shouldValidateIslamicFinanceBusinessRules() {
        // Given
        Money amount = Money.of(new BigDecimal("1000.00"), "AED");

        // Then - Islamic finance principles
        assertThat(amount.isPositive()).isTrue();
        assertThat(amount.isHalal()).isTrue(); // No interest-based calculations
        assertThat(amount.canBeUsedForMurabaha()).isTrue();
    }
}