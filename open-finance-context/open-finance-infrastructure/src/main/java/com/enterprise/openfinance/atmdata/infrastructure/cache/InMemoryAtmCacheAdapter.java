package com.enterprise.openfinance.atmdata.infrastructure.cache;

import com.enterprise.openfinance.atmdata.domain.model.AtmListResult;
import com.enterprise.openfinance.atmdata.domain.port.out.AtmCachePort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryAtmCacheAdapter implements AtmCachePort {

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Override
    public Optional<AtmListResult> getAtms(String key, Instant now) {
        CacheEntry entry = cache.get(key);
        if (entry == null || now.isAfter(entry.expiresAt())) {
            cache.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.result().withCacheHit(true));
    }

    @Override
    public void putAtms(String key, AtmListResult result, Instant expiresAt) {
        cache.put(key, new CacheEntry(result.withCacheHit(false), expiresAt));
    }

    private record CacheEntry(AtmListResult result, Instant expiresAt) {
    }
}
