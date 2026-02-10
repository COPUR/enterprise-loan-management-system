package com.enterprise.openfinance.businessfinancialdata.infrastructure.cache;

import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporateAccountListResult;
import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporateBalanceListResult;
import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporatePagedResult;
import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporateTransactionSnapshot;
import com.enterprise.openfinance.businessfinancialdata.domain.port.out.CorporateCachePort;
import com.enterprise.openfinance.businessfinancialdata.infrastructure.config.CorporateCacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(prefix = "openfinance.businessfinancialdata.cache", name = "mode", havingValue = "inmemory")
public class InMemoryCorporateCacheAdapter implements CorporateCachePort {

    private final Map<String, CacheItem<CorporateAccountListResult>> accountsCache = new ConcurrentHashMap<>();
    private final Map<String, CacheItem<CorporateBalanceListResult>> balancesCache = new ConcurrentHashMap<>();
    private final Map<String, CacheItem<CorporatePagedResult<CorporateTransactionSnapshot>>> transactionsCache = new ConcurrentHashMap<>();
    private final int maxEntries;

    public InMemoryCorporateCacheAdapter(CorporateCacheProperties properties) {
        this.maxEntries = properties.getMaxEntries();
    }

    @Override
    public Optional<CorporateAccountListResult> getAccounts(String key, Instant now) {
        return get(accountsCache, key, now);
    }

    @Override
    public void putAccounts(String key, CorporateAccountListResult value, Instant expiresAt) {
        put(accountsCache, key, value, expiresAt);
    }

    @Override
    public Optional<CorporateBalanceListResult> getBalances(String key, Instant now) {
        return get(balancesCache, key, now);
    }

    @Override
    public void putBalances(String key, CorporateBalanceListResult value, Instant expiresAt) {
        put(balancesCache, key, value, expiresAt);
    }

    @Override
    public Optional<CorporatePagedResult<CorporateTransactionSnapshot>> getTransactions(String key, Instant now) {
        return get(transactionsCache, key, now);
    }

    @Override
    public void putTransactions(String key,
                                CorporatePagedResult<CorporateTransactionSnapshot> value,
                                Instant expiresAt) {
        put(transactionsCache, key, value, expiresAt);
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
