package com.amanahfi.platform.security.domain;

import lombok.Builder;
import lombok.Value;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

/**
 * DPoP (Demonstration of Proof-of-Possession) token
 * RFC 9449 compliant implementation
 */
@Value
@Builder
public class DPoPToken {
    
    /**
     * JWT header
     */
    Map<String, Object> header;
    
    /**
     * JWT payload
     */
    Map<String, Object> payload;
    
    /**
     * JWT signature
     */
    String signature;
    
    /**
     * Raw JWT token
     */
    String rawToken;
    
    /**
     * HTTP method (GET, POST, etc.)
     */
    String httpMethod;
    
    /**
     * HTTP URI
     */
    String httpUri;
    
    /**
     * JWK thumbprint of the public key
     */
    String jwkThumbprint;
    
    /**
     * Access token hash (ath claim)
     */
    String accessTokenHash;
    
    /**
     * Token issuance time
     */
    Instant issuedAt;
    
    /**
     * Token expiration time
     */
    Instant expiresAt;
    
    /**
     * Unique token identifier (jti claim)
     */
    String tokenId;
    
    /**
     * Nonce to prevent replay attacks
     */
    String nonce;
    
    /**
     * Check if DPoP token is valid
     */
    public boolean isValid() {
        return issuedAt != null &&
               expiresAt != null &&
               !Instant.now().isAfter(expiresAt) &&
               httpMethod != null &&
               httpUri != null &&
               jwkThumbprint != null &&
               tokenId != null;
    }
    
    /**
     * Check if DPoP token is expired
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
    
    /**
     * Check if DPoP token matches HTTP method
     */
    public boolean matchesHttpMethod(String method) {
        return httpMethod != null && httpMethod.equalsIgnoreCase(method);
    }
    
    /**
     * Check if DPoP token matches HTTP URI
     */
    public boolean matchesHttpUri(String uri) {
        return httpUri != null && httpUri.equals(uri);
    }
    
    /**
     * Check if DPoP token matches access token
     */
    public boolean matchesAccessToken(String accessToken) {
        if (accessTokenHash == null || accessToken == null) {
            return false;
        }
        
        // Calculate SHA-256 hash of access token
        String calculatedHash = calculateSHA256Hash(accessToken);
        return accessTokenHash.equals(calculatedHash);
    }
    
    /**
     * Get algorithm from header
     */
    public String getAlgorithm() {
        return header != null ? (String) header.get("alg") : null;
    }
    
    /**
     * Get key type from header
     */
    public String getKeyType() {
        return header != null ? (String) header.get("typ") : null;
    }
    
    /**
     * Get JWK from header
     */
    public Map<String, Object> getJWK() {
        return header != null ? (Map<String, Object>) header.get("jwk") : null;
    }
    
    /**
     * Validate DPoP token structure and claims
     */
    public void validate() {
        if (rawToken == null || rawToken.trim().isEmpty()) {
            throw new IllegalArgumentException("DPoP token cannot be null or empty");
        }
        
        if (header == null || header.isEmpty()) {
            throw new IllegalArgumentException("DPoP token header cannot be null or empty");
        }
        
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("DPoP token payload cannot be null or empty");
        }
        
        if (signature == null || signature.trim().isEmpty()) {
            throw new IllegalArgumentException("DPoP token signature cannot be null or empty");
        }
        
        if (httpMethod == null || httpMethod.trim().isEmpty()) {
            throw new IllegalArgumentException("HTTP method cannot be null or empty");
        }
        
        if (httpUri == null || httpUri.trim().isEmpty()) {
            throw new IllegalArgumentException("HTTP URI cannot be null or empty");
        }
        
        if (jwkThumbprint == null || jwkThumbprint.trim().isEmpty()) {
            throw new IllegalArgumentException("JWK thumbprint cannot be null or empty");
        }
        
        if (tokenId == null || tokenId.trim().isEmpty()) {
            throw new IllegalArgumentException("Token ID (jti) cannot be null or empty");
        }
        
        if (issuedAt == null) {
            throw new IllegalArgumentException("Issued at time cannot be null");
        }
        
        if (expiresAt == null) {
            throw new IllegalArgumentException("Expiration time cannot be null");
        }
        
        if (issuedAt.isAfter(expiresAt)) {
            throw new IllegalArgumentException("Issued at time cannot be after expiration time");
        }
        
        if (isExpired()) {
            throw new SecurityException("DPoP token is expired");
        }
        
        // Validate algorithm
        String algorithm = getAlgorithm();
        if (algorithm == null || (!algorithm.equals("RS256") && !algorithm.equals("ES256"))) {
            throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }
        
        // Validate token type
        String tokenType = getKeyType();
        if (tokenType == null || !tokenType.equals("dpop+jwt")) {
            throw new IllegalArgumentException("Invalid token type: " + tokenType);
        }
        
        // Validate JWK presence
        Map<String, Object> jwk = getJWK();
        if (jwk == null || jwk.isEmpty()) {
            throw new IllegalArgumentException("JWK must be present in DPoP token header");
        }
    }
    
    /**
     * Create DPoP token from raw JWT
     */
    public static DPoPToken fromRawToken(String rawToken, String httpMethod, String httpUri) {
        // Parse JWT token
        String[] parts = rawToken.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format");
        }
        
        // Decode header and payload
        Map<String, Object> header = decodeJWTComponent(parts[0]);
        Map<String, Object> payload = decodeJWTComponent(parts[1]);
        String signature = parts[2];
        
        // Extract claims
        String jti = (String) payload.get("jti");
        String htm = (String) payload.get("htm");
        String htu = (String) payload.get("htu");
        String jkt = (String) payload.get("jkt");
        String ath = (String) payload.get("ath");
        Long iat = (Long) payload.get("iat");
        Long exp = (Long) payload.get("exp");
        String nonce = (String) payload.get("nonce");
        
        return DPoPToken.builder()
            .header(header)
            .payload(payload)
            .signature(signature)
            .rawToken(rawToken)
            .httpMethod(htm != null ? htm : httpMethod)
            .httpUri(htu != null ? htu : httpUri)
            .jwkThumbprint(jkt)
            .accessTokenHash(ath)
            .issuedAt(iat != null ? Instant.ofEpochSecond(iat) : null)
            .expiresAt(exp != null ? Instant.ofEpochSecond(exp) : null)
            .tokenId(jti)
            .nonce(nonce)
            .build();
    }
    
    // Helper methods
    
    private static Map<String, Object> decodeJWTComponent(String component) {
        try {
            // Base64 decode
            byte[] decodedBytes = Base64.getUrlDecoder().decode(component);
            String json = new String(decodedBytes, StandardCharsets.UTF_8);
            
            // Parse JSON to Map
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            
        } catch (Exception e) {
            throw new SecurityException("Failed to decode JWT component: " + e.getMessage(), e);
        }
    }
    
    private String calculateSHA256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            throw new SecurityException("SHA-256 algorithm not available", e);
        }
    }
}