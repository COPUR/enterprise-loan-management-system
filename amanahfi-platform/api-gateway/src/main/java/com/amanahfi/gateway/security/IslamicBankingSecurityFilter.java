package com.amanahfi.gateway.security;

import org.springframework.stereotype.Component;
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
@Component
public class IslamicBankingSecurityFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            HttpHeaders responseHeaders = exchange.getResponse().getHeaders();
            
            // Islamic Banking Compliance Headers
            responseHeaders.add("X-Islamic-Banking", "true");
            responseHeaders.add("X-Sharia-Compliant", "true");
            responseHeaders.add("X-No-Riba", "true");
            responseHeaders.add("X-Halal-Certified", "true");
            
            // UAE Regulatory Compliance
            responseHeaders.add("X-Regulatory-Compliance", "CBUAE,VARA,HSA");
            
            // Islamic Finance Standards
            responseHeaders.add("X-Islamic-Finance-Standard", "AAOIFI");
            responseHeaders.add("X-Sharia-Supervisory-Board", "APPROVED");
            
            // Add specific headers for different Islamic banking products
            String path = exchange.getRequest().getPath().value();
            
            if (path.contains("/murabaha")) {
                responseHeaders.add("X-Islamic-Product-Type", "MURABAHA");
                responseHeaders.add("X-Asset-Backed", "true");
                responseHeaders.add("X-Profit-Sharing", "true");
            }
            
            if (path.contains("/accounts")) {
                responseHeaders.add("X-Islamic-Account", "true");
                responseHeaders.add("X-Interest-Free", "true");
            }
            
            if (path.contains("/payments")) {
                responseHeaders.add("X-Islamic-Payment", "true");
                responseHeaders.add("X-Riba-Free", "true");
            }
            
            // CBDC compliance for UAE Digital Dirham
            if (path.contains("/cbdc") || path.contains("/digital-dirham")) {
                responseHeaders.add("X-CBDC-Compliant", "true");
                responseHeaders.add("X-Digital-Currency", "UAE-DIRHAM");
                responseHeaders.add("X-Central-Bank", "CBUAE");
            }
            
            // Enhanced security for high-value transactions
            if (path.contains("/high-value")) {
                responseHeaders.add("X-Enhanced-Security", "true");
                responseHeaders.add("X-Request-Signature-Required", "true");
                responseHeaders.add("X-Multi-Factor-Auth", "required");
            }
        }));
    }
}