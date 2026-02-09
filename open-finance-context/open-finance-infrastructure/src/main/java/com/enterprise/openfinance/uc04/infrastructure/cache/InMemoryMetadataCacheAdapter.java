package com.enterprise.openfinance.uc04.infrastructure.cache;

import com.enterprise.openfinance.uc04.domain.model.AccountSchemeMetadata;
import com.enterprise.openfinance.uc04.domain.model.MetadataItemResult;
import com.enterprise.openfinance.uc04.domain.model.MetadataListResult;
import com.enterprise.openfinance.uc04.domain.model.MetadataQueryResult;
import com.enterprise.openfinance.uc04.domain.model.PartyMetadata;
import com.enterprise.openfinance.uc04.domain.model.StandingOrderMetadata;
import com.enterprise.openfinance.uc04.domain.model.TransactionMetadata;
import com.enterprise.openfinance.uc04.domain.port.out.MetadataCachePort;
import com.enterprise.openfinance.uc04.infrastructure.config.Uc04CacheProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryMetadataCacheAdapter implements MetadataCachePort {

    private final Map<String, CacheItem<MetadataQueryResult<TransactionMetadata>>> transactionCache = new ConcurrentHashMap<>();
    private final Map<String, CacheItem<MetadataListResult<PartyMetadata>>> partiesCache = new ConcurrentHashMap<>();
    private final Map<String, CacheItem<MetadataItemResult<AccountSchemeMetadata>>> accountCache = new ConcurrentHashMap<>();
    private final Map<String, CacheItem<MetadataQueryResult<StandingOrderMetadata>>> standingOrdersCache = new ConcurrentHashMap<>();
    private final int maxEntries;

    public InMemoryMetadataCacheAdapter(Uc04CacheProperties properties) {
        this.maxEntries = properties.getMaxEntries();
    }

    @Override
    public Optional<MetadataQueryResult<TransactionMetadata>> getTransactions(String key, Instant now) {
        return get(transactionCache, key, now);
    }

    @Override
    public void putTransactions(String key, MetadataQueryResult<TransactionMetadata> value, Instant expiresAt) {
        put(transactionCache, key, value, expiresAt);
    }

    @Override
    public Optional<MetadataListResult<PartyMetadata>> getParties(String key, Instant now) {
        return get(partiesCache, key, now);
    }

    @Override
    public void putParties(String key, MetadataListResult<PartyMetadata> value, Instant expiresAt) {
        put(partiesCache, key, value, expiresAt);
    }

    @Override
    public Optional<MetadataItemResult<AccountSchemeMetadata>> getAccount(String key, Instant now) {
        return get(accountCache, key, now);
    }

    @Override
    public void putAccount(String key, MetadataItemResult<AccountSchemeMetadata> value, Instant expiresAt) {
        put(accountCache, key, value, expiresAt);
    }

    @Override
    public Optional<MetadataQueryResult<StandingOrderMetadata>> getStandingOrders(String key, Instant now) {
        return get(standingOrdersCache, key, now);
    }

    @Override
    public void putStandingOrders(String key, MetadataQueryResult<StandingOrderMetadata> value, Instant expiresAt) {
        put(standingOrdersCache, key, value, expiresAt);
    }

    private static <T> Optional<T> get(Map<String, CacheItem<T>> cache, String key, Instant now) {
        CacheItem<T> item = cache.get(key);
        if (item == null) {
            return Optional.empty();
        }
        if (!item.expiresAt().isAfter(now)) {
            cache.remove(key);
            return Optional.empty();
        }
        return Optional.of(item.value());
    }

    private <T> void put(Map<String, CacheItem<T>> cache, String key, T value, Instant expiresAt) {
        if (cache.size() >= maxEntries) {
            evictOne(cache);
        }
        cache.put(key, new CacheItem<>(value, expiresAt));
    }

    private static <T> void evictOne(Map<String, CacheItem<T>> cache) {
        String candidate = cache.keySet().stream().findFirst().orElse(null);
        if (candidate != null) {
            cache.remove(candidate);
        }
    }

    private record CacheItem<T>(T value, Instant expiresAt) {
    }
}
