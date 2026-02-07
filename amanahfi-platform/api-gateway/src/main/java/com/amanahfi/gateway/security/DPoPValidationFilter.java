package com.amanahfi.gateway.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * DPoP validation filter for FAPI 2.0 protected endpoints.
 * Adds DPoP-Nonce on validation failures to support client retry.
 */
public class DPoPValidationFilter implements WebFilter {

    private final DPoPTokenValidator validator;

    public DPoPValidationFilter(DPoPTokenValidator validator) {
        this.validator = validator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (!path.startsWith("/api/v1/payments")) {
            return chain.filter(exchange);
        }

        String dPoPToken = exchange.getRequest().getHeaders().getFirst("DPoP");
        String accessToken = exchange.getRequest().getHeaders().getFirst("Authorization");

        return validator.validateDPoPToken(dPoPToken, accessToken, exchange)
            .flatMap(result -> {
                if (result.isValid()) {
                    return chain.filter(exchange);
                }

                exchange.getResponse().getHeaders().add("DPoP-Nonce", result.getNonce());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            });
    }
}
