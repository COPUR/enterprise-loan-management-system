package com.enterprise.openfinance.fxservices.infrastructure.cache;

import com.enterprise.openfinance.fxservices.domain.model.FxQuoteItemResult;
import com.enterprise.openfinance.fxservices.domain.port.out.FxCachePort;
import com.enterprise.openfinance.fxservices.infrastructure.config.FxServicesCacheProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryFxCacheAdapter implements FxCachePort {

    private final ConcurrentHashMap<String, CacheEntry<FxQuoteItemResult>> cache = new ConcurrentHashMap<>();
    private final int maxEntries;

    @Autowired
    public InMemoryFxCacheAdapter(FxServicesCacheProperties properties) {
        this(properties.getMaxEntries());
    }

    InMemoryFxCacheAdapter(int maxEntries) {
        this.maxEntries = Math.max(1, maxEntries);
    }

    @Override
    public Optional<FxQuoteItemResult> getQuote(String key, Instant now) {
        CacheEntry<FxQuoteItemResult> entry = cache.get(key);
        if (entry == null || !entry.expiresAt().isAfter(now)) {
            cache.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.value().withCacheHit(true));
    }

    @Override
    public void putQuote(String key, FxQuoteItemResult result, Instant expiresAt) {
        if (cache.size() >= maxEntries && !cache.containsKey(key)) {
            String oldest = cache.keys().nextElement();
            cache.remove(oldest);
        }
        cache.put(key, new CacheEntry<>(result.withCacheHit(false), expiresAt));
    }

    private record CacheEntry<T>(T value, Instant expiresAt) {
    }
}
