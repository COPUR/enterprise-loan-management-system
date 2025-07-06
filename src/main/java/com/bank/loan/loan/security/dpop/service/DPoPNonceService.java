package com.bank.loan.loan.security.dpop.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Service
public class DPoPNonceService {
    
    private static final String NONCE_CACHE_PREFIX = "dpop:nonce:";
    private static final long NONCE_EXPIRATION_SECONDS = 300; // 5 minutes
    private static final int NONCE_LENGTH = 32; // 32 bytes = 256 bits
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final SecureRandom secureRandom;
    
    public DPoPNonceService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.secureRandom = new SecureRandom();
    }
    
    public String generateNonce() {
        byte[] nonceBytes = new byte[NONCE_LENGTH];
        secureRandom.nextBytes(nonceBytes);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(nonceBytes);
        
        // Store nonce in cache
        String cacheKey = NONCE_CACHE_PREFIX + nonce;
        redisTemplate.opsForValue().set(cacheKey, "valid", NONCE_EXPIRATION_SECONDS, TimeUnit.SECONDS);
        
        return nonce;
    }
    
    public boolean validateNonce(String nonce) {
        if (nonce == null || nonce.trim().isEmpty()) {
            return false;
        }
        
        String cacheKey = NONCE_CACHE_PREFIX + nonce;
        Boolean exists = redisTemplate.hasKey(cacheKey);
        
        if (Boolean.TRUE.equals(exists)) {
            // Remove nonce after validation (one-time use)
            redisTemplate.delete(cacheKey);
            return true;
        }
        
        return false;
    }
    
    public boolean isNonceRequired(String clientId) {
        // For now, nonces are optional but recommended
        // In a real implementation, this could be configured per client
        return false;
    }
    
    public boolean isNonceExpired(String nonce) {
        if (nonce == null || nonce.trim().isEmpty()) {
            return true;
        }
        
        String cacheKey = NONCE_CACHE_PREFIX + nonce;
        return !Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey));
    }
    
    public void invalidateNonce(String nonce) {
        if (nonce != null && !nonce.trim().isEmpty()) {
            String cacheKey = NONCE_CACHE_PREFIX + nonce;
            redisTemplate.delete(cacheKey);
        }
    }
    
    public void cleanupExpiredNonces() {
        // Redis TTL will automatically clean up expired nonces
        // This method can be used for additional cleanup if needed
    }
}