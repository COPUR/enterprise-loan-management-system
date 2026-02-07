package com.amanahfi.gateway.security;

import org.springframework.http.HttpHeaders;
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
        exchange.getResponse().beforeCommit(() -> {
            HttpHeaders headers = exchange.getResponse().getHeaders();

            // Add FAPI 2.0 required security headers if not already present
            if (!headers.containsKey("X-XSS-Protection")) {
                headers.add("X-XSS-Protection", "1; mode=block");
            }
            if (!headers.containsKey("Content-Security-Policy")) {
                headers.add("Content-Security-Policy",
                    "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; " +
                    "img-src 'self' data:; font-src 'self'; object-src 'none'; frame-ancestors 'none';");
            }
            if (!headers.containsKey("X-Permitted-Cross-Domain-Policies")) {
                headers.add("X-Permitted-Cross-Domain-Policies", "none");
            }
            if (!headers.containsKey("Permissions-Policy")) {
                headers.add("Permissions-Policy", "geolocation=(), microphone=(), camera=(), payment=()");
            }

            return Mono.empty();
        });

        return chain.filter(exchange);
    }
}
