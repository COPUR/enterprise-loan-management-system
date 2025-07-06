package com.bank.loan.loan.security.config;

import com.bank.loan.loan.security.dpop.filter.DPoPValidationFilter;
import com.bank.loan.loan.security.dpop.service.DPoPNonceService;
import com.bank.loan.loan.security.dpop.service.DPoPProofValidationService;
import com.bank.loan.loan.security.dpop.service.DPoPTokenBindingService;
import com.bank.loan.loan.security.dpop.service.DPoPTokenValidationService;
import com.bank.loan.loan.security.filter.FAPIRateLimitingFilter;
import com.bank.loan.loan.security.filter.FAPIRequestValidationFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Arrays;

/**
 * Enhanced FAPI Security Configuration with DPoP Support
 * Implements FAPI 2.0 Security Profile with DPoP (Demonstrating Proof-of-Possession)
 * Combining existing FAPI security features with new DPoP validation
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties(SecurityProperties.class)
public class EnhancedFAPISecurityConfig {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EnhancedFAPISecurityConfig.class);

    private final SecurityProperties securityProperties;
    private final RedisTemplate<String, Object> redisTemplate;

    public EnhancedFAPISecurityConfig(SecurityProperties securityProperties,
                                     RedisTemplate<String, Object> redisTemplate) {
        this.securityProperties = securityProperties;
        this.redisTemplate = redisTemplate;
    }

    // DPoP Service Beans
    @Bean
    public DPoPProofValidationService dpopProofValidationService() {
        return new DPoPProofValidationService(redisTemplate);
    }

    @Bean
    public DPoPTokenBindingService dpopTokenBindingService() {
        return new DPoPTokenBindingService(jwtEncoder());
    }

    @Bean
    public DPoPTokenValidationService dpopTokenValidationService() {
        return new DPoPTokenValidationService(jwtDecoder(), dpopProofValidationService());
    }

    @Bean
    public DPoPNonceService dpopNonceService() {
        return new DPoPNonceService(redisTemplate);
    }

    @Bean
    public DPoPValidationFilter dpopValidationFilter() {
        return new DPoPValidationFilter(dpopTokenValidationService(), dpopNonceService());
    }

    @Bean
    @Primary
    public SecurityFilterChain enhancedSecurityFilterChain(HttpSecurity http,
                                                          FAPIRateLimitingFilter rateLimitingFilter,
                                                          FAPIRequestValidationFilter requestValidationFilter) throws Exception {
        log.info("Configuring Enhanced FAPI 2.0 + DPoP security filter chain");
        
        return http
                // Disable CSRF for APIs (use JWT + DPoP instead)
                .csrf(AbstractHttpConfigurer::disable)
                
                // Enhanced FAPI + DPoP Security Headers
                .headers(headers -> headers
                    .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                    .contentTypeOptions(Customizer.withDefaults())
                    .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                        .maxAgeInSeconds(31536000)
                        .includeSubDomains(true)
                        .preload(true))
                    .addHeaderWriter((request, response) -> {
                        // FAPI-specific security headers
                        response.setHeader("X-FAPI-Interaction-ID", java.util.UUID.randomUUID().toString());
                        response.setHeader("Cache-Control", "no-store");
                        response.setHeader("Pragma", "no-cache");
                        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
                        
                        // DPoP-specific headers
                        response.setHeader("DPoP-Nonce-Endpoint", "/oauth2/nonce");
                        response.setHeader("X-DPoP-Supported", "true");
                    }))
                
                // Session Management - Stateless for FAPI + DPoP
                .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // Enhanced CORS Configuration for DPoP
                .cors(cors -> cors.configurationSource(enhancedCorsConfigurationSource()))
                
                // Enhanced Authorization Rules with DPoP-protected endpoints
                .authorizeHttpRequests(authz -> authz
                    // Public endpoints (no DPoP required)
                    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                    .requestMatchers("/api/v1/public/**").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    
                    // OAuth2/OpenID Connect endpoints (no DPoP on these)
                    .requestMatchers("/oauth2/par", "/oauth2/authorize", "/oauth2/token").permitAll()
                    .requestMatchers("/oauth2/nonce", "/oauth2/status").permitAll()
                    
                    // FAPI endpoints require DPoP validation
                    .requestMatchers("/api/v1/fapi/**").hasRole("FAPI_CLIENT")
                    
                    // Core banking endpoints require DPoP validation
                    .requestMatchers("/api/v1/loans/**").hasAnyRole("CUSTOMER", "LOAN_OFFICER", "ADMIN")
                    .requestMatchers("/api/v1/payments/**").hasAnyRole("CUSTOMER", "LOAN_OFFICER", "ADMIN")
                    .requestMatchers("/api/v1/customers/**").hasAnyRole("LOAN_OFFICER", "ADMIN")
                    
                    // AI endpoints require specific roles + DPoP
                    .requestMatchers("/api/v1/ai/**").hasAnyRole("LOAN_OFFICER", "UNDERWRITER", "AI_ANALYST")
                    
                    // Admin endpoints require admin role + DPoP
                    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                    .requestMatchers("/api/v1/audit/**").hasAnyRole("AUDITOR", "ADMIN")
                    
                    // All other requests require authentication + DPoP
                    .anyRequest().authenticated())
                
                // JWT Configuration with DPoP awareness
                .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt
                        .decoder(jwtDecoder())
                        .jwtAuthenticationConverter(enhancedJwtAuthenticationConverter())))
                
                // Filter Chain Order:
                // 1. FAPI Rate Limiting (per client)
                // 2. FAPI Request Validation (headers, structure)
                // 3. DPoP Validation (proof and token binding)
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(requestValidationFilter, FAPIRateLimitingFilter.class)
                .addFilterBefore(dpopValidationFilter(), FAPIRequestValidationFilter.class)
                
                .build();
    }

    @Bean
    public CorsConfigurationSource enhancedCorsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Enhanced CORS configuration for FAPI + DPoP
        configuration.setAllowedOriginPatterns(securityProperties.getCors().getAllowedOrigins());
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin",
            // FAPI headers
            "X-FAPI-Interaction-ID", "X-FAPI-Auth-Date", "X-FAPI-Customer-IP-Address",
            // DPoP headers
            "DPoP", "DPoP-Nonce"
        ));
        configuration.setExposedHeaders(Arrays.asList(
            // FAPI headers
            "X-FAPI-Interaction-ID", "X-RateLimit-Remaining", "X-RateLimit-Retry-After",
            // DPoP headers
            "WWW-Authenticate", "DPoP-Nonce", "X-DPoP-Supported"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(Duration.ofMinutes(30));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/oauth2/**", configuration);
        
        return source;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // Configure JWT decoder with enhanced validation for DPoP-bound tokens
        return NimbusJwtDecoder.withJwkSetUri(securityProperties.getJwt().getJwkSetUri())
                .jwsAlgorithm(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.PS256)
                .jwsAlgorithm(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256)
                .jwsAlgorithm(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.ES256)
                .build();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        JWK jwk = new RSAKey.Builder(rsaPublicKey())
                .privateKey(rsaPrivateKey())
                .keyID(java.util.UUID.randomUUID().toString())
                .algorithm(com.nimbusds.jose.JWSAlgorithm.PS256)
                .build();
        JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwks);
    }

    @Bean
    public JwtAuthenticationConverter enhancedJwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        authoritiesConverter.setAuthoritiesClaimName("roles");

        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        
        // Enhanced converter that can handle DPoP-bound tokens
        authenticationConverter.setPrincipalClaimName("sub");
        
        return authenticationConverter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(securityProperties.getPassword().getStrength());
    }

    @Bean
    public RSAPublicKey rsaPublicKey() {
        return (RSAPublicKey) keyPair().getPublic();
    }

    @Bean
    public RSAPrivateKey rsaPrivateKey() {
        return (RSAPrivateKey) keyPair().getPrivate();
    }

    @Bean
    public KeyPair keyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate RSA key pair for Enhanced FAPI + DPoP", e);
        }
    }
}