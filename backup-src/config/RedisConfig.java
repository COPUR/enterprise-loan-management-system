package com.bank.loanmanagement.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RedisConfig {
    
    @Autowired
    private CacheProperties cacheProperties;
    
    // Cache configuration for banking operations
    public static final String CUSTOMER_CACHE = "customers";
    public static final String LOAN_CACHE = "loans";
    public static final String PAYMENT_CACHE = "payments";
    public static final String CREDIT_ASSESSMENT_CACHE = "credit_assessments";
    public static final String COMPLIANCE_CACHE = "compliance_data";
    public static final String SECURITY_CACHE = "security_tokens";
    public static final String RATE_LIMIT_CACHE = "rate_limits";
    
    // TTL configurations for different cache types
    public Duration getCacheTTL(String cacheType) {
        CacheProperties.Ttl ttl = cacheProperties.getTtl();
        
        return switch (cacheType) {
            case CUSTOMER_CACHE -> Duration.ofMinutes(ttl.getCustomersMinutes() != null ? ttl.getCustomersMinutes() : 30);
            case LOAN_CACHE -> Duration.ofMinutes(ttl.getLoansMinutes() != null ? ttl.getLoansMinutes() : 15);
            case PAYMENT_CACHE -> Duration.ofMinutes(ttl.getPaymentsMinutes() != null ? ttl.getPaymentsMinutes() : 5);
            case CREDIT_ASSESSMENT_CACHE -> Duration.ofMinutes(ttl.getCreditAssessmentMinutes() != null ? ttl.getCreditAssessmentMinutes() : 60);
            case COMPLIANCE_CACHE -> Duration.ofMinutes(ttl.getComplianceMinutes() != null ? ttl.getComplianceMinutes() : 360);
            case SECURITY_CACHE -> Duration.ofMinutes(ttl.getSecurityMinutes() != null ? ttl.getSecurityMinutes() : 2);
            case RATE_LIMIT_CACHE -> Duration.ofMinutes(ttl.getRateLimitMinutes() != null ? ttl.getRateLimitMinutes() : 1);
            default -> Duration.ofMinutes(10);
        };
    }
    
    // Redis connection configuration
    public static class ConnectionConfig {
        public static final String REDIS_HOST = System.getenv().getOrDefault("REDIS_HOST", "localhost");
        public static final int REDIS_PORT = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));
        public static final String REDIS_PASSWORD = System.getenv("REDIS_PASSWORD");
        public static final int DATABASE_INDEX = 0;
        public static final int CONNECTION_TIMEOUT = 5000; // 5 seconds
        public static final int READ_TIMEOUT = 3000; // 3 seconds
        public static final int MAX_CONNECTIONS = 50;
        public static final int MAX_IDLE_CONNECTIONS = 10;
        public static final int MIN_IDLE_CONNECTIONS = 2;
    }
    
    // Cache key patterns for banking operations
    public static class CacheKeys {
        public static final String CUSTOMER_BY_ID = "customer:id:%d";
        public static final String CUSTOMER_BY_EMAIL = "customer:email:%s";
        public static final String CUSTOMER_CREDIT_LIMIT = "customer:credit:%d";
        
        public static final String LOAN_BY_ID = "loan:id:%s";
        public static final String LOANS_BY_CUSTOMER = "loans:customer:%d";
        public static final String LOAN_INSTALLMENTS = "loan:installments:%s";
        public static final String LOAN_STATUS = "loan:status:%s";
        
        public static final String PAYMENT_BY_ID = "payment:id:%s";
        public static final String PAYMENTS_BY_LOAN = "payments:loan:%s";
        public static final String PAYMENT_HISTORY = "payments:customer:%d";
        
        public static final String CREDIT_ASSESSMENT = "credit:assessment:%d";
        public static final String CREDIT_SCORE = "credit:score:%d";
        
        public static final String TDD_COVERAGE = "compliance:tdd_coverage";
        public static final String FAPI_COMPLIANCE = "compliance:fapi_score";
        public static final String BANKING_STANDARDS = "compliance:banking_standards";
        
        public static final String JWT_TOKEN = "security:jwt:%s";
        public static final String SESSION_DATA = "security:session:%s";
        public static final String FAPI_REQUEST_ID = "security:fapi:%s";
        
        public static final String RATE_LIMIT_CLIENT = "rate_limit:client:%s";
        public static final String RATE_LIMIT_IP = "rate_limit:ip:%s";
        public static final String RATE_LIMIT_GLOBAL = "rate_limit:global";
    }
    
    // Cache eviction strategies
    public static class EvictionStrategy {
        public static final String LRU = "allkeys-lru"; // Least Recently Used
        public static final String LFU = "allkeys-lfu"; // Least Frequently Used
        public static final String RANDOM = "allkeys-random"; // Random eviction
        public static final String TTL = "volatile-ttl"; // TTL-based eviction
    }
    
    
    // Banking-specific cache warming strategies
    public static class WarmingStrategy {
        // High-priority data to preload
        public static final String[] HIGH_PRIORITY_CUSTOMERS = {
            "customer:id:1", "customer:id:2", "customer:id:3"
        };
        
        // Frequently accessed compliance data
        public static final String[] COMPLIANCE_KEYS = {
            "compliance:tdd_coverage", "compliance:fapi_score", "compliance:banking_standards"
        };
        
        // Security data for active sessions
        public static final String ACTIVE_SESSIONS_PATTERN = "security:session:*";
    }
    
    // Performance monitoring keys
    public static class MetricsKeys {
        public static final String CACHE_HITS = "metrics:cache:hits:%s";
        public static final String CACHE_MISSES = "metrics:cache:misses:%s";
        public static final String CACHE_EVICTIONS = "metrics:cache:evictions:%s";
        public static final String CACHE_OPERATIONS = "metrics:cache:operations:%s";
        public static final String RESPONSE_TIMES = "metrics:cache:response_time:%s";
    }
}