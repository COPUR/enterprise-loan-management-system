package com.bank.loan.loan.security.config;

import com.bank.loan.loan.security.dpop.health.DPoPValidationHealthIndicator;
import com.bank.loan.loan.security.fapi.health.FAPIComplianceHealthIndicator;
import com.bank.loan.loan.security.oauth2.health.OAuth2SecurityHealthIndicator;
import com.bank.loan.loan.security.par.health.PAREndpointHealthIndicator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Monitoring Configuration for FAPI 2.0 + DPoP Security
 * Configures custom metrics, health indicators, and monitoring components
 */
@Configuration
public class MonitoringConfiguration {

    /**
     * DPoP Validation Health Indicator
     */
    @Bean
    public HealthIndicator dpopValidationHealthIndicator(RedisTemplate<String, Object> redisTemplate) {
        return new DPoPValidationHealthIndicator(redisTemplate);
    }

    /**
     * PAR Endpoint Health Indicator
     */
    @Bean
    public HealthIndicator parEndpointHealthIndicator(RedisTemplate<String, Object> redisTemplate) {
        return new PAREndpointHealthIndicator(redisTemplate);
    }

    /**
     * FAPI Compliance Health Indicator
     */
    @Bean
    public HealthIndicator fapiComplianceHealthIndicator() {
        return new FAPIComplianceHealthIndicator();
    }

    /**
     * OAuth2 Security Health Indicator
     */
    @Bean
    public HealthIndicator oauth2SecurityHealthIndicator() {
        return new OAuth2SecurityHealthIndicator();
    }

    /**
     * DPoP Metrics Configuration
     */
    @Bean
    public DPoPMetrics dpopMetrics(MeterRegistry meterRegistry) {
        return new DPoPMetrics(meterRegistry);
    }

    /**
     * PAR Metrics Configuration
     */
    @Bean
    public PARMetrics parMetrics(MeterRegistry meterRegistry) {
        return new PARMetrics(meterRegistry);
    }

    /**
     * FAPI Metrics Configuration
     */
    @Bean
    public FAPIMetrics fapiMetrics(MeterRegistry meterRegistry) {
        return new FAPIMetrics(meterRegistry);
    }

    /**
     * OAuth2 Metrics Configuration
     */
    @Bean
    public OAuth2Metrics oauth2Metrics(MeterRegistry meterRegistry) {
        return new OAuth2Metrics(meterRegistry);
    }

    /**
     * DPoP Metrics Collector
     */
    public static class DPoPMetrics {
        private final Timer validationTimer;
        private final Counter validationSuccessCounter;
        private final Counter validationFailureCounter;
        private final Counter jtiCacheHitsCounter;
        private final Counter jtiCacheMissesCounter;
        private final Counter replayAttacksCounter;
        private final Counter nonceUsageCounter;
        private final Timer keyValidationTimer;

        public DPoPMetrics(MeterRegistry meterRegistry) {
            this.validationTimer = Timer.builder("dpop.validation.duration")
                    .description("Time taken to validate DPoP proof")
                    .register(meterRegistry);

            this.validationSuccessCounter = Counter.builder("dpop.validation.success")
                    .description("Total successful DPoP validations")
                    .register(meterRegistry);

            this.validationFailureCounter = Counter.builder("dpop.validation.failure")
                    .description("Total failed DPoP validations")
                    .tag("error_type", "unknown")
                    .register(meterRegistry);

            this.jtiCacheHitsCounter = Counter.builder("dpop.jti.cache.hits")
                    .description("DPoP JTI cache hits")
                    .register(meterRegistry);

            this.jtiCacheMissesCounter = Counter.builder("dpop.jti.cache.misses")
                    .description("DPoP JTI cache misses")
                    .register(meterRegistry);

            this.replayAttacksCounter = Counter.builder("dpop.replay.attacks")
                    .description("Detected DPoP replay attacks")
                    .register(meterRegistry);

            this.nonceUsageCounter = Counter.builder("dpop.nonce.usage")
                    .description("DPoP nonce usage statistics")
                    .register(meterRegistry);

            this.keyValidationTimer = Timer.builder("dpop.key.validation.duration")
                    .description("Time taken to validate DPoP key")
                    .register(meterRegistry);
        }

        public Timer.Sample startValidationTimer() {
            return Timer.start(validationTimer);
        }

        public void recordValidationSuccess() {
            validationSuccessCounter.increment();
        }

        public void recordValidationFailure(String errorType, String clientId) {
            Counter.builder("dpop.validation.failure")
                    .tag("error_type", errorType)
                    .tag("client_id", clientId)
                    .register(validationFailureCounter.meterRegistry)
                    .increment();
        }

        public void recordJTICacheHit() {
            jtiCacheHitsCounter.increment();
        }

        public void recordJTICacheMiss() {
            jtiCacheMissesCounter.increment();
        }

        public void recordReplayAttack(String clientId, String jti) {
            Counter.builder("dpop.replay.attacks")
                    .tag("client_id", clientId)
                    .tag("jti", jti)
                    .register(replayAttacksCounter.meterRegistry)
                    .increment();
        }

        public void recordNonceUsage(String clientId, String status) {
            Counter.builder("dpop.nonce.usage")
                    .tag("client_id", clientId)
                    .tag("status", status)
                    .register(nonceUsageCounter.meterRegistry)
                    .increment();
        }

        public Timer.Sample startKeyValidationTimer() {
            return Timer.start(keyValidationTimer);
        }
    }

    /**
     * PAR Metrics Collector
     */
    public static class PARMetrics {
        private final Timer requestProcessingTimer;
        private final Counter requestSuccessCounter;
        private final Counter requestFailureCounter;
        private final Counter requestUriUsageCounter;
        private final Gauge cacheSize;
        private final Counter expiredRequestsCounter;

        public PARMetrics(MeterRegistry meterRegistry) {
            this.requestProcessingTimer = Timer.builder("par.request.processing.duration")
                    .description("Time taken to process PAR requests")
                    .register(meterRegistry);

            this.requestSuccessCounter = Counter.builder("par.request.success")
                    .description("Total successful PAR requests")
                    .register(meterRegistry);

            this.requestFailureCounter = Counter.builder("par.request.failure")
                    .description("Total failed PAR requests")
                    .register(meterRegistry);

            this.requestUriUsageCounter = Counter.builder("par.request.uri.usage")
                    .description("PAR request URI usage")
                    .register(meterRegistry);

            this.cacheSize = Gauge.builder("par.cache.size")
                    .description("Current PAR cache size")
                    .register(meterRegistry, this, PARMetrics::getCurrentCacheSize);

            this.expiredRequestsCounter = Counter.builder("par.expired.requests")
                    .description("PAR requests that expired")
                    .register(meterRegistry);
        }

        public Timer.Sample startRequestProcessingTimer() {
            return Timer.start(requestProcessingTimer);
        }

        public void recordRequestSuccess() {
            requestSuccessCounter.increment();
        }

        public void recordRequestFailure(String errorType, String clientId) {
            Counter.builder("par.request.failure")
                    .tag("error_type", errorType)
                    .tag("client_id", clientId)
                    .register(requestFailureCounter.meterRegistry)
                    .increment();
        }

        public void recordRequestUriUsage(String clientId) {
            Counter.builder("par.request.uri.usage")
                    .tag("client_id", clientId)
                    .register(requestUriUsageCounter.meterRegistry)
                    .increment();
        }

        public void recordExpiredRequest() {
            expiredRequestsCounter.increment();
        }

        private double getCurrentCacheSize() {
            // Implementation would return actual cache size
            return 0.0;
        }
    }

    /**
     * FAPI Metrics Collector
     */
    public static class FAPIMetrics {
        private final Timer requestProcessingTimer;
        private final Counter securityViolationsCounter;
        private final Counter headerValidationFailuresCounter;
        private final Counter interactionIdsCounter;

        public FAPIMetrics(MeterRegistry meterRegistry) {
            this.requestProcessingTimer = Timer.builder("fapi.request.processing.duration")
                    .description("Time taken to process FAPI requests")
                    .register(meterRegistry);

            this.securityViolationsCounter = Counter.builder("fapi.security.violations")
                    .description("FAPI security violations detected")
                    .register(meterRegistry);

            this.headerValidationFailuresCounter = Counter.builder("fapi.header.validation.failures")
                    .description("FAPI header validation failures")
                    .register(meterRegistry);

            this.interactionIdsCounter = Counter.builder("fapi.interaction.ids")
                    .description("FAPI interaction ID usage")
                    .register(meterRegistry);
        }

        public Timer.Sample startRequestProcessingTimer() {
            return Timer.start(requestProcessingTimer);
        }

        public void recordSecurityViolation(String violationType, String clientId, String endpoint) {
            Counter.builder("fapi.security.violations")
                    .tag("violation_type", violationType)
                    .tag("client_id", clientId)
                    .tag("endpoint", endpoint)
                    .register(securityViolationsCounter.meterRegistry)
                    .increment();
        }

        public void recordHeaderValidationFailure(String headerName, String clientId) {
            Counter.builder("fapi.header.validation.failures")
                    .tag("header_name", headerName)
                    .tag("client_id", clientId)
                    .register(headerValidationFailuresCounter.meterRegistry)
                    .increment();
        }

        public void recordInteractionId(String clientId) {
            Counter.builder("fapi.interaction.ids")
                    .tag("client_id", clientId)
                    .register(interactionIdsCounter.meterRegistry)
                    .increment();
        }
    }

    /**
     * OAuth2 Metrics Collector
     */
    public static class OAuth2Metrics {
        private final Counter tokenRequestsCounter;
        private final Timer tokenValidationTimer;
        private final Counter clientAuthenticationFailuresCounter;
        private final Timer privateKeyJwtValidationTimer;

        public OAuth2Metrics(MeterRegistry meterRegistry) {
            this.tokenRequestsCounter = Counter.builder("oauth2.token.requests")
                    .description("Total OAuth2 token requests")
                    .register(meterRegistry);

            this.tokenValidationTimer = Timer.builder("oauth2.token.validation.duration")
                    .description("Time taken to validate OAuth2 tokens")
                    .register(meterRegistry);

            this.clientAuthenticationFailuresCounter = Counter.builder("oauth2.client.authentication.failures")
                    .description("OAuth2 client authentication failures")
                    .register(meterRegistry);

            this.privateKeyJwtValidationTimer = Timer.builder("oauth2.private.key.jwt.validation.duration")
                    .description("Time taken to validate private_key_jwt")
                    .register(meterRegistry);
        }

        public void recordTokenRequest(String grantType, String clientId, String status) {
            Counter.builder("oauth2.token.requests")
                    .tag("grant_type", grantType)
                    .tag("client_id", clientId)
                    .tag("status", status)
                    .register(tokenRequestsCounter.meterRegistry)
                    .increment();
        }

        public Timer.Sample startTokenValidationTimer() {
            return Timer.start(tokenValidationTimer);
        }

        public void recordClientAuthenticationFailure(String clientId, String authMethod) {
            Counter.builder("oauth2.client.authentication.failures")
                    .tag("client_id", clientId)
                    .tag("auth_method", authMethod)
                    .register(clientAuthenticationFailuresCounter.meterRegistry)
                    .increment();
        }

        public Timer.Sample startPrivateKeyJwtValidationTimer() {
            return Timer.start(privateKeyJwtValidationTimer);
        }
    }
}