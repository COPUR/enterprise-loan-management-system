package com.bank.loanmanagement.loan.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Security configuration properties for FAPI compliance
 * Comprehensive security settings for banking applications
 */
@Data
@Component
@ConfigurationProperties(prefix = "banking.security")
public class SecurityProperties {

    private Jwt jwt = new Jwt();
    private Password password = new Password();
    private Cors cors = new Cors();
    private RateLimit rateLimit = new RateLimit();

    // Explicit getters to ensure they exist for compilation
    public Jwt getJwt() {
        return jwt;
    }

    public Password getPassword() {
        return password;
    }

    public Cors getCors() {
        return cors;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    @Data
    public static class Jwt {
        private String secret = "banking-jwt-secret-key-should-be-very-long-and-secure";
        private long expiration = 86400; // 24 hours in seconds
        private String issuer = "enterprise-loan-management-system";
        private String audience = "banking-api-clients";
        private String algorithm = "RS256";
        private String jwkSetUri = "/.well-known/jwks.json";
    }

    @Data
    public static class Password {
        private int strength = 12; // BCrypt strength
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins = List.of(
            "https://localhost:3000",
            "https://banking-frontend.local",
            "https://*.banking-domain.com"
        );
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
        private List<String> allowedHeaders = List.of(
            "Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin",
            "X-FAPI-Interaction-ID", "X-FAPI-Auth-Date", "X-FAPI-Customer-IP-Address"
        );
        private boolean allowCredentials = true;
        private long maxAge = 1800; // 30 minutes
    }

    @Data
    public static class RateLimit {
        private int requestsPerMinute = 60;
        private int burstLimit = 100;
        private long windowSizeMinutes = 1;
        private boolean enableRateLimiting = true;
    }
}