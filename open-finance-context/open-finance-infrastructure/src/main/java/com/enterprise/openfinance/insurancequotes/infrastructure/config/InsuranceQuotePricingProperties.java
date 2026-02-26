package com.enterprise.openfinance.insurancequotes.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "open-finance.insurance-quotes.pricing")
public class InsuranceQuotePricingProperties {

    private BigDecimal basePremium = new BigDecimal("750.00");

    public BigDecimal getBasePremium() {
        return basePremium;
    }

    public void setBasePremium(BigDecimal basePremium) {
        this.basePremium = basePremium;
    }
}
