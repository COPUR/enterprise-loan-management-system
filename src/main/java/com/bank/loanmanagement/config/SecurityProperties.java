package com.bank.loanmanagement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    private Jwt jwt = new Jwt();
    private Bcrypt bcrypt = new Bcrypt();
    private Cors cors = new Cors();
    private RateLimiting rateLimiting = new RateLimiting();

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Bcrypt getBcrypt() {
        return bcrypt;
    }

    public void setBcrypt(Bcrypt bcrypt) {
        this.bcrypt = bcrypt;
    }

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public RateLimiting getRateLimiting() {
        return rateLimiting;
    }

    public void setRateLimiting(RateLimiting rateLimiting) {
        this.rateLimiting = rateLimiting;
    }

    public static class Jwt {
        private String secret;
        private Integer expirationHours;
        private Integer refreshExpirationHours;
        private String issuer;
        private String audience;
        private String algorithm;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public Integer getExpirationHours() {
            return expirationHours;
        }

        public void setExpirationHours(Integer expirationHours) {
            this.expirationHours = expirationHours;
        }

        public Integer getRefreshExpirationHours() {
            return refreshExpirationHours;
        }

        public void setRefreshExpirationHours(Integer refreshExpirationHours) {
            this.refreshExpirationHours = refreshExpirationHours;
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

        public String getAlgorithm() {
            return algorithm;
        }

        public void setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }
    }

    public static class Bcrypt {
        private Integer strength;

        public Integer getStrength() {
            return strength;
        }

        public void setStrength(Integer strength) {
            this.strength = strength;
        }
    }

    public static class Cors {
        private List<String> allowedOrigins;
        private List<String> allowedMethods;
        private List<String> allowedHeaders;
        private Boolean allowCredentials;
        private Long maxAge;

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public List<String> getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(List<String> allowedMethods) {
            this.allowedMethods = allowedMethods;
        }

        public List<String> getAllowedHeaders() {
            return allowedHeaders;
        }

        public void setAllowedHeaders(List<String> allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }

        public Boolean getAllowCredentials() {
            return allowCredentials;
        }

        public void setAllowCredentials(Boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
        }

        public Long getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(Long maxAge) {
            this.maxAge = maxAge;
        }
    }

    public static class RateLimiting {
        private Integer requestsPerMinute;
        private Integer burstLimit;
        private Long windowSizeMs;

        public Integer getRequestsPerMinute() {
            return requestsPerMinute;
        }

        public void setRequestsPerMinute(Integer requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
        }

        public Integer getBurstLimit() {
            return burstLimit;
        }

        public void setBurstLimit(Integer burstLimit) {
            this.burstLimit = burstLimit;
        }

        public Long getWindowSizeMs() {
            return windowSizeMs;
        }

        public void setWindowSizeMs(Long windowSizeMs) {
            this.windowSizeMs = windowSizeMs;
        }
    }
}