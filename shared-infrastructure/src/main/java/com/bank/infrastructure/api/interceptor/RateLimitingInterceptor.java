package com.bank.infrastructure.api.interceptor;

import com.bank.infrastructure.api.config.RateLimitingConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Rate Limiting Interceptor
 * 
 * Implements sophisticated rate limiting for API endpoints:
 * - Multiple rate limiting strategies (fixed window, sliding window)
 * - Different limits for different user tiers
 * - IP-based and user-based rate limiting
 * - Distributed rate limiting using Redis
 * - Graceful degradation and circuit breaker integration
 */
@Component
public class RateLimitingInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingInterceptor.class);
    
    private final RedisTemplate<String, String> redisTemplate;
    private final RateLimitingConfiguration config;
    
    // Rate limiting strategies
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final String SLIDING_WINDOW_PREFIX = "sliding_window:";
    
    @Autowired
    public RateLimitingInterceptor(RedisTemplate<String, String> redisTemplate, 
                                  RateLimitingConfiguration config) {
        this.redisTemplate = redisTemplate;
        this.config = config;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!config.isEnabled()) {
            return true;
        }
        
        try {
            String clientId = getClientIdentifier(request);
            String endpoint = getEndpointIdentifier(request);
            RateLimitTier tier = getUserTier(request);
            
            // Check multiple rate limiting strategies
            RateLimitResult result = checkRateLimit(clientId, endpoint, tier);
            
            // Set rate limit headers
            setRateLimitHeaders(response, result);
            
            if (result.isAllowed()) {
                return true;
            } else {
                // Rate limit exceeded
                handleRateLimitExceeded(request, response, result);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error in rate limiting interceptor", e);
            // Fail open - allow request if rate limiting fails
            return true;
        }
    }
    
    private RateLimitResult checkRateLimit(String clientId, String endpoint, RateLimitTier tier) {
        RateLimitConfig limitConfig = config.getConfigForTier(tier);
        
        // Check global rate limit
        RateLimitResult globalResult = checkFixedWindowRateLimit(
            "global:" + clientId, 
            limitConfig.getGlobalLimit(), 
            limitConfig.getGlobalWindow()
        );
        
        if (!globalResult.isAllowed()) {
            return globalResult;
        }
        
        // Check endpoint-specific rate limit
        RateLimitResult endpointResult = checkFixedWindowRateLimit(
            "endpoint:" + clientId + ":" + endpoint,
            limitConfig.getEndpointLimit(),
            limitConfig.getEndpointWindow()
        );
        
        if (!endpointResult.isAllowed()) {
            return endpointResult;
        }
        
        // Check sliding window rate limit for more sophisticated control
        RateLimitResult slidingResult = checkSlidingWindowRateLimit(
            "sliding:" + clientId,
            limitConfig.getSlidingWindowLimit(),
            limitConfig.getSlidingWindowDuration()
        );
        
        return slidingResult;
    }
    
    private RateLimitResult checkFixedWindowRateLimit(String key, int limit, Duration window) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        long windowStart = Instant.now().toEpochMilli() / window.toMillis();
        String windowKey = redisKey + ":" + windowStart;
        
        try {
            String currentCountStr = redisTemplate.opsForValue().get(windowKey);
            int currentCount = currentCountStr != null ? Integer.parseInt(currentCountStr) : 0;
            
            if (currentCount >= limit) {
                long resetTime = (windowStart + 1) * window.toMillis();
                return new RateLimitResult(false, currentCount, limit, resetTime);
            }
            
            // Increment counter
            redisTemplate.opsForValue().increment(windowKey);
            redisTemplate.expire(windowKey, window.toSeconds(), TimeUnit.SECONDS);
            
            long resetTime = (windowStart + 1) * window.toMillis();
            return new RateLimitResult(true, currentCount + 1, limit, resetTime);
            
        } catch (Exception e) {
            logger.warn("Failed to check fixed window rate limit for key: {}", windowKey, e);
            return new RateLimitResult(true, 0, limit, 0);
        }
    }
    
    private RateLimitResult checkSlidingWindowRateLimit(String key, int limit, Duration window) {
        String redisKey = SLIDING_WINDOW_PREFIX + key;
        long now = Instant.now().toEpochMilli();
        long windowStart = now - window.toMillis();
        
        try {
            // Remove old entries
            redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);
            
            // Count current entries
            Long currentCount = redisTemplate.opsForZSet().count(redisKey, windowStart, now);
            
            if (currentCount >= limit) {
                return new RateLimitResult(false, currentCount.intValue(), limit, now + window.toMillis());
            }
            
            // Add current request
            redisTemplate.opsForZSet().add(redisKey, String.valueOf(now), now);
            redisTemplate.expire(redisKey, window.toSeconds(), TimeUnit.SECONDS);
            
            return new RateLimitResult(true, currentCount.intValue() + 1, limit, now + window.toMillis());
            
        } catch (Exception e) {
            logger.warn("Failed to check sliding window rate limit for key: {}", redisKey, e);
            return new RateLimitResult(true, 0, limit, 0);
        }
    }
    
    private String getClientIdentifier(HttpServletRequest request) {
        // Priority order: User ID > API Key > IP Address
        String userId = getUserIdFromRequest(request);
        if (userId != null) {
            return "user:" + userId;
        }
        
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null) {
            return "api:" + apiKey;
        }
        
        return "ip:" + getClientIpAddress(request);
    }
    
    private String getEndpointIdentifier(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        
        // Normalize path by removing IDs and query parameters
        path = path.replaceAll("/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", "/{id}");
        path = path.replaceAll("/[0-9]+", "/{id}");
        
        return method + ":" + path;
    }
    
    private RateLimitTier getUserTier(HttpServletRequest request) {
        String tierHeader = request.getHeader("X-User-Tier");
        if (tierHeader != null) {
            try {
                return RateLimitTier.valueOf(tierHeader.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.debug("Invalid user tier header: {}", tierHeader);
            }
        }
        
        // Default to BASIC tier
        return RateLimitTier.BASIC;
    }
    
    private String getUserIdFromRequest(HttpServletRequest request) {
        // Extract user ID from JWT token or session
        // This is a placeholder - implement based on your authentication mechanism
        return request.getHeader("X-User-ID");
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "X-Originating-IP",
            "CF-Connecting-IP",
            "True-Client-IP"
        };
        
        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        
        return request.getRemoteAddr();
    }
    
    private void setRateLimitHeaders(HttpServletResponse response, RateLimitResult result) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.getLimit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.getRemaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(result.getResetTime()));
        
        if (!result.isAllowed()) {
            response.setHeader("Retry-After", String.valueOf((result.getResetTime() - System.currentTimeMillis()) / 1000));
        }
    }
    
    private void handleRateLimitExceeded(HttpServletRequest request, HttpServletResponse response, 
                                        RateLimitResult result) {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/problem+json");
        
        String retryAfter = String.valueOf((result.getResetTime() - System.currentTimeMillis()) / 1000);
        response.setHeader("Retry-After", retryAfter);
        
        try {
            String responseBody = String.format("""
                {
                    "type": "https://banking.example.com/problems/rate-limit-exceeded",
                    "title": "Rate Limit Exceeded",
                    "status": 429,
                    "detail": "Too many requests. Please try again later.",
                    "instance": "%s",
                    "timestamp": "%s",
                    "requestId": "%s",
                    "retryAfter": %s,
                    "limit": %d,
                    "remaining": %d,
                    "resetTime": %d
                }
                """,
                request.getRequestURI(),
                Instant.now().toString(),
                request.getAttribute("requestId"),
                retryAfter,
                result.getLimit(),
                result.getRemaining(),
                result.getResetTime()
            );
            
            response.getWriter().write(responseBody);
        } catch (Exception e) {
            logger.error("Failed to write rate limit response", e);
        }
        
        logger.warn("Rate limit exceeded for client: {} on endpoint: {}",
            getClientIdentifier(request),
            getEndpointIdentifier(request));
    }
    
    // Inner classes
    
    public enum RateLimitTier {
        BASIC, PREMIUM, ENTERPRISE
    }
    
    public static class RateLimitResult {
        private final boolean allowed;
        private final int current;
        private final int limit;
        private final long resetTime;
        
        public RateLimitResult(boolean allowed, int current, int limit, long resetTime) {
            this.allowed = allowed;
            this.current = current;
            this.limit = limit;
            this.resetTime = resetTime;
        }
        
        public boolean isAllowed() { return allowed; }
        public int getCurrent() { return current; }
        public int getLimit() { return limit; }
        public int getRemaining() { return Math.max(0, limit - current); }
        public long getResetTime() { return resetTime; }
    }
    
    public static class RateLimitConfig {
        private final int globalLimit;
        private final Duration globalWindow;
        private final int endpointLimit;
        private final Duration endpointWindow;
        private final int slidingWindowLimit;
        private final Duration slidingWindowDuration;
        
        public RateLimitConfig(int globalLimit, Duration globalWindow,
                              int endpointLimit, Duration endpointWindow,
                              int slidingWindowLimit, Duration slidingWindowDuration) {
            this.globalLimit = globalLimit;
            this.globalWindow = globalWindow;
            this.endpointLimit = endpointLimit;
            this.endpointWindow = endpointWindow;
            this.slidingWindowLimit = slidingWindowLimit;
            this.slidingWindowDuration = slidingWindowDuration;
        }
        
        public int getGlobalLimit() { return globalLimit; }
        public Duration getGlobalWindow() { return globalWindow; }
        public int getEndpointLimit() { return endpointLimit; }
        public Duration getEndpointWindow() { return endpointWindow; }
        public int getSlidingWindowLimit() { return slidingWindowLimit; }
        public Duration getSlidingWindowDuration() { return slidingWindowDuration; }
    }
}
