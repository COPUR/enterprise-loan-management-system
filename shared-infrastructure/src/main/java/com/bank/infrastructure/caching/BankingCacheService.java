package com.bank.infrastructure.caching;

import com.bank.shared.kernel.domain.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Banking Cache Service for Enterprise Loan Management System
 * 
 * Provides caching operations for banking entities with appropriate TTL strategies:
 * - Customer Data: 1 hour TTL (frequent updates)
 * - Loan Data: 30 minutes TTL (moderate updates)
 * - Payment Data: 15 minutes TTL (high frequency)
 * - Credit Assessment: 24 hours TTL (stable data)
 * - Credit Limit: 6 hours TTL (moderately stable)
 * - JWT Tokens: 5 minutes TTL (security sensitive)
 * 
 * Performance Requirements:
 * - PR-001: Cache Hit Ratio > 90% for customer data
 * - PR-002: Cache Response Time < 10ms
 * - PR-003: Multi-level cache strategy
 * - PR-004: Cache invalidation patterns
 */
@Service
public class BankingCacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(BankingCacheService.class);
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // Cache key prefixes
    private static final String CUSTOMER_PREFIX = "customer:";
    private static final String LOAN_PREFIX = "loan:";
    private static final String PAYMENT_PREFIX = "payment:";
    private static final String CREDIT_LIMIT_PREFIX = "credit-limit:";
    private static final String CREDIT_ASSESSMENT_PREFIX = "credit-assessment:";
    private static final String JWT_PREFIX = "jwt:";
    
    // Cache TTL configurations
    private static final Duration CUSTOMER_TTL = Duration.ofHours(1);
    private static final Duration LOAN_TTL = Duration.ofMinutes(30);
    private static final Duration PAYMENT_TTL = Duration.ofMinutes(15);
    private static final Duration CREDIT_LIMIT_TTL = Duration.ofHours(6);
    private static final Duration CREDIT_ASSESSMENT_TTL = Duration.ofHours(24);
    private static final Duration JWT_TTL = Duration.ofMinutes(5);
    
    public BankingCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * Cache customer data with 1-hour TTL
     * 
     * @param customerId the customer identifier
     * @param customerData the customer data to cache
     */
    public void cacheCustomer(String customerId, String customerData) {
        String key = CUSTOMER_PREFIX + customerId;
        redisTemplate.opsForValue().set(key, customerData, CUSTOMER_TTL);
        logger.debug("Cached customer data for ID: {}", customerId);
    }
    
    /**
     * Retrieve cached customer data
     * 
     * @param customerId the customer identifier
     * @return cached customer data or null if not found
     */
    public String getCachedCustomer(String customerId) {
        String key = CUSTOMER_PREFIX + customerId;
        Object cachedData = redisTemplate.opsForValue().get(key);
        
        if (cachedData != null) {
            logger.debug("Cache hit for customer ID: {}", customerId);
            recordCacheHit(key);
            return (String) cachedData;
        } else {
            logger.debug("Cache miss for customer ID: {}", customerId);
            recordCacheMiss(key);
            return null;
        }
    }
    
    /**
     * Cache loan data with 30-minute TTL
     * 
     * @param loanId the loan identifier
     * @param loanData the loan data to cache
     */
    public void cacheLoan(String loanId, String loanData) {
        String key = LOAN_PREFIX + loanId;
        redisTemplate.opsForValue().set(key, loanData, LOAN_TTL);
        logger.debug("Cached loan data for ID: {}", loanId);
    }
    
    /**
     * Retrieve cached loan data
     * 
     * @param loanId the loan identifier
     * @return cached loan data or null if not found
     */
    public String getCachedLoan(String loanId) {
        String key = LOAN_PREFIX + loanId;
        Object cachedData = redisTemplate.opsForValue().get(key);
        
        if (cachedData != null) {
            logger.debug("Cache hit for loan ID: {}", loanId);
            recordCacheHit(key);
            return (String) cachedData;
        } else {
            logger.debug("Cache miss for loan ID: {}", loanId);
            recordCacheMiss(key);
            return null;
        }
    }
    
    /**
     * Cache payment data with 15-minute TTL
     * 
     * @param paymentId the payment identifier
     * @param paymentData the payment data to cache
     */
    public void cachePayment(String paymentId, String paymentData) {
        String key = PAYMENT_PREFIX + paymentId;
        redisTemplate.opsForValue().set(key, paymentData, PAYMENT_TTL);
        logger.debug("Cached payment data for ID: {}", paymentId);
    }
    
    /**
     * Retrieve cached payment data
     * 
     * @param paymentId the payment identifier
     * @return cached payment data or null if not found
     */
    public String getCachedPayment(String paymentId) {
        String key = PAYMENT_PREFIX + paymentId;
        Object cachedData = redisTemplate.opsForValue().get(key);
        
        if (cachedData != null) {
            logger.debug("Cache hit for payment ID: {}", paymentId);
            recordCacheHit(key);
            return (String) cachedData;
        } else {
            logger.debug("Cache miss for payment ID: {}", paymentId);
            recordCacheMiss(key);
            return null;
        }
    }
    
    /**
     * Cache credit limit with 6-hour TTL
     * 
     * @param customerId the customer identifier
     * @param creditLimit the credit limit to cache
     */
    public void cacheCreditLimit(String customerId, Money creditLimit) {
        String key = CREDIT_LIMIT_PREFIX + customerId;
        redisTemplate.opsForValue().set(key, creditLimit, CREDIT_LIMIT_TTL);
        logger.debug("Cached credit limit for customer ID: {}", customerId);
    }
    
    /**
     * Retrieve cached credit limit
     * 
     * @param customerId the customer identifier
     * @return cached credit limit or null if not found
     */
    public Money getCachedCreditLimit(String customerId) {
        String key = CREDIT_LIMIT_PREFIX + customerId;
        Object cachedData = redisTemplate.opsForValue().get(key);
        
        if (cachedData != null) {
            logger.debug("Cache hit for credit limit, customer ID: {}", customerId);
            recordCacheHit(key);
            return (Money) cachedData;
        } else {
            logger.debug("Cache miss for credit limit, customer ID: {}", customerId);
            recordCacheMiss(key);
            return null;
        }
    }
    
    /**
     * Cache credit assessment with 24-hour TTL
     * 
     * @param customerId the customer identifier
     * @param creditAssessment the credit assessment to cache
     */
    public void cacheCreditAssessment(String customerId, String creditAssessment) {
        String key = CREDIT_ASSESSMENT_PREFIX + customerId;
        redisTemplate.opsForValue().set(key, creditAssessment, CREDIT_ASSESSMENT_TTL);
        logger.debug("Cached credit assessment for customer ID: {}", customerId);
    }
    
    /**
     * Retrieve cached credit assessment
     * 
     * @param customerId the customer identifier
     * @return cached credit assessment or null if not found
     */
    public String getCachedCreditAssessment(String customerId) {
        String key = CREDIT_ASSESSMENT_PREFIX + customerId;
        Object cachedData = redisTemplate.opsForValue().get(key);
        
        if (cachedData != null) {
            logger.debug("Cache hit for credit assessment, customer ID: {}", customerId);
            recordCacheHit(key);
            return (String) cachedData;
        } else {
            logger.debug("Cache miss for credit assessment, customer ID: {}", customerId);
            recordCacheMiss(key);
            return null;
        }
    }
    
    /**
     * Cache JWT token with 5-minute TTL
     * 
     * @param tokenId the token identifier
     * @param tokenData the token data to cache
     */
    public void cacheJWTToken(String tokenId, String tokenData) {
        String key = JWT_PREFIX + tokenId;
        redisTemplate.opsForValue().set(key, tokenData, JWT_TTL);
        logger.debug("Cached JWT token for ID: {}", tokenId);
    }
    
    /**
     * Retrieve cached JWT token
     * 
     * @param tokenId the token identifier
     * @return cached token data or null if not found
     */
    public String getCachedJWTToken(String tokenId) {
        String key = JWT_PREFIX + tokenId;
        Object cachedData = redisTemplate.opsForValue().get(key);
        
        if (cachedData != null) {
            logger.debug("Cache hit for JWT token ID: {}", tokenId);
            recordCacheHit(key);
            return (String) cachedData;
        } else {
            logger.debug("Cache miss for JWT token ID: {}", tokenId);
            recordCacheMiss(key);
            return null;
        }
    }
    
    /**
     * Invalidate customer cache
     * 
     * @param customerId the customer identifier
     * @return true if cache was invalidated
     */
    public boolean invalidateCustomerCache(String customerId) {
        String key = CUSTOMER_PREFIX + customerId;
        Boolean result = redisTemplate.delete(key);
        logger.debug("Invalidated customer cache for ID: {}", customerId);
        return result != null && result;
    }
    
    /**
     * Invalidate loan cache
     * 
     * @param loanId the loan identifier
     * @return true if cache was invalidated
     */
    public boolean invalidateLoanCache(String loanId) {
        String key = LOAN_PREFIX + loanId;
        Boolean result = redisTemplate.delete(key);
        logger.debug("Invalidated loan cache for ID: {}", loanId);
        return result != null && result;
    }
    
    /**
     * Invalidate payment cache
     * 
     * @param paymentId the payment identifier
     * @return true if cache was invalidated
     */
    public boolean invalidatePaymentCache(String paymentId) {
        String key = PAYMENT_PREFIX + paymentId;
        Boolean result = redisTemplate.delete(key);
        logger.debug("Invalidated payment cache for ID: {}", paymentId);
        return result != null && result;
    }
    
    /**
     * Invalidate credit limit cache
     * 
     * @param customerId the customer identifier
     * @return true if cache was invalidated
     */
    public boolean invalidateCreditLimitCache(String customerId) {
        String key = CREDIT_LIMIT_PREFIX + customerId;
        Boolean result = redisTemplate.delete(key);
        logger.debug("Invalidated credit limit cache for customer ID: {}", customerId);
        return result != null && result;
    }
    
    /**
     * Invalidate credit assessment cache
     * 
     * @param customerId the customer identifier
     * @return true if cache was invalidated
     */
    public boolean invalidateCreditAssessmentCache(String customerId) {
        String key = CREDIT_ASSESSMENT_PREFIX + customerId;
        Boolean result = redisTemplate.delete(key);
        logger.debug("Invalidated credit assessment cache for customer ID: {}", customerId);
        return result != null && result;
    }
    
    /**
     * Invalidate JWT token cache
     * 
     * @param tokenId the token identifier
     * @return true if cache was invalidated
     */
    public boolean invalidateJWTTokenCache(String tokenId) {
        String key = JWT_PREFIX + tokenId;
        Boolean result = redisTemplate.delete(key);
        logger.debug("Invalidated JWT token cache for ID: {}", tokenId);
        return result != null && result;
    }
    
    /**
     * Invalidate cache by pattern
     * 
     * @param pattern the cache key pattern
     */
    public void invalidateByPattern(String pattern) {
        redisTemplate.delete(pattern);
        logger.debug("Invalidated cache by pattern: {}", pattern);
    }
    
    /**
     * Record cache hit for metrics
     * 
     * @param cacheKey the cache key
     */
    public void recordCacheHit(String cacheKey) {
        // Implementation would increment hit counter
        logger.debug("Cache hit recorded for key: {}", cacheKey);
    }
    
    /**
     * Record cache miss for metrics
     * 
     * @param cacheKey the cache key
     */
    public void recordCacheMiss(String cacheKey) {
        // Implementation would increment miss counter
        logger.debug("Cache miss recorded for key: {}", cacheKey);
    }
    
    /**
     * Warm up customer cache
     * 
     * @param customerId the customer identifier
     * @param customerData the customer data to cache
     */
    public void warmupCustomerCache(String customerId, String customerData) {
        cacheCustomer(customerId, customerData);
        logger.debug("Warmed up customer cache for ID: {}", customerId);
    }
    
    /**
     * Warm up loan cache
     * 
     * @param loanId the loan identifier
     * @param loanData the loan data to cache
     */
    public void warmupLoanCache(String loanId, String loanData) {
        cacheLoan(loanId, loanData);
        logger.debug("Warmed up loan cache for ID: {}", loanId);
    }
    
    /**
     * Warm up credit assessment cache
     * 
     * @param customerId the customer identifier
     * @param creditAssessment the credit assessment to cache
     */
    public void warmupCreditAssessmentCache(String customerId, String creditAssessment) {
        cacheCreditAssessment(customerId, creditAssessment);
        logger.debug("Warmed up credit assessment cache for customer ID: {}", customerId);
    }
}