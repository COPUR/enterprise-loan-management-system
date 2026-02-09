package com.enterprise.openfinance.uc08.infrastructure.cache;

import com.enterprise.openfinance.uc08.domain.model.BulkFileReport;
import com.enterprise.openfinance.uc08.domain.port.out.BulkCachePort;
import com.enterprise.openfinance.uc08.infrastructure.config.Uc08CacheProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryBulkCacheAdapter implements BulkCachePort {

    private final Map<String, CacheItem<BulkFileReport>> reportCache = new ConcurrentHashMap<>();
    private final int maxEntries;

    public InMemoryBulkCacheAdapter(Uc08CacheProperties properties) {
        this.maxEntries = properties.getMaxEntries();
    }

    @Override
    public Optional<BulkFileReport> getReport(String key, Instant now) {
        CacheItem<BulkFileReport> item = reportCache.get(key);
        if (item == null) {
            return Optional.empty();
        }
        if (!item.expiresAt().isAfter(now)) {
            reportCache.remove(key);
            return Optional.empty();
        }
        return Optional.of(item.value());
    }

    @Override
    public void putReport(String key, BulkFileReport report, Instant expiresAt) {
        if (reportCache.size() >= maxEntries) {
            evictOne();
        }
        reportCache.put(key, new CacheItem<>(report, expiresAt));
    }

    private void evictOne() {
        String candidate = reportCache.keySet().stream().findFirst().orElse(null);
        if (candidate != null) {
            reportCache.remove(candidate);
        }
    }

    private record CacheItem<T>(T value, Instant expiresAt) {
    }
}
