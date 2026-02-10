package com.enterprise.openfinance.bankingmetadata.infrastructure.config;

import com.enterprise.openfinance.bankingmetadata.infrastructure.security.DpopAwareBearerTokenResolver;
import com.enterprise.openfinance.bankingmetadata.infrastructure.security.DpopProofValidationFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(MetadataSecurityProperties.class)
public class MetadataSecurityConfiguration {

    @Bean
    public SecurityFilterChain metadataSecurityFilterChain(HttpSecurity http,
                                                           BearerTokenResolver tokenResolver,
                                                           DpopProofValidationFilter dpopProofValidationFilter,
                                                           Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/open-finance/v1/metadata/accounts/*/transactions").hasAuthority("SCOPE_read_transactions")
                        .requestMatchers(HttpMethod.GET, "/open-finance/v1/metadata/accounts/*/parties").hasAuthority("SCOPE_read_parties")
                        .requestMatchers(HttpMethod.GET, "/open-finance/v1/metadata/accounts/*").hasAuthority("SCOPE_read_metadata")
                        .requestMatchers(HttpMethod.GET, "/open-finance/v1/metadata/standing-orders").hasAuthority("SCOPE_read_standing_orders")
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(tokenResolver)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler()))
                .addFilterAfter(dpopProofValidationFilter, BearerTokenAuthenticationFilter.class)
                .cors(Customizer.withDefaults())
                .build();
    }

    @Bean
    public BearerTokenResolver tokenResolver() {
        return new DpopAwareBearerTokenResolver();
    }

    @Bean
    public Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return new JwtAuthenticationConverter();
    }

    @Bean
    public JwtDecoder jwtDecoder(MetadataSecurityProperties properties) {
        SecretKeySpec key = new SecretKeySpec(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefault();
        if (properties.getIssuer() != null && !properties.getIssuer().isBlank()) {
            validator = new DelegatingOAuth2TokenValidator<>(validator, JwtValidators.createDefaultWithIssuer(properties.getIssuer()));
        }
        if (properties.getAudience() != null && !properties.getAudience().isBlank()) {
            validator = new DelegatingOAuth2TokenValidator<>(validator, audienceValidator(properties.getAudience()));
        }
        decoder.setJwtValidator(validator);
        return decoder;
    }

    private static OAuth2TokenValidator<Jwt> audienceValidator(String audience) {
        return token -> token.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Missing required audience", null));
    }
}
