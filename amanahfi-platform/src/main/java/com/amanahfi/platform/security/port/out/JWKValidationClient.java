package com.amanahfi.platform.security.port.out;

import java.util.Map;

/**
 * Port for JWK validation and signature verification
 */
public interface JWKValidationClient {
    
    /**
     * Validate JWK structure and parameters
     */
    void validateJWK(Map<String, Object> jwk);
    
    /**
     * Verify JWT signature using JWK
     */
    boolean verifySignature(String jwt, Map<String, Object> jwk);
    
    /**
     * Calculate JWK thumbprint according to RFC 7638
     */
    String calculateJWKThumbprint(Map<String, Object> jwk);
    
    /**
     * Extract public key from JWK
     */
    Object extractPublicKey(Map<String, Object> jwk);
    
    /**
     * Validate JWK algorithm support
     */
    boolean isAlgorithmSupported(String algorithm);
    
    /**
     * Get supported algorithms
     */
    java.util.Set<String> getSupportedAlgorithms();
}