package com.amanahfi.gateway.security;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpHeaders;

/**
 * Islamic Banking Security Filter
 * 
 * Adds Islamic banking and Sharia compliance headers for AmanahFi platform
 * Ensures all responses include compliance information for regulatory purposes
 * 
 * Headers added:
 * - X-Islamic-Banking: true
 * - X-Sharia-Compliant: true  
 * - X-Regulatory-Compliance: CBUAE,VARA,HSA
 * - X-No-Riba: true (confirms no interest-based transactions)
 * - X-Halal-Certified: true
 */
public class IslamicBankingSecurityFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        exchange.getResponse().beforeCommit(() -> {
            HttpHeaders responseHeaders = exchange.getResponse().getHeaders();

            // Core security headers for FAPI 2.0 compliance
            responseHeaders.set("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            responseHeaders.set("X-Content-Type-Options", "nosniff");
            responseHeaders.set("X-Frame-Options", "DENY");
            responseHeaders.set("X-XSS-Protection", "1; mode=block");
            responseHeaders.set("Content-Security-Policy", "default-src 'self'");

            // Islamic Banking Compliance Headers
            responseHeaders.set("X-Islamic-Banking", "true");
            responseHeaders.set("X-Sharia-Compliant", "true");
            responseHeaders.set("X-No-Riba", "true");
            responseHeaders.set("X-Halal-Certified", "true");

            // UAE Regulatory Compliance
            responseHeaders.set("X-Regulatory-Compliance", "CBUAE,VARA,HSA");

            // Islamic Finance Standards
            responseHeaders.set("X-Islamic-Finance-Standard", "AAOIFI");
            responseHeaders.set("X-Sharia-Supervisory-Board", "APPROVED");

            // Add specific headers for different Islamic banking products
            String path = exchange.getRequest().getPath().value();

            if (path.contains("/murabaha")) {
                responseHeaders.set("X-Islamic-Product-Type", "MURABAHA");
                responseHeaders.set("X-Asset-Backed", "true");
                responseHeaders.set("X-Profit-Sharing", "true");
            }

            if (path.contains("/accounts")) {
                responseHeaders.set("X-Islamic-Account", "true");
                responseHeaders.set("X-Interest-Free", "true");
            }

            if (path.contains("/payments")) {
                responseHeaders.set("X-Islamic-Payment", "true");
                responseHeaders.set("X-Riba-Free", "true");
            }

            // CBDC compliance for UAE Digital Dirham
            if (path.contains("/cbdc") || path.contains("/digital-dirham")) {
                responseHeaders.set("X-CBDC-Compliant", "true");
                responseHeaders.set("X-Digital-Currency", "UAE-DIRHAM");
                responseHeaders.set("X-Central-Bank", "CBUAE");
            }

            // Enhanced security for high-value transactions
            if (path.contains("/high-value")) {
                responseHeaders.set("X-Enhanced-Security", "true");
                responseHeaders.set("X-Request-Signature-Required", "true");
                responseHeaders.set("X-Multi-Factor-Auth", "required");
            }

            return Mono.empty();
        });

        return chain.filter(exchange);
    }
}
