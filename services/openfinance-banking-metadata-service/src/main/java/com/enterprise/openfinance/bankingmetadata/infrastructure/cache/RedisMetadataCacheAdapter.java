package com.enterprise.openfinance.bankingmetadata.infrastructure.cache;

import com.enterprise.openfinance.bankingmetadata.domain.model.AccountSchemeMetadata;
import com.enterprise.openfinance.bankingmetadata.domain.model.MetadataItemResult;
import com.enterprise.openfinance.bankingmetadata.domain.model.MetadataListResult;
import com.enterprise.openfinance.bankingmetadata.domain.model.MetadataQueryResult;
import com.enterprise.openfinance.bankingmetadata.domain.model.PartyMetadata;
import com.enterprise.openfinance.bankingmetadata.domain.model.StandingOrderMetadata;
import com.enterprise.openfinance.bankingmetadata.domain.model.TransactionMetadata;
import com.enterprise.openfinance.bankingmetadata.domain.port.out.MetadataCachePort;
import com.enterprise.openfinance.bankingmetadata.infrastructure.config.MetadataCacheProperties;
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
@ConditionalOnProperty(prefix = "openfinance.bankingmetadata.cache", name = "mode", havingValue = "redis", matchIfMissing = true)
public class RedisMetadataCacheAdapter implements MetadataCachePort {

    private static final String TRANSACTIONS_NAMESPACE = "transactions";
    private static final String PARTIES_NAMESPACE = "parties";
    private static final String ACCOUNT_NAMESPACE = "account";
    private static final String STANDING_ORDERS_NAMESPACE = "standing-orders";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final MetadataCacheProperties properties;
    private final Clock clock;

    public RedisMetadataCacheAdapter(StringRedisTemplate redisTemplate,
                                     ObjectMapper objectMapper,
                                     MetadataCacheProperties properties,
                                     Clock metadataClock) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.clock = metadataClock;
    }

    @Override
    public Optional<MetadataQueryResult<TransactionMetadata>> getTransactions(String key, Instant now) {
        JavaType valueType = objectMapper.getTypeFactory()
                .constructParametricType(MetadataQueryResult.class, TransactionMetadata.class);
        return get(TRANSACTIONS_NAMESPACE, key, valueType);
    }

    @Override
    public void putTransactions(String key, MetadataQueryResult<TransactionMetadata> value, Instant expiresAt) {
        put(TRANSACTIONS_NAMESPACE, key, value, expiresAt);
    }

    @Override
    public Optional<MetadataListResult<PartyMetadata>> getParties(String key, Instant now) {
        JavaType valueType = objectMapper.getTypeFactory()
                .constructParametricType(MetadataListResult.class, PartyMetadata.class);
        return get(PARTIES_NAMESPACE, key, valueType);
    }

    @Override
    public void putParties(String key, MetadataListResult<PartyMetadata> value, Instant expiresAt) {
        put(PARTIES_NAMESPACE, key, value, expiresAt);
    }

    @Override
    public Optional<MetadataItemResult<AccountSchemeMetadata>> getAccount(String key, Instant now) {
        JavaType valueType = objectMapper.getTypeFactory()
                .constructParametricType(MetadataItemResult.class, AccountSchemeMetadata.class);
        return get(ACCOUNT_NAMESPACE, key, valueType);
    }

    @Override
    public void putAccount(String key, MetadataItemResult<AccountSchemeMetadata> value, Instant expiresAt) {
        put(ACCOUNT_NAMESPACE, key, value, expiresAt);
    }

    @Override
    public Optional<MetadataQueryResult<StandingOrderMetadata>> getStandingOrders(String key, Instant now) {
        JavaType valueType = objectMapper.getTypeFactory()
                .constructParametricType(MetadataQueryResult.class, StandingOrderMetadata.class);
        return get(STANDING_ORDERS_NAMESPACE, key, valueType);
    }

    @Override
    public void putStandingOrders(String key, MetadataQueryResult<StandingOrderMetadata> value, Instant expiresAt) {
        put(STANDING_ORDERS_NAMESPACE, key, value, expiresAt);
    }

    private <T> Optional<T> get(String namespace, String key, JavaType valueType) {
        String payload = redisTemplate.opsForValue().get(composeKey(namespace, key));
        if (payload == null || payload.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(payload, valueType));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to deserialize cache payload", exception);
        }
    }

    private <T> void put(String namespace, String key, T value, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(clock), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        redisTemplate.opsForValue().set(composeKey(namespace, key), serialize(value), ttl);
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

