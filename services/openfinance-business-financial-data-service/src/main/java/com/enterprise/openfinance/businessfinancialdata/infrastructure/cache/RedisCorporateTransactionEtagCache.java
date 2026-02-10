package com.enterprise.openfinance.businessfinancialdata.infrastructure.cache;

import com.enterprise.openfinance.businessfinancialdata.infrastructure.config.CorporateCacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "openfinance.businessfinancialdata.cache", name = "mode", havingValue = "redis", matchIfMissing = true)
public class RedisCorporateTransactionEtagCache implements CorporateTransactionEtagCache {

    private static final String ETAG_NAMESPACE = "etag";

    private final StringRedisTemplate redisTemplate;
    private final CorporateCacheProperties cacheProperties;

    public RedisCorporateTransactionEtagCache(StringRedisTemplate redisTemplate,
                                              CorporateCacheProperties cacheProperties) {
        this.redisTemplate = redisTemplate;
        this.cacheProperties = cacheProperties;
    }

    @Override
    public Optional<String> get(String requestSignature) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(composeKey(requestSignature)));
    }

    @Override
    public void put(String requestSignature, String etag) {
        redisTemplate.opsForValue().set(composeKey(requestSignature), etag, cacheProperties.getEtagTtl());
    }

    private String composeKey(String requestSignature) {
        return cacheProperties.getKeyPrefix() + ':' + ETAG_NAMESPACE + ':' + requestSignature;
    }
}
