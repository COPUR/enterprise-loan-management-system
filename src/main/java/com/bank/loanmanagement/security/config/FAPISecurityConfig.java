package com.bank.loanmanagement.security.config;

import com.bank.loanmanagement.security.filter.FAPIRateLimitingFilter;
import com.bank.loanmanagement.security.filter.FAPIRequestValidationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
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
import java.util.List;

/**
 * FAPI (Financial-grade API) Security Configuration
 * Implements Financial-grade API security profile for banking applications
 * Following OpenID Foundation FAPI specifications
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties(SecurityProperties.class)
@RequiredArgsConstructor
public class FAPISecurityConfig {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FAPISecurityConfig.class);

    private final SecurityProperties securityProperties;
    private final FAPIRateLimitingFilter rateLimitingFilter;
    private final FAPIRequestValidationFilter requestValidationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring FAPI-compliant security filter chain");
        
        return http
                // Disable CSRF for APIs (use JWT instead)
                .csrf(AbstractHttpConfigurer::disable)
                
                // FAPI Security Headers
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
                    }))
                
                // Session Management - Stateless for FAPI
                .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // CORS Configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                // Authorization Rules
                .authorizeHttpRequests(authz -> authz
                    // Public endpoints
                    .requestMatchers("/actuator/health", "/api/v1/public/**").permitAll()
                    
                    // FAPI-specific endpoints
                    .requestMatchers("/api/v1/fapi/**").hasRole("FAPI_CLIENT")
                    
                    // AI endpoints require specific roles
                    .requestMatchers("/api/v1/ai/**").hasAnyRole("LOAN_OFFICER", "UNDERWRITER", "AI_ANALYST")
                    
                    // Admin endpoints
                    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                    
                    // All other requests require authentication
                    .anyRequest().authenticated())
                
                // JWT Configuration
                .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt
                        .decoder(jwtDecoder())
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())))
                
                // Add FAPI-specific filters
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(requestValidationFilter, FAPIRateLimitingFilter.class)
                
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // FAPI-compliant CORS configuration
        configuration.setAllowedOriginPatterns(securityProperties.getCors().getAllowedOrigins());
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin",
            "X-FAPI-Interaction-ID", "X-FAPI-Auth-Date", "X-FAPI-Customer-IP-Address"
        ));
        configuration.setExposedHeaders(Arrays.asList(
            "X-FAPI-Interaction-ID", "X-RateLimit-Remaining", "X-RateLimit-Retry-After"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(Duration.ofMinutes(30));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        
        return source;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(securityProperties.getJwt().getJwkSetUri())
                .jwsAlgorithm(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256)
                .build();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        JWK jwk = new RSAKey.Builder(rsaPublicKey())
                .privateKey(rsaPrivateKey())
                .keyID(java.util.UUID.randomUUID().toString())
                .build();
        JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwks);
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        authoritiesConverter.setAuthoritiesClaimName("roles");

        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
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
            throw new IllegalStateException("Failed to generate RSA key pair for FAPI", e);
        }
    }
}