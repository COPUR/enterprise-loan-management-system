package com.bank.loanmanagement.loan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Minimal Enterprise Security Configuration
 * Simplified security for testing enterprise architecture components
 */
@Configuration
@EnableWebSecurity
@Profile({"enterprise-minimal"})
public class EnterpriseMinimalSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/api/actuator/**").permitAll()
                .requestMatchers("/api/v3/api-docs/**", "/api/swagger-ui/**").permitAll()
                .requestMatchers("/graphql", "/graphiql/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                
                // Banking API endpoints - allow all for testing
                .requestMatchers("/api/loans/**").permitAll()
                .requestMatchers("/api/payments/**").permitAll()
                .requestMatchers("/api/customers/**").permitAll()
                .requestMatchers("/api/admin/**").permitAll()
                
                // Allow all other requests for testing
                .anyRequest().permitAll()
            )
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.disable())
            );

        return http.build();
    }
}