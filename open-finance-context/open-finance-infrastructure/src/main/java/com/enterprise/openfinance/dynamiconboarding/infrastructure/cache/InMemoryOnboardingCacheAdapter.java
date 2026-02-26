package com.enterprise.openfinance.dynamiconboarding.infrastructure.cache;

import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingAccountItemResult;
import com.enterprise.openfinance.dynamiconboarding.domain.port.out.OnboardingCachePort;
import com.enterprise.openfinance.dynamiconboarding.infrastructure.config.DynamicOnboardingCacheProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryOnboardingCacheAdapter implements OnboardingCachePort {

    private final ConcurrentHashMap<String, CacheEntry<OnboardingAccountItemResult>> cache = new ConcurrentHashMap<>();
    private final int maxEntries;

    @Autowired
    public InMemoryOnboardingCacheAdapter(DynamicOnboardingCacheProperties properties) {
        this(properties.getMaxEntries());
    }

    InMemoryOnboardingCacheAdapter(int maxEntries) {
        this.maxEntries = Math.max(1, maxEntries);
    }

    @Override
    public Optional<OnboardingAccountItemResult> getAccount(String key, Instant now) {
        CacheEntry<OnboardingAccountItemResult> entry = cache.get(key);
        if (entry == null || !entry.expiresAt().isAfter(now)) {
            cache.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.value().withCacheHit(true));
    }

    @Override
    public void putAccount(String key, OnboardingAccountItemResult result, Instant expiresAt) {
        if (cache.size() >= maxEntries && !cache.containsKey(key)) {
            String oldest = cache.keys().nextElement();
            cache.remove(oldest);
        }
        cache.put(key, new CacheEntry<>(result.withCacheHit(false), expiresAt));
    }

    private record CacheEntry<T>(T value, Instant expiresAt) {
    }
}
