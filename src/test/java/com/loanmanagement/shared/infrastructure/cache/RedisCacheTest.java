package com.loanmanagement.shared.infrastructure.cache;

import com.loanmanagement.customer.domain.model.Customer;
import com.loanmanagement.loan.domain.model.Loan;
import com.loanmanagement.shared.infrastructure.cache.service.BankingCacheService;
import com.loanmanagement.shared.infrastructure.cache.service.BankingCacheServiceImpl;
import com.loanmanagement.shared.infrastructure.cache.service.CacheMetricsService;
import com.loanmanagement.shared.infrastructure.cache.service.CacheMetricsServiceImpl;
import com.loanmanagement.shared.infrastructure.cache.service.MultiLevelCacheManager;
import com.loanmanagement.shared.infrastructure.cache.service.MultiLevelCacheManagerImpl;
import com.loanmanagement.shared.infrastructure.cache.config.BankingCacheKeyGenerator;
import com.loanmanagement.shared.infrastructure.cache.config.CacheProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Test-Driven Development tests for Redis Caching Layer
 * These tests are designed to FAIL initially and drive the implementation
 * of enterprise-grade Redis caching functionality from backup-src
 */
@DisplayName("Redis Caching Layer - TDD Tests")
class RedisCacheTest {

    private BankingCacheService cacheService;
    private CacheManager cacheManager;
    private RedisTemplate<String, Object> redisTemplate;
    private CacheMetricsService metricsService;
    private MultiLevelCacheManager multiLevelCacheManager;

    @BeforeEach
    void setUp() {
        // Mock setup for TDD - these will be properly configured when integrating with Spring
        CacheProperties cacheProperties = new CacheProperties();
        BankingCacheKeyGenerator keyGenerator = new BankingCacheKeyGenerator();
        
        // For now, these are null as we're doing pure TDD
        redisTemplate = null; // Will be configured with Redis connection
        cacheManager = null; // Will be RedisCacheManager
        
        // Create implementations for testing
        cacheService = new BankingCacheServiceImpl(cacheManager, redisTemplate, keyGenerator, cacheProperties);
        metricsService = new CacheMetricsServiceImpl();
        multiLevelCacheManager = new MultiLevelCacheManagerImpl(redisTemplate);
    }

    @Test
    @DisplayName("Should cache customer data with appropriate TTL")
    void shouldCacheCustomerDataWithTTL() {
        // Given: Customer data to cache
        Customer customer = createTestCustomer("CUST-001");

        String cacheKey = "customer:CUST-001";
        Duration customerTTL = Duration.ofMinutes(30);

        // When: Cache customer data
        cacheService.cacheCustomer(customer, customerTTL);

        // Then: Customer should be retrievable from cache
        Customer cachedCustomer = cacheService.getCustomer("CUST-001");
        assertThat(cachedCustomer).isNotNull();
        assertThat(cachedCustomer.getId()).isEqualTo("CUST-001");
        // Additional customer validation will be added with implementation

        // And: Cache entry should have correct TTL
        Long ttl = cacheService.getTTL(cacheKey);
        assertThat(ttl).isGreaterThan(0);
        assertThat(ttl).isLessThanOrEqualTo(customerTTL.getSeconds());
    }

    @Test
    @DisplayName("Should implement multi-level caching (L1 + L2)")
    void shouldImplementMultiLevelCaching() {
        // Given: Data to cache at multiple levels
        String key = "loan:LOAN-001";
        Loan loan = createTestLoan("LOAN-001");

        // When: Store in multi-level cache
        multiLevelCacheManager.put(key, loan);

        // Then: Data should be in L1 (in-memory) cache
        Object l1Value = multiLevelCacheManager.getFromL1(key);
        assertThat(l1Value).isNotNull();
        assertThat(l1Value).isInstanceOf(Loan.class);

        // And: Data should be in L2 (Redis) cache
        Object l2Value = multiLevelCacheManager.getFromL2(key);
        assertThat(l2Value).isNotNull();
        assertThat(l2Value).isInstanceOf(Loan.class);

        // When: L1 cache is cleared
        multiLevelCacheManager.clearL1();

        // Then: Data should still be retrievable from L2
        Object retrievedValue = multiLevelCacheManager.get(key);
        assertThat(retrievedValue).isNotNull();
        assertThat(((Loan) retrievedValue).getId()).isEqualTo("LOAN-001");
    }

    @Test
    @DisplayName("Should implement cache-aside pattern for loan processing")
    void shouldImplementCacheAsidePatternForLoanProcessing() {
        // Given: Loan processing request
        String loanId = "LOAN-002";
        String customerId = "CUST-002";

        // When: Request loan data (cache miss)
        Loan loan = cacheService.getLoanWithCacheAside(loanId);

        // Then: Should fallback to database and cache result
        assertThat(loan).isNotNull();
        assertThat(loan.getId()).isEqualTo(loanId);

        // And: Subsequent requests should hit cache
        long startTime = System.currentTimeMillis();
        Loan cachedLoan = cacheService.getLoanWithCacheAside(loanId);
        long duration = System.currentTimeMillis() - startTime;

        assertThat(cachedLoan).isNotNull();
        assertThat(duration).isLessThan(10); // Should be very fast from cache
        assertThat(metricsService.getCacheHitRatio("loan")).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Should implement write-through caching for critical data")
    void shouldImplementWriteThroughCachingForCriticalData() {
        // Given: Critical payment data
        PaymentData payment = new PaymentData(
            "PAY-001",
            "LOAN-001", 
            "1500.00",
            "USD",
            java.time.LocalDateTime.now()
        );

        // When: Save payment with write-through caching
        cacheService.savePaymentWithWriteThrough(payment);

        // Then: Data should be in both database and cache
        PaymentData cachedPayment = cacheService.getPayment("PAY-001");
        assertThat(cachedPayment).isNotNull();
        assertThat(cachedPayment.getPaymentId()).isEqualTo("PAY-001");
        assertThat(cachedPayment.getAmount()).isEqualTo("1500.00");

        // And: Database should also contain the data (verified through service)
        boolean existsInDatabase = cacheService.verifyPaymentInDatabase("PAY-001");
        assertThat(existsInDatabase).isTrue();
    }

    @Test
    @DisplayName("Should implement pattern-based cache invalidation")
    void shouldImplementPatternBasedCacheInvalidation() {
        // Given: Multiple cache entries for a customer
        String customerId = "CUST-003";
        cacheService.cacheCustomer(createTestCustomer(customerId), Duration.ofHours(1));
        cacheService.cacheLoanForCustomer(customerId, "LOAN-003", Duration.ofMinutes(30));
        cacheService.cachePaymentHistoryForCustomer(customerId, List.of(), Duration.ofMinutes(15));
        cacheService.cacheCreditAssessmentForCustomer(customerId, createTestCreditAssessment(), Duration.ofHours(2));

        // When: Invalidate all cache entries for customer
        cacheService.invalidateCustomerPattern(customerId);

        // Then: All customer-related cache entries should be cleared
        assertThat(cacheService.getCustomer(customerId)).isNull();
        assertThat(cacheService.getLoanForCustomer(customerId, "LOAN-003")).isNull();
        assertThat(cacheService.getPaymentHistoryForCustomer(customerId)).isNull();
        assertThat(cacheService.getCreditAssessmentForCustomer(customerId)).isNull();
    }

    @Test
    @DisplayName("Should implement banking-specific cache categories with different TTL")
    void shouldImplementBankingSpecificCacheCategoriesWithDifferentTTL() {
        // Given: Different types of banking data
        Customer customer = createTestCustomer("CUST-004");
        Loan loan = createTestLoan("LOAN-004");
        ComplianceData complianceData = createTestComplianceData();
        ReferenceData rateData = createTestReferenceData();

        // When: Cache with category-specific TTL
        cacheService.cacheWithCategory("customer", "CUST-004", customer); // 30 min TTL
        cacheService.cacheWithCategory("loan", "LOAN-004", loan); // 2 min TTL
        cacheService.cacheWithCategory("compliance", "COMP-001", complianceData); // 6 hour TTL
        cacheService.cacheWithCategory("rates", "USD", rateData); // 1 hour TTL

        // Then: Each category should have appropriate TTL
        assertThat(cacheService.getTTLForCategory("customer", "CUST-004"))
            .isBetween(25L * 60, 30L * 60); // 25-30 minutes
        
        assertThat(cacheService.getTTLForCategory("loan", "LOAN-004"))
            .isBetween(60L, 2L * 60); // 1-2 minutes
        
        assertThat(cacheService.getTTLForCategory("compliance", "COMP-001"))
            .isBetween(5L * 60 * 60, 6L * 60 * 60); // 5-6 hours
        
        assertThat(cacheService.getTTLForCategory("rates", "USD"))
            .isBetween(55L * 60, 60L * 60); // 55-60 minutes
    }

    @Test
    @DisplayName("Should implement cache warming for critical data")
    void shouldImplementCacheWarmingForCriticalData() {
        // Given: System startup scenario
        List<String> criticalCustomerIds = List.of("CUST-VIP-001", "CUST-VIP-002", "CUST-VIP-003");
        List<String> activeReferenceDataKeys = List.of("USD_RATES", "EUR_RATES", "COMPLIANCE_RULES");

        // When: Perform cache warming
        cacheService.warmCriticalData(criticalCustomerIds, activeReferenceDataKeys);

        // Then: Critical data should be preloaded in cache
        for (String customerId : criticalCustomerIds) {
            assertThat(cacheService.getCustomer(customerId)).isNotNull();
        }

        for (String referenceKey : activeReferenceDataKeys) {
            assertThat(cacheService.getReferenceData(referenceKey)).isNotNull();
        }

        // And: Cache hit ratio should be high for subsequent requests
        String testCustomerId = criticalCustomerIds.get(0);
        cacheService.getCustomer(testCustomerId); // Should hit cache
        assertThat(metricsService.getCacheHitRatio("customer")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should implement rate limiting with Redis backing")
    void shouldImplementRateLimitingWithRedisBacking() {
        // Given: Rate limiting configuration
        String clientId = "API-CLIENT-001";
        int requestsPerMinute = 100;

        // When: Make requests within rate limit
        for (int i = 0; i < 50; i++) {
            boolean allowed = cacheService.isRequestAllowed(clientId, requestsPerMinute);
            assertThat(allowed).isTrue();
        }

        // When: Exceed rate limit
        for (int i = 0; i < 60; i++) {
            cacheService.isRequestAllowed(clientId, requestsPerMinute);
        }

        // Then: Additional requests should be rejected
        boolean shouldBeRejected = cacheService.isRequestAllowed(clientId, requestsPerMinute);
        assertThat(shouldBeRejected).isFalse();

        // And: Rate limit data should be persisted in Redis
        long currentCount = cacheService.getCurrentRequestCount(clientId);
        assertThat(currentCount).isGreaterThan(requestsPerMinute);
    }

    @Test
    @DisplayName("Should implement FAPI-compliant token caching")
    void shouldImplementFAPICompliantTokenCaching() {
        // Given: FAPI-compliant access token
        FAPITokenData tokenData = new FAPITokenData(
            "access_token_123",
            "client_001",
            List.of("read", "write"),
            java.time.Instant.now().plusSeconds(3600),
            "interaction_id_456"
        );

        // When: Cache token with FAPI compliance
        cacheService.cacheFAPIToken(tokenData);

        // Then: Token should be retrievable with security constraints
        FAPITokenData cachedToken = cacheService.getFAPIToken("access_token_123");
        assertThat(cachedToken).isNotNull();
        assertThat(cachedToken.getClientId()).isEqualTo("client_001");
        assertThat(cachedToken.getScopes()).contains("read", "write");

        // And: Token should expire automatically
        assertThat(cacheService.getTTL("fapi:token:access_token_123"))
            .isLessThanOrEqualTo(3600L);

        // And: Token should be auditable for compliance
        List<CacheAuditEntry> auditEntries = cacheService.getTokenCacheAuditLog("access_token_123");
        assertThat(auditEntries).isNotEmpty();
        assertThat(auditEntries.get(0).getOperation()).isEqualTo("CACHE_STORE");
    }

    @Test
    @DisplayName("Should provide comprehensive cache metrics and monitoring")
    void shouldProvideComprehensiveCacheMetricsAndMonitoring() {
        // Given: Cache operations have been performed
        performVariousCacheOperations();

        // When: Request cache metrics
        CacheMetrics metrics = metricsService.getOverallMetrics();

        // Then: Comprehensive metrics should be available
        assertThat(metrics.getTotalRequests()).isGreaterThan(0);
        assertThat(metrics.getCacheHits()).isGreaterThan(0);
        assertThat(metrics.getCacheMisses()).isGreaterThan(0);
        assertThat(metrics.getHitRatio()).isBetween(0.0, 1.0);

        // And: Category-specific metrics should be available
        Map<String, CacheMetrics> categoryMetrics = metricsService.getMetricsByCategory();
        assertThat(categoryMetrics).containsKeys("customer", "loan", "payment", "compliance");

        // And: Performance metrics should be tracked
        assertThat(metrics.getAverageResponseTime()).isGreaterThan(0);
        assertThat(metrics.getP95ResponseTime()).isGreaterThan(metrics.getAverageResponseTime());

        // And: Memory usage should be monitored
        MemoryMetrics memoryMetrics = metricsService.getMemoryMetrics();
        assertThat(memoryMetrics.getUsedMemory()).isGreaterThan(0);
        assertThat(memoryMetrics.getMemoryUsagePercentage()).isBetween(0.0, 100.0);
    }

    @Test
    @DisplayName("Should handle cache failures gracefully with circuit breaker")
    void shouldHandleCacheFailuresGracefullyWithCircuitBreaker() {
        // Given: Cache service with circuit breaker
        ResilientCacheService resilientCacheService = new ResilientCacheService(cacheService);

        // When: Cache service becomes unavailable
        cacheService.simulateFailure();

        // Then: Circuit breaker should open and fallback to database
        Customer customer = resilientCacheService.getCustomerResilient("CUST-005");
        assertThat(customer).isNotNull(); // Should fallback to database

        // And: Circuit breaker status should indicate open state
        assertThat(resilientCacheService.getCircuitBreakerState()).isEqualTo("OPEN");

        // When: Cache service recovers
        cacheService.simulateRecovery();

        // Then: Circuit breaker should eventually close
        // (This would require time-based testing or manual triggering)
        resilientCacheService.testConnection(); // Manual health check
        assertThat(resilientCacheService.getCircuitBreakerState()).isEqualTo("CLOSED");
    }

    // Helper methods for test data creation
    private Customer createTestCustomer(String customerId) {
        // This would create a test customer using the proper Customer domain model
        // Implementation will be added when Customer model is fully available
        return null; // Placeholder - will be implemented
    }

    private Loan createTestLoan(String loanId) {
        // This would create a test loan object
        // Implementation depends on Loan domain model
        return null; // Placeholder - will be implemented
    }

    private ComplianceData createTestComplianceData() {
        return new ComplianceData("COMP-001", "KYC_VERIFIED", java.time.LocalDateTime.now());
    }

    private ReferenceData createTestReferenceData() {
        return new ReferenceData("USD", "3.25", "INTEREST_RATE");
    }

    private CreditAssessment createTestCreditAssessment() {
        return new CreditAssessment("HIGH", 750, "APPROVED");
    }

    private void performVariousCacheOperations() {
        // Simulate various cache operations for metrics testing
        cacheService.getCustomer("CUST-METRICS-001"); // Cache miss
        cacheService.cacheCustomer(createTestCustomer("CUST-METRICS-001"), Duration.ofMinutes(30));
        cacheService.getCustomer("CUST-METRICS-001"); // Cache hit
        cacheService.getCustomer("CUST-METRICS-002"); // Cache miss
    }

    // Supporting classes for testing (to be implemented)
    private static class PaymentData {
        private final String paymentId;
        private final String loanId;
        private final String amount;
        private final String currency;
        private final java.time.LocalDateTime timestamp;

        public PaymentData(String paymentId, String loanId, String amount, String currency, java.time.LocalDateTime timestamp) {
            this.paymentId = paymentId;
            this.loanId = loanId;
            this.amount = amount;
            this.currency = currency;
            this.timestamp = timestamp;
        }

        public String getPaymentId() { return paymentId; }
        public String getAmount() { return amount; }
    }

    private static class ComplianceData {
        private final String id;
        private final String status;
        private final java.time.LocalDateTime timestamp;

        public ComplianceData(String id, String status, java.time.LocalDateTime timestamp) {
            this.id = id;
            this.status = status;
            this.timestamp = timestamp;
        }
    }

    private static class ReferenceData {
        private final String key;
        private final String value;
        private final String type;

        public ReferenceData(String key, String value, String type) {
            this.key = key;
            this.value = value;
            this.type = type;
        }
    }

    private static class CreditAssessment {
        private final String riskLevel;
        private final int creditScore;
        private final String status;

        public CreditAssessment(String riskLevel, int creditScore, String status) {
            this.riskLevel = riskLevel;
            this.creditScore = creditScore;
            this.status = status;
        }
    }

    private static class FAPITokenData {
        private final String accessToken;
        private final String clientId;
        private final List<String> scopes;
        private final java.time.Instant expiresAt;
        private final String interactionId;

        public FAPITokenData(String accessToken, String clientId, List<String> scopes, java.time.Instant expiresAt, String interactionId) {
            this.accessToken = accessToken;
            this.clientId = clientId;
            this.scopes = scopes;
            this.expiresAt = expiresAt;
            this.interactionId = interactionId;
        }

        public String getClientId() { return clientId; }
        public List<String> getScopes() { return scopes; }
    }

    private static class CacheAuditEntry {
        private final String operation;
        private final java.time.LocalDateTime timestamp;

        public CacheAuditEntry(String operation, java.time.LocalDateTime timestamp) {
            this.operation = operation;
            this.timestamp = timestamp;
        }

        public String getOperation() { return operation; }
    }

    private static class CacheMetrics {
        private final long totalRequests;
        private final long cacheHits;
        private final long cacheMisses;
        private final double hitRatio;
        private final double averageResponseTime;
        private final double p95ResponseTime;

        public CacheMetrics(long totalRequests, long cacheHits, long cacheMisses, double hitRatio, double averageResponseTime, double p95ResponseTime) {
            this.totalRequests = totalRequests;
            this.cacheHits = cacheHits;
            this.cacheMisses = cacheMisses;
            this.hitRatio = hitRatio;
            this.averageResponseTime = averageResponseTime;
            this.p95ResponseTime = p95ResponseTime;
        }

        public long getTotalRequests() { return totalRequests; }
        public long getCacheHits() { return cacheHits; }
        public long getCacheMisses() { return cacheMisses; }
        public double getHitRatio() { return hitRatio; }
        public double getAverageResponseTime() { return averageResponseTime; }
        public double getP95ResponseTime() { return p95ResponseTime; }
    }

    private static class MemoryMetrics {
        private final long usedMemory;
        private final double memoryUsagePercentage;

        public MemoryMetrics(long usedMemory, double memoryUsagePercentage) {
            this.usedMemory = usedMemory;
            this.memoryUsagePercentage = memoryUsagePercentage;
        }

        public long getUsedMemory() { return usedMemory; }
        public double getMemoryUsagePercentage() { return memoryUsagePercentage; }
    }
}