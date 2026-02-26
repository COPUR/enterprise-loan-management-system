package com.enterprise.openfinance.productcatalog.infrastructure.cache;

import com.enterprise.openfinance.productcatalog.domain.model.ProductListResult;
import com.enterprise.openfinance.productcatalog.domain.port.out.ProductCachePort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryProductCacheAdapter implements ProductCachePort {

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Override
    public Optional<ProductListResult> getProducts(String key, Instant now) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (now.isAfter(entry.expiresAt())) {
            cache.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.result());
    }

    @Override
    public void putProducts(String key, ProductListResult result, Instant expiresAt) {
        cache.put(key, new CacheEntry(result, expiresAt));
    }

    private record CacheEntry(
            ProductListResult result,
            Instant expiresAt
    ) {
    }
}
