package com.masrufi.framework.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

/**
 * Value object representing monetary amounts in the MasruFi Framework
 * 
 * This immutable class handles monetary calculations with proper precision
 * and currency support for Islamic finance operations. It ensures
 * mathematical accuracy in financial calculations and supports
 * multi-currency operations.
 * 
 * Features:
 * - Immutable design for thread safety
 * - Currency-aware operations
 * - Precision handling for financial calculations
 * - Support for multiple currencies including digital currencies
 * 
 * @author MasruFi Development Team
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode
public final class Money {
    
    private final BigDecimal amount;
    private final String currency;
    
    /**
     * Private constructor to ensure creation through factory methods
     */
    private Money(BigDecimal amount, String currency) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (currency == null || currency.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency cannot be null or empty");
        }
        
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency.toUpperCase();
    }
    
    /**
     * Create Money instance from amount and currency
     */
    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }
    
    /**
     * Create Money instance from string amount and currency
     */
    public static Money of(String amount, String currency) {
        return new Money(new BigDecimal(amount), currency);
    }
    
    /**
     * Create Money instance from double amount and currency
     */
    public static Money of(double amount, String currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
    }
    
    /**
     * Create zero money in the specified currency
     */
    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }
    
    /**
     * Add another Money amount (must be same currency)
     */
    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }
    
    /**
     * Subtract another Money amount (must be same currency)
     */
    public Money subtract(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }
    
    /**
     * Multiply by a decimal factor
     */
    public Money multiply(BigDecimal factor) {
        return new Money(this.amount.multiply(factor), this.currency);
    }
    
    /**
     * Divide by a decimal factor
     */
    public Money divide(BigDecimal divisor) {
        return new Money(this.amount.divide(divisor, 2, RoundingMode.HALF_UP), this.currency);
    }
    
    /**
     * Divide by another Money amount to get a ratio
     */
    public BigDecimal divide(Money other) {
        validateSameCurrency(other);
        return this.amount.divide(other.amount, 4, RoundingMode.HALF_UP);
    }
    
    /**
     * Check if this amount is greater than another
     */
    public boolean isGreaterThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }
    
    /**
     * Check if this amount is less than another
     */
    public boolean isLessThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }
    
    /**
     * Check if this amount is equal to another
     */
    public boolean isEqualTo(Money other) {
        if (other == null) return false;
        return this.currency.equals(other.currency) && 
               this.amount.compareTo(other.amount) == 0;
    }
    
    /**
     * Check if amount is zero
     */
    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }
    
    /**
     * Check if amount is positive
     */
    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Check if amount is negative
     */
    public boolean isNegative() {
        return this.amount.compareTo(BigDecimal.ZERO) < 0;
    }
    
    /**
     * Get absolute value
     */
    public Money abs() {
        return new Money(this.amount.abs(), this.currency);
    }
    
    /**
     * Negate the amount
     */
    public Money negate() {
        return new Money(this.amount.negate(), this.currency);
    }
    
    /**
     * Convert to different currency (would require exchange rate service)
     */
    public Money convertTo(String targetCurrency, BigDecimal exchangeRate) {
        if (this.currency.equals(targetCurrency)) {
            return this;
        }
        return new Money(this.amount.multiply(exchangeRate), targetCurrency);
    }
    
    /**
     * Format as string with currency symbol
     */
    public String toFormattedString() {
        return String.format("%s %.2f", currency, amount);
    }
    
    /**
     * Validate that two Money instances have the same currency
     */
    private void validateSameCurrency(Money other) {
        if (other == null) {
            throw new IllegalArgumentException("Cannot operate with null Money");
        }
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                String.format("Currency mismatch: %s vs %s", this.currency, other.currency));
        }
    }
    
    @Override
    public String toString() {
        return toFormattedString();
    }
    
    /**
     * Support for common currencies and digital currencies
     */
    public static class Currencies {
        // Fiat currencies
        public static final String USD = "USD";
        public static final String EUR = "EUR";
        public static final String AED = "AED";
        public static final String SAR = "SAR";
        public static final String QAR = "QAR";
        public static final String KWD = "KWD";
        public static final String BHD = "BHD";
        public static final String OMR = "OMR";
        
        // UAE Digital Currencies
        public static final String UAE_CBDC = "UAE-CBDC";
        public static final String ADIB_DD = "ADIB-DD";
        public static final String ENBD_DC = "ENBD-DC";
        public static final String FAB_DT = "FAB-DT";
        public static final String CBD_DD = "CBD-DD";
        public static final String RAK_DC = "RAK-DC";
        public static final String MASHREQ_DC = "MASHREQ-DC";
    }
}