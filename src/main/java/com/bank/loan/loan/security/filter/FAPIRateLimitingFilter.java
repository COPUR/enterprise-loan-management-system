package com.bank.loanmanagement.loan.security.filter;

import com.bank.loanmanagement.loan.security.config.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * FAPI-compliant rate limiting filter
 * Implements sophisticated rate limiting for banking APIs
 * Following FAPI security profile recommendations
 */
@Component
@RequiredArgsConstructor
public class FAPIRateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FAPIRateLimitingFilter.class);
    
    private final SecurityProperties securityProperties;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        if (!securityProperties.getRateLimit().isEnableRateLimiting()) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientId = extractClientId(request);
        String endpoint = request.getRequestURI();
        
        // Create rate limit key
        String rateLimitKey = String.format("rate_limit:%s:%s", clientId, endpoint);
        
        try {
            if (isRateLimited(rateLimitKey)) {
                handleRateLimitExceeded(response, clientId, endpoint);
                return;
            }
            
            // Increment request count
            incrementRequestCount(rateLimitKey);
            
            // Add rate limit headers
            addRateLimitHeaders(response, rateLimitKey);
            
            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            log.error("Error in rate limiting filter for client: {}", clientId, e);
            // Continue processing on error to avoid blocking legitimate requests
            filterChain.doFilter(request, response);
        }
    }

    private String extractClientId(HttpServletRequest request) {
        // Extract client ID from JWT token or API key
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // In a real implementation, decode JWT and extract client_id
            return "extracted-client-id"; // Placeholder
        }
        
        // Fallback to IP address for anonymous requests
        return getClientIpAddress(request);
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

    private boolean isRateLimited(String rateLimitKey) {
        String currentCount = redisTemplate.opsForValue().get(rateLimitKey);
        
        if (currentCount == null) {
            return false; // First request
        }
        
        int requestCount = Integer.parseInt(currentCount);
        int maxRequests = securityProperties.getRateLimit().getRequestsPerMinute();
        
        if (requestCount >= maxRequests) {
            log.warn("Rate limit exceeded for key: {} (count: {}, limit: {})", 
                    rateLimitKey, requestCount, maxRequests);
            return true;
        }
        
        return false;
    }

    private void incrementRequestCount(String rateLimitKey) {
        String currentCount = redisTemplate.opsForValue().get(rateLimitKey);
        
        if (currentCount == null) {
            // First request in window
            redisTemplate.opsForValue().set(rateLimitKey, "1", 
                Duration.ofMinutes(securityProperties.getRateLimit().getWindowSizeMinutes()));
        } else {
            // Increment existing count
            redisTemplate.opsForValue().increment(rateLimitKey);
        }
    }

    private void addRateLimitHeaders(HttpServletResponse response, String rateLimitKey) {
        String currentCount = redisTemplate.opsForValue().get(rateLimitKey);
        int requestCount = currentCount != null ? Integer.parseInt(currentCount) : 0;
        int maxRequests = securityProperties.getRateLimit().getRequestsPerMinute();
        int remaining = Math.max(0, maxRequests - requestCount);
        
        response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequests));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + 
            TimeUnit.MINUTES.toMillis(securityProperties.getRateLimit().getWindowSizeMinutes())));
    }

    private void handleRateLimitExceeded(HttpServletResponse response, String clientId, String endpoint) 
            throws IOException {
        
        log.warn("Rate limit exceeded for client: {} on endpoint: {}", clientId, endpoint);
        
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader("Retry-After", String.valueOf(
            securityProperties.getRateLimit().getWindowSizeMinutes() * 60));
        response.setHeader("X-RateLimit-Retry-After", String.valueOf(
            System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(
                securityProperties.getRateLimit().getWindowSizeMinutes())));
        
        String errorResponse = """
            {
                "error": "rate_limit_exceeded",
                "error_description": "Too many requests. Please retry after the specified time.",
                "retry_after": %d,
                "limit": %d
            }
            """.formatted(
                securityProperties.getRateLimit().getWindowSizeMinutes() * 60,
                securityProperties.getRateLimit().getRequestsPerMinute()
            );
        
        response.getWriter().write(errorResponse);
        response.getWriter().flush();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Skip rate limiting for health checks and public endpoints
        return path.startsWith("/actuator/health") || 
               path.startsWith("/api/v1/public/") ||
               path.endsWith(".css") ||
               path.endsWith(".js") ||
               path.endsWith(".ico");
    }
}