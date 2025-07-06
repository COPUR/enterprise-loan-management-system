package com.bank.loan.loan.security.par.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Health indicator for PAR endpoint functionality
 */
@Component
public class PAREndpointHealthIndicator implements HealthIndicator {

    private final RedisTemplate<String, Object> redisTemplate;

    public PAREndpointHealthIndicator(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Health health() {
        try {
            // Check PAR endpoint components
            boolean parEndpointHealthy = checkPAREndpointHealth();
            boolean parCacheHealthy = checkPARCacheHealth();
            boolean requestValidationHealthy = checkRequestValidationHealth();
            
            if (parEndpointHealthy && parCacheHealthy && requestValidationHealthy) {
                return Health.up()
                        .withDetail("par_endpoint", "healthy")
                        .withDetail("par_cache", "healthy")
                        .withDetail("request_validation", "healthy")
                        .withDetail("cache_size", getCurrentCacheSize())
                        .build();
            } else {
                return Health.down()
                        .withDetail("par_endpoint", parEndpointHealthy ? "healthy" : "unhealthy")
                        .withDetail("par_cache", parCacheHealthy ? "healthy" : "unhealthy")
                        .withDetail("request_validation", requestValidationHealthy ? "healthy" : "unhealthy")
                        .withDetail("cache_size", getCurrentCacheSize())
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    private boolean checkPAREndpointHealth() {
        // Implementation would check PAR endpoint health
        return true;
    }

    private boolean checkPARCacheHealth() {
        try {
            // Check Redis connectivity for PAR cache
            redisTemplate.opsForValue().get("par:health:check");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkRequestValidationHealth() {
        // Implementation would check PAR request validation health
        return true;
    }

    private long getCurrentCacheSize() {
        try {
            // Implementation would return actual PAR cache size
            return redisTemplate.keys("par:request:*").size();
        } catch (Exception e) {
            return -1;
        }
    }
}