package com.bank.infrastructure.caching;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Multi-Level Cache Service for Banking Platform
 * 
 * Implements a sophisticated 3-level caching strategy:
 * 
 * L1 Cache (Caffeine): In-memory, ultra-fast access for hot data
 * - Customer profiles currently being processed
 * - Active loan calculations
 * - Session-level data
 * 
 * L2 Cache (Redis): Distributed cache for warm data
 * - Customer credit profiles
 * - Loan portfolios
 * - Event aggregates
 * 
 * L3 Cache (Database): Persistent storage with materialized views
 * - Analytics data
 * - Report results
 * - Historical aggregates
 */
@Service
public class MultiLevelCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final MeterRegistry meterRegistry;
    
    // L1 Cache - In-memory (Caffeine)
    private final Map<String, Cache<String, Object>> l1Caches = new ConcurrentHashMap<>();
    
    // Cache configuration for different data types
    private static final Map<String, CacheConfiguration> CACHE_CONFIGS = Map.of(
        "customers", new CacheConfiguration(1000, Duration.ofMinutes(15), Duration.ofMinutes(30)),
        "loans", new CacheConfiguration(2000, Duration.ofMinutes(30), Duration.ofHours(2)),
        "payments", new CacheConfiguration(500, Duration.ofMinutes(5), Duration.ofMinutes(30)),
        "creditProfiles", new CacheConfiguration(1500, Duration.ofHours(1), Duration.ofHours(4)),
        "portfolios", new CacheConfiguration(800, Duration.ofHours(2), Duration.ofHours(8)),
        "analytics", new CacheConfiguration(200, Duration.ofHours(6), Duration.ofHours(24)),
        "islamicContracts", new CacheConfiguration(1000, Duration.ofMinutes(30), Duration.ofHours(2)),
        "shariahCompliance", new CacheConfiguration(300, Duration.ofHours(4), Duration.ofHours(12))
    );
    
    // Metrics
    private final Timer l1CacheTimer;
    private final Timer l2CacheTimer;
    private final Counter l1CacheHits;
    private final Counter l1CacheMisses;
    private final Counter l2CacheHits;
    private final Counter l2CacheMisses;
    private final Counter cacheEvictions;
    
    public MultiLevelCacheService(RedisTemplate<String, Object> redisTemplate, MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics
        this.l1CacheTimer = Timer.builder("cache.l1.access")
            .description("L1 cache access time")
            .register(meterRegistry);
        this.l2CacheTimer = Timer.builder("cache.l2.access")
            .description("L2 cache access time")
            .register(meterRegistry);
        this.l1CacheHits = Counter.builder("cache.l1.hits")
            .description("L1 cache hits")
            .register(meterRegistry);
        this.l1CacheMisses = Counter.builder("cache.l1.misses")
            .description("L1 cache misses")
            .register(meterRegistry);
        this.l2CacheHits = Counter.builder("cache.l2.hits")
            .description("L2 cache hits")
            .register(meterRegistry);
        this.l2CacheMisses = Counter.builder("cache.l2.misses")
            .description("L2 cache misses")
            .register(meterRegistry);
        this.cacheEvictions = Counter.builder("cache.evictions")
            .description("Cache evictions")
            .register(meterRegistry);
        
        // Initialize L1 caches
        initializeL1Caches();
    }
    
    /**
     * Get value from cache with automatic fallback through cache levels
     */
    public <T> T get(String cacheType, String key, Class<T> valueType, Function<String, T> loader) {
        return get(cacheType, key, valueType, loader, false);
    }
    
    /**
     * Get value from cache with option to bypass L1 cache
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String cacheType, String key, Class<T> valueType, Function<String, T> loader, boolean bypassL1) {
        String fullKey = buildKey(cacheType, key);
        
        // Try L1 cache first (unless bypassed)
        if (!bypassL1) {
            T l1Value = l1CacheTimer.recordCallable(() -> {
                Cache<String, Object> l1Cache = getL1Cache(cacheType);
                Object cachedValue = l1Cache.getIfPresent(fullKey);
                if (cachedValue != null) {
                    l1CacheHits.increment();
                    return (T) cachedValue;
                }
                l1CacheMisses.increment();
                return null;
            });
            
            if (l1Value != null) {
                return l1Value;
            }
        }
        
        // Try L2 cache (Redis)
        T l2Value = l2CacheTimer.recordCallable(() -> {
            try {
                Object cachedValue = redisTemplate.opsForValue().get(fullKey);
                if (cachedValue != null) {
                    l2CacheHits.increment();
                    
                    // Populate L1 cache for future requests
                    if (!bypassL1) {
                        populateL1Cache(cacheType, fullKey, cachedValue);
                    }
                    
                    return (T) cachedValue;
                }
                l2CacheMisses.increment();
                return null;
            } catch (Exception e) {
                meterRegistry.counter("cache.l2.errors").increment();
                return null;
            }
        });
        
        if (l2Value != null) {
            return l2Value;
        }
        
        // Load from source (L3 - database)
        T loadedValue = loader.apply(key);
        if (loadedValue != null) {
            // Store in both cache levels
            put(cacheType, key, loadedValue);
        }
        
        return loadedValue;
    }
    
    /**
     * Store value in appropriate cache levels
     */
    public <T> void put(String cacheType, String key, T value) {
        String fullKey = buildKey(cacheType, key);
        CacheConfiguration config = CACHE_CONFIGS.get(cacheType);
        
        if (config != null) {
            // Store in L1 cache
            Cache<String, Object> l1Cache = getL1Cache(cacheType);
            l1Cache.put(fullKey, value);
            
            // Store in L2 cache (Redis) with TTL
            try {
                redisTemplate.opsForValue().set(fullKey, value, config.getL2Ttl());
            } catch (Exception e) {
                meterRegistry.counter("cache.l2.write.errors").increment();
            }
        }
    }
    
    /**
     * Remove value from all cache levels
     */
    public void evict(String cacheType, String key) {
        String fullKey = buildKey(cacheType, key);
        
        // Remove from L1 cache
        Cache<String, Object> l1Cache = getL1Cache(cacheType);
        l1Cache.invalidate(fullKey);
        
        // Remove from L2 cache
        try {
            redisTemplate.delete(fullKey);
        } catch (Exception e) {
            meterRegistry.counter("cache.l2.eviction.errors").increment();
        }
        
        cacheEvictions.increment();
    }
    
    /**
     * Clear all caches for a specific type
     */
    public void evictAll(String cacheType) {
        // Clear L1 cache
        Cache<String, Object> l1Cache = getL1Cache(cacheType);
        l1Cache.invalidateAll();
        
        // Clear L2 cache (Redis) - delete by pattern
        try {
            String pattern = buildKey(cacheType, "*");
            redisTemplate.delete(redisTemplate.keys(pattern));
        } catch (Exception e) {
            meterRegistry.counter("cache.l2.clear.errors").increment();
        }
        
        meterRegistry.counter("cache.clear.all", "type", cacheType).increment();
    }
    
    /**
     * Get cache statistics
     */
    public MultiLevelCacheStats getCacheStats() {
        Map<String, CacheStats> l1Stats = new ConcurrentHashMap<>();
        
        for (Map.Entry<String, Cache<String, Object>> entry : l1Caches.entrySet()) {
            l1Stats.put(entry.getKey(), entry.getValue().stats());
        }
        
        return new MultiLevelCacheStats(
            l1Stats,
            l1CacheHits.count(),
            l1CacheMisses.count(),
            l2CacheHits.count(),
            l2CacheMisses.count(),
            cacheEvictions.count()
        );
    }
    
    /**
     * Warm up cache with commonly accessed data
     */
    public void warmUpCache(String cacheType, Map<String, Object> data) {
        data.forEach((key, value) -> put(cacheType, key, value));
        meterRegistry.counter("cache.warmup", "type", cacheType).increment();
    }
    
    /**
     * Health check for cache layers
     */
    public CacheHealthStatus getHealthStatus() {
        boolean l1Healthy = true;
        boolean l2Healthy = true;
        
        try {
            // Test L1 cache
            Cache<String, Object> testCache = getL1Cache("test");
            testCache.put("health-check", "ok");
            testCache.getIfPresent("health-check");
            testCache.invalidate("health-check");
        } catch (Exception e) {
            l1Healthy = false;
        }
        
        try {
            // Test L2 cache (Redis)
            redisTemplate.opsForValue().set("health-check", "ok", Duration.ofSeconds(5));
            redisTemplate.opsForValue().get("health-check");
            redisTemplate.delete("health-check");
        } catch (Exception e) {
            l2Healthy = false;
        }
        
        return new CacheHealthStatus(l1Healthy, l2Healthy);
    }
    
    // Private helper methods
    
    private void initializeL1Caches() {
        for (Map.Entry<String, CacheConfiguration> entry : CACHE_CONFIGS.entrySet()) {
            String cacheType = entry.getKey();
            CacheConfiguration config = entry.getValue();
            
            Cache<String, Object> cache = Caffeine.newBuilder()
                .maximumSize(config.getMaxSize())
                .expireAfterWrite(config.getL1Ttl())
                .recordStats()
                .evictionListener((key, value, cause) -> {
                    cacheEvictions.increment();
                    meterRegistry.counter("cache.l1.evictions", "type", cacheType, "cause", cause.name()).increment();
                })
                .build();
            
            l1Caches.put(cacheType, cache);
        }
    }
    
    private Cache<String, Object> getL1Cache(String cacheType) {
        return l1Caches.computeIfAbsent(cacheType, type -> {
            CacheConfiguration config = CACHE_CONFIGS.getOrDefault(type, 
                new CacheConfiguration(1000, Duration.ofMinutes(15), Duration.ofHours(1)));
            
            return Caffeine.newBuilder()
                .maximumSize(config.getMaxSize())
                .expireAfterWrite(config.getL1Ttl())
                .recordStats()
                .build();
        });
    }
    
    private void populateL1Cache(String cacheType, String key, Object value) {
        Cache<String, Object> l1Cache = getL1Cache(cacheType);
        l1Cache.put(key, value);
    }
    
    private String buildKey(String cacheType, String key) {
        return "banking:" + cacheType + ":" + key;
    }
    
    /**
     * Cache configuration for different data types
     */
    private static class CacheConfiguration {
        private final long maxSize;
        private final Duration l1Ttl;
        private final Duration l2Ttl;
        
        public CacheConfiguration(long maxSize, Duration l1Ttl, Duration l2Ttl) {
            this.maxSize = maxSize;
            this.l1Ttl = l1Ttl;
            this.l2Ttl = l2Ttl;
        }
        
        public long getMaxSize() { return maxSize; }
        public Duration getL1Ttl() { return l1Ttl; }
        public Duration getL2Ttl() { return l2Ttl; }
    }
    
    /**
     * Cache statistics aggregation
     */
    public static class MultiLevelCacheStats {
        private final Map<String, CacheStats> l1Stats;
        private final double l1Hits;
        private final double l1Misses;
        private final double l2Hits;
        private final double l2Misses;
        private final double evictions;
        
        public MultiLevelCacheStats(Map<String, CacheStats> l1Stats, double l1Hits, double l1Misses,
                                   double l2Hits, double l2Misses, double evictions) {
            this.l1Stats = l1Stats;
            this.l1Hits = l1Hits;
            this.l1Misses = l1Misses;
            this.l2Hits = l2Hits;
            this.l2Misses = l2Misses;
            this.evictions = evictions;
        }
        
        public double getL1HitRate() {
            return l1Hits + l1Misses > 0 ? l1Hits / (l1Hits + l1Misses) : 0;
        }
        
        public double getL2HitRate() {
            return l2Hits + l2Misses > 0 ? l2Hits / (l2Hits + l2Misses) : 0;
        }
        
        public double getOverallHitRate() {
            double totalHits = l1Hits + l2Hits;
            double totalRequests = l1Hits + l1Misses + l2Hits + l2Misses;
            return totalRequests > 0 ? totalHits / totalRequests : 0;
        }
        
        // Getters
        public Map<String, CacheStats> getL1Stats() { return l1Stats; }
        public double getL1Hits() { return l1Hits; }
        public double getL1Misses() { return l1Misses; }
        public double getL2Hits() { return l2Hits; }
        public double getL2Misses() { return l2Misses; }
        public double getEvictions() { return evictions; }
    }
    
    /**
     * Cache health status
     */
    public static class CacheHealthStatus {
        private final boolean l1Healthy;
        private final boolean l2Healthy;
        
        public CacheHealthStatus(boolean l1Healthy, boolean l2Healthy) {
            this.l1Healthy = l1Healthy;
            this.l2Healthy = l2Healthy;
        }
        
        public boolean isHealthy() {
            return l1Healthy && l2Healthy;
        }
        
        public boolean isL1Healthy() { return l1Healthy; }
        public boolean isL2Healthy() { return l2Healthy; }
    }
}