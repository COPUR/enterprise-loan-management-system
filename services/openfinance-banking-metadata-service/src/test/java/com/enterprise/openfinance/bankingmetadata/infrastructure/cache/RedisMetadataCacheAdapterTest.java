package com.enterprise.openfinance.bankingmetadata.infrastructure.cache;

import com.enterprise.openfinance.bankingmetadata.domain.model.AccountSchemeMetadata;
import com.enterprise.openfinance.bankingmetadata.domain.model.GeoLocation;
import com.enterprise.openfinance.bankingmetadata.domain.model.MetadataItemResult;
import com.enterprise.openfinance.bankingmetadata.domain.model.MetadataQueryResult;
import com.enterprise.openfinance.bankingmetadata.domain.model.TransactionMetadata;
import com.enterprise.openfinance.bankingmetadata.infrastructure.config.MetadataCacheProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
class RedisMetadataCacheAdapterTest {

    @Test
    void shouldPutAndGetTransactionsFromRedis() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        MetadataCacheProperties properties = new MetadataCacheProperties();
        properties.setKeyPrefix("test:metadata");

        Clock fixedClock = Clock.fixed(Instant.parse("2026-02-10T12:00:00Z"), ZoneOffset.UTC);
        RedisMetadataCacheAdapter adapter = new RedisMetadataCacheAdapter(
                redisTemplate,
                objectMapper,
                properties,
                fixedClock
        );

        MetadataQueryResult<TransactionMetadata> payload = new MetadataQueryResult<>(
                List.of(new TransactionMetadata(
                        "TX-1",
                        "ACC-1",
                        Instant.parse("2026-02-10T10:00:00Z"),
                        new BigDecimal("10.00"),
                        BigDecimal.ZERO,
                        "AED",
                        "Merchant A",
                        "5411",
                        new GeoLocation(25.2, 55.3),
                        null
                )),
                1,
                20,
                1,
                false
        );

        adapter.putTransactions("consent:1", payload, Instant.parse("2026-02-10T12:00:30Z"));

        verify(valueOperations).set(
                eq("test:metadata:transactions:consent:1"),
                startsWith("{"),
                eq(Duration.ofSeconds(30))
        );

        when(valueOperations.get("test:metadata:transactions:consent:1"))
                .thenReturn(objectMapper.writeValueAsString(payload));

        assertThat(adapter.getTransactions("consent:1", Instant.now(fixedClock))).hasValue(payload);
    }

    @Test
    void shouldSkipPutWhenExpiryIsInPast() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        RedisMetadataCacheAdapter adapter = new RedisMetadataCacheAdapter(
                redisTemplate,
                new ObjectMapper().findAndRegisterModules(),
                new MetadataCacheProperties(),
                Clock.fixed(Instant.parse("2026-02-10T12:00:00Z"), ZoneOffset.UTC)
        );

        adapter.putAccount("consent:1", new MetadataItemResult<>(new AccountSchemeMetadata(
                "ACC-1",
                "IBAN",
                null
        ), false), Instant.parse("2026-02-10T11:59:59Z"));

        verify(valueOperations, never()).set(any(String.class), any(String.class), any(Duration.class));
    }
}
