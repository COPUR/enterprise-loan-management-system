package com.amanahfi.gateway.security;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * PKCE validation filter for OAuth 2.1 authorization requests.
 */
public class PkceValidationFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (exchange.getRequest().getMethod() != HttpMethod.GET) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getPath().value();
        if (!"/oauth2/authorize".equals(path)) {
            return chain.filter(exchange);
        }

        String codeChallenge = exchange.getRequest().getQueryParams().getFirst("code_challenge");
        if (codeChallenge == null || codeChallenge.isBlank()) {
            return writePkceError(exchange, "Missing PKCE parameter: code_challenge");
        }

        String method = exchange.getRequest().getQueryParams().getFirst("code_challenge_method");
        if (method == null || method.isBlank()) {
            return writePkceError(exchange, "Missing PKCE parameter: code_challenge_method");
        }

        return chain.filter(exchange);
    }

    private Mono<Void> writePkceError(ServerWebExchange exchange, String description) {
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = """
            {
                "error": "invalid_request",
                "error_description": "%s",
                "timestamp": "%s"
            }
            """.formatted(description.replace("\"", "'"), Instant.now().toString());

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }
}
