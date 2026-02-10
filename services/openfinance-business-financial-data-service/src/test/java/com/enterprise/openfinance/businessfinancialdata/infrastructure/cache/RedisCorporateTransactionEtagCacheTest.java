package com.enterprise.openfinance.businessfinancialdata.infrastructure.cache;

import com.enterprise.openfinance.businessfinancialdata.infrastructure.config.CorporateCacheProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
class RedisCorporateTransactionEtagCacheTest {

    @Test
    void shouldStoreAndRetrieveEtag() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        CorporateCacheProperties properties = new CorporateCacheProperties();
        properties.setKeyPrefix("openfinance:businessfinancialdata");
        properties.setEtagTtl(Duration.ofSeconds(45));

        RedisCorporateTransactionEtagCache cache = new RedisCorporateTransactionEtagCache(redisTemplate, properties);
        cache.put("signature-1", "\"etag-1\"");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(keyCaptor.capture(), org.mockito.ArgumentMatchers.eq("\"etag-1\""), org.mockito.ArgumentMatchers.eq(Duration.ofSeconds(45)));

        when(valueOperations.get(keyCaptor.getValue())).thenReturn("\"etag-1\"");
        assertThat(cache.get("signature-1")).contains("\"etag-1\"");
    }
}
