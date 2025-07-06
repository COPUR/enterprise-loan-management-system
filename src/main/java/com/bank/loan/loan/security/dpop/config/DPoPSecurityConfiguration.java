package com.bank.loan.loan.security.dpop.config;

import com.bank.loan.loan.security.dpop.filter.DPoPValidationFilter;
import com.bank.loan.loan.security.dpop.service.DPoPNonceService;
import com.bank.loan.loan.security.dpop.service.DPoPProofValidationService;
import com.bank.loan.loan.security.dpop.service.DPoPTokenBindingService;
import com.bank.loan.loan.security.dpop.service.DPoPTokenValidationService;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class DPoPSecurityConfiguration {
    
    @Bean
    public DPoPProofValidationService dpopProofValidationService(RedisTemplate<String, Object> redisTemplate) {
        return new DPoPProofValidationService(redisTemplate);
    }
    
    @Bean
    public DPoPTokenBindingService dpopTokenBindingService(JwtEncoder jwtEncoder) {
        return new DPoPTokenBindingService(jwtEncoder);
    }
    
    @Bean
    public DPoPTokenValidationService dpopTokenValidationService(JwtDecoder jwtDecoder,
                                                                DPoPProofValidationService dpopProofValidationService) {
        return new DPoPTokenValidationService(jwtDecoder, dpopProofValidationService);
    }
    
    @Bean
    public DPoPNonceService dpopNonceService(RedisTemplate<String, Object> redisTemplate) {
        return new DPoPNonceService(redisTemplate);
    }
    
    @Bean
    public DPoPValidationFilter dpopValidationFilter(DPoPTokenValidationService dpopTokenValidationService,
                                                     DPoPNonceService dpopNonceService) {
        return new DPoPValidationFilter(dpopTokenValidationService, dpopNonceService);
    }
    
    @Bean
    public SecurityFilterChain dpopSecurityFilterChain(HttpSecurity http, 
                                                       DPoPValidationFilter dpopValidationFilter) throws Exception {
        return http
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(null)) // Will be configured with actual decoder
            )
            .addFilterBefore(dpopValidationFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
    
    @Bean
    public CorsConfigurationSource dpopCorsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("WWW-Authenticate", "DPoP-Nonce", "X-FAPI-Interaction-ID"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    @Bean
    @ConfigurationProperties(prefix = "dpop")
    public DPoPConfigurationProperties dpopConfigurationProperties() {
        return new DPoPConfigurationProperties();
    }
    
    @Bean
    public AntPathMatcher dpopProtectedEndpointMatcher() {
        return new AntPathMatcher();
    }
    
    @Bean
    public List<String> dpopExemptEndpoints() {
        return Arrays.asList(
            "/oauth2/token",
            "/oauth2/par",
            "/oauth2/authorize", 
            "/actuator/health",
            "/actuator/info",
            "/api/public/**",
            "/swagger-ui/**",
            "/v3/api-docs/**"
        );
    }
    
    @Bean
    public DPoPHeaderConfiguration dpopHeaderConfiguration() {
        return new DPoPHeaderConfiguration();
    }
    
    @Bean
    public DPoPErrorResponseConfiguration dpopErrorResponseConfiguration() {
        return new DPoPErrorResponseConfiguration();
    }
    
    @Bean
    public DPoPValidationConfiguration dpopValidationConfiguration() {
        return new DPoPValidationConfiguration();
    }
    
    @Bean
    public DPoPKeyValidationConfiguration dpopKeyValidationConfiguration() {
        return new DPoPKeyValidationConfiguration();
    }
    
    @Bean
    public DPoPCacheConfiguration dpopCacheConfiguration() {
        return new DPoPCacheConfiguration();
    }
    
    @Bean
    public DPoPCacheCleanupConfiguration dpopCacheCleanupConfiguration() {
        return new DPoPCacheCleanupConfiguration();
    }
    
    @Bean
    public DPoPMetricsConfiguration dpopMetricsConfiguration() {
        return new DPoPMetricsConfiguration();
    }
    
    @Bean
    public DPoPHealthConfiguration dpopHealthConfiguration() {
        return new DPoPHealthConfiguration();
    }
    
    // Configuration classes
    public static class DPoPConfigurationProperties {
        private int proofExpirationTime = 60;
        private int jtiCacheSize = 10000;
        private int nonceExpirationTime = 300;
        private int clockSkewTolerance = 30;
        private List<String> requiredAlgorithms = Arrays.asList("ES256", "RS256", "PS256");
        private int minimumKeySize = 256;
        
        // Getters and setters
        public int getProofExpirationTime() { return proofExpirationTime; }
        public void setProofExpirationTime(int proofExpirationTime) { this.proofExpirationTime = proofExpirationTime; }
        
        public int getJtiCacheSize() { return jtiCacheSize; }
        public void setJtiCacheSize(int jtiCacheSize) { this.jtiCacheSize = jtiCacheSize; }
        
        public int getNonceExpirationTime() { return nonceExpirationTime; }
        public void setNonceExpirationTime(int nonceExpirationTime) { this.nonceExpirationTime = nonceExpirationTime; }
        
        public int getClockSkewTolerance() { return clockSkewTolerance; }
        public void setClockSkewTolerance(int clockSkewTolerance) { this.clockSkewTolerance = clockSkewTolerance; }
        
        public List<String> getRequiredAlgorithms() { return requiredAlgorithms; }
        public void setRequiredAlgorithms(List<String> requiredAlgorithms) { this.requiredAlgorithms = requiredAlgorithms; }
        
        public int getMinimumKeySize() { return minimumKeySize; }
        public void setMinimumKeySize(int minimumKeySize) { this.minimumKeySize = minimumKeySize; }
    }
    
    public static class DPoPHeaderConfiguration {
        private String dpopHeaderName = "DPoP";
        private String dpopNonceHeaderName = "DPoP-Nonce";
        private String authorizationHeaderName = "Authorization";
        
        public String getDpopHeaderName() { return dpopHeaderName; }
        public String getDpopNonceHeaderName() { return dpopNonceHeaderName; }
        public String getAuthorizationHeaderName() { return authorizationHeaderName; }
    }
    
    public static class DPoPErrorResponseConfiguration {
        private String wwwAuthenticateHeaderName = "WWW-Authenticate";
        private String dpopErrorScheme = "DPoP";
        private String useDpopNonceError = "use_dpop_nonce";
        private String invalidDpopProofError = "invalid_dpop_proof";
        
        public String getWwwAuthenticateHeaderName() { return wwwAuthenticateHeaderName; }
        public String getDpopErrorScheme() { return dpopErrorScheme; }
        public String getUseDpopNonceError() { return useDpopNonceError; }
        public String getInvalidDpopProofError() { return invalidDpopProofError; }
    }
    
    public static class DPoPValidationConfiguration {
        private boolean requireDpopForAllEndpoints = false;
        private boolean allowDpopNonceOptional = true;
        private boolean validateAccessTokenHash = true;
        private boolean enforceHttpsOnly = true;
        private boolean strictJwtValidation = true;
        
        public boolean isRequireDpopForAllEndpoints() { return requireDpopForAllEndpoints; }
        public boolean isAllowDpopNonceOptional() { return allowDpopNonceOptional; }
        public boolean isValidateAccessTokenHash() { return validateAccessTokenHash; }
        public boolean isEnforceHttpsOnly() { return enforceHttpsOnly; }
        public boolean isStrictJwtValidation() { return strictJwtValidation; }
    }
    
    public static class DPoPKeyValidationConfiguration {
        private List<String> allowedKeyTypes = Arrays.asList("EC", "RSA");
        private List<String> allowedCurves = Arrays.asList("P-256", "P-384", "P-521");
        private List<String> allowedAlgorithms = Arrays.asList("ES256", "RS256", "PS256");
        private int minRsaKeySize = 2048;
        private boolean requireKeyId = false;
        private boolean allowWeakKeys = false;
        
        public List<String> getAllowedKeyTypes() { return allowedKeyTypes; }
        public List<String> getAllowedCurves() { return allowedCurves; }
        public List<String> getAllowedAlgorithms() { return allowedAlgorithms; }
        public int getMinRsaKeySize() { return minRsaKeySize; }
        public boolean isRequireKeyId() { return requireKeyId; }
        public boolean isAllowWeakKeys() { return allowWeakKeys; }
    }
    
    public static class DPoPCacheConfiguration {
        private String cacheType = "redis";
        private String jtiCachePrefix = "dpop:jti:";
        private String nonceCachePrefix = "dpop:nonce:";
        private int defaultTtl = 300;
        
        public String getCacheType() { return cacheType; }
        public String getJtiCachePrefix() { return jtiCachePrefix; }
        public String getNonceCachePrefix() { return nonceCachePrefix; }
        public int getDefaultTtl() { return defaultTtl; }
    }
    
    public static class DPoPCacheCleanupConfiguration {
        private int cleanupInterval = 600;
        private int batchSize = 1000;
        private boolean autoCleanupEnabled = true;
        
        public int getCleanupInterval() { return cleanupInterval; }
        public int getBatchSize() { return batchSize; }
        public boolean isAutoCleanupEnabled() { return autoCleanupEnabled; }
    }
    
    public static class DPoPMetricsConfiguration {
        private boolean enabled = true;
        private String metricsPrefix = "dpop";
        private boolean includeDetailedMetrics = true;
        private boolean trackValidationTime = true;
        private boolean trackCacheHitRate = true;
        
        public boolean isEnabled() { return enabled; }
        public String getMetricsPrefix() { return metricsPrefix; }
        public boolean isIncludeDetailedMetrics() { return includeDetailedMetrics; }
        public boolean isTrackValidationTime() { return trackValidationTime; }
        public boolean isTrackCacheHitRate() { return trackCacheHitRate; }
    }
    
    public static class DPoPHealthConfiguration {
        private boolean enabled = true;
        private int healthCheckInterval = 30;
        private int redisHealthTimeout = 5;
        private int jwtValidationHealthTimeout = 10;
        
        public boolean isEnabled() { return enabled; }
        public int getHealthCheckInterval() { return healthCheckInterval; }
        public int getRedisHealthTimeout() { return redisHealthTimeout; }
        public int getJwtValidationHealthTimeout() { return jwtValidationHealthTimeout; }
    }
}