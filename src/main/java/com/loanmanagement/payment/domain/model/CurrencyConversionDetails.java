package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Value object representing currency conversion details.
 */
@Value
@Builder
@With
public class CurrencyConversionDetails {
    
    String fromCurrency;
    String toCurrency;
    BigDecimal originalAmount;
    BigDecimal convertedAmount;
    BigDecimal exchangeRate;
    String rateProvider;
    LocalDateTime conversionTime;
    String conversionId;
    
    public static CurrencyConversionDetails create(
            String fromCurrency,
            String toCurrency,
            BigDecimal originalAmount,
            BigDecimal convertedAmount,
            BigDecimal exchangeRate,
            String rateProvider,
            String conversionId) {
        
        if (fromCurrency == null || fromCurrency.trim().isEmpty()) {
            throw new IllegalArgumentException("From currency cannot be null or empty");
        }
        if (toCurrency == null || toCurrency.trim().isEmpty()) {
            throw new IllegalArgumentException("To currency cannot be null or empty");
        }
        if (originalAmount == null || originalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Original amount must be positive");
        }
        if (convertedAmount == null || convertedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Converted amount must be positive");
        }
        if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Exchange rate must be positive");
        }
        if (rateProvider == null || rateProvider.trim().isEmpty()) {
            throw new IllegalArgumentException("Rate provider cannot be null or empty");
        }
        if (conversionId == null || conversionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Conversion ID cannot be null or empty");
        }
        if (fromCurrency.equals(toCurrency)) {
            throw new IllegalArgumentException("From and to currencies cannot be the same");
        }
        
        return CurrencyConversionDetails.builder()
                .fromCurrency(fromCurrency.trim().toUpperCase())
                .toCurrency(toCurrency.trim().toUpperCase())
                .originalAmount(originalAmount)
                .convertedAmount(convertedAmount)
                .exchangeRate(exchangeRate)
                .rateProvider(rateProvider.trim())
                .conversionTime(LocalDateTime.now())
                .conversionId(conversionId.trim())
                .build();
    }
    
    public boolean isConversionRequired() {
        return !fromCurrency.equals(toCurrency);
    }
    
    public BigDecimal getConversionFee() {
        // Calculate conversion fee as 0.1% of original amount
        return originalAmount.multiply(BigDecimal.valueOf(0.001));
    }
}