package com.amanahfi.gateway.security;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatus;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.Instant;
import java.time.Duration;
import java.util.Map;

/**
 * Rate Limiting Filter for FAPI 2.0 Compliance
 * 
 * Implements rate limiting to protect against:
 * - Brute force attacks
 * - DDoS attacks  
 * - API abuse
 * - Resource exhaustion
 * 
 * Rate limits are applied per:
 * - Client ID (primary)
 * - IP address (fallback)
 * - User (if authenticated)
 * 
 * Different limits for different endpoint types:
 * - Authentication: 5 requests/minute
 * - Financial operations: 10 requests/minute
 * - Read operations: 100 requests/minute
 * - Admin operations: 2 requests/minute
 */
public class RateLimitingFilter implements WebFilter {

    // In-memory rate limiting (production would use Redis)
    private final Map<String, RateLimitEntry> rateLimitStore = new ConcurrentHashMap<>();
    
    // Rate limits per endpoint type
    private static final int AUTH_RATE_LIMIT = 5;           // /oauth2/** endpoints
    private static final int FINANCIAL_RATE_LIMIT = 10;     // /api/v1/payments/**, /api/v1/accounts/**
    private static final int READ_RATE_LIMIT = 100;         // GET requests
    private static final int ADMIN_RATE_LIMIT = 2;          // /api/v1/admin/**
    private static final int DEFAULT_RATE_LIMIT = 60;       // Default limit
    
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (exchange.getRequest().getMethod() != null 
            && "OPTIONS".equalsIgnoreCase(exchange.getRequest().getMethod().name())) {
            return chain.filter(exchange);
        }

        String clientKey = getClientKey(exchange);
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();
        
        int rateLimit = determineRateLimit(path, method);
        
        // Check rate limit
        RateLimitResult result = checkRateLimit(clientKey, rateLimit);
        
        // Add rate limit headers
        addRateLimitHeaders(exchange, result, rateLimit);
        
        if (result.isExceeded()) {
            return handleRateLimitExceeded(exchange);
        }
        
        return chain.filter(exchange);
    }

    /**
     * Determines the appropriate rate limit based on endpoint and method
     */
    private int determineRateLimit(String path, String method) {
        // Authentication endpoints - stricter limits
        if (path.startsWith("/oauth2/") || path.startsWith("/.well-known/")) {
            return AUTH_RATE_LIMIT;
        }
        
        // Admin endpoints - very strict
        if (path.startsWith("/api/v1/admin/")) {
            return ADMIN_RATE_LIMIT;
        }
        
        // Financial operations - moderate limits
        if (path.startsWith("/api/v1/payments") || 
            path.startsWith("/api/v1/accounts") ||
            path.startsWith("/api/v1/murabaha") ||
            path.startsWith("/api/v1/customers") ||
            path.startsWith("/api/v1/compliance")) {
            return FINANCIAL_RATE_LIMIT;
        }
        
        // Read operations - higher limits
        if ("GET".equals(method)) {
            return READ_RATE_LIMIT;
        }
        
        return DEFAULT_RATE_LIMIT;
    }

    /**
     * Gets client key for rate limiting (prioritizes client ID over IP)
     */
    private String getClientKey(ServerWebExchange exchange) {
        // Try to get client ID from header
        String clientId = exchange.getRequest().getHeaders().getFirst("X-Client-ID");
        if (clientId != null && !clientId.trim().isEmpty()) {
            return "client:" + clientId;
        }
        
        // Try to get client ID from Authorization header (if JWT)
        String authorization = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            // In real implementation, would decode JWT to get client_id
            // For demo, we'll use a hash of the token
            return "token:" + authorization.hashCode();
        }
        
        // Fallback to IP address
        String clientIp = getClientIpAddress(exchange);
        return "ip:" + clientIp;
    }

    /**
     * Gets client IP address from various headers
     */
    private String getClientIpAddress(ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        // Fallback to remote address
        return exchange.getRequest().getRemoteAddress() != null ? 
            exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }

    /**
     * Checks rate limit for client key
     */
    private RateLimitResult checkRateLimit(String clientKey, int limit) {
        Instant now = Instant.now();
        
        rateLimitStore.compute(clientKey, (key, entry) -> {
            if (entry == null || now.isAfter(entry.getWindowStart().plus(RATE_LIMIT_WINDOW))) {
                // New window
                return new RateLimitEntry(now, new AtomicInteger(1));
            } else {
                // Same window
                entry.getRequestCount().incrementAndGet();
                return entry;
            }
        });
        
        RateLimitEntry entry = rateLimitStore.get(clientKey);
        int currentCount = entry.getRequestCount().get();
        
        return new RateLimitResult(
            currentCount > limit,
            currentCount,
            limit,
            entry.getWindowStart().plus(RATE_LIMIT_WINDOW)
        );
    }

    /**
     * Adds rate limit headers to response
     */
    private void addRateLimitHeaders(ServerWebExchange exchange, RateLimitResult result, int limit) {
        exchange.getResponse().getHeaders().set("X-RateLimit-Limit", String.valueOf(limit));
        exchange.getResponse().getHeaders().set("X-RateLimit-Remaining", 
            String.valueOf(Math.max(0, limit - result.getCurrentCount())));
        exchange.getResponse().getHeaders().set("X-RateLimit-Reset", 
            String.valueOf(result.getResetTime().getEpochSecond()));
        
        if (result.isExceeded()) {
            exchange.getResponse().getHeaders().set("Retry-After", "60"); // Retry after 1 minute
        }
    }

    /**
     * Handles rate limit exceeded response
     */
    private Mono<Void> handleRateLimitExceeded(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        String errorResponse = """
            {
                "error": "rate_limit_exceeded",
                "error_description": "Too many requests. Please retry after the specified time.",
                "timestamp": "%s"
            }
            """.formatted(Instant.now().toString());
        
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(errorResponse.getBytes());
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    /**
     * Rate limit entry for tracking requests per window
     */
    private static class RateLimitEntry {
        private final Instant windowStart;
        private final AtomicInteger requestCount;

        public RateLimitEntry(Instant windowStart, AtomicInteger requestCount) {
            this.windowStart = windowStart;
            this.requestCount = requestCount;
        }

        public Instant getWindowStart() { return windowStart; }
        public AtomicInteger getRequestCount() { return requestCount; }
    }

    /**
     * Rate limit check result
     */
    private static class RateLimitResult {
        private final boolean exceeded;
        private final int currentCount;
        private final int limit;
        private final Instant resetTime;

        public RateLimitResult(boolean exceeded, int currentCount, int limit, Instant resetTime) {
            this.exceeded = exceeded;
            this.currentCount = currentCount;
            this.limit = limit;
            this.resetTime = resetTime;
        }

        public boolean isExceeded() { return exceeded; }
        public int getCurrentCount() { return currentCount; }
        public int getLimit() { return limit; }
        public Instant getResetTime() { return resetTime; }
    }
}
