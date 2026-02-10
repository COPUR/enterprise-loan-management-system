package com.enterprise.openfinance.personalfinancialdata.infrastructure.cache;

import com.enterprise.openfinance.personalfinancialdata.domain.model.AccountSnapshot;
import com.enterprise.openfinance.personalfinancialdata.domain.model.BalanceSnapshot;
import com.enterprise.openfinance.personalfinancialdata.domain.port.out.AisCachePort;
import com.enterprise.openfinance.personalfinancialdata.infrastructure.config.AisCacheProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "openfinance.personalfinancialdata.cache", name = "mode", havingValue = "redis", matchIfMissing = true)
public class RedisAisCacheAdapter implements AisCachePort {

    private static final String ACCOUNTS_NAMESPACE = "accounts";
    private static final String BALANCES_NAMESPACE = "balances";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AisCacheProperties properties;
    private final Clock clock;

    public RedisAisCacheAdapter(StringRedisTemplate redisTemplate,
                                ObjectMapper objectMapper,
                                AisCacheProperties properties,
                                Clock accountInformationClock) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.clock = accountInformationClock;
    }

    @Override
    public Optional<List<AccountSnapshot>> getAccounts(String key, Instant now) {
        JavaType targetType = objectMapper.getTypeFactory().constructCollectionType(List.class, AccountSnapshot.class);
        return get(ACCOUNTS_NAMESPACE, key, targetType);
    }

    @Override
    public void putAccounts(String key, List<AccountSnapshot> accounts, Instant expiresAt) {
        put(ACCOUNTS_NAMESPACE, key, accounts, expiresAt);
    }

    @Override
    public Optional<List<BalanceSnapshot>> getBalances(String key, Instant now) {
        JavaType targetType = objectMapper.getTypeFactory().constructCollectionType(List.class, BalanceSnapshot.class);
        return get(BALANCES_NAMESPACE, key, targetType);
    }

    @Override
    public void putBalances(String key, List<BalanceSnapshot> balances, Instant expiresAt) {
        put(BALANCES_NAMESPACE, key, balances, expiresAt);
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

