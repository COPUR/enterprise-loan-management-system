package com.bank.infrastructure.resilience.pattern;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Bulkhead pattern implementation to isolate resources and prevent cascading failures.
 * Provides both semaphore and thread pool isolation strategies.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BulkheadPattern {
    
    private final BulkheadRegistry bulkheadRegistry;
    private final ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry;
    private final MeterRegistry meterRegistry;
    
    /**
     * Execute with semaphore-based bulkhead (for I/O bound operations)
     */
    public <T> T executeWithSemaphoreBulkhead(
            String resourceName,
            Supplier<T> supplier,
            BulkheadProfile profile) {
        
        Bulkhead bulkhead = getOrCreateSemaphoreBulkhead(resourceName, profile);
        
        try {
            return bulkhead.executeSupplier(supplier);
        } catch (Exception e) {
            log.error("Bulkhead execution failed for resource: {}", resourceName, e);
            recordBulkheadRejection(resourceName, "semaphore");
            throw new BulkheadFullException(resourceName, profile.getMaxConcurrentCalls());
        }
    }
    
    /**
     * Execute with thread pool bulkhead (for CPU bound operations)
     */
    public <T> CompletableFuture<T> executeWithThreadPoolBulkhead(
            String resourceName,
            Supplier<T> supplier,
            ThreadPoolBulkheadProfile profile) {
        
        ThreadPoolBulkhead bulkhead = getOrCreateThreadPoolBulkhead(resourceName, profile);
        
        return bulkhead.executeSupplier(supplier)
                .exceptionally(throwable -> {
                    log.error("Thread pool bulkhead execution failed for resource: {}", 
                            resourceName, throwable);
                    recordBulkheadRejection(resourceName, "threadpool");
                    throw new BulkheadFullException(resourceName, profile.getMaxThreadPoolSize());
                });
    }
    
    /**
     * Execute with timeout and bulkhead protection
     */
    public <T> T executeWithTimeoutAndBulkhead(
            String resourceName,
            Supplier<T> supplier,
            Duration timeout,
            BulkheadProfile profile) {
        
        Bulkhead bulkhead = getOrCreateSemaphoreBulkhead(resourceName, profile);
        
        CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> 
            bulkhead.executeSupplier(supplier)
        );
        
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.error("Operation timed out for resource: {} after {}ms", 
                    resourceName, timeout.toMillis());
            throw new OperationTimeoutException(resourceName, timeout);
        } catch (Exception e) {
            log.error("Operation failed for resource: {}", resourceName, e);
            throw new RuntimeException("Operation failed", e);
        }
    }
    
    /**
     * Check if bulkhead has available permits
     */
    public boolean hasAvailablePermits(String resourceName) {
        try {
            Bulkhead bulkhead = bulkheadRegistry.bulkhead(resourceName);
            return bulkhead.getMetrics().getAvailableConcurrentCalls() > 0;
        } catch (Exception e) {
            return true; // Default to available if bulkhead doesn't exist
        }
    }
    
    /**
     * Get current bulkhead metrics
     */
    public BulkheadMetrics getMetrics(String resourceName) {
        try {
            Bulkhead bulkhead = bulkheadRegistry.bulkhead(resourceName);
            Bulkhead.Metrics metrics = bulkhead.getMetrics();
            
            return BulkheadMetrics.builder()
                    .resourceName(resourceName)
                    .availableConcurrentCalls(metrics.getAvailableConcurrentCalls())
                    .maxAllowedConcurrentCalls(metrics.getMaxAllowedConcurrentCalls())
                    .build();
        } catch (Exception e) {
            log.warn("Unable to get metrics for bulkhead: {}", resourceName);
            return BulkheadMetrics.empty(resourceName);
        }
    }
    
    /**
     * Dynamically adjust bulkhead size based on system load
     */
    public void adjustBulkheadSize(String resourceName, int newSize) {
        log.info("Adjusting bulkhead size for {} to {}", resourceName, newSize);
        
        // Create new bulkhead with adjusted size
        BulkheadConfig newConfig = BulkheadConfig.custom()
                .maxConcurrentCalls(newSize)
                .maxWaitDuration(Duration.ofMillis(100))
                .build();
        
        // Replace existing bulkhead
        bulkheadRegistry.replace(resourceName, Bulkhead.of(resourceName, newConfig));
        
        // Record adjustment
        meterRegistry.counter("bulkhead.size.adjustment",
                "resource", resourceName,
                "new_size", String.valueOf(newSize)
        ).increment();
    }
    
    /**
     * Create composite bulkhead for resource isolation
     */
    public <T> T executeWithCompositeBulkhead(
            String primaryResource,
            String secondaryResource,
            Supplier<T> supplier) {
        
        // First acquire primary resource
        Bulkhead primaryBulkhead = getOrCreateSemaphoreBulkhead(
                primaryResource, BulkheadProfile.defaultProfile());
        
        return primaryBulkhead.executeSupplier(() -> {
            // Then acquire secondary resource
            Bulkhead secondaryBulkhead = getOrCreateSemaphoreBulkhead(
                    secondaryResource, BulkheadProfile.defaultProfile());
            
            return secondaryBulkhead.executeSupplier(supplier);
        });
    }
    
    // Private helper methods
    
    private Bulkhead getOrCreateSemaphoreBulkhead(String resourceName, BulkheadProfile profile) {
        return bulkheadRegistry.bulkhead(resourceName, () -> {
            BulkheadConfig config = BulkheadConfig.custom()
                    .maxConcurrentCalls(profile.getMaxConcurrentCalls())
                    .maxWaitDuration(profile.getMaxWaitDuration())
                    .build();
            
            Bulkhead bulkhead = Bulkhead.of(resourceName, config);
            
            // Register metrics
            registerBulkheadMetrics(bulkhead);
            
            // Register event listeners
            registerBulkheadEventListeners(bulkhead);
            
            return config;
        });
    }
    
    private ThreadPoolBulkhead getOrCreateThreadPoolBulkhead(
            String resourceName, ThreadPoolBulkheadProfile profile) {
        
        return threadPoolBulkheadRegistry.bulkhead(resourceName, () -> {
            ThreadPoolBulkheadConfig config = ThreadPoolBulkheadConfig.custom()
                    .maxThreadPoolSize(profile.getMaxThreadPoolSize())
                    .coreThreadPoolSize(profile.getCoreThreadPoolSize())
                    .queueCapacity(profile.getQueueCapacity())
                    .keepAliveDuration(profile.getKeepAliveDuration())
                    .build();
            
            ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of(resourceName, config);
            
            // Register metrics
            registerThreadPoolBulkheadMetrics(bulkhead);
            
            return config;
        });
    }
    
    private void registerBulkheadMetrics(Bulkhead bulkhead) {
        String resourceName = bulkhead.getName();
        
        // Available concurrent calls gauge
        meterRegistry.gauge("bulkhead.available.concurrent.calls",
                Tags.of("resource", resourceName),
                bulkhead,
                b -> b.getMetrics().getAvailableConcurrentCalls());
        
        // Max allowed concurrent calls gauge
        meterRegistry.gauge("bulkhead.max.allowed.concurrent.calls",
                Tags.of("resource", resourceName),
                bulkhead,
                b -> b.getMetrics().getMaxAllowedConcurrentCalls());
    }
    
    private void registerThreadPoolBulkheadMetrics(ThreadPoolBulkhead bulkhead) {
        String resourceName = bulkhead.getName();
        
        // Queue depth gauge
        meterRegistry.gauge("bulkhead.threadpool.queue.depth",
                Tags.of("resource", resourceName),
                bulkhead,
                b -> b.getMetrics().getQueueDepth());
        
        // Thread pool size gauge
        meterRegistry.gauge("bulkhead.threadpool.size",
                Tags.of("resource", resourceName),
                bulkhead,
                b -> b.getMetrics().getThreadPoolSize());
        
        // Core thread pool size gauge
        meterRegistry.gauge("bulkhead.threadpool.core.size",
                Tags.of("resource", resourceName),
                bulkhead,
                b -> b.getMetrics().getCoreThreadPoolSize());
    }
    
    private void registerBulkheadEventListeners(Bulkhead bulkhead) {
        bulkhead.getEventPublisher()
                .onCallPermitted(event -> 
                    log.debug("Bulkhead {} permitted call", event.getBulkheadName()))
                .onCallRejected(event -> {
                    log.warn("Bulkhead {} rejected call - bulkhead full", 
                            event.getBulkheadName());
                    recordBulkheadRejection(event.getBulkheadName(), "semaphore");
                })
                .onCallFinished(event -> 
                    log.debug("Bulkhead {} call finished", event.getBulkheadName()));
    }
    
    private void recordBulkheadRejection(String resourceName, String bulkheadType) {
        meterRegistry.counter("bulkhead.calls.rejected",
                "resource", resourceName,
                "type", bulkheadType
        ).increment();
    }
}

// Supporting classes

@lombok.Data
@lombok.Builder
class BulkheadProfile {
    private int maxConcurrentCalls;
    private Duration maxWaitDuration;
    
    public static BulkheadProfile defaultProfile() {
        return BulkheadProfile.builder()
                .maxConcurrentCalls(25)
                .maxWaitDuration(Duration.ofMillis(100))
                .build();
    }
    
    public static BulkheadProfile criticalService() {
        return BulkheadProfile.builder()
                .maxConcurrentCalls(50)
                .maxWaitDuration(Duration.ofMillis(500))
                .build();
    }
    
    public static BulkheadProfile backgroundTask() {
        return BulkheadProfile.builder()
                .maxConcurrentCalls(10)
                .maxWaitDuration(Duration.ZERO)
                .build();
    }
}

@lombok.Data
@lombok.Builder
class ThreadPoolBulkheadProfile {
    private int maxThreadPoolSize;
    private int coreThreadPoolSize;
    private int queueCapacity;
    private Duration keepAliveDuration;
    
    public static ThreadPoolBulkheadProfile defaultProfile() {
        return ThreadPoolBulkheadProfile.builder()
                .maxThreadPoolSize(10)
                .coreThreadPoolSize(5)
                .queueCapacity(100)
                .keepAliveDuration(Duration.ofMinutes(1))
                .build();
    }
}

@lombok.Data
@lombok.Builder
class BulkheadMetrics {
    private String resourceName;
    private int availableConcurrentCalls;
    private int maxAllowedConcurrentCalls;
    
    public static BulkheadMetrics empty(String resourceName) {
        return BulkheadMetrics.builder()
                .resourceName(resourceName)
                .availableConcurrentCalls(0)
                .maxAllowedConcurrentCalls(0)
                .build();
    }
}

class BulkheadFullException extends RuntimeException {
    public BulkheadFullException(String resourceName, int maxConcurrentCalls) {
        super(String.format("Bulkhead full for resource '%s'. Max concurrent calls: %d", 
                resourceName, maxConcurrentCalls));
    }
}

class OperationTimeoutException extends RuntimeException {
    public OperationTimeoutException(String resourceName, Duration timeout) {
        super(String.format("Operation timed out for resource '%s' after %dms", 
                resourceName, timeout.toMillis()));
    }
}