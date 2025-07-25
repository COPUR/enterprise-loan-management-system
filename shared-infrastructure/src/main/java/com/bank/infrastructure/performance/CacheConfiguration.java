package com.bank.infrastructure.performance;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Cache Configuration for Enterprise Banking System
 * 
 * Non-Functional Requirements:
 * - NFR-005: Performance & Scalability
 * - NFR-006: Response Time Optimization
 */
@Configuration
@EnableCaching
public class CacheConfiguration {
    
    /**
     * Simple in-memory cache for development and testing
     */
    @Bean
    @Profile({"dev", "test"})
    public CacheManager simpleCacheManager() {
        return new ConcurrentMapCacheManager(
            "customers",
            "loans", 
            "payments",
            "user-sessions",
            "rate-limits"
        );
    }
    
    // Note: For production, Redis cache configuration would be added here
    // @Bean
    // @Profile("prod")
    // public CacheManager redisCacheManager() { ... }
}