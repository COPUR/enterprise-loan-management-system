package com.enterprise.openfinance.uc13.infrastructure.cache;

import com.enterprise.openfinance.uc13.domain.model.PayRequestResult;
import com.enterprise.openfinance.uc13.domain.port.out.PayRequestCachePort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryPayRequestCacheAdapter implements PayRequestCachePort {

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Override
    public Optional<PayRequestResult> getStatus(String key, Instant now) {
        CacheEntry entry = cache.get(key);
        if (entry == null || now.isAfter(entry.expiresAt())) {
            cache.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.result().withCacheHit(true));
    }

    @Override
    public void putStatus(String key, PayRequestResult result, Instant expiresAt) {
        cache.put(key, new CacheEntry(result.withCacheHit(false), expiresAt));
    }

    private record CacheEntry(PayRequestResult result, Instant expiresAt) {
    }
}
