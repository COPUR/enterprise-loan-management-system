package com.enterprise.openfinance.personalfinancialdata.infrastructure.cache;

import com.enterprise.openfinance.personalfinancialdata.domain.model.AccountSnapshot;
import com.enterprise.openfinance.personalfinancialdata.domain.model.BalanceSnapshot;
import com.enterprise.openfinance.personalfinancialdata.infrastructure.config.AisCacheProperties;
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
class RedisAisCacheAdapterTest {

    @Test
    void shouldPutAndGetAccountsFromRedis() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        AisCacheProperties properties = new AisCacheProperties();
        properties.setKeyPrefix("test:personal");

        Clock fixedClock = Clock.fixed(Instant.parse("2026-02-10T12:00:00Z"), ZoneOffset.UTC);
        RedisAisCacheAdapter adapter = new RedisAisCacheAdapter(redisTemplate, objectMapper, properties, fixedClock);

        List<AccountSnapshot> accounts = List.of(new AccountSnapshot(
                "ACC-1",
                "PSU-1",
                "AE210001",
                "AED",
                "Current",
                "Enabled",
                "IBAN",
                "Primary"
        ));

        adapter.putAccounts("consent:1", accounts, Instant.parse("2026-02-10T12:00:30Z"));

        verify(valueOperations).set(
                eq("test:personal:accounts:consent:1"),
                startsWith("["),
                eq(Duration.ofSeconds(30))
        );

        when(valueOperations.get("test:personal:accounts:consent:1"))
                .thenReturn(objectMapper.writeValueAsString(accounts));

        assertThat(adapter.getAccounts("consent:1", Instant.now(fixedClock))).hasValue(accounts);
    }

    @Test
    void shouldSkipPutWhenExpiryIsInPast() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AisCacheProperties properties = new AisCacheProperties();
        Clock fixedClock = Clock.fixed(Instant.parse("2026-02-10T12:00:00Z"), ZoneOffset.UTC);
        RedisAisCacheAdapter adapter = new RedisAisCacheAdapter(
                redisTemplate,
                new ObjectMapper().findAndRegisterModules(),
                properties,
                fixedClock
        );

        List<BalanceSnapshot> balances = List.of(new BalanceSnapshot(
                "ACC-1",
                "InterimBooked",
                new BigDecimal("25.00"),
                "AED",
                Instant.parse("2026-02-10T11:55:00Z")
        ));

        adapter.putBalances("consent:1", balances, Instant.parse("2026-02-10T11:59:59Z"));

        verify(valueOperations, never()).set(any(String.class), any(String.class), any(Duration.class));
    }
}
