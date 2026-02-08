package com.amanahfi.platform.security.port.out;

import java.time.Instant;

/**
 * Port for DPoP nonce storage and replay protection
 */
public interface DPoPNonceStore {
    
    /**
     * Generate a new DPoP nonce
     */
    String generateNonce();
    
    /**
     * Check if nonce is valid and not reused
     */
    boolean isValidNonce(String nonce);
    
    /**
     * Mark nonce as used
     */
    void markNonceAsUsed(String nonce);
    
    /**
     * Check if token ID has been used (replay protection)
     */
    boolean isTokenIdUsed(String tokenId);
    
    /**
     * Store token ID with expiration
     */
    void storeTokenId(String tokenId, Instant expiresAt);
    
    /**
     * Clean up expired nonces and token IDs
     */
    void cleanupExpired();
    
    /**
     * Invalidate all nonces for a user (on logout)
     */
    void invalidateNoncesForUser(String userId);
    
    /**
     * Get nonce statistics
     */
    NonceStatistics getStatistics();
    
    /**
     * Nonce statistics
     */
    record NonceStatistics(
        long totalNonces,
        long activeNonces,
        long usedNonces,
        long expiredNonces,
        long replayAttempts
    ) {}
}