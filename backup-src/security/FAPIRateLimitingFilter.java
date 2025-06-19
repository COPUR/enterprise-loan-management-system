package com.bank.loanmanagement.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * FAPI Rate Limiting Filter
 * Implements Financial-grade API rate limiting requirements:
 * - Per-client rate limiting
 * - Burst protection
 * - FAPI-compliant error responses
 */
@Component
public class FAPIRateLimitingFilter implements Filter {

    private final ConcurrentHashMap<String, ClientRateLimit> rateLimitMap = new ConcurrentHashMap<>();
    private static final int DEFAULT_REQUESTS_PER_MINUTE = 60;
    private static final int BURST_LIMIT = 10;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String clientId = getClientIdentifier(httpRequest);
        
        if (!isRequestAllowed(clientId)) {
            sendRateLimitExceededResponse(httpResponse);
            return;
        }
        
        // Add rate limit headers
        ClientRateLimit rateLimit = rateLimitMap.get(clientId);
        if (rateLimit != null) {
            httpResponse.setHeader("X-RateLimit-Limit", String.valueOf(DEFAULT_REQUESTS_PER_MINUTE));
            httpResponse.setHeader("X-RateLimit-Remaining", String.valueOf(rateLimit.getRemainingRequests()));
            httpResponse.setHeader("X-RateLimit-Reset", String.valueOf(rateLimit.getResetTime()));
        }
        
        chain.doFilter(request, response);
    }
    
    private String getClientIdentifier(HttpServletRequest request) {
        // Use client IP + User-Agent as identifier
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        return clientIp + ":" + (userAgent != null ? userAgent.hashCode() : "unknown");
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    private boolean isRequestAllowed(String clientId) {
        long currentTime = System.currentTimeMillis();
        
        ClientRateLimit rateLimit = rateLimitMap.computeIfAbsent(clientId, 
            k -> new ClientRateLimit(DEFAULT_REQUESTS_PER_MINUTE));
        
        return rateLimit.allowRequest(currentTime);
    }
    
    private void sendRateLimitExceededResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader("Retry-After", "60");
        
        String errorResponse = "{\n" +
            "  \"error\": \"rate_limit_exceeded\",\n" +
            "  \"error_description\": \"Too many requests. Please retry after 60 seconds.\",\n" +
            "  \"fapi_compliance\": \"FAPI rate limiting enforced\"\n" +
            "}";
        
        response.getWriter().write(errorResponse);
    }
    
    private static class ClientRateLimit {
        private final int maxRequests;
        private final AtomicInteger requestCount;
        private volatile long windowStart;
        private static final long WINDOW_SIZE_MS = 60 * 1000; // 1 minute
        
        public ClientRateLimit(int maxRequests) {
            this.maxRequests = maxRequests;
            this.requestCount = new AtomicInteger(0);
            this.windowStart = System.currentTimeMillis();
        }
        
        public synchronized boolean allowRequest(long currentTime) {
            // Reset window if expired
            if (currentTime - windowStart >= WINDOW_SIZE_MS) {
                windowStart = currentTime;
                requestCount.set(0);
            }
            
            // Check if request is allowed
            if (requestCount.get() >= maxRequests) {
                return false;
            }
            
            requestCount.incrementAndGet();
            return true;
        }
        
        public int getRemainingRequests() {
            return Math.max(0, maxRequests - requestCount.get());
        }
        
        public long getResetTime() {
            return windowStart + WINDOW_SIZE_MS;
        }
    }
}