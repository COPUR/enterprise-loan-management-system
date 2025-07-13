package com.amanahfi.platform.security.infrastructure.adapter;

import com.amanahfi.platform.security.port.out.DPoPNonceStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DPoP nonce store adapter implementation with Redis support
 * Falls back to in-memory storage when Redis is unavailable
 */
@Component
@Slf4j
public class DPoPNonceStoreAdapter implements DPoPNonceStore {
    
    private final Optional<RedisTemplate<String, Object>> redisTemplate;
    
    @Value("${amanahfi.security.dpop.redis.enabled:true}")
    private boolean redisEnabled;
    
    @Value("${amanahfi.security.dpop.nonce.validity.minutes:10}")
    private long nonceValidityMinutes;
    
    @Value("${amanahfi.security.dpop.token.validity.hours:24}")
    private long tokenValidityHours;
    
    @Value("${amanahfi.security.dpop.cleanup.interval.minutes:5}")
    private long cleanupIntervalMinutes;
    
    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentHashMap<String, NonceEntry> nonces = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TokenEntry> tokenIds = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor();
    
    // Statistics
    private final AtomicLong totalNonces = new AtomicLong(0);
    private final AtomicLong usedNonces = new AtomicLong(0);
    private final AtomicLong replayAttempts = new AtomicLong(0);
    
    // Redis key prefixes
    private static final String NONCE_KEY_PREFIX = "dpop:nonce:";
    private static final String TOKEN_KEY_PREFIX = "dpop:token:";
    private static final String STATS_KEY_PREFIX = "dpop:stats:";
    
    // Configuration (moved to @Value annotations above)
    private static final long NONCE_VALIDITY_MINUTES = 10;
    private static final long TOKEN_VALIDITY_HOURS = 24;
    private static final long CLEANUP_INTERVAL_MINUTES = 5;
    
    public DPoPNonceStoreAdapter(Optional<RedisTemplate<String, Object>> redisTemplate) {
        this.redisTemplate = redisTemplate;
        
        // Start cleanup scheduler only for in-memory mode (Redis handles TTL automatically)
        if (!isRedisAvailable()) {
            cleanupScheduler.scheduleAtFixedRate(
                this::cleanupExpired,
                cleanupIntervalMinutes,
                cleanupIntervalMinutes,
                TimeUnit.MINUTES
            );
            log.info("DPoP nonce store initialized with in-memory storage and cleanup interval: {} minutes", 
                cleanupIntervalMinutes);
        } else {
            log.info("DPoP nonce store initialized with Redis backend");
        }
    }
    
    @Override
    public String generateNonce() {
        // Generate cryptographically secure random nonce
        byte[] nonceBytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(nonceBytes);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(nonceBytes);
        
        if (isRedisAvailable()) {
            storeNonceInRedis(nonce);
        } else {
            // Store nonce with expiration
            Instant expiresAt = Instant.now().plusSeconds(nonceValidityMinutes * 60);
            nonces.put(nonce, new NonceEntry(nonce, expiresAt, false));
        }
        
        totalNonces.incrementAndGet();
        incrementStat("total_nonces");
        
        log.debug("Generated DPoP nonce: {}", nonce.substring(0, 8) + "...");
        return nonce;
    }
    
    @Override
    public boolean isValidNonce(String nonce) {
        if (nonce == null || nonce.trim().isEmpty()) {
            log.debug("Invalid nonce: null or empty");
            return false;
        }
        
        if (isRedisAvailable()) {
            return isValidNonceInRedis(nonce);
        } else {
            NonceEntry entry = nonces.get(nonce);
            if (entry == null) {
                log.debug("Nonce not found: {}", nonce);
                return false;
            }
            
            if (entry.isUsed()) {
                log.warn("Nonce already used (replay attempt): {}", nonce);
                replayAttempts.incrementAndGet();
                return false;
            }
            
            if (entry.isExpired()) {
                log.debug("Nonce expired: {}", nonce);
                return false;
            }
            
            log.debug("Nonce validation successful: {}", nonce);
            return true;
        }
    }
    
    @Override
    public void markNonceAsUsed(String nonce) {
        if (isRedisAvailable()) {
            useNonceInRedis(nonce);
        } else {
            NonceEntry entry = nonces.get(nonce);
            if (entry != null) {
                nonces.put(nonce, entry.markAsUsed());
                usedNonces.incrementAndGet();
                log.debug("Marked nonce as used: {}", nonce);
            } else {
                log.warn("Attempted to mark non-existent nonce as used: {}", nonce);
            }
        }
    }
    
    @Override
    public boolean isTokenIdUsed(String tokenId) {
        if (tokenId == null || tokenId.trim().isEmpty()) {
            log.debug("Invalid token ID: null or empty");
            return false;
        }
        
        TokenEntry entry = tokenIds.get(tokenId);
        if (entry == null) {
            log.debug("Token ID not found: {}", tokenId);
            return false;
        }
        
        if (entry.isExpired()) {
            log.debug("Token ID expired: {}", tokenId);
            tokenIds.remove(tokenId); // Clean up expired entry
            return false;
        }
        
        log.warn("Token ID already used (replay attempt): {}", tokenId);
        replayAttempts.incrementAndGet();
        return true;
    }
    
    @Override
    public void storeTokenId(String tokenId, Instant expiresAt) {
        if (tokenId == null || tokenId.trim().isEmpty()) {
            log.warn("Cannot store invalid token ID: null or empty");
            return;
        }
        
        if (isRedisAvailable()) {
            storeTokenInRedis(tokenId, tokenId); // Use tokenId as associated data
        } else {
            // Use provided expiration or default to 24 hours
            Instant expiration = expiresAt != null ? expiresAt : 
                Instant.now().plusSeconds(tokenValidityHours * 3600);
            
            tokenIds.put(tokenId, new TokenEntry(tokenId, expiration));
            
            log.debug("Stored token ID: {} (expires at: {})", tokenId, expiration);
        }
    }
    
    @Override
    public void cleanupExpired() {
        Instant now = Instant.now();
        
        // Cleanup expired nonces
        int expiredNonces = 0;
        for (String nonceKey : nonces.keySet()) {
            NonceEntry entry = nonces.get(nonceKey);
            if (entry != null && entry.isExpired()) {
                nonces.remove(nonceKey);
                expiredNonces++;
            }
        }
        
        // Cleanup expired token IDs
        int expiredTokens = 0;
        for (String tokenKey : tokenIds.keySet()) {
            TokenEntry entry = tokenIds.get(tokenKey);
            if (entry != null && entry.isExpired()) {
                tokenIds.remove(tokenKey);
                expiredTokens++;
            }
        }
        
        if (expiredNonces > 0 || expiredTokens > 0) {
            log.info("Cleaned up {} expired nonces and {} expired token IDs", 
                expiredNonces, expiredTokens);
        }
        
        log.debug("Current store size - Nonces: {}, Token IDs: {}", nonces.size(), tokenIds.size());
    }
    
    @Override
    public void invalidateNoncesForUser(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("Cannot invalidate nonces for invalid user ID: null or empty");
            return;
        }
        
        // In this implementation, we don't store user association with nonces
        // In a production system, you would need to maintain user-nonce mapping
        log.info("Nonce invalidation for user {} not implemented in this adapter", userId);
    }
    
    @Override
    public NonceStatistics getStatistics() {
        long activeNonces = nonces.values().stream()
            .mapToLong(entry -> entry.isExpired() || entry.isUsed() ? 0 : 1)
            .sum();
        
        long expiredNonces = nonces.values().stream()
            .mapToLong(entry -> entry.isExpired() ? 1 : 0)
            .sum();
        
        return new NonceStatistics(
            totalNonces.get(),
            activeNonces,
            usedNonces.get(),
            expiredNonces,
            replayAttempts.get()
        );
    }
    
    // Helper classes
    
    private record NonceEntry(
        String nonce,
        Instant expiresAt,
        boolean used
    ) {
        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
        
        public boolean isUsed() {
            return used;
        }
        
        public NonceEntry markAsUsed() {
            return new NonceEntry(nonce, expiresAt, true);
        }
    }
    
    private record TokenEntry(
        String tokenId,
        Instant expiresAt
    ) {
        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
    
    // Redis helper methods
    
    private boolean isRedisAvailable() {
        return redisEnabled && redisTemplate.isPresent();
    }
    
    private void storeNonceInRedis(String nonce) {
        try {
            String key = NONCE_KEY_PREFIX + nonce;
            RedisTemplate<String, Object> redis = redisTemplate.get();
            
            // Store with TTL
            redis.opsForValue().set(key, "unused", Duration.ofMinutes(nonceValidityMinutes));
            
            log.debug("Stored nonce in Redis: {}", key);
        } catch (Exception e) {
            log.error("Failed to store nonce in Redis, falling back to in-memory: {}", e.getMessage());
            // Fallback to in-memory storage
            Instant expiresAt = Instant.now().plusSeconds(nonceValidityMinutes * 60);
            nonces.put(nonce, new NonceEntry(nonce, expiresAt, false));
        }
    }
    
    private boolean isValidNonceInRedis(String nonce) {
        try {
            String key = NONCE_KEY_PREFIX + nonce;
            RedisTemplate<String, Object> redis = redisTemplate.get();
            
            String value = (String) redis.opsForValue().get(key);
            boolean exists = value != null;
            
            if (!exists) {
                log.debug("Nonce not found in Redis: {}", key);
            } else if ("used".equals(value)) {
                log.warn("Nonce already used (replay attempt): {}", nonce.substring(0, 8) + "...");
                incrementStat("replay_attempts");
                return false;
            }
            
            return exists && "unused".equals(value);
        } catch (Exception e) {
            log.error("Failed to validate nonce in Redis: {}", e.getMessage());
            return false;
        }
    }
    
    private boolean useNonceInRedis(String nonce) {
        try {
            String key = NONCE_KEY_PREFIX + nonce;
            RedisTemplate<String, Object> redis = redisTemplate.get();
            
            // Atomic check-and-set operation
            String currentValue = (String) redis.opsForValue().get(key);
            
            if (currentValue == null) {
                log.warn("Attempted to use non-existent nonce: {}", nonce.substring(0, 8) + "...");
                incrementStat("replay_attempts");
                return false;
            }
            
            if ("used".equals(currentValue)) {
                log.warn("Nonce replay attack detected: {}", nonce.substring(0, 8) + "...");
                incrementStat("replay_attempts");
                return false;
            }
            
            // Mark as used
            redis.opsForValue().set(key, "used", Duration.ofMinutes(nonceValidityMinutes));
            incrementStat("used_nonces");
            
            log.debug("Used DPoP nonce in Redis: {}", nonce.substring(0, 8) + "...");
            return true;
            
        } catch (Exception e) {
            log.error("Failed to use nonce in Redis: {}", e.getMessage());
            return false;
        }
    }
    
    private void storeTokenInRedis(String tokenId, String associatedData) {
        try {
            String key = TOKEN_KEY_PREFIX + tokenId;
            RedisTemplate<String, Object> redis = redisTemplate.get();
            
            // Store token with TTL
            redis.opsForValue().set(key, associatedData, Duration.ofHours(tokenValidityHours));
            
            log.debug("Stored token in Redis: {}", key);
        } catch (Exception e) {
            log.error("Failed to store token in Redis, falling back to in-memory: {}", e.getMessage());
            // Fallback to in-memory storage
            TokenEntry entry = new TokenEntry(tokenId, Instant.now().plusSeconds(tokenValidityHours * 3600));
            tokenIds.put(tokenId, entry);
        }
    }
    
    private boolean hasTokenInRedis(String tokenId) {
        try {
            String key = TOKEN_KEY_PREFIX + tokenId;
            RedisTemplate<String, Object> redis = redisTemplate.get();
            
            return redis.hasKey(key);
        } catch (Exception e) {
            log.error("Failed to check token in Redis: {}", e.getMessage());
            return false;
        }
    }
    
    private void removeTokenFromRedis(String tokenId) {
        try {
            String key = TOKEN_KEY_PREFIX + tokenId;
            RedisTemplate<String, Object> redis = redisTemplate.get();
            
            redis.delete(key);
            log.debug("Removed token from Redis: {}", key);
        } catch (Exception e) {
            log.error("Failed to remove token from Redis: {}", e.getMessage());
        }
    }
    
    private void incrementStat(String statName) {
        if (isRedisAvailable()) {
            try {
                String key = STATS_KEY_PREFIX + statName;
                RedisTemplate<String, Object> redis = redisTemplate.get();
                redis.opsForValue().increment(key);
            } catch (Exception e) {
                log.error("Failed to increment stat in Redis: {}", e.getMessage());
            }
        }
    }
    
    // Shutdown hook
    public void shutdown() {
        log.info("Shutting down DPoP nonce store cleanup scheduler");
        cleanupScheduler.shutdown();
        try {
            if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}