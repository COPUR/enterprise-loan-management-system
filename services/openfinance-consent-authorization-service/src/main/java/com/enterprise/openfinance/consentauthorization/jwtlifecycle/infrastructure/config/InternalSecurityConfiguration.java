package com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.config;

import com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.security.InternalBearerAuthenticationFilter;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.security.InternalTokenAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class InternalSecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            InternalBearerAuthenticationFilter internalBearerAuthenticationFilter,
            InternalTokenAuthenticationEntryPoint authenticationEntryPoint
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/internal/v1/authenticate").permitAll()
                        .requestMatchers("/internal/v1/logout", "/internal/v1/business").authenticated()
                        .requestMatchers("/internal/v1/system/secrets/**").authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(internalBearerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                .headers(Customizer.withDefaults());

        return http.build();
    }
}
