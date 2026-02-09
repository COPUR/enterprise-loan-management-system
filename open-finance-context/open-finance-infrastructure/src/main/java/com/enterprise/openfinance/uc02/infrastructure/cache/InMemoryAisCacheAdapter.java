package com.enterprise.openfinance.uc02.infrastructure.cache;

import com.enterprise.openfinance.uc02.domain.model.AccountSnapshot;
import com.enterprise.openfinance.uc02.domain.model.BalanceSnapshot;
import com.enterprise.openfinance.uc02.domain.port.out.AisCachePort;
import com.enterprise.openfinance.uc02.infrastructure.config.Uc02CacheProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryAisCacheAdapter implements AisCachePort {

    private final Map<String, CacheItem<List<AccountSnapshot>>> accountsCache = new ConcurrentHashMap<>();
    private final Map<String, CacheItem<List<BalanceSnapshot>>> balancesCache = new ConcurrentHashMap<>();
    private final int maxEntries;

    public InMemoryAisCacheAdapter(Uc02CacheProperties properties) {
        this.maxEntries = properties.getMaxEntries();
    }

    @Override
    public Optional<List<AccountSnapshot>> getAccounts(String key, Instant now) {
        return get(accountsCache, key, now);
    }

    @Override
    public void putAccounts(String key, List<AccountSnapshot> accounts, Instant expiresAt) {
        put(accountsCache, key, accounts, expiresAt);
    }

    @Override
    public Optional<List<BalanceSnapshot>> getBalances(String key, Instant now) {
        return get(balancesCache, key, now);
    }

    @Override
    public void putBalances(String key, List<BalanceSnapshot> balances, Instant expiresAt) {
        put(balancesCache, key, balances, expiresAt);
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
