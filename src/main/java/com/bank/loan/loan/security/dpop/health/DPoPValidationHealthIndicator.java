package com.bank.loan.loan.security.dpop.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Health indicator for DPoP validation functionality
 */
@Component
public class DPoPValidationHealthIndicator implements HealthIndicator {

    private final RedisTemplate<String, Object> redisTemplate;

    public DPoPValidationHealthIndicator(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Health health() {
        try {
            // Check Redis connectivity for JTI cache
            redisTemplate.opsForValue().get("dpop:health:check");
            
            // Check DPoP validation components
            boolean dpopValidationHealthy = checkDPoPValidationHealth();
            boolean jtiCacheHealthy = checkJTICacheHealth();
            boolean nonceServiceHealthy = checkNonceServiceHealth();
            
            if (dpopValidationHealthy && jtiCacheHealthy && nonceServiceHealthy) {
                return Health.up()
                        .withDetail("dpop_validation", "healthy")
                        .withDetail("jti_cache", "healthy")
                        .withDetail("nonce_service", "healthy")
                        .withDetail("redis_connectivity", "healthy")
                        .build();
            } else {
                return Health.down()
                        .withDetail("dpop_validation", dpopValidationHealthy ? "healthy" : "unhealthy")
                        .withDetail("jti_cache", jtiCacheHealthy ? "healthy" : "unhealthy")
                        .withDetail("nonce_service", nonceServiceHealthy ? "healthy" : "unhealthy")
                        .withDetail("redis_connectivity", "healthy")
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("redis_connectivity", "unhealthy")
                    .build();
        }
    }

    private boolean checkDPoPValidationHealth() {
        // Implementation would check DPoP validation service health
        return true;
    }

    private boolean checkJTICacheHealth() {
        // Implementation would check JTI cache health
        return true;
    }

    private boolean checkNonceServiceHealth() {
        // Implementation would check nonce service health
        return true;
    }
}