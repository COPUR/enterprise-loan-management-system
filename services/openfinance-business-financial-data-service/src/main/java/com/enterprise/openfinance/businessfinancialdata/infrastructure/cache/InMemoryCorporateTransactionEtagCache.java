package com.enterprise.openfinance.businessfinancialdata.infrastructure.cache;

import com.enterprise.openfinance.businessfinancialdata.infrastructure.config.CorporateCacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(prefix = "openfinance.businessfinancialdata.cache", name = "mode", havingValue = "inmemory")
public class InMemoryCorporateTransactionEtagCache implements CorporateTransactionEtagCache {

    private final Map<String, CacheItem> etagCache = new ConcurrentHashMap<>();
    private final CorporateCacheProperties cacheProperties;
    private final Clock clock;

    public InMemoryCorporateTransactionEtagCache(CorporateCacheProperties cacheProperties, Clock corporateTreasuryClock) {
        this.cacheProperties = cacheProperties;
        this.clock = corporateTreasuryClock;
    }

    @Override
    public Optional<String> get(String requestSignature) {
        CacheItem item = etagCache.get(requestSignature);
        if (item == null) {
            return Optional.empty();
        }
        if (!item.expiresAt().isAfter(Instant.now(clock))) {
            etagCache.remove(requestSignature);
            return Optional.empty();
        }
        return Optional.of(item.etag());
    }

    @Override
    public void put(String requestSignature, String etag) {
        Instant expiresAt = Instant.now(clock).plus(cacheProperties.getEtagTtl());
        etagCache.put(requestSignature, new CacheItem(etag, expiresAt));
    }

    private record CacheItem(String etag, Instant expiresAt) {
    }
}
