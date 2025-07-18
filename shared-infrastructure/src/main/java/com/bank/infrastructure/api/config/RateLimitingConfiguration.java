package com.bank.infrastructure.api.config;

import com.bank.infrastructure.api.interceptor.RateLimitingInterceptor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;

/**
 * Rate Limiting Configuration
 * 
 * Configures rate limiting policies for different user tiers and endpoints:
 * - BASIC: Standard rate limits for regular users
 * - PREMIUM: Higher limits for premium customers
 * - ENTERPRISE: Highest limits for enterprise clients
 */
@Configuration
@ConfigurationProperties(prefix = "banking.api.rate-limiting")
public class RateLimitingConfiguration {
    
    private boolean enabled = true;
    private Map<String, TierConfig> tiers = Map.of(
        "BASIC", new TierConfig(
            100, Duration.ofMinutes(1),    // Global: 100 requests per minute
            20, Duration.ofMinutes(1),     // Endpoint: 20 requests per minute
            500, Duration.ofHours(1)       // Sliding: 500 requests per hour
        ),
        "PREMIUM", new TierConfig(
            500, Duration.ofMinutes(1),    // Global: 500 requests per minute
            100, Duration.ofMinutes(1),    // Endpoint: 100 requests per minute
            2000, Duration.ofHours(1)      // Sliding: 2000 requests per hour
        ),
        "ENTERPRISE", new TierConfig(
            2000, Duration.ofMinutes(1),   // Global: 2000 requests per minute
            500, Duration.ofMinutes(1),    // Endpoint: 500 requests per minute
            10000, Duration.ofHours(1)     // Sliding: 10000 requests per hour
        )
    );
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public Map<String, TierConfig> getTiers() {
        return tiers;
    }
    
    public void setTiers(Map<String, TierConfig> tiers) {
        this.tiers = tiers;
    }
    
    public RateLimitingInterceptor.RateLimitConfig getConfigForTier(RateLimitingInterceptor.RateLimitTier tier) {
        TierConfig config = tiers.get(tier.name());
        if (config == null) {
            config = tiers.get("BASIC"); // Default to BASIC if tier not found
        }
        
        return new RateLimitingInterceptor.RateLimitConfig(
            config.globalLimit, config.globalWindow,
            config.endpointLimit, config.endpointWindow,
            config.slidingWindowLimit, config.slidingWindowDuration
        );
    }
    
    public static class TierConfig {
        private int globalLimit;
        private Duration globalWindow;
        private int endpointLimit;
        private Duration endpointWindow;
        private int slidingWindowLimit;
        private Duration slidingWindowDuration;
        
        public TierConfig() {}
        
        public TierConfig(int globalLimit, Duration globalWindow,
                         int endpointLimit, Duration endpointWindow,
                         int slidingWindowLimit, Duration slidingWindowDuration) {
            this.globalLimit = globalLimit;
            this.globalWindow = globalWindow;
            this.endpointLimit = endpointLimit;
            this.endpointWindow = endpointWindow;
            this.slidingWindowLimit = slidingWindowLimit;
            this.slidingWindowDuration = slidingWindowDuration;
        }
        
        // Getters and setters
        public int getGlobalLimit() { return globalLimit; }
        public void setGlobalLimit(int globalLimit) { this.globalLimit = globalLimit; }
        
        public Duration getGlobalWindow() { return globalWindow; }
        public void setGlobalWindow(Duration globalWindow) { this.globalWindow = globalWindow; }
        
        public int getEndpointLimit() { return endpointLimit; }
        public void setEndpointLimit(int endpointLimit) { this.endpointLimit = endpointLimit; }
        
        public Duration getEndpointWindow() { return endpointWindow; }
        public void setEndpointWindow(Duration endpointWindow) { this.endpointWindow = endpointWindow; }
        
        public int getSlidingWindowLimit() { return slidingWindowLimit; }
        public void setSlidingWindowLimit(int slidingWindowLimit) { this.slidingWindowLimit = slidingWindowLimit; }
        
        public Duration getSlidingWindowDuration() { return slidingWindowDuration; }
        public void setSlidingWindowDuration(Duration slidingWindowDuration) { this.slidingWindowDuration = slidingWindowDuration; }
    }
}