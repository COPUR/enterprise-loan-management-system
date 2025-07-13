package com.amanahfi.gateway.security;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Security Headers Filter for FAPI 2.0 compliance
 * 
 * Adds required security headers that are not handled by Spring Security defaults
 */
@Component
public class SecurityHeadersFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            // Add FAPI 2.0 required security headers
            exchange.getResponse().getHeaders().add("X-XSS-Protection", "1; mode=block");
            exchange.getResponse().getHeaders().add("Content-Security-Policy", 
                "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data:; font-src 'self'; object-src 'none'; frame-ancestors 'none';");
            exchange.getResponse().getHeaders().add("X-Permitted-Cross-Domain-Policies", "none");
            exchange.getResponse().getHeaders().add("Permissions-Policy", 
                "geolocation=(), microphone=(), camera=(), payment=()");
        }));
    }
}