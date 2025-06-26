package com.banking.loan.ratelimit;

import com.banking.loan.exception.BankingExceptionCatalogue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Adaptive rate limiting service with AI-powered dynamic threshold adjustment
 * Implements multiple rate limiting algorithms and intelligent throttling
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdaptiveRateLimitingService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChatClient chatClient;
    private final BankingExceptionCatalogue exceptionCatalogue;

    // In-memory cache for rate limit configurations (with Redis backup)
    private final Map<String, RateLimitConfig> rateLimitConfigs = new ConcurrentHashMap<>();
    
    // AI-powered adaptive thresholds
    private final Map<String, AdaptiveThreshold> adaptiveThresholds = new ConcurrentHashMap<>();

    /**
     * Rate limit configuration
     */
    public static class RateLimitConfig {
        public String key;
        public int requestsPerWindow;
        public Duration windowSize;
        public RateLimitAlgorithm algorithm;
        public boolean adaptiveEnabled;
        public double adaptiveFactor;
        public int burstCapacity;
        
        public RateLimitConfig(String key, int requestsPerWindow, Duration windowSize, 
                              RateLimitAlgorithm algorithm, boolean adaptiveEnabled) {
            this.key = key;
            this.requestsPerWindow = requestsPerWindow;
            this.windowSize = windowSize;
            this.algorithm = algorithm;
            this.adaptiveEnabled = adaptiveEnabled;
            this.adaptiveFactor = 1.0;
            this.burstCapacity = requestsPerWindow * 2; // Default burst capacity
        }
    }

    /**
     * Adaptive threshold tracking
     */
    public static class AdaptiveThreshold {
        public double currentMultiplier = 1.0;
        public Instant lastAdjustment = Instant.now();
        public int consecutiveViolations = 0;
        public double averageResponseTime = 0.0;
        public int totalRequests = 0;
        public int totalViolations = 0;
    }

    /**
     * Rate limiting algorithms
     */
    public enum RateLimitAlgorithm {
        TOKEN_BUCKET,
        SLIDING_WINDOW_LOG,
        SLIDING_WINDOW_COUNTER,
        FIXED_WINDOW,
        LEAKY_BUCKET
    }

    /**
     * Rate limit result
     */
    public static class RateLimitResult {
        public boolean allowed;
        public long remainingRequests;
        public Duration retryAfter;
        public String reason;
        public double confidence;
        public Map<String, Object> metadata;
        
        public RateLimitResult(boolean allowed, long remainingRequests, Duration retryAfter, String reason) {
            this.allowed = allowed;
            this.remainingRequests = remainingRequests;
            this.retryAfter = retryAfter;
            this.reason = reason;
            this.confidence = 1.0;
            this.metadata = new HashMap<>();
        }
    }

    /**
     * Initialize default rate limit configurations for banking operations
     */
    public void initializeDefaultConfigurations() {
        // Loan operations - moderate limits
        rateLimitConfigs.put("loan:create", new RateLimitConfig(
            "loan:create", 10, Duration.ofMinutes(1), RateLimitAlgorithm.TOKEN_BUCKET, true));
        rateLimitConfigs.put("loan:query", new RateLimitConfig(
            "loan:query", 100, Duration.ofMinutes(1), RateLimitAlgorithm.SLIDING_WINDOW_COUNTER, true));
        
        // Payment operations - stricter limits
        rateLimitConfigs.put("payment:transfer", new RateLimitConfig(
            "payment:transfer", 5, Duration.ofMinutes(1), RateLimitAlgorithm.TOKEN_BUCKET, true));
        rateLimitConfigs.put("payment:query", new RateLimitConfig(
            "payment:query", 50, Duration.ofMinutes(1), RateLimitAlgorithm.SLIDING_WINDOW_COUNTER, true));
        
        // AI operations - more tolerant but with burst protection
        rateLimitConfigs.put("ai:fraud-check", new RateLimitConfig(
            "ai:fraud-check", 20, Duration.ofMinutes(1), RateLimitAlgorithm.LEAKY_BUCKET, true));
        rateLimitConfigs.put("ai:recommendation", new RateLimitConfig(
            "ai:recommendation", 30, Duration.ofMinutes(1), RateLimitAlgorithm.SLIDING_WINDOW_LOG, true));
        
        // Authentication operations
        rateLimitConfigs.put("auth:login", new RateLimitConfig(
            "auth:login", 15, Duration.ofMinutes(5), RateLimitAlgorithm.SLIDING_WINDOW_LOG, false));
        rateLimitConfigs.put("auth:token-refresh", new RateLimitConfig(
            "auth:token-refresh", 10, Duration.ofMinutes(1), RateLimitAlgorithm.TOKEN_BUCKET, true));
        
        // Admin operations
        rateLimitConfigs.put("admin:bulk-operation", new RateLimitConfig(
            "admin:bulk-operation", 2, Duration.ofMinutes(5), RateLimitAlgorithm.FIXED_WINDOW, false));
        
        log.info("Initialized {} default rate limit configurations", rateLimitConfigs.size());
    }

    /**
     * Check rate limit for a given operation and user
     */
    public RateLimitResult checkRateLimit(String operation, String userId, String clientId) {
        String key = buildRateLimitKey(operation, userId, clientId);
        RateLimitConfig config = rateLimitConfigs.get(operation);
        
        if (config == null) {
            log.warn("No rate limit configuration found for operation: {}", operation);
            return new RateLimitResult(true, Long.MAX_VALUE, Duration.ZERO, "No limits configured");
        }

        // Apply adaptive adjustments if enabled
        if (config.adaptiveEnabled) {
            applyAdaptiveAdjustments(operation, config);
        }

        // Execute rate limiting based on algorithm
        RateLimitResult result = switch (config.algorithm) {
            case TOKEN_BUCKET -> executeTokenBucketRateLimit(key, config);
            case SLIDING_WINDOW_LOG -> executeSlidingWindowLogRateLimit(key, config);
            case SLIDING_WINDOW_COUNTER -> executeSlidingWindowCounterRateLimit(key, config);
            case FIXED_WINDOW -> executeFixedWindowRateLimit(key, config);
            case LEAKY_BUCKET -> executeLeakyBucketRateLimit(key, config);
        };

        // Track metrics for adaptive learning
        trackRateLimitMetrics(operation, result);
        
        // Use AI to analyze patterns and suggest adjustments
        if (!result.allowed && shouldTriggerAIAnalysis(operation)) {
            analyzeRateLimitPattern(operation, userId, clientId, result);
        }

        log.debug("Rate limit check for {}: allowed={}, remaining={}, algorithm={}", 
            key, result.allowed, result.remainingRequests, config.algorithm);
        
        return result;
    }

    /**
     * Token bucket algorithm implementation
     */
    private RateLimitResult executeTokenBucketRateLimit(String key, RateLimitConfig config) {
        String script = """
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local refill_rate = tonumber(ARGV[2])
            local window_seconds = tonumber(ARGV[3])
            local current_time = tonumber(ARGV[4])
            
            local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')
            local tokens = tonumber(bucket[1]) or capacity
            local last_refill = tonumber(bucket[2]) or current_time
            
            local time_passed = current_time - last_refill
            local new_tokens = math.min(capacity, tokens + (time_passed * refill_rate))
            
            if new_tokens >= 1 then
                new_tokens = new_tokens - 1
                redis.call('HMSET', key, 'tokens', new_tokens, 'last_refill', current_time)
                redis.call('EXPIRE', key, window_seconds)
                return {1, new_tokens}
            else
                redis.call('HMSET', key, 'tokens', new_tokens, 'last_refill', current_time)
                redis.call('EXPIRE', key, window_seconds)
                return {0, 0}
            end
            """;

        double refillRate = (double) config.requestsPerWindow / config.windowSize.getSeconds();
        List<Object> result = redisTemplate.execute(
            RedisScript.of(script, List.class),
            Collections.singletonList(key),
            config.burstCapacity, refillRate, config.windowSize.getSeconds(), Instant.now().getEpochSecond()
        );

        boolean allowed = ((Long) result.get(0)) == 1;
        long remaining = ((Long) result.get(1));
        
        return new RateLimitResult(allowed, remaining, 
            allowed ? Duration.ZERO : Duration.ofSeconds(1), 
            allowed ? "Request allowed" : "Token bucket depleted");
    }

    /**
     * Sliding window log algorithm implementation
     */
    private RateLimitResult executeSlidingWindowLogRateLimit(String key, RateLimitConfig config) {
        String script = """
            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local window_seconds = tonumber(ARGV[2])
            local current_time = tonumber(ARGV[3])
            
            local cutoff_time = current_time - window_seconds
            
            redis.call('ZREMRANGEBYSCORE', key, 0, cutoff_time)
            local current_count = redis.call('ZCARD', key)
            
            if current_count < limit then
                redis.call('ZADD', key, current_time, current_time .. ':' .. math.random())
                redis.call('EXPIRE', key, window_seconds)
                return {1, limit - current_count - 1}
            else
                return {0, 0}
            end
            """;

        List<Object> result = redisTemplate.execute(
            RedisScript.of(script, List.class),
            Collections.singletonList(key),
            (int) (config.requestsPerWindow * config.adaptiveFactor),
            config.windowSize.getSeconds(),
            Instant.now().getEpochSecond()
        );

        boolean allowed = ((Long) result.get(0)) == 1;
        long remaining = ((Long) result.get(1));
        
        return new RateLimitResult(allowed, remaining,
            allowed ? Duration.ZERO : config.windowSize,
            allowed ? "Request allowed" : "Sliding window limit exceeded");
    }

    /**
     * Sliding window counter algorithm implementation
     */
    private RateLimitResult executeSlidingWindowCounterRateLimit(String key, RateLimitConfig config) {
        String script = """
            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local window_seconds = tonumber(ARGV[2])
            local current_time = tonumber(ARGV[3])
            
            local current_window = math.floor(current_time / window_seconds)
            local previous_window = current_window - 1
            
            local current_key = key .. ':' .. current_window
            local previous_key = key .. ':' .. previous_window
            
            local current_count = tonumber(redis.call('GET', current_key)) or 0
            local previous_count = tonumber(redis.call('GET', previous_key)) or 0
            
            local time_in_current = current_time % window_seconds
            local weight = time_in_current / window_seconds
            local estimated_count = previous_count * (1 - weight) + current_count
            
            if estimated_count < limit then
                redis.call('INCR', current_key)
                redis.call('EXPIRE', current_key, window_seconds * 2)
                return {1, limit - estimated_count - 1}
            else
                return {0, 0}
            end
            """;

        List<Object> result = redisTemplate.execute(
            RedisScript.of(script, List.class),
            Collections.singletonList(key),
            (int) (config.requestsPerWindow * config.adaptiveFactor),
            config.windowSize.getSeconds(),
            Instant.now().getEpochSecond()
        );

        boolean allowed = ((Long) result.get(0)) == 1;
        long remaining = Math.max(0, ((Double) result.get(1)).longValue());
        
        return new RateLimitResult(allowed, remaining,
            allowed ? Duration.ZERO : Duration.ofSeconds((long) (config.windowSize.getSeconds() * 0.1)),
            allowed ? "Request allowed" : "Sliding window counter limit exceeded");
    }

    /**
     * Fixed window algorithm implementation
     */
    private RateLimitResult executeFixedWindowRateLimit(String key, RateLimitConfig config) {
        long windowStart = Instant.now().getEpochSecond() / config.windowSize.getSeconds();
        String windowKey = key + ":" + windowStart;
        
        Long currentCount = redisTemplate.opsForValue().increment(windowKey);
        redisTemplate.expire(windowKey, config.windowSize);
        
        int adjustedLimit = (int) (config.requestsPerWindow * config.adaptiveFactor);
        boolean allowed = currentCount <= adjustedLimit;
        long remaining = Math.max(0, adjustedLimit - currentCount);
        
        return new RateLimitResult(allowed, remaining,
            allowed ? Duration.ZERO : config.windowSize,
            allowed ? "Request allowed" : "Fixed window limit exceeded");
    }

    /**
     * Leaky bucket algorithm implementation
     */
    private RateLimitResult executeLeakyBucketRateLimit(String key, RateLimitConfig config) {
        String script = """
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local leak_rate = tonumber(ARGV[2])
            local window_seconds = tonumber(ARGV[3])
            local current_time = tonumber(ARGV[4])
            
            local bucket = redis.call('HMGET', key, 'level', 'last_leak')
            local level = tonumber(bucket[1]) or 0
            local last_leak = tonumber(bucket[2]) or current_time
            
            local time_passed = current_time - last_leak
            local leaked = time_passed * leak_rate
            local new_level = math.max(0, level - leaked)
            
            if new_level < capacity then
                new_level = new_level + 1
                redis.call('HMSET', key, 'level', new_level, 'last_leak', current_time)
                redis.call('EXPIRE', key, window_seconds)
                return {1, capacity - new_level}
            else
                redis.call('HMSET', key, 'level', new_level, 'last_leak', current_time)
                redis.call('EXPIRE', key, window_seconds)
                return {0, 0}
            end
            """;

        double leakRate = (double) config.requestsPerWindow / config.windowSize.getSeconds();
        List<Object> result = redisTemplate.execute(
            RedisScript.of(script, List.class),
            Collections.singletonList(key),
            config.burstCapacity, leakRate, config.windowSize.getSeconds(), Instant.now().getEpochSecond()
        );

        boolean allowed = ((Long) result.get(0)) == 1;
        long remaining = ((Long) result.get(1));
        
        return new RateLimitResult(allowed, remaining,
            allowed ? Duration.ZERO : Duration.ofSeconds(1),
            allowed ? "Request allowed" : "Leaky bucket overflow");
    }

    /**
     * Apply adaptive adjustments based on historical data and AI analysis
     */
    private void applyAdaptiveAdjustments(String operation, RateLimitConfig config) {
        AdaptiveThreshold threshold = adaptiveThresholds.computeIfAbsent(operation, 
            k -> new AdaptiveThreshold());
        
        // Check if enough time has passed for adjustment
        if (Duration.between(threshold.lastAdjustment, Instant.now()).toMinutes() < 5) {
            return;
        }

        // Calculate violation rate
        double violationRate = threshold.totalRequests > 0 ? 
            (double) threshold.totalViolations / threshold.totalRequests : 0.0;

        // AI-powered adaptive adjustment
        if (violationRate > 0.1 && threshold.averageResponseTime > 1000) {
            // High violation rate and slow responses - tighten limits
            config.adaptiveFactor = Math.max(0.5, config.adaptiveFactor * 0.9);
            log.info("Tightening rate limits for {} due to high violation rate: {}", operation, violationRate);
        } else if (violationRate < 0.01 && threshold.averageResponseTime < 500) {
            // Low violation rate and fast responses - relax limits
            config.adaptiveFactor = Math.min(2.0, config.adaptiveFactor * 1.1);
            log.info("Relaxing rate limits for {} due to low violation rate: {}", operation, violationRate);
        }

        threshold.lastAdjustment = Instant.now();
    }

    /**
     * Track metrics for adaptive learning
     */
    private void trackRateLimitMetrics(String operation, RateLimitResult result) {
        AdaptiveThreshold threshold = adaptiveThresholds.computeIfAbsent(operation, 
            k -> new AdaptiveThreshold());
        
        threshold.totalRequests++;
        if (!result.allowed) {
            threshold.totalViolations++;
            threshold.consecutiveViolations++;
        } else {
            threshold.consecutiveViolations = 0;
        }
    }

    /**
     * Determine if AI analysis should be triggered
     */
    private boolean shouldTriggerAIAnalysis(String operation) {
        AdaptiveThreshold threshold = adaptiveThresholds.get(operation);
        return threshold != null && threshold.consecutiveViolations >= 5;
    }

    /**
     * AI-powered rate limit pattern analysis
     */
    private void analyzeRateLimitPattern(String operation, String userId, String clientId, RateLimitResult result) {
        try {
            String analysisPrompt = String.format(
                "Analyze this banking API rate limit violation pattern: " +
                "Operation=%s, UserId=%s, ClientId=%s, ConsecutiveViolations=%d, " +
                "ViolationRate=%.2f%%. Recommend: 1) Immediate actions, " +
                "2) Rate limit adjustments, 3) User communication strategy, 4) Security concerns.",
                operation, userId, clientId,
                adaptiveThresholds.get(operation).consecutiveViolations,
                adaptiveThresholds.get(operation).totalViolations * 100.0 / adaptiveThresholds.get(operation).totalRequests
            );

            String analysis = chatClient.prompt()
                .user(analysisPrompt)
                .call()
                .content();

            log.info("AI rate limit analysis for {}: {}", operation, analysis);
            
            // Apply AI recommendations
            applyAIRecommendations(operation, analysis);
            
        } catch (Exception e) {
            log.error("Failed to perform AI rate limit analysis for operation: {}", operation, e);
        }
    }

    /**
     * Apply AI recommendations for rate limiting
     */
    private void applyAIRecommendations(String operation, String analysis) {
        RateLimitConfig config = rateLimitConfigs.get(operation);
        if (config == null) return;

        // Parse AI recommendations and apply them
        if (analysis.toLowerCase().contains("increase limits") || analysis.toLowerCase().contains("too strict")) {
            config.adaptiveFactor = Math.min(2.0, config.adaptiveFactor * 1.2);
            log.info("AI recommended increasing limits for {}, new factor: {}", operation, config.adaptiveFactor);
        } else if (analysis.toLowerCase().contains("decrease limits") || analysis.toLowerCase().contains("suspicious")) {
            config.adaptiveFactor = Math.max(0.3, config.adaptiveFactor * 0.8);
            log.info("AI recommended decreasing limits for {}, new factor: {}", operation, config.adaptiveFactor);
        }

        // Check for security concerns
        if (analysis.toLowerCase().contains("security") || analysis.toLowerCase().contains("attack")) {
            // Enable stricter monitoring
            config.adaptiveEnabled = true;
            config.adaptiveFactor = Math.min(config.adaptiveFactor, 0.5);
            log.warn("AI detected potential security threat for {}, enabling strict mode", operation);
        }
    }

    /**
     * Build rate limit key
     */
    private String buildRateLimitKey(String operation, String userId, String clientId) {
        return String.format("rate_limit:%s:%s:%s", operation, userId != null ? userId : "anonymous", 
                           clientId != null ? clientId : "unknown");
    }

    /**
     * Get rate limit status for monitoring
     */
    public Map<String, Object> getRateLimitStatus() {
        Map<String, Object> status = new HashMap<>();
        
        rateLimitConfigs.forEach((operation, config) -> {
            Map<String, Object> operationStatus = new HashMap<>();
            operationStatus.put("requestsPerWindow", config.requestsPerWindow);
            operationStatus.put("windowSize", config.windowSize.toString());
            operationStatus.put("algorithm", config.algorithm.toString());
            operationStatus.put("adaptiveFactor", config.adaptiveFactor);
            
            AdaptiveThreshold threshold = adaptiveThresholds.get(operation);
            if (threshold != null) {
                operationStatus.put("totalRequests", threshold.totalRequests);
                operationStatus.put("totalViolations", threshold.totalViolations);
                operationStatus.put("violationRate", threshold.totalRequests > 0 ? 
                    threshold.totalViolations * 100.0 / threshold.totalRequests : 0.0);
                operationStatus.put("consecutiveViolations", threshold.consecutiveViolations);
            }
            
            status.put(operation, operationStatus);
        });
        
        return status;
    }

    /**
     * Update rate limit configuration dynamically
     */
    public void updateRateLimitConfig(String operation, int requestsPerWindow, Duration windowSize, 
                                    RateLimitAlgorithm algorithm, boolean adaptiveEnabled) {
        RateLimitConfig config = rateLimitConfigs.get(operation);
        if (config != null) {
            config.requestsPerWindow = requestsPerWindow;
            config.windowSize = windowSize;
            config.algorithm = algorithm;
            config.adaptiveEnabled = adaptiveEnabled;
            
            log.info("Updated rate limit configuration for {}: requests={}, window={}, algorithm={}", 
                operation, requestsPerWindow, windowSize, algorithm);
        } else {
            rateLimitConfigs.put(operation, new RateLimitConfig(operation, requestsPerWindow, windowSize, algorithm, adaptiveEnabled));
            log.info("Created new rate limit configuration for {}", operation);
        }
    }
}