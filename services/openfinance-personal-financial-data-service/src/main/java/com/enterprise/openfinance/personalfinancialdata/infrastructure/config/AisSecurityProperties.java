package com.enterprise.openfinance.personalfinancialdata.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "openfinance.personalfinancialdata.security")
public class AisSecurityProperties {

    private String jwtSecret = "0123456789abcdef0123456789abcdef";
    private String issuer = "";
    private String audience = "";
    private boolean dpopRequired = true;
    private Duration dpopMaxProofAge = Duration.ofMinutes(5);
    private Duration dpopClockSkew = Duration.ofSeconds(30);

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public boolean isDpopRequired() {
        return dpopRequired;
    }

    public void setDpopRequired(boolean dpopRequired) {
        this.dpopRequired = dpopRequired;
    }

    public Duration getDpopMaxProofAge() {
        return dpopMaxProofAge;
    }

    public void setDpopMaxProofAge(Duration dpopMaxProofAge) {
        this.dpopMaxProofAge = dpopMaxProofAge;
    }

    public Duration getDpopClockSkew() {
        return dpopClockSkew;
    }

    public void setDpopClockSkew(Duration dpopClockSkew) {
        this.dpopClockSkew = dpopClockSkew;
    }
}
