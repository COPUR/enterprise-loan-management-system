package com.amanahfi.shared.domain.money;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Money Value Object for Islamic Finance Operations
 * 
 * Implements precise monetary calculations following Islamic banking principles:
 * - No negative amounts (debt must be explicitly modeled)
 * - Precise decimal arithmetic for compliance
 * - Support for profit-sharing calculations
 * - Currency validation for MENAT region
 */
@Getter
public final class Money {
    
    private static final int CURRENCY_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final Pattern MONEY_PATTERN = Pattern.compile("^([A-Z]{3})\\s+([0-9]+(?:\\.[0-9]{1,2})?)$");
    
    @NotNull
    private final BigDecimal amount;
    
    @NotNull
    private final String currency;

    /**
     * Gets the currency code (alias for getCurrency for backward compatibility)
     */
    public String getCurrencyCode() {
        return currency;
    }

    @JsonCreator
    private Money(@JsonProperty("amount") BigDecimal amount, 
                  @JsonProperty("currency") String currency) {
        validateAmount(amount);
        validateCurrency(currency);
        
        this.amount = amount.setScale(CURRENCY_SCALE, ROUNDING_MODE);
        this.currency = currency.toUpperCase();
    }

    /**
     * Creates Money with specified amount and currency
     */
    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    /**
     * Creates Money with specified amount and currency
     */
    public static Money of(String amount, String currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    /**
     * Creates zero Money in specified currency
     */
    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    // MENAT Region Currency Factory Methods
    public static Money aed(BigDecimal amount) {
        return new Money(amount, "AED");
    }

    public static Money sar(BigDecimal amount) {
        return new Money(amount, "SAR");
    }

    public static Money usd(BigDecimal amount) {
        return new Money(amount, "USD");
    }

    public static Money eur(BigDecimal amount) {
        return new Money(amount, "EUR");
    }

    public static Money qar(BigDecimal amount) {
        return new Money(amount, "QAR");
    }

    public static Money kwd(BigDecimal amount) {
        return new Money(amount, "KWD");
    }

    public static Money bhd(BigDecimal amount) {
        return new Money(amount, "BHD");
    }

    public static Money tryLira(BigDecimal amount) {
        return new Money(amount, "TRY");
    }

    public static Money pkr(BigDecimal amount) {
        return new Money(amount, "PKR");
    }

    /**
     * Parses Money from string format "CUR 1000.00"
     */
    public static Money parse(String moneyString) {
        if (moneyString == null || moneyString.trim().isEmpty()) {
            throw new IllegalArgumentException("Money string cannot be null or empty");
        }

        Matcher matcher = MONEY_PATTERN.matcher(moneyString.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid money format. Expected: 'CUR 1000.00'");
        }

        String currency = matcher.group(1);
        BigDecimal amount = new BigDecimal(matcher.group(2));
        
        return new Money(amount, currency);
    }

    /**
     * Adds another Money object (same currency required)
     */
    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    /**
     * Subtracts another Money object (same currency required)
     */
    public Money subtract(Money other) {
        validateSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Subtraction would result in negative amount, which violates Islamic finance principles");
        }
        
        return new Money(result, this.currency);
    }

    /**
     * Multiplies Money by a scalar value
     */
    public Money multiply(BigDecimal multiplier) {
        if (multiplier == null) {
            throw new IllegalArgumentException("Multiplier cannot be null");
        }
        if (multiplier.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Multiplier cannot be negative in Islamic finance");
        }
        
        return new Money(this.amount.multiply(multiplier), this.currency);
    }

    /**
     * Divides Money by a scalar value
     */
    public Money divide(BigDecimal divisor) {
        if (divisor == null) {
            throw new IllegalArgumentException("Divisor cannot be null");
        }
        if (divisor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Divisor must be positive in Islamic finance");
        }
        
        return new Money(this.amount.divide(divisor, CURRENCY_SCALE, ROUNDING_MODE), this.currency);
    }

    /**
     * Checks if this Money is zero
     */
    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Checks if this Money is positive
     */
    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if this Money is greater than another Money
     */
    public boolean isGreaterThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    /**
     * Checks if this Money is less than another Money
     */
    public boolean isLessThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }

    /**
     * Islamic finance compliance check - ensures amount is Halal
     */
    public boolean isHalal() {
        return isPositive() || isZero(); // No negative debt representation
    }

    /**
     * Checks if this Money can be used for Murabaha contracts
     */
    public boolean canBeUsedForMurabaha() {
        return isHalal() && isPositive(); // Must be positive for asset financing
    }

    /**
     * Validates that the amount is not null and not negative
     */
    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative in Islamic finance");
        }
    }

    /**
     * Validates that the currency is not null or empty
     */
    private void validateCurrency(String currency) {
        if (currency == null || currency.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency cannot be null or empty");
        }
        if (currency.length() != 3) {
            throw new IllegalArgumentException("Currency must be a valid 3-letter ISO code");
        }
    }

    /**
     * Validates that two Money objects have the same currency
     */
    private void validateSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                String.format("Currency mismatch: %s vs %s", this.currency, other.currency)
            );
        }
    }

    /**
     * Formatted string representation with currency symbol
     * @return formatted string with currency symbol (e.g., "USD 100.00")
     */
    public String toFormattedString() {
        return String.format("%s %s", currency, amount.toPlainString());
    }

    @Override
    public String toString() {
        return String.format("%s %s", currency, amount.toPlainString());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Money money = (Money) obj;
        return amount.compareTo(money.amount) == 0 && Objects.equals(currency, money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }
}