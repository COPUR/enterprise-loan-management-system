package com.enterprise.openfinance.uc11.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "open-finance.uc11.rate")
public class Uc11RateProperties {

    private boolean weekendRatesEnabled = true;
    private int rateScale = 6;
    private Map<String, BigDecimal> pairs = new LinkedHashMap<>();

    public Uc11RateProperties() {
        pairs.put("AED-USD", new BigDecimal("0.272290"));
        pairs.put("USD-AED", new BigDecimal("3.672500"));
        pairs.put("GBP-USD", new BigDecimal("1.275000"));
    }

    public boolean isWeekendRatesEnabled() {
        return weekendRatesEnabled;
    }

    public void setWeekendRatesEnabled(boolean weekendRatesEnabled) {
        this.weekendRatesEnabled = weekendRatesEnabled;
    }

    public int getRateScale() {
        return rateScale;
    }

    public void setRateScale(int rateScale) {
        this.rateScale = rateScale;
    }

    public Map<String, BigDecimal> getPairs() {
        return pairs;
    }

    public void setPairs(Map<String, BigDecimal> pairs) {
        this.pairs = pairs;
    }
}
