package com.bank.loanmanagement.security.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.time.Duration;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenAnalytics {
    private String clientId;
    private Duration period;
    private Long activeTokenCount;
    private Double tokenCreationRate;
    private Long rateLimitHits;
    private Long securityViolations;
    private Instant generatedAt;
    private Long totalTokensIssued;
    private Long activeTokens;
    private Double averageTokenLifetime;
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String clientId;
        private Duration period;
        private Long activeTokenCount;
        private Double tokenCreationRate;
        private Long rateLimitHits;
        private Long securityViolations;
        private Instant generatedAt;
        private Long totalTokensIssued;
        private Long activeTokens;
        private Double averageTokenLifetime;
        
        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }
        
        public Builder period(Duration period) {
            this.period = period;
            return this;
        }
        
        public Builder activeTokenCount(Long activeTokenCount) {
            this.activeTokenCount = activeTokenCount;
            return this;
        }
        
        public Builder tokenCreationRate(Double tokenCreationRate) {
            this.tokenCreationRate = tokenCreationRate;
            return this;
        }
        
        public Builder rateLimitHits(Long rateLimitHits) {
            this.rateLimitHits = rateLimitHits;
            return this;
        }
        
        public Builder securityViolations(Long securityViolations) {
            this.securityViolations = securityViolations;
            return this;
        }
        
        public Builder generatedAt(Instant generatedAt) {
            this.generatedAt = generatedAt;
            return this;
        }
        
        public TokenAnalytics build() {
            return new TokenAnalytics(clientId, period, activeTokenCount, tokenCreationRate, 
                rateLimitHits, securityViolations, generatedAt, totalTokensIssued, 
                activeTokens, averageTokenLifetime);
        }
    }
}