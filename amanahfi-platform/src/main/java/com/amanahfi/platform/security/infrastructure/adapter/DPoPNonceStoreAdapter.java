package com.amanahfi.platform.security.infrastructure.adapter;

import com.amanahfi.platform.security.port.out.DPoPNonceStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DPoP nonce store adapter implementation using in-memory storage
 * In production, this should use Redis or another distributed cache
 */
@Component
@Slf4j
public class DPoPNonceStoreAdapter implements DPoPNonceStore {
    
    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentHashMap<String, NonceEntry> nonces = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TokenEntry> tokenIds = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor();
    
    // Statistics
    private final AtomicLong totalNonces = new AtomicLong(0);
    private final AtomicLong usedNonces = new AtomicLong(0);
    private final AtomicLong replayAttempts = new AtomicLong(0);
    
    // Configuration
    private static final long NONCE_VALIDITY_MINUTES = 10;
    private static final long TOKEN_VALIDITY_HOURS = 24;
    private static final long CLEANUP_INTERVAL_MINUTES = 5;
    
    public DPoPNonceStoreAdapter() {
        // Start cleanup scheduler
        cleanupScheduler.scheduleAtFixedRate(
            this::cleanupExpired,
            CLEANUP_INTERVAL_MINUTES,
            CLEANUP_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        );
        
        log.info("DPoP nonce store initialized with cleanup interval: {} minutes", CLEANUP_INTERVAL_MINUTES);
    }
    
    @Override
    public String generateNonce() {
        // Generate cryptographically secure random nonce
        byte[] nonceBytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(nonceBytes);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(nonceBytes);
        
        // Store nonce with expiration
        Instant expiresAt = Instant.now().plusSeconds(NONCE_VALIDITY_MINUTES * 60);
        nonces.put(nonce, new NonceEntry(nonce, expiresAt, false));
        
        totalNonces.incrementAndGet();
        
        log.debug("Generated DPoP nonce: {} (expires at: {})", nonce, expiresAt);
        return nonce;
    }
    
    @Override
    public boolean isValidNonce(String nonce) {
        if (nonce == null || nonce.trim().isEmpty()) {
            log.debug("Invalid nonce: null or empty");
            return false;
        }
        
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
    
    @Override
    public void markNonceAsUsed(String nonce) {
        NonceEntry entry = nonces.get(nonce);
        if (entry != null) {
            nonces.put(nonce, entry.markAsUsed());
            usedNonces.incrementAndGet();
            log.debug("Marked nonce as used: {}", nonce);
        } else {
            log.warn("Attempted to mark non-existent nonce as used: {}", nonce);
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
        
        // Use provided expiration or default to 24 hours
        Instant expiration = expiresAt != null ? expiresAt : 
            Instant.now().plusSeconds(TOKEN_VALIDITY_HOURS * 3600);
        
        tokenIds.put(tokenId, new TokenEntry(tokenId, expiration));
        
        log.debug("Stored token ID: {} (expires at: {})", tokenId, expiration);
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