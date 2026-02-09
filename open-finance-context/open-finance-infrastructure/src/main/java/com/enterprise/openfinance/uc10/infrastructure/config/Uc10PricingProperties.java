package com.enterprise.openfinance.uc10.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "open-finance.uc10.pricing")
public class Uc10PricingProperties {

    private BigDecimal basePremium = new BigDecimal("750.00");

    public BigDecimal getBasePremium() {
        return basePremium;
    }

    public void setBasePremium(BigDecimal basePremium) {
        this.basePremium = basePremium;
    }
}
