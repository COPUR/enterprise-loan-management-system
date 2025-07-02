package com.bank.loanmanagement.loan.security.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.HashMap;

@Component
@ConfigurationProperties(prefix = "fapi.security")
@Data
public class FAPISecurityConfiguration {
    private String issuerUri;
    private String jwksUri;
    private Long accessTokenTtl = 3600L;
    private Long refreshTokenTtl = 86400L;
    private String signingAlgorithm = "PS256";
    private Map<String, RateLimit> rateLimits = new HashMap<>();
    private String issuer = "enterprise-loan-management-system";
    
    public Map<String, RateLimit> getRateLimits() {
        return rateLimits;
    }
    
    @Data
    public static class RateLimit {
        private int maxRequests = 100;
        private int windowSeconds = 60;
        
        public int getMaxRequests() {
            return maxRequests;
        }
        
        public int getWindowSeconds() {
            return windowSeconds;
        }
    }
}