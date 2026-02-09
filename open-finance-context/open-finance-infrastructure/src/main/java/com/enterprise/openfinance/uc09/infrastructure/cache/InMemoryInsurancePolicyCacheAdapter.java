package com.enterprise.openfinance.uc09.infrastructure.cache;

import com.enterprise.openfinance.uc09.domain.model.InsurancePolicyItemResult;
import com.enterprise.openfinance.uc09.domain.model.InsurancePolicyListResult;
import com.enterprise.openfinance.uc09.domain.port.out.InsurancePolicyCachePort;
import com.enterprise.openfinance.uc09.infrastructure.config.Uc09CacheProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryInsurancePolicyCacheAdapter implements InsurancePolicyCachePort {

    private final Map<String, CacheItem<InsurancePolicyListResult>> listCache = new ConcurrentHashMap<>();
    private final Map<String, CacheItem<InsurancePolicyItemResult>> itemCache = new ConcurrentHashMap<>();
    private final int maxEntries;

    public InMemoryInsurancePolicyCacheAdapter(Uc09CacheProperties properties) {
        this.maxEntries = properties.getMaxEntries();
    }

    @Override
    public Optional<InsurancePolicyListResult> getPolicyList(String key, Instant now) {
        CacheItem<InsurancePolicyListResult> item = listCache.get(key);
        if (item == null) {
            return Optional.empty();
        }
        if (!item.expiresAt().isAfter(now)) {
            listCache.remove(key);
            return Optional.empty();
        }
        return Optional.of(item.value());
    }

    @Override
    public void putPolicyList(String key, InsurancePolicyListResult result, Instant expiresAt) {
        evictIfNeeded();
        listCache.put(key, new CacheItem<>(result, expiresAt));
    }

    @Override
    public Optional<InsurancePolicyItemResult> getPolicy(String key, Instant now) {
        CacheItem<InsurancePolicyItemResult> item = itemCache.get(key);
        if (item == null) {
            return Optional.empty();
        }
        if (!item.expiresAt().isAfter(now)) {
            itemCache.remove(key);
            return Optional.empty();
        }
        return Optional.of(item.value());
    }

    @Override
    public void putPolicy(String key, InsurancePolicyItemResult result, Instant expiresAt) {
        evictIfNeeded();
        itemCache.put(key, new CacheItem<>(result, expiresAt));
    }

    private void evictIfNeeded() {
        while (listCache.size() + itemCache.size() >= maxEntries) {
            if (!listCache.isEmpty()) {
                String key = listCache.keySet().iterator().next();
                listCache.remove(key);
                continue;
            }
            if (!itemCache.isEmpty()) {
                String key = itemCache.keySet().iterator().next();
                itemCache.remove(key);
                continue;
            }
            return;
        }
    }

    private record CacheItem<T>(T value, Instant expiresAt) {
    }
}
