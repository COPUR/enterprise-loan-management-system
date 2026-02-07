package com.amanahfi.platform.shared.domain;

import com.amanahfi.shared.domain.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Money Value Object Tests")
class MoneyTest {

    @Test
    @DisplayName("Creates money from decimal amount and currency")
    void createsMoneyFromDecimalAmountAndCurrency() {
        Money money = Money.of(new BigDecimal("100.50"), "AED");

        assertEquals(new BigDecimal("100.50"), money.getAmount());
        assertEquals("AED", money.getCurrencyCode());
        assertTrue(money.isPositive());
        assertTrue(money.isHalal());
    }

    @Test
    @DisplayName("Creates money from string amount and currency")
    void createsMoneyFromStringAmountAndCurrency() {
        Money money = Money.of("250.75", "sar");

        assertEquals(new BigDecimal("250.75"), money.getAmount());
        assertEquals("SAR", money.getCurrencyCode());
    }

    @Test
    @DisplayName("Rejects negative amount")
    void rejectsNegativeAmount() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Money.of(new BigDecimal("-1.00"), "AED")
        );
        assertEquals("Amount cannot be negative in Islamic finance", ex.getMessage());
    }

    @Test
    @DisplayName("Rejects null amount")
    void rejectsNullAmount() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Money.of((BigDecimal) null, "AED")
        );
        assertEquals("Amount cannot be null", ex.getMessage());
    }

    @Test
    @DisplayName("Rejects null currency")
    void rejectsNullCurrency() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Money.of(new BigDecimal("1.00"), null)
        );
        assertEquals("Currency cannot be null or empty", ex.getMessage());
    }

    @Test
    @DisplayName("Supports MENAT currency factories")
    void supportsMenatFactories() {
        assertEquals("AED", Money.aed(new BigDecimal("1")).getCurrencyCode());
        assertEquals("SAR", Money.sar(new BigDecimal("1")).getCurrencyCode());
        assertEquals("USD", Money.usd(new BigDecimal("1")).getCurrencyCode());
        assertEquals("TRY", Money.tryLira(new BigDecimal("1")).getCurrencyCode());
        assertEquals("PKR", Money.pkr(new BigDecimal("1")).getCurrencyCode());
    }

    @Test
    @DisplayName("Creates zero money for currency")
    void createsZeroMoneyForCurrency() {
        Money zero = Money.zero("AED");
        assertTrue(zero.isZero());
        assertEquals("AED", zero.getCurrencyCode());
    }

    @Test
    @DisplayName("Parses formatted money string")
    void parsesFormattedMoneyString() {
        Money money = Money.parse("AED 1000.25");

        assertEquals(new BigDecimal("1000.25"), money.getAmount());
        assertEquals("AED", money.getCurrencyCode());
    }

    @Test
    @DisplayName("Adds money values in same currency")
    void addsMoneyValuesInSameCurrency() {
        Money a = Money.aed(new BigDecimal("100.50"));
        Money b = Money.aed(new BigDecimal("200.25"));

        Money result = a.add(b);

        assertEquals(new BigDecimal("300.75"), result.getAmount());
        assertEquals("AED", result.getCurrencyCode());
    }

    @Test
    @DisplayName("Rejects add with different currencies")
    void rejectsAddWithDifferentCurrencies() {
        Money a = Money.aed(new BigDecimal("100.00"));
        Money b = Money.usd(new BigDecimal("100.00"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> a.add(b));
        assertTrue(ex.getMessage().contains("Currency mismatch"));
    }

    @Test
    @DisplayName("Subtracts without going negative")
    void subtractsWithoutGoingNegative() {
        Money a = Money.aed(new BigDecimal("300.00"));
        Money b = Money.aed(new BigDecimal("99.99"));

        Money result = a.subtract(b);

        assertEquals(new BigDecimal("200.01"), result.getAmount());
    }

    @Test
    @DisplayName("Rejects subtraction resulting in negative value")
    void rejectsSubtractionResultingInNegativeValue() {
        Money a = Money.aed(new BigDecimal("100.00"));
        Money b = Money.aed(new BigDecimal("101.00"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> a.subtract(b));
        assertEquals("Subtraction would result in negative amount, which violates Islamic finance principles", ex.getMessage());
    }

    @Test
    @DisplayName("Multiplies by positive scalar")
    void multipliesByPositiveScalar() {
        Money money = Money.aed(new BigDecimal("100.00"));
        Money result = money.multiply(new BigDecimal("1.15"));

        assertEquals(new BigDecimal("115.00"), result.getAmount());
    }

    @Test
    @DisplayName("Divides by positive scalar")
    void dividesByPositiveScalar() {
        Money money = Money.aed(new BigDecimal("230.00"));
        Money result = money.divide(new BigDecimal("2"));

        assertEquals(new BigDecimal("115.00"), result.getAmount());
    }

    @Test
    @DisplayName("Rejects divide by zero")
    void rejectsDivideByZero() {
        Money money = Money.aed(new BigDecimal("100.00"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> money.divide(BigDecimal.ZERO));
        assertEquals("Divisor must be positive in Islamic finance", ex.getMessage());
    }

    @ParameterizedTest
    @CsvSource({
            "100.00, 50.00, true, false",
            "100.00, 100.00, false, false",
            "50.00, 100.00, false, true"
    })
    @DisplayName("Compares money values")
    void comparesMoneyValues(String left, String right, boolean greater, boolean less) {
        Money a = Money.aed(new BigDecimal(left));
        Money b = Money.aed(new BigDecimal(right));

        assertEquals(greater, a.isGreaterThan(b));
        assertEquals(less, a.isLessThan(b));
    }

    @Test
    @DisplayName("Formats money as currency followed by amount")
    void formatsMoneyAsCurrencyFollowedByAmount() {
        Money money = Money.aed(new BigDecimal("1250.75"));

        assertEquals("AED 1250.75", money.toString());
        assertEquals("AED 1250.75", money.toFormattedString());
    }
}
