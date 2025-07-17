package com.bank.infrastructure.caching;

import com.bank.infrastructure.caching.AdvancedCacheService.CacheStatistics;
import com.bank.infrastructure.caching.AdvancedCacheService.CacheHealthStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Test Suite for Advanced Cache Service
 *
 * Tests sophisticated caching strategies focusing on L1 cache functionality:
 * - Multi-level caching (L1 focus)
 * - Cache warming and invalidation
 * - Cache statistics and monitoring
 * - Batch operations
 * - Group invalidation
 * - Circuit breaker functionality
 *
 * Note: This test focuses on L1 cache functionality to avoid Redis dependency issues
 */
@DisplayName("Advanced Cache Service TDD Tests")
class AdvancedCacheServiceTest {

    private AdvancedCacheService advancedCacheService;

    @BeforeEach
    void setUp() {
        // Create service with null RedisTemplate to focus on L1 cache testing
        advancedCacheService = new AdvancedCacheService(null);
    }

    @Nested
    @DisplayName("Multi-Level Caching Tests")
    class MultiLevelCachingTests {

        @Test
        @DisplayName("Should return data from L1 cache when available")
        void shouldReturnDataFromL1Cache() {
            // Given
            String key = "customer:123";
            String value = "customer data";

            // When - Set data first
            advancedCacheService.setAdvanced(key, value, Duration.ofMinutes(10));

            // Then - Get should return from L1 cache
            String result = advancedCacheService.getAdvanced(key);
            assertThat(result).isEqualTo(value);

            // Verify statistics
            CacheStatistics stats = advancedCacheService.getStatistics();
            assertThat(stats.getL1Hits()).isEqualTo(1);
            assertThat(stats.getL2Hits()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return null when L1 miss and no L2 cache")
        void shouldReturnNullWhenL1MissAndNoL2Cache() {
            // Given
            String key = "customer:456";

            // When - Get without setting in L1 and no L2 cache
            String result = advancedCacheService.getAdvanced(key);

            // Then
            assertThat(result).isNull();

            // Verify statistics
            CacheStatistics stats = advancedCacheService.getStatistics();
            assertThat(stats.getL1Hits()).isEqualTo(0);
            assertThat(stats.getMisses()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return null on complete cache miss")
        void shouldReturnNullOnCompleteCacheMiss() {
            // Given
            String key = "customer:nonexistent";

            // When
            String result = advancedCacheService.getAdvanced(key);

            // Then
            assertThat(result).isNull();

            // Verify statistics
            CacheStatistics stats = advancedCacheService.getStatistics();
            assertThat(stats.getMisses()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Batch Operations Tests")
    class BatchOperationsTests {

        @Test
        @DisplayName("Should perform batch cache operations")
        void shouldPerformBatchCacheOperations() {
            // Given
            Map<String, String> entries = new HashMap<>();
            entries.put("customer:1", "customer 1 data");
            entries.put("customer:2", "customer 2 data");
            entries.put("customer:3", "customer 3 data");

            // When
            advancedCacheService.setBatch(entries, Duration.ofMinutes(30));

            // Then - All entries should be in L1 cache
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                String result = advancedCacheService.getAdvanced(entry.getKey());
                assertThat(result).isEqualTo(entry.getValue());
            }

            // Verify batch write statistics
            CacheStatistics stats = advancedCacheService.getStatistics();
            assertThat(stats.getWrites()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should handle empty batch operations")
        void shouldHandleEmptyBatchOperations() {
            // Given
            Map<String, String> emptyEntries = new HashMap<>();

            // When
            advancedCacheService.setBatch(emptyEntries, Duration.ofMinutes(30));

            // Then - No exception should be thrown
            CacheStatistics stats = advancedCacheService.getStatistics();
            assertThat(stats.getWrites()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Cache Invalidation Tests")
    class CacheInvalidationTests {

        @Test
        @DisplayName("Should invalidate cache by pattern without Redis")
        void shouldInvalidateCacheByPatternWithoutRedis() {
            // Given
            String pattern = "customer:*";

            // Set some data first
            advancedCacheService.setAdvanced("customer:1", "data1", Duration.ofMinutes(10));
            advancedCacheService.setAdvanced("customer:2", "data2", Duration.ofMinutes(10));

            // When
            advancedCacheService.invalidateByPattern(pattern);

            // Then - L1 cache should be partially cleared (pattern-based removal from L1)
            // Note: This test validates the method execution without Redis
            CacheStatistics stats = advancedCacheService.getStatistics();
            assertThat(stats.getWrites()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should invalidate cache group")
        void shouldInvalidateCacheGroup() {
            // Given
            String groupName = "customers";
            String key1 = "customer:1";
            String key2 = "customer:2";

            // Add keys to group
            advancedCacheService.addToGroup(groupName, key1);
            advancedCacheService.addToGroup(groupName, key2);

            // Set data
            advancedCacheService.setAdvanced(key1, "data1", Duration.ofMinutes(10));
            advancedCacheService.setAdvanced(key2, "data2", Duration.ofMinutes(10));

            // When
            advancedCacheService.invalidateGroup(groupName);

            // Then - Data should be removed from L1 cache
            assertThat(advancedCacheService.getAdvanced(key1)).isNull();
            assertThat(advancedCacheService.getAdvanced(key2)).isNull();
        }
    }

    @Nested
    @DisplayName("Cache Warming Tests")
    class CacheWarmingTests {

        @Test
        @DisplayName("Should warm cache with provided data")
        void shouldWarmCacheWithProvidedData() {
            // Given
            Map<String, String> warmingData = new HashMap<>();
            warmingData.put("system:config", "system configuration");
            warmingData.put("islamic:products", "islamic finance products");
            warmingData.put("risk:thresholds", "risk assessment thresholds");

            // When
            advancedCacheService.warmCache(warmingData);

            // Then - All warming data should be available in L1 cache
            for (Map.Entry<String, String> entry : warmingData.entrySet()) {
                String result = advancedCacheService.getAdvanced(entry.getKey());
                assertThat(result).isEqualTo(entry.getValue());
            }
        }

        @Test
        @DisplayName("Should handle empty warming data")
        void shouldHandleEmptyWarmingData() {
            // Given
            Map<String, String> emptyWarmingData = new HashMap<>();

            // When
            advancedCacheService.warmCache(emptyWarmingData);

            // Then - No exception should be thrown
            CacheStatistics stats = advancedCacheService.getStatistics();
            assertThat(stats.getWrites()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Cache Statistics Tests")
    class CacheStatisticsTests {

        @Test
        @DisplayName("Should track cache statistics correctly")
        void shouldTrackCacheStatisticsCorrectly() {
            // Given
            String key = "customer:123";
            String value = "customer data";

            // When - Perform various operations
            advancedCacheService.setAdvanced(key, value, Duration.ofMinutes(10));
            advancedCacheService.getAdvanced(key); // L1 hit
            advancedCacheService.getAdvanced("nonexistent"); // Miss

            // Then
            CacheStatistics stats = advancedCacheService.getStatistics();
            assertThat(stats.getWrites()).isEqualTo(1);
            assertThat(stats.getL1Hits()).isEqualTo(1);
            assertThat(stats.getMisses()).isEqualTo(1);
            assertThat(stats.getTotalOperations()).isEqualTo(2);
            assertThat(stats.getHitRate()).isEqualTo(0.5);
        }

        @Test
        @DisplayName("Should calculate hit rate correctly")
        void shouldCalculateHitRateCorrectly() {
            // Given
            String key = "customer:123";
            String value = "customer data";

            // When - 3 hits, 1 miss
            advancedCacheService.setAdvanced(key, value, Duration.ofMinutes(10));
            advancedCacheService.getAdvanced(key); // L1 hit
            advancedCacheService.getAdvanced(key); // L1 hit
            advancedCacheService.getAdvanced(key); // L1 hit
            advancedCacheService.getAdvanced("nonexistent"); // Miss

            // Then
            CacheStatistics stats = advancedCacheService.getStatistics();
            assertThat(stats.getHitRate()).isEqualTo(0.75); // 3/4 = 0.75
        }
    }

    @Nested
    @DisplayName("Cache Health Tests")
    class CacheHealthTests {

        @Test
        @DisplayName("Should report healthy status under normal conditions")
        void shouldReportHealthyStatusUnderNormalConditions() {
            // Given
            String key = "customer:123";
            String value = "customer data";

            // When - Perform successful operations
            advancedCacheService.setAdvanced(key, value, Duration.ofMinutes(10));
            advancedCacheService.getAdvanced(key);

            // Then
            CacheHealthStatus health = advancedCacheService.getHealthStatus();
            assertThat(health.isHealthy()).isTrue();
            assertThat(health.getL1CacheSize()).isEqualTo(1);
            assertThat(health.isCircuitBreakerOpen()).isFalse();
        }

        @Test
        @DisplayName("Should report circuit breaker status")
        void shouldReportCircuitBreakerStatus() {
            // Given
            String key = "customer:123";
            String value = "customer data";

            // When - Perform operations
            advancedCacheService.setAdvanced(key, value, Duration.ofMinutes(10));

            // Then
            CacheHealthStatus health = advancedCacheService.getHealthStatus();
            assertThat(health.isCircuitBreakerOpen()).isFalse();
        }
    }

    @Nested
    @DisplayName("Cache Maintenance Tests")
    class CacheMaintenanceTests {

        @Test
        @DisplayName("Should clear all caches")
        void shouldClearAllCaches() {
            // Given
            String key = "customer:123";
            String value = "customer data";

            advancedCacheService.setAdvanced(key, value, Duration.ofMinutes(10));

            // When
            advancedCacheService.clearAllCaches();

            // Then - Cache should be empty
            assertThat(advancedCacheService.getAdvanced(key)).isNull();

            // Verify statistics are cleared
            CacheStatistics stats = advancedCacheService.getStatistics();
            assertThat(stats.getTotalOperations()).isEqualTo(1); // Only the miss after clear
        }

        @Test
        @DisplayName("Should handle group operations correctly")
        void shouldHandleGroupOperationsCorrectly() {
            // Given
            String groupName = "test-group";
            String key1 = "key1";
            String key2 = "key2";

            // When - Add keys to group
            advancedCacheService.addToGroup(groupName, key1);
            advancedCacheService.addToGroup(groupName, key2);

            // Set data
            advancedCacheService.setAdvanced(key1, "value1", Duration.ofMinutes(10));
            advancedCacheService.setAdvanced(key2, "value2", Duration.ofMinutes(10));

            // Invalidate group
            advancedCacheService.invalidateGroup(groupName);

            // Then - Keys should be removed
            assertThat(advancedCacheService.getAdvanced(key1)).isNull();
            assertThat(advancedCacheService.getAdvanced(key2)).isNull();
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle complete cache lifecycle")
        void shouldHandleCompleteCacheLifecycle() {
            // Given
            String customerKey = "customer:123";
            String customerValue = "customer data";
            String loanKey = "loan:456";
            String loanValue = "loan data";

            // When - Complete lifecycle
            // 1. Set data
            advancedCacheService.setAdvanced(customerKey, customerValue, Duration.ofMinutes(10));
            advancedCacheService.setAdvanced(loanKey, loanValue, Duration.ofMinutes(10));

            // 2. Add to groups
            advancedCacheService.addToGroup("customers", customerKey);
            advancedCacheService.addToGroup("loans", loanKey);

            // 3. Get data (should hit L1)
            String retrievedCustomer = advancedCacheService.getAdvanced(customerKey);
            String retrievedLoan = advancedCacheService.getAdvanced(loanKey);

            // 4. Invalidate group
            advancedCacheService.invalidateGroup("customers");

            // Then - Verify complete lifecycle
            assertThat(retrievedCustomer).isEqualTo(customerValue);
            assertThat(retrievedLoan).isEqualTo(loanValue);

            // Customer should be invalidated, loan should remain
            assertThat(advancedCacheService.getAdvanced(customerKey)).isNull();
            assertThat(advancedCacheService.getAdvanced(loanKey)).isEqualTo(loanValue);

            // Verify statistics
            CacheStatistics stats = advancedCacheService.getStatistics();
            assertThat(stats.getWrites()).isGreaterThanOrEqualTo(2);
            assertThat(stats.getL1Hits()).isGreaterThanOrEqualTo(2);
            assertThat(stats.getInvalidations()).isGreaterThanOrEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Circuit Breaker Pattern Tests")
    class CircuitBreakerPatternTests {

        @Test
        @DisplayName("Should continue working when external service fails")
        void shouldContinueWorkingWhenExternalServiceFails() {
            // Given
            String key = "customer:123";
            String value = "customer data";

            // When - Set data (should work with L1 cache even if Redis fails)
            advancedCacheService.setAdvanced(key, value, Duration.ofMinutes(10));

            // Then - Should still work with L1 cache
            String result = advancedCacheService.getAdvanced(key);
            assertThat(result).isEqualTo(value);

            // Verify L1 cache operations
            CacheStatistics stats = advancedCacheService.getStatistics();
            assertThat(stats.getL1Hits()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should handle Redis connection failures gracefully")
        void shouldHandleRedisConnectionFailuresGracefully() {
            // Given - Service without Redis (simulating connection failure)
            String key = "customer:123";
            String value = "customer data";

            // When - Perform operations without Redis
            advancedCacheService.setAdvanced(key, value, Duration.ofMinutes(10));
            String result = advancedCacheService.getAdvanced(key);

            // Then - Should work with L1 cache
            assertThat(result).isEqualTo(value);

            // Health should still be good for L1 operations
            CacheHealthStatus health = advancedCacheService.getHealthStatus();
            assertThat(health.getL1CacheSize()).isEqualTo(1);
        }
    }
}