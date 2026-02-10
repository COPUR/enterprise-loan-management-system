package com.enterprise.openfinance.businessfinancialdata.infrastructure.cache;

import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporateAccountListResult;
import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporatePagedResult;
import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporateTransactionSnapshot;
import com.enterprise.openfinance.businessfinancialdata.infrastructure.config.CorporateCacheProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
class RedisCorporateCacheAdapterTest {

    @Test
    void shouldWriteAndReadAccountsFromRedis() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        CorporateCacheProperties properties = properties();
        Clock clock = Clock.fixed(Instant.parse("2026-02-10T10:00:00Z"), ZoneOffset.UTC);
        RedisCorporateCacheAdapter adapter = new RedisCorporateCacheAdapter(redisTemplate, objectMapper(), properties, clock);

        CorporateAccountListResult payload = new CorporateAccountListResult(List.of(), false);
        adapter.putAccounts("key-1", payload, Instant.parse("2026-02-10T10:00:30Z"));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), ttlCaptor.capture());
        assertThat(keyCaptor.getValue()).isEqualTo("openfinance:businessfinancialdata:accounts:key-1");
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofSeconds(30));

        when(valueOperations.get("openfinance:businessfinancialdata:accounts:key-1")).thenReturn(valueCaptor.getValue());
        assertThat(adapter.getAccounts("key-1", Instant.now(clock))).contains(payload);
    }

    @Test
    void shouldReturnEmptyWhenCachePayloadMissing() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        RedisCorporateCacheAdapter adapter = new RedisCorporateCacheAdapter(
                redisTemplate,
                objectMapper(),
                properties(),
                Clock.systemUTC()
        );

        assertThat(adapter.getAccounts("missing", Instant.now())).isEmpty();
    }

    @Test
    void shouldRoundTripTransactionPage() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        CorporateCacheProperties properties = properties();
        Clock clock = Clock.fixed(Instant.parse("2026-02-10T10:00:00Z"), ZoneOffset.UTC);
        RedisCorporateCacheAdapter adapter = new RedisCorporateCacheAdapter(redisTemplate, objectMapper(), properties, clock);

        CorporatePagedResult<CorporateTransactionSnapshot> payload = new CorporatePagedResult<>(
                List.of(new CorporateTransactionSnapshot(
                        "TX-001",
                        "ACC-001",
                        new BigDecimal("10.00"),
                        "AED",
                        Instant.parse("2026-02-10T09:00:00Z"),
                        "BOOK",
                        null,
                        "desc"
                )),
                1,
                20,
                1,
                false
        );
        adapter.putTransactions("tx-key", payload, Instant.parse("2026-02-10T10:00:30Z"));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), org.mockito.ArgumentMatchers.any(Duration.class));
        when(valueOperations.get(keyCaptor.getValue())).thenReturn(valueCaptor.getValue());

        assertThat(adapter.getTransactions("tx-key", Instant.now(clock)))
                .isPresent()
                .get()
                .extracting(CorporatePagedResult::totalRecords)
                .isEqualTo(1L);
    }

    private static CorporateCacheProperties properties() {
        CorporateCacheProperties properties = new CorporateCacheProperties();
        properties.setKeyPrefix("openfinance:businessfinancialdata");
        properties.setTtl(Duration.ofSeconds(30));
        properties.setEtagTtl(Duration.ofSeconds(30));
        return properties;
    }

    private static ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .findAndAddModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();
    }
}
