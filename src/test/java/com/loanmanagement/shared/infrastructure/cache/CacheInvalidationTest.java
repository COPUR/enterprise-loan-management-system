package com.loanmanagement.shared.infrastructure.cache;

import com.loanmanagement.shared.infrastructure.cache.service.CacheInvalidationService;
import com.loanmanagement.shared.infrastructure.cache.service.CacheInvalidationServiceImpl;
import com.loanmanagement.shared.infrastructure.cache.service.BankingCacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test-Driven Development tests for Cache Invalidation Strategies
 * These tests verify intelligent cache eviction patterns and consistency mechanisms
 */
@DisplayName("Cache Invalidation Strategies - TDD Tests")
class CacheInvalidationTest {

    private CacheInvalidationService invalidationService;
    private RedisTemplate<String, Object> redisTemplate;
    private BankingCacheService cacheService;
    private TaskScheduler taskScheduler;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        cacheService = mock(BankingCacheService.class);
        taskScheduler = mock(TaskScheduler.class);
        
        invalidationService = new CacheInvalidationServiceImpl(
            redisTemplate, cacheService, taskScheduler
        );
    }

    @Test
    @DisplayName("Should invalidate cache entries by pattern")
    void shouldInvalidateCacheEntriesByPattern() {
        // Given: Pattern-based invalidation request
        String pattern = "customer:*";
        
        // When: Invalidate by pattern
        assertThatNoException().isThrownBy(() -> 
            invalidationService.invalidateByPattern(pattern)
        );
        
        // Then: Should execute Redis pattern deletion
        // Note: Actual Redis interaction would be tested in integration tests
        var statistics = invalidationService.getInvalidationStatistics();
        assertThat(statistics.getPatternInvalidations()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should invalidate customer-related caches comprehensively")
    void shouldInvalidateCustomerRelatedCachesComprehensively() {
        // Given: Customer with cached data
        String customerId = "CUST-001";
        
        // When: Invalidate customer-related caches
        assertThatNoException().isThrownBy(() -> 
            invalidationService.invalidateCustomerRelatedCaches(customerId)
        );
        
        // Then: Should invalidate customer and related caches
        verify(cacheService).invalidateCustomer(customerId);
        
        var statistics = invalidationService.getInvalidationStatistics();
        assertThat(statistics.getEntityInvalidations()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should handle cascade invalidation with dependencies")
    void shouldHandleCascadeInvalidationWithDependencies() {
        // Given: Key with dependencies
        String parentKey = "customer:CUST-001";
        String dependentKey1 = "loan:customer:CUST-001:LOAN-001";
        String dependentKey2 = "payment_history:CUST-001";
        
        Set<String> dependentKeys = Set.of(dependentKey1, dependentKey2);
        
        // Register dependencies
        invalidationService.registerDependency(parentKey, dependentKey1);
        invalidationService.registerDependency(parentKey, dependentKey2);
        
        // When: Invalidate with dependencies
        assertThatNoException().isThrownBy(() -> 
            invalidationService.invalidateWithDependencies(parentKey, dependentKeys)
        );
        
        // Then: Should track cascade invalidations
        var statistics = invalidationService.getInvalidationStatistics();
        assertThat(statistics.getCascadeInvalidations()).isGreaterThan(0);
        assertThat(statistics.getDependencies()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should schedule and manage timed invalidations")
    void shouldScheduleAndManageTimedInvalidations() {
        // Given: Key to be invalidated after delay
        String key = "temporary:session:123";
        Duration delay = Duration.ofMinutes(30);
        
        // When: Schedule invalidation
        assertThatNoException().isThrownBy(() -> 
            invalidationService.scheduleInvalidation(key, delay)
        );
        
        // Then: Should schedule the invalidation
        verify(taskScheduler).schedule(any(Runnable.class), any());
        
        // When: Cancel scheduled invalidation
        assertThatNoException().isThrownBy(() -> 
            invalidationService.cancelScheduledInvalidation(key)
        );
        
        // Then: Should handle cancellation gracefully
        // (Actual cancellation verification would require more complex mocking)
    }

    @Test
    @DisplayName("Should support conditional invalidation")
    void shouldSupportConditionalInvalidation() {
        // Given: Key with cached value and condition
        String key = "user:session:active";
        Object cachedValue = "session_data";
        
        when(redisTemplate.opsForValue().get(key)).thenReturn(cachedValue);
        
        CacheInvalidationService.InvalidationCondition condition = 
            value -> value.toString().contains("expired");
        
        // When: Apply conditional invalidation (should not invalidate)
        assertThatNoException().isThrownBy(() -> 
            invalidationService.invalidateIf(key, condition)
        );
        
        // Then: Should not delete the key (condition not met)
        verify(redisTemplate, never()).delete(key);
        
        // When: Apply condition that matches
        CacheInvalidationService.InvalidationCondition matchingCondition = 
            value -> value.toString().contains("session");
        
        assertThatNoException().isThrownBy(() -> 
            invalidationService.invalidateIf(key, matchingCondition)
        );
        
        // Then: Should delete the key (condition met)
        verify(redisTemplate).delete(key);
    }

    @Test
    @DisplayName("Should handle entity lifecycle events")
    void shouldHandleEntityLifecycleEvents() {
        // Given: Entity update event
        String entityType = "customer";
        String entityId = "CUST-001";
        
        // When: Handle entity update
        assertThatNoException().isThrownBy(() -> 
            invalidationService.onEntityUpdated(entityType, entityId)
        );
        
        // Then: Should invalidate customer-related caches
        verify(cacheService).invalidateCustomer(entityId);
        
        // When: Handle entity deletion
        assertThatNoException().isThrownBy(() -> 
            invalidationService.onEntityDeleted(entityType, entityId)
        );
        
        // Then: Should perform aggressive invalidation for deleted entity
        var events = invalidationService.getRecentInvalidations();
        assertThat(events).isNotEmpty();
        assertThat(events.stream())
            .anyMatch(event -> event.getType().equals("ENTITY_DELETE"));
    }

    @Test
    @DisplayName("Should track invalidation statistics accurately")
    void shouldTrackInvalidationStatisticsAccurately() {
        // Given: Various invalidation operations
        invalidationService.invalidateByPattern("test:*");
        invalidationService.invalidateCustomerRelatedCaches("CUST-001");
        invalidationService.invalidateExpiredEntries();
        
        // When: Get statistics
        var statistics = invalidationService.getInvalidationStatistics();
        
        // Then: Should have accurate counts
        assertThat(statistics.getPatternInvalidations()).isEqualTo(1);
        assertThat(statistics.getEntityInvalidations()).isEqualTo(1);
        assertThat(statistics.getTimeBasedInvalidations()).isEqualTo(1);
        
        // When: Reset statistics
        invalidationService.resetInvalidationStatistics();
        statistics = invalidationService.getInvalidationStatistics();
        
        // Then: Should reset all counts
        assertThat(statistics.getPatternInvalidations()).isEqualTo(0);
        assertThat(statistics.getEntityInvalidations()).isEqualTo(0);
        assertThat(statistics.getTimeBasedInvalidations()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should support bulk invalidation operations")
    void shouldSupportBulkInvalidationOperations() {
        // Given: Multiple keys to invalidate
        List<String> keys = List.of(
            "customer:CUST-001",
            "customer:CUST-002", 
            "loan:LOAN-001",
            "payment:PAY-001"
        );
        
        // When: Perform bulk invalidation
        assertThatNoException().isThrownBy(() -> 
            invalidationService.invalidateMultiple(keys)
        );
        
        // Then: Should delete all keys
        verify(redisTemplate).delete(keys);
        
        // When: Warm cache after invalidation
        assertThatNoException().isThrownBy(() -> 
            invalidationService.warmAfterInvalidation(keys)
        );
        
        // Then: Should attempt cache warming
        var events = invalidationService.getRecentInvalidations();
        assertThat(events.stream())
            .anyMatch(event -> event.getType().equals("WARM"));
    }

    @Test
    @DisplayName("Should manage dependency relationships")
    void shouldManageDependencyRelationships() {
        // Given: Parent-child cache relationships
        String parentKey = "customer:CUST-001";
        String childKey1 = "loan:customer:CUST-001:LOAN-001";
        String childKey2 = "payment:customer:CUST-001:PAY-001";
        
        // When: Register dependencies
        invalidationService.registerDependency(parentKey, childKey1);
        invalidationService.registerDependency(parentKey, childKey2);
        
        // Then: Should track dependencies
        var statistics = invalidationService.getInvalidationStatistics();
        assertThat(statistics.getDependencies()).isGreaterThan(0);
        
        // When: Unregister dependency
        invalidationService.unregisterDependency(parentKey, childKey1);
        
        // Then: Should remove specific dependency
        // (Verification would require access to internal dependency map)
        
        // When: Invalidate parent with cascade
        Set<String> dependents = Set.of(childKey2);
        assertThatNoException().isThrownBy(() -> 
            invalidationService.invalidateWithDependencies(parentKey, dependents)
        );
        
        // Then: Should cascade to dependents
        verify(redisTemplate).delete(dependents);
    }

    @Test
    @DisplayName("Should handle relationship change events")
    void shouldHandleRelationshipChangeEvents() {
        // Given: Relationship change between entities
        String parentEntity = "customer:CUST-001";
        String childEntity = "loan:LOAN-001";
        
        // When: Handle relationship change
        assertThatNoException().isThrownBy(() -> 
            invalidationService.onRelationshipChanged(parentEntity, childEntity)
        );
        
        // Then: Should invalidate both entities
        var events = invalidationService.getRecentInvalidations();
        assertThat(events.stream())
            .anyMatch(event -> event.getType().equals("RELATIONSHIP"));
    }

    @Test
    @DisplayName("Should provide comprehensive invalidation event logging")
    void shouldProvideComprehensiveInvalidationEventLogging() {
        // Given: Various invalidation operations
        invalidationService.invalidateByPattern("test:*");
        invalidationService.invalidateCustomerRelatedCaches("CUST-001");
        invalidationService.onEntityUpdated("loan", "LOAN-001");
        
        // When: Get recent invalidation events
        var events = invalidationService.getRecentInvalidations();
        
        // Then: Should log all invalidation events
        assertThat(events).isNotEmpty();
        assertThat(events).hasSize(3);
        
        // And: Events should have proper types
        assertThat(events.stream().map(event -> event.getType()))
            .contains("PATTERN", "CUSTOMER", "ENTITY_UPDATE");
        
        // And: Events should have timestamps
        assertThat(events.stream().map(event -> event.getTimestamp()))
            .allMatch(timestamp -> timestamp != null);
    }
}