package com.bank.loan.loan.security.dpop;

import com.bank.loan.loan.security.dpop.config.DPoPSecurityConfiguration;
import com.bank.loan.loan.security.dpop.filter.DPoPValidationFilter;
import com.bank.loan.loan.security.dpop.service.DPoPProofValidationService;
import com.bank.loan.loan.security.dpop.service.DPoPTokenValidationService;
import com.bank.loan.loan.security.dpop.service.DPoPNonceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {DPoPSecurityConfiguration.class})
@ActiveProfiles("test")
@DisplayName("DPoP Security Configuration Tests")
class DPoPSecurityConfigurationTest {

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private DPoPSecurityConfiguration dpopSecurityConfiguration;

    @Nested
    @DisplayName("Bean Configuration Tests")
    class BeanConfigurationTests {

        @Test
        @DisplayName("Should create DPoP proof validation service bean")
        void shouldCreateDPoPProofValidationServiceBean() {
            DPoPProofValidationService service = dpopSecurityConfiguration.dpopProofValidationService();

            assertThat(service).isNotNull();
            assertThat(service).isInstanceOf(DPoPProofValidationService.class);
        }

        @Test
        @DisplayName("Should create DPoP token validation service bean")
        void shouldCreateDPoPTokenValidationServiceBean() {
            DPoPProofValidationService proofValidationService = dpopSecurityConfiguration.dpopProofValidationService();
            DPoPTokenValidationService service = dpopSecurityConfiguration.dpopTokenValidationService(
                    jwtDecoder, proofValidationService);

            assertThat(service).isNotNull();
            assertThat(service).isInstanceOf(DPoPTokenValidationService.class);
        }

        @Test
        @DisplayName("Should create DPoP nonce service bean")
        void shouldCreateDPoPNonceServiceBean() {
            DPoPNonceService service = dpopSecurityConfiguration.dpopNonceService();

            assertThat(service).isNotNull();
            assertThat(service).isInstanceOf(DPoPNonceService.class);
        }

        @Test
        @DisplayName("Should create DPoP validation filter bean")
        void shouldCreateDPoPValidationFilterBean() {
            DPoPProofValidationService proofValidationService = dpopSecurityConfiguration.dpopProofValidationService();
            DPoPTokenValidationService tokenValidationService = dpopSecurityConfiguration.dpopTokenValidationService(
                    jwtDecoder, proofValidationService);
            DPoPNonceService nonceService = dpopSecurityConfiguration.dpopNonceService();

            DPoPValidationFilter filter = dpopSecurityConfiguration.dpopValidationFilter(
                    tokenValidationService, nonceService);

            assertThat(filter).isNotNull();
            assertThat(filter).isInstanceOf(DPoPValidationFilter.class);
        }
    }

    @Nested
    @DisplayName("Security Configuration Tests")
    class SecurityConfigurationTests {

        @Test
        @DisplayName("Should configure HTTP security with DPoP support")
        void shouldConfigureHttpSecurityWithDPoPSupport() throws Exception {
            HttpSecurity httpSecurity = mock(HttpSecurity.class);
            when(httpSecurity.oauth2ResourceServer(any())).thenReturn(httpSecurity);
            when(httpSecurity.addFilterBefore(any(), any())).thenReturn(httpSecurity);

            SecurityFilterChain filterChain = dpopSecurityConfiguration.dpopSecurityFilterChain(httpSecurity);

            assertThat(filterChain).isNotNull();
            verify(httpSecurity).oauth2ResourceServer(any());
            verify(httpSecurity).addFilterBefore(any(), any());
        }

        @Test
        @DisplayName("Should configure CORS for DPoP endpoints")
        void shouldConfigureCorsForDPoPEndpoints() {
            var corsConfigurationSource = dpopSecurityConfiguration.dpopCorsConfigurationSource();

            assertThat(corsConfigurationSource).isNotNull();
        }
    }

    @Nested
    @DisplayName("DPoP Configuration Properties Tests")
    class DPoPConfigurationPropertiesTests {

        @Test
        @DisplayName("Should have default DPoP configuration properties")
        void shouldHaveDefaultDPoPConfigurationProperties() {
            var properties = dpopSecurityConfiguration.dpopConfigurationProperties();

            assertThat(properties.getProofExpirationTime()).isEqualTo(60); // 60 seconds
            assertThat(properties.getJtiCacheSize()).isEqualTo(10000);
            assertThat(properties.getNonceExpirationTime()).isEqualTo(300); // 5 minutes
            assertThat(properties.getClockSkewTolerance()).isEqualTo(30); // 30 seconds
            assertThat(properties.getRequiredAlgorithms()).containsExactly("ES256", "RS256", "PS256");
            assertThat(properties.getMinimumKeySize()).isEqualTo(256);
        }

        @Test
        @DisplayName("Should allow custom DPoP configuration properties")
        void shouldAllowCustomDPoPConfigurationProperties() {
            var properties = dpopSecurityConfiguration.dpopConfigurationProperties();
            properties.setProofExpirationTime(120);
            properties.setJtiCacheSize(20000);
            properties.setNonceExpirationTime(600);
            properties.setClockSkewTolerance(60);

            assertThat(properties.getProofExpirationTime()).isEqualTo(120);
            assertThat(properties.getJtiCacheSize()).isEqualTo(20000);
            assertThat(properties.getNonceExpirationTime()).isEqualTo(600);
            assertThat(properties.getClockSkewTolerance()).isEqualTo(60);
        }
    }

    @Nested
    @DisplayName("DPoP Endpoint Configuration Tests")
    class DPoPEndpointConfigurationTests {

        @Test
        @DisplayName("Should configure DPoP-protected endpoints")
        void shouldConfigureDPoPProtectedEndpoints() {
            var endpointMatcher = dpopSecurityConfiguration.dpopProtectedEndpointMatcher();

            assertThat(endpointMatcher).isNotNull();
        }

        @Test
        @DisplayName("Should configure DPoP-exempt endpoints")
        void shouldConfigureDPoPExemptEndpoints() {
            var exemptEndpoints = dpopSecurityConfiguration.dpopExemptEndpoints();

            assertThat(exemptEndpoints).isNotNull();
            assertThat(exemptEndpoints).contains(
                    "/oauth2/token",
                    "/oauth2/par",
                    "/oauth2/authorize",
                    "/actuator/health",
                    "/actuator/info"
            );
        }
    }

    @Nested
    @DisplayName("DPoP Header Configuration Tests")
    class DPoPHeaderConfigurationTests {

        @Test
        @DisplayName("Should configure DPoP header names")
        void shouldConfigureDPoPHeaderNames() {
            var headerConfig = dpopSecurityConfiguration.dpopHeaderConfiguration();

            assertThat(headerConfig.getDpopHeaderName()).isEqualTo("DPoP");
            assertThat(headerConfig.getDpopNonceHeaderName()).isEqualTo("DPoP-Nonce");
            assertThat(headerConfig.getAuthorizationHeaderName()).isEqualTo("Authorization");
        }

        @Test
        @DisplayName("Should configure DPoP error response headers")
        void shouldConfigureDPoPErrorResponseHeaders() {
            var errorConfig = dpopSecurityConfiguration.dpopErrorResponseConfiguration();

            assertThat(errorConfig.getWwwAuthenticateHeaderName()).isEqualTo("WWW-Authenticate");
            assertThat(errorConfig.getDpopErrorScheme()).isEqualTo("DPoP");
            assertThat(errorConfig.getUseDpopNonceError()).isEqualTo("use_dpop_nonce");
            assertThat(errorConfig.getInvalidDpopProofError()).isEqualTo("invalid_dpop_proof");
        }
    }

    @Nested
    @DisplayName("DPoP Validation Configuration Tests")
    class DPoPValidationConfigurationTests {

        @Test
        @DisplayName("Should configure DPoP validation rules")
        void shouldConfigureDPoPValidationRules() {
            var validationConfig = dpopSecurityConfiguration.dpopValidationConfiguration();

            assertThat(validationConfig.isRequireDpopForAllEndpoints()).isFalse();
            assertThat(validationConfig.isAllowDpopNonceOptional()).isTrue();
            assertThat(validationConfig.isValidateAccessTokenHash()).isTrue();
            assertThat(validationConfig.isEnforceHttpsOnly()).isTrue();
            assertThat(validationConfig.isStrictJwtValidation()).isTrue();
        }

        @Test
        @DisplayName("Should configure DPoP key validation rules")
        void shouldConfigureDPoPKeyValidationRules() {
            var keyValidationConfig = dpopSecurityConfiguration.dpopKeyValidationConfiguration();

            assertThat(keyValidationConfig.getAllowedKeyTypes()).containsExactly("EC", "RSA");
            assertThat(keyValidationConfig.getAllowedCurves()).containsExactly("P-256", "P-384", "P-521");
            assertThat(keyValidationConfig.getMinRsaKeySize()).isEqualTo(2048);
            assertThat(keyValidationConfig.isRequireKeyId()).isFalse();
            assertThat(keyValidationConfig.isAllowWeakKeys()).isFalse();
        }
    }

    @Nested
    @DisplayName("DPoP Cache Configuration Tests")
    class DPoPCacheConfigurationTests {

        @Test
        @DisplayName("Should configure JTI cache with Redis")
        void shouldConfigureJtiCacheWithRedis() {
            var cacheConfig = dpopSecurityConfiguration.dpopCacheConfiguration();

            assertThat(cacheConfig.getCacheType()).isEqualTo("redis");
            assertThat(cacheConfig.getJtiCachePrefix()).isEqualTo("dpop:jti:");
            assertThat(cacheConfig.getNonceCachePrefix()).isEqualTo("dpop:nonce:");
            assertThat(cacheConfig.getDefaultTtl()).isEqualTo(300); // 5 minutes
        }

        @Test
        @DisplayName("Should configure cache cleanup policy")
        void shouldConfigureCacheCleanupPolicy() {
            var cleanupConfig = dpopSecurityConfiguration.dpopCacheCleanupConfiguration();

            assertThat(cleanupConfig.getCleanupInterval()).isEqualTo(600); // 10 minutes
            assertThat(cleanupConfig.getBatchSize()).isEqualTo(1000);
            assertThat(cleanupConfig.isAutoCleanupEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("DPoP Monitoring Configuration Tests")
    class DPoPMonitoringConfigurationTests {

        @Test
        @DisplayName("Should configure DPoP metrics")
        void shouldConfigureDPoPMetrics() {
            var metricsConfig = dpopSecurityConfiguration.dpopMetricsConfiguration();

            assertThat(metricsConfig.isEnabled()).isTrue();
            assertThat(metricsConfig.getMetricsPrefix()).isEqualTo("dpop");
            assertThat(metricsConfig.isIncludeDetailedMetrics()).isTrue();
            assertThat(metricsConfig.isTrackValidationTime()).isTrue();
            assertThat(metricsConfig.isTrackCacheHitRate()).isTrue();
        }

        @Test
        @DisplayName("Should configure DPoP health indicators")
        void shouldConfigureDPoPHealthIndicators() {
            var healthConfig = dpopSecurityConfiguration.dpopHealthConfiguration();

            assertThat(healthConfig.isEnabled()).isTrue();
            assertThat(healthConfig.getHealthCheckInterval()).isEqualTo(30); // 30 seconds
            assertThat(healthConfig.getRedisHealthTimeout()).isEqualTo(5); // 5 seconds
            assertThat(healthConfig.getJwtValidationHealthTimeout()).isEqualTo(10); // 10 seconds
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should configure all DPoP components together")
        void shouldConfigureAllDPoPComponentsTogether() {
            // Test that all components can be created and wired together
            var proofValidationService = dpopSecurityConfiguration.dpopProofValidationService();
            var tokenValidationService = dpopSecurityConfiguration.dpopTokenValidationService(
                    jwtDecoder, proofValidationService);
            var nonceService = dpopSecurityConfiguration.dpopNonceService();
            var validationFilter = dpopSecurityConfiguration.dpopValidationFilter(
                    tokenValidationService, nonceService);

            assertThat(proofValidationService).isNotNull();
            assertThat(tokenValidationService).isNotNull();
            assertThat(nonceService).isNotNull();
            assertThat(validationFilter).isNotNull();
        }

        @Test
        @DisplayName("Should have consistent configuration across all components")
        void shouldHaveConsistentConfigurationAcrossAllComponents() {
            var properties = dpopSecurityConfiguration.dpopConfigurationProperties();
            var validationConfig = dpopSecurityConfiguration.dpopValidationConfiguration();
            var keyValidationConfig = dpopSecurityConfiguration.dpopKeyValidationConfiguration();

            // Verify that configurations are consistent
            assertThat(properties.getRequiredAlgorithms())
                    .containsExactlyElementsOf(keyValidationConfig.getAllowedAlgorithms());
            assertThat(properties.getMinimumKeySize())
                    .isEqualTo(keyValidationConfig.getMinRsaKeySize() / 8); // bits to bytes
        }
    }
}