package com.bank.infrastructure.caching;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Lightweight Redis metrics bridge used by cache configuration.
 */
public class RedisCacheMetrics {

    private final MeterRegistry meterRegistry;
    private final RedisConnectionFactory connectionFactory;

    public RedisCacheMetrics(MeterRegistry meterRegistry, RedisConnectionFactory connectionFactory) {
        this.meterRegistry = meterRegistry;
        this.connectionFactory = connectionFactory;
    }

    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }

    public RedisConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }
}
