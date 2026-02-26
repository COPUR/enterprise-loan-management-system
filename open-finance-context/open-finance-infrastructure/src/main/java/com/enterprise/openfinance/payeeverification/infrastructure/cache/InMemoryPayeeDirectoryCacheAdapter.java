package com.enterprise.openfinance.payeeverification.infrastructure.cache;

import com.enterprise.openfinance.payeeverification.domain.model.DirectoryEntry;
import com.enterprise.openfinance.payeeverification.domain.port.out.PayeeDirectoryCachePort;
import com.enterprise.openfinance.payeeverification.infrastructure.config.CoPCacheProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryPayeeDirectoryCacheAdapter implements PayeeDirectoryCachePort {

    private final Map<String, CacheItem> cache = new ConcurrentHashMap<>();
    private final int maxEntries;

    public InMemoryPayeeDirectoryCacheAdapter(CoPCacheProperties properties) {
        this.maxEntries = properties.getMaxEntries();
    }

    @Override
    public Optional<DirectoryEntry> get(String key, Instant now) {
        CacheItem item = cache.get(key);
        if (item == null) {
            return Optional.empty();
        }
        if (!item.expiresAt().isAfter(now)) {
            cache.remove(key);
            return Optional.empty();
        }
        return Optional.of(item.entry());
    }

    @Override
    public void put(String key, DirectoryEntry entry, Instant expiresAt) {
        if (cache.size() >= maxEntries) {
            evictOne();
        }
        cache.put(key, new CacheItem(entry, expiresAt));
    }

    private void evictOne() {
        String candidate = cache.keySet().stream().findFirst().orElse(null);
        if (candidate != null) {
            cache.remove(candidate);
        }
    }

    private record CacheItem(DirectoryEntry entry, Instant expiresAt) {
    }
}
