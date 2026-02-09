package com.enterprise.openfinance.uc10.infrastructure.cache;

import com.enterprise.openfinance.uc10.domain.model.MotorQuoteItemResult;
import com.enterprise.openfinance.uc10.domain.port.out.MotorQuoteCachePort;
import com.enterprise.openfinance.uc10.infrastructure.config.Uc10CacheProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryMotorQuoteCacheAdapter implements MotorQuoteCachePort {

    private final ConcurrentHashMap<String, CacheEntry<MotorQuoteItemResult>> cache = new ConcurrentHashMap<>();
    private final int maxEntries;

    @Autowired
    public InMemoryMotorQuoteCacheAdapter(Uc10CacheProperties properties) {
        this(properties.getMaxEntries());
    }

    InMemoryMotorQuoteCacheAdapter(int maxEntries) {
        this.maxEntries = Math.max(1, maxEntries);
    }

    @Override
    public Optional<MotorQuoteItemResult> getQuote(String key, Instant now) {
        CacheEntry<MotorQuoteItemResult> entry = cache.get(key);
        if (entry == null || !entry.expiresAt().isAfter(now)) {
            cache.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.value().withCacheHit(true));
    }

    @Override
    public void putQuote(String key, MotorQuoteItemResult result, Instant expiresAt) {
        if (cache.size() >= maxEntries && !cache.containsKey(key)) {
            String oldestKey = cache.keys().nextElement();
            cache.remove(oldestKey);
        }
        cache.put(key, new CacheEntry<>(result.withCacheHit(false), expiresAt));
    }

    private record CacheEntry<T>(T value, Instant expiresAt) {
    }
}
