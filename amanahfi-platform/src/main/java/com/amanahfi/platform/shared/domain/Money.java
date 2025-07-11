package com.amanahfi.platform.shared.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Value object representing monetary amounts in the AmanahFi Platform
 * 
 * This immutable value object ensures safe handling of monetary values
 * throughout the Islamic finance platform. It provides precise decimal
 * arithmetic and currency-aware operations essential for financial calculations.
 * 
 * Key Features:
 * - Immutable value object for thread safety
 * - Precise decimal arithmetic using BigDecimal
 * - Currency-aware operations
 * - Islamic finance compliance considerations
 * - Multi-currency support for MENAT region
 * - Defensive programming with validation
 * 
 * Supported Currencies (MENAT Region):
 * - AED (UAE Dirham) - Primary currency
 * - SAR (Saudi Riyal)
 * - TRY (Turkish Lira)
 * - PKR (Pakistani Rupee)
 * - AZN (Azerbaijani Manat)
 * - IRR (Iranian Rial)
 * - ILS (Israeli Shekel)
 * - EUR, USD (International currencies)
 * 
 * Islamic Finance Considerations:
 * - Supports fractional precision for profit calculations
 * - Handles Zakat calculations with precision
 * - Compliant with Islamic accounting principles
 * - Supports multiple rounding modes for different scenarios
 * 
 * @author AmanahFi Development Team
 * @version 1.0.0
 * @since 2024
 */
@Value
public class Money {

    /**
     * The monetary amount using BigDecimal for precision
     */
    BigDecimal amount;

    /**
     * The currency of this monetary amount
     */
    Currency currency;

    /**
     * Standard constructor with validation
     * 
     * @param amount The monetary amount
     * @param currency The currency
     * @throws IllegalArgumentException if amount or currency is null
     */
    @JsonCreator
    public Money(@JsonProperty("amount") BigDecimal amount, 
                 @JsonProperty("currency") Currency currency) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
        
        this.amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_EVEN);
        this.currency = currency;
    }

    /**
     * Constructor with string currency code
     * 
     * @param amount The monetary amount
     * @param currencyCode The ISO currency code
     */
    public Money(BigDecimal amount, String currencyCode) {
        this(amount, Currency.getInstance(currencyCode));
    }

    /**
     * Constructor with double amount (for convenience)
     * WARNING: Use with caution due to floating-point precision issues
     * 
     * @param amount The monetary amount as double
     * @param currency The currency
     */
    public Money(double amount, Currency currency) {
        this(BigDecimal.valueOf(amount), currency);
    }

    /**
     * Constructor with string amount for precise input
     * 
     * @param amount The monetary amount as string
     * @param currency The currency
     */
    public Money(String amount, Currency currency) {
        this(new BigDecimal(amount), currency);
    }

    // Factory methods for common currencies in MENAT region

    /**
     * Creates Money in UAE Dirhams (AED)
     */
    public static Money aed(BigDecimal amount) {
        return new Money(amount, Currency.getInstance("AED"));
    }

    /**
     * Creates Money in UAE Dirhams (AED) from string
     */
    public static Money aed(String amount) {
        return new Money(amount, Currency.getInstance("AED"));
    }

    /**
     * Creates Money in Saudi Riyals (SAR)
     */
    public static Money sar(BigDecimal amount) {
        return new Money(amount, Currency.getInstance("SAR"));
    }

    /**
     * Creates Money in Turkish Lira (TRY)
     */
    public static Money tryLira(BigDecimal amount) {
        return new Money(amount, Currency.getInstance("TRY"));
    }

    /**
     * Creates Money in Pakistani Rupees (PKR)
     */
    public static Money pkr(BigDecimal amount) {
        return new Money(amount, Currency.getInstance("PKR"));
    }

    /**
     * Creates Money in US Dollars (USD)
     */
    public static Money usd(BigDecimal amount) {
        return new Money(amount, Currency.getInstance("USD"));
    }

    /**
     * Creates Money in Euros (EUR)
     */
    public static Money eur(BigDecimal amount) {
        return new Money(amount, Currency.getInstance("EUR"));
    }

    /**
     * Creates zero amount in the specified currency
     */
    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    /**
     * Creates zero amount in UAE Dirhams
     */
    public static Money zeroAed() {
        return zero(Currency.getInstance("AED"));
    }

    // Arithmetic operations

    /**
     * Adds another Money amount to this one
     * 
     * @param other The Money to add
     * @return New Money instance with the sum
     * @throws IllegalArgumentException if currencies don't match
     */
    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    /**
     * Subtracts another Money amount from this one
     * 
     * @param other The Money to subtract
     * @return New Money instance with the difference
     * @throws IllegalArgumentException if currencies don't match
     */
    public Money subtract(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    /**
     * Multiplies this Money by a scalar value
     * 
     * @param multiplier The multiplier
     * @return New Money instance with the product
     */
    public Money multiply(BigDecimal multiplier) {
        if (multiplier == null) {
            throw new IllegalArgumentException("Multiplier cannot be null");
        }
        return new Money(this.amount.multiply(multiplier), this.currency);
    }

    /**
     * Multiplies this Money by a double value
     * 
     * @param multiplier The multiplier
     * @return New Money instance with the product
     */
    public Money multiply(double multiplier) {
        return multiply(BigDecimal.valueOf(multiplier));
    }

    /**
     * Divides this Money by a scalar value
     * 
     * @param divisor The divisor
     * @return New Money instance with the quotient
     * @throws ArithmeticException if division is not exact
     */
    public Money divide(BigDecimal divisor) {
        if (divisor == null) {
            throw new IllegalArgumentException("Divisor cannot be null");
        }
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        return new Money(this.amount.divide(divisor, currency.getDefaultFractionDigits(), RoundingMode.HALF_EVEN), 
                        this.currency);
    }

    /**
     * Divides this Money by another Money (returns ratio as BigDecimal)
     * 
     * @param other The Money to divide by
     * @return The ratio as BigDecimal
     * @throws IllegalArgumentException if currencies don't match
     */
    public BigDecimal divide(Money other) {
        validateSameCurrency(other);
        if (other.amount.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        return this.amount.divide(other.amount, 10, RoundingMode.HALF_EVEN);
    }

    /**
     * Returns the absolute value of this Money
     */
    public Money abs() {
        return new Money(this.amount.abs(), this.currency);
    }

    /**
     * Returns the negated value of this Money
     */
    public Money negate() {
        return new Money(this.amount.negate(), this.currency);
    }

    // Comparison operations

    /**
     * Checks if this Money is greater than another
     * 
     * @param other The Money to compare with
     * @return true if this amount is greater
     * @throws IllegalArgumentException if currencies don't match
     */
    public boolean isGreaterThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    /**
     * Checks if this Money is greater than or equal to another
     * 
     * @param other The Money to compare with
     * @return true if this amount is greater than or equal
     * @throws IllegalArgumentException if currencies don't match
     */
    public boolean isGreaterThanOrEqual(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) >= 0;
    }

    /**
     * Checks if this Money is less than another
     * 
     * @param other The Money to compare with
     * @return true if this amount is less
     * @throws IllegalArgumentException if currencies don't match
     */
    public boolean isLessThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }

    /**
     * Checks if this Money is less than or equal to another
     * 
     * @param other The Money to compare with
     * @return true if this amount is less than or equal
     * @throws IllegalArgumentException if currencies don't match
     */
    public boolean isLessThanOrEqual(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) <= 0;
    }

    /**
     * Checks if this Money is zero
     */
    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Checks if this Money is positive
     */
    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if this Money is negative
     */
    public boolean isNegative() {
        return this.amount.compareTo(BigDecimal.ZERO) < 0;
    }

    // Utility methods

    /**
     * Validates that another Money has the same currency
     * 
     * @param other The Money to validate
     * @throws IllegalArgumentException if currencies don't match
     */
    private void validateSameCurrency(Money other) {
        if (other == null) {
            throw new IllegalArgumentException("Other Money cannot be null");
        }
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                String.format("Currency mismatch: %s vs %s", 
                    this.currency.getCurrencyCode(), 
                    other.currency.getCurrencyCode())
            );
        }
    }

    /**
     * Returns the currency code
     */
    public String getCurrencyCode() {
        return currency.getCurrencyCode();
    }

    /**
     * Converts to string representation
     */
    @Override
    public String toString() {
        return String.format("%s %s", amount.toPlainString(), currency.getCurrencyCode());
    }

    /**
     * Formatted string representation with currency symbol
     */
    public String toFormattedString() {
        return String.format("%s %s", amount.toPlainString(), currency.getSymbol());
    }

    /**
     * Equality based on amount and currency
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return Objects.equals(amount, money.amount) && Objects.equals(currency, money.currency);
    }

    /**
     * Hash code based on amount and currency
     */
    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }
}