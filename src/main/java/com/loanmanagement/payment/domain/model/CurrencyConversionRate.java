package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Value object representing currency conversion rate details.
 */
@Value
@Builder
@With
public class CurrencyConversionRate {
    
    String conversionId;
    String fromCurrency;
    String toCurrency;
    BigDecimal rate;
    BigDecimal inverseRate;
    LocalDateTime rateDate;
    LocalDateTime validFrom;
    LocalDateTime validUntil;
    String rateProvider;
    String rateSource;
    BigDecimal bidRate;
    BigDecimal askRate;
    BigDecimal midRate;
    BigDecimal spread;
    ConversionType conversionType;
    Map<String, String> metadata;

    public enum ConversionType {
        SPOT, FORWARD, SWAP, CROSS_RATE, OFFICIAL_RATE, MARKET_RATE
    }

    public static class CurrencyConversionRateBuilder {
        public CurrencyConversionRateBuilder rate(BigDecimal rate) {
            if (rate != null && rate.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Exchange rate must be greater than zero");
            }
            this.rate = rate;
            return this;
        }

        public CurrencyConversionRateBuilder bidRate(BigDecimal rate) {
            if (rate != null && rate.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Bid rate must be greater than zero");
            }
            this.bidRate = rate;
            return this;
        }

        public CurrencyConversionRateBuilder askRate(BigDecimal rate) {
            if (rate != null && rate.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Ask rate must be greater than zero");
            }
            this.askRate = rate;
            return this;
        }

        public CurrencyConversionRate build() {
            if (conversionId == null || conversionId.trim().isEmpty()) {
                throw new IllegalArgumentException("Conversion ID is required");
            }
            if (fromCurrency == null || fromCurrency.trim().isEmpty()) {
                throw new IllegalArgumentException("From currency is required");
            }
            if (toCurrency == null || toCurrency.trim().isEmpty()) {
                throw new IllegalArgumentException("To currency is required");
            }
            if (fromCurrency.equals(toCurrency)) {
                throw new IllegalArgumentException("From and to currencies cannot be the same");
            }
            if (rate == null) {
                throw new IllegalArgumentException("Exchange rate is required");
            }
            if (rateDate == null) {
                this.rateDate = LocalDateTime.now();
            }
            if (validFrom == null) {
                this.validFrom = LocalDateTime.now();
            }
            if (conversionType == null) {
                this.conversionType = ConversionType.SPOT;
            }
            if (metadata == null) {
                this.metadata = Map.of();
            }
            
            // Calculate inverse rate
            if (inverseRate == null) {
                this.inverseRate = BigDecimal.ONE.divide(rate, 8, java.math.RoundingMode.HALF_UP);
            }
            
            // Calculate mid rate from bid/ask if available
            if (midRate == null && bidRate != null && askRate != null) {
                this.midRate = bidRate.add(askRate).divide(BigDecimal.valueOf(2), 8, java.math.RoundingMode.HALF_UP);
            }
            
            // Calculate spread if bid/ask available
            if (spread == null && bidRate != null && askRate != null) {
                this.spread = askRate.subtract(bidRate);
            }
            
            return new CurrencyConversionRate(
                conversionId, fromCurrency, toCurrency, rate, inverseRate,
                rateDate, validFrom, validUntil, rateProvider, rateSource,
                bidRate, askRate, midRate, spread, conversionType, metadata
            );
        }
    }

    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return (validFrom == null || !validFrom.isAfter(now)) &&
               (validUntil == null || !validUntil.isBefore(now));
    }

    public boolean isExpired() {
        return validUntil != null && validUntil.isBefore(LocalDateTime.now());
    }

    public boolean isActive() {
        return isValid() && !isExpired();
    }

    public BigDecimal convert(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        return amount.multiply(rate);
    }

    public BigDecimal convertInverse(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        return amount.multiply(inverseRate);
    }

    public boolean isCrossRate() {
        return conversionType == ConversionType.CROSS_RATE;
    }

    public boolean isSpotRate() {
        return conversionType == ConversionType.SPOT;
    }

    public boolean isForwardRate() {
        return conversionType == ConversionType.FORWARD;
    }

    public boolean hasBidAskSpread() {
        return bidRate != null && askRate != null && spread != null;
    }

    public BigDecimal getSpreadPercentage() {
        if (spread == null || midRate == null || midRate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return spread.divide(midRate, 6, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
    }

    public String getCurrencyPair() {
        return fromCurrency + "/" + toCurrency;
    }

    public String getInverseCurrencyPair() {
        return toCurrency + "/" + fromCurrency;
    }
}