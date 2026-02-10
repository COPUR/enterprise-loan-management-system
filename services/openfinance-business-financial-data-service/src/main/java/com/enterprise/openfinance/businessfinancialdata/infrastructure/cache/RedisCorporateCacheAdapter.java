package com.enterprise.openfinance.businessfinancialdata.infrastructure.cache;

import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporateAccountListResult;
import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporateBalanceListResult;
import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporatePagedResult;
import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporateTransactionSnapshot;
import com.enterprise.openfinance.businessfinancialdata.domain.port.out.CorporateCachePort;
import com.enterprise.openfinance.businessfinancialdata.infrastructure.config.CorporateCacheProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "openfinance.businessfinancialdata.cache", name = "mode", havingValue = "redis", matchIfMissing = true)
public class RedisCorporateCacheAdapter implements CorporateCachePort {

    private static final String ACCOUNTS_NAMESPACE = "accounts";
    private static final String BALANCES_NAMESPACE = "balances";
    private static final String TRANSACTIONS_NAMESPACE = "transactions";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final CorporateCacheProperties properties;
    private final Clock clock;

    public RedisCorporateCacheAdapter(StringRedisTemplate redisTemplate,
                                      ObjectMapper objectMapper,
                                      CorporateCacheProperties properties,
                                      Clock corporateTreasuryClock) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.clock = corporateTreasuryClock;
    }

    @Override
    public Optional<CorporateAccountListResult> getAccounts(String key, Instant now) {
        return get(ACCOUNTS_NAMESPACE, key, CorporateAccountListResult.class);
    }

    @Override
    public void putAccounts(String key, CorporateAccountListResult value, Instant expiresAt) {
        put(ACCOUNTS_NAMESPACE, key, value, expiresAt);
    }

    @Override
    public Optional<CorporateBalanceListResult> getBalances(String key, Instant now) {
        return get(BALANCES_NAMESPACE, key, CorporateBalanceListResult.class);
    }

    @Override
    public void putBalances(String key, CorporateBalanceListResult value, Instant expiresAt) {
        put(BALANCES_NAMESPACE, key, value, expiresAt);
    }

    @Override
    public Optional<CorporatePagedResult<CorporateTransactionSnapshot>> getTransactions(String key, Instant now) {
        JavaType targetType = objectMapper.getTypeFactory().constructParametricType(
                CorporatePagedResult.class,
                CorporateTransactionSnapshot.class
        );
        return get(TRANSACTIONS_NAMESPACE, key, targetType);
    }

    @Override
    public void putTransactions(String key,
                                CorporatePagedResult<CorporateTransactionSnapshot> value,
                                Instant expiresAt) {
        put(TRANSACTIONS_NAMESPACE, key, value, expiresAt);
    }

    private <T> Optional<T> get(String namespace, String key, Class<T> targetType) {
        return get(namespace, key, objectMapper.getTypeFactory().constructType(targetType));
    }

    private <T> Optional<T> get(String namespace, String key, JavaType targetType) {
        String payload = redisTemplate.opsForValue().get(composeKey(namespace, key));
        if (payload == null || payload.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(payload, targetType));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to deserialize cache payload", exception);
        }
    }

    private <T> void put(String namespace, String key, T value, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(clock), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        String payload = serialize(value);
        redisTemplate.opsForValue().set(composeKey(namespace, key), payload, ttl);
    }

    private String composeKey(String namespace, String key) {
        return properties.getKeyPrefix() + ':' + namespace + ':' + key;
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize cache payload", exception);
        }
    }
}
