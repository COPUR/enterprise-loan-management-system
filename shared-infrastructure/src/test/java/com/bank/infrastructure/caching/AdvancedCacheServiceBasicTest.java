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
 * Basic TDD Test Suite for Advanced Cache Service
 * 
 * Tests L1 cache functionality without Redis dependencies:
 * - L1 cache operations
 * - Cache statistics
 * - Cache health monitoring
 * - Group invalidation
 * - Cache warming
 * 
 * Note: This test focuses on L1 cache to avoid Java 23/Mockito compatibility issues
 */
@DisplayName("Advanced Cache Service Basic Tests")
class AdvancedCacheServiceBasicTest {
    
    private AdvancedCacheService advancedCacheService;
    
    @BeforeEach
    void setUp() {
        // Create service with null RedisTemplate to focus on L1 cache testing
        advancedCacheService = new AdvancedCacheService(null);
    }
    
    @Nested
    @DisplayName("L1 Cache Operations Tests")
    class L1CacheOperationsTests {
        
        @Test
        @DisplayName("Should store and retrieve data from L1 cache")
        void shouldStoreAndRetrieveDataFromL1Cache() {
            // Given
            String key = "customer:123";
            String value = "customer data";
            
            // When
            advancedCacheService.setAdvanced(key, value, Duration.ofMinutes(10));
            String result = advancedCacheService.getAdvanced(key);
            
            // Then
            assertThat(result).isEqualTo(value);
            
            // Verify statistics
            CacheStatistics stats = advancedCacheService.getStatistics();
            assertThat(stats.getL1Hits()).isEqualTo(1);
            assertThat(stats.getWrites()).isEqualTo(1);
        }
        
        @Test
        @DisplayName("Should return null for non-existent keys")
        void shouldReturnNullForNonExistentKeys() {
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
        
        @Test
        @DisplayName("Should overwrite existing cache entries")
        void shouldOverwriteExistingCacheEntries() {
            // Given
            String key = "customer:123";
            String value1 = "customer data 1";
            String value2 = "customer data 2";
            
            // When
            advancedCacheService.setAdvanced(key, value1, Duration.ofMinutes(10));
            advancedCacheService.setAdvanced(key, value2, Duration.ofMinutes(10));
            String result = advancedCacheService.getAdvanced(key);
            
            // Then
            assertThat(result).isEqualTo(value2);
            
            // Verify statistics
            CacheStatistics stats = advancedCacheService.getStatistics();
            assertThat(stats.getL1Hits()).isEqualTo(1);
            assertThat(stats.getWrites()).isEqualTo(2);
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
            
            // Verify statistics
            CacheStatistics stats = advancedCacheService.getStatistics();
            assertThat(stats.getWrites()).isEqualTo(3);
            assertThat(stats.getL1Hits()).isEqualTo(3);
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
        
        @Test
        @DisplayName("Should handle group invalidation for non-existent group")
        void shouldHandleGroupInvalidationForNonExistentGroup() {
            // Given
            String nonExistentGroup = "nonexistent";
            
            // When
            advancedCacheService.invalidateGroup(nonExistentGroup);
            
            // Then - No exception should be thrown
            CacheStatistics stats = advancedCacheService.getStatistics();
            assertThat(stats.getInvalidations()).isEqualTo(0);
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
        
        @Test
        @DisplayName("Should handle zero operations correctly")
        void shouldHandleZeroOperationsCorrectly() {
            // Given - No operations performed
            
            // When
            CacheStatistics stats = advancedCacheService.getStatistics();
            
            // Then
            assertThat(stats.getTotalOperations()).isEqualTo(0);
            assertThat(stats.getHitRate()).isEqualTo(0.0);
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
        @DisplayName("Should report cache size correctly")
        void shouldReportCacheSizeCorrectly() {
            // Given
            Map<String, String> entries = new HashMap<>();
            entries.put("customer:1", "data1");
            entries.put("customer:2", "data2");
            entries.put("customer:3", "data3");
            
            // When
            advancedCacheService.setBatch(entries, Duration.ofMinutes(10));
            
            // Then
            CacheHealthStatus health = advancedCacheService.getHealthStatus();
            assertThat(health.getL1CacheSize()).isEqualTo(3);
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
            
            // Verify cache size is zero
            CacheHealthStatus health = advancedCacheService.getHealthStatus();
            assertThat(health.getL1CacheSize()).isEqualTo(0);
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
            assertThat(stats.getWrites()).isEqualTo(2);
            assertThat(stats.getL1Hits()).isEqualTo(3); // customerKey (hit), loanKey (hit), loanKey (hit after invalidation)
        }
    }
}