package com.amanahfi.platform.security.application;

import com.amanahfi.platform.security.domain.DPoPToken;
import com.amanahfi.platform.security.port.out.DPoPNonceStore;
import com.amanahfi.platform.security.port.out.JWKValidationClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for validating DPoP (Demonstration of Proof-of-Possession) tokens
 * RFC 9449 compliant implementation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DPoPValidationService {
    
    private final JWKValidationClient jwkValidationClient;
    private final DPoPNonceStore nonceStore;
    
    /**
     * Validate DPoP token
     */
    public void validateDPoPToken(DPoPToken dpopToken, String accessToken, String httpMethod, String httpUri) {
        log.debug("Validating DPoP token: {}", dpopToken.getTokenId());
        
        try {
            // Basic token validation
            dpopToken.validate();
            
            // Validate HTTP method and URI binding
            validateHttpBinding(dpopToken, httpMethod, httpUri);
            
            // Validate access token binding
            validateAccessTokenBinding(dpopToken, accessToken);
            
            // Validate JWK and signature
            validateJWKAndSignature(dpopToken);
            
            // Validate nonce (replay protection)
            validateNonce(dpopToken);
            
            // Check token uniqueness (prevent replay)
            validateTokenUniqueness(dpopToken);
            
            log.debug("DPoP token validation successful: {}", dpopToken.getTokenId());
            
        } catch (Exception e) {
            log.error("DPoP token validation failed: {} - Error: {}", 
                dpopToken.getTokenId(), e.getMessage());
            throw new DPoPValidationException("DPoP token validation failed", e);
        }
    }
    
    /**
     * Validate HTTP method and URI binding
     */
    private void validateHttpBinding(DPoPToken dpopToken, String httpMethod, String httpUri) {
        log.debug("Validating HTTP binding for DPoP token: {}", dpopToken.getTokenId());
        
        if (!dpopToken.matchesHttpMethod(httpMethod)) {
            throw new DPoPValidationException("DPoP token HTTP method mismatch. Expected: " + 
                httpMethod + ", Actual: " + dpopToken.getHttpMethod());
        }
        
        if (!dpopToken.matchesHttpUri(httpUri)) {
            throw new DPoPValidationException("DPoP token HTTP URI mismatch. Expected: " + 
                httpUri + ", Actual: " + dpopToken.getHttpUri());
        }
        
        log.debug("HTTP binding validation successful for DPoP token: {}", dpopToken.getTokenId());
    }
    
    /**
     * Validate access token binding
     */
    private void validateAccessTokenBinding(DPoPToken dpopToken, String accessToken) {
        log.debug("Validating access token binding for DPoP token: {}", dpopToken.getTokenId());
        
        if (accessToken != null && !dpopToken.matchesAccessToken(accessToken)) {
            throw new DPoPValidationException("DPoP token access token hash mismatch");
        }
        
        log.debug("Access token binding validation successful for DPoP token: {}", dpopToken.getTokenId());
    }
    
    /**
     * Validate JWK and signature
     */
    private void validateJWKAndSignature(DPoPToken dpopToken) {
        log.debug("Validating JWK and signature for DPoP token: {}", dpopToken.getTokenId());
        
        // Get JWK from token header
        Map<String, Object> jwk = dpopToken.getJWK();
        if (jwk == null) {
            throw new DPoPValidationException("JWK not found in DPoP token header");
        }
        
        // Validate JWK format and parameters
        jwkValidationClient.validateJWK(jwk);
        
        // Verify token signature using the JWK
        boolean signatureValid = jwkValidationClient.verifySignature(
            dpopToken.getRawToken(), jwk);
        
        if (!signatureValid) {
            throw new DPoPValidationException("DPoP token signature verification failed");
        }
        
        // Validate JWK thumbprint
        String calculatedThumbprint = jwkValidationClient.calculateJWKThumbprint(jwk);
        if (!calculatedThumbprint.equals(dpopToken.getJwkThumbprint())) {
            throw new DPoPValidationException("JWK thumbprint mismatch");
        }
        
        log.debug("JWK and signature validation successful for DPoP token: {}", dpopToken.getTokenId());
    }
    
    /**
     * Validate nonce for replay protection
     */
    private void validateNonce(DPoPToken dpopToken) {
        log.debug("Validating nonce for DPoP token: {}", dpopToken.getTokenId());
        
        String nonce = dpopToken.getNonce();
        if (nonce != null) {
            // Check if nonce is valid and not reused
            if (!nonceStore.isValidNonce(nonce)) {
                throw new DPoPValidationException("Invalid or reused nonce");
            }
            
            // Mark nonce as used
            nonceStore.markNonceAsUsed(nonce);
        }
        
        log.debug("Nonce validation successful for DPoP token: {}", dpopToken.getTokenId());
    }
    
    /**
     * Validate token uniqueness to prevent replay attacks
     */
    private void validateTokenUniqueness(DPoPToken dpopToken) {
        log.debug("Validating token uniqueness for DPoP token: {}", dpopToken.getTokenId());
        
        String tokenId = dpopToken.getTokenId();
        
        // Check if token ID has been used before
        if (nonceStore.isTokenIdUsed(tokenId)) {
            throw new DPoPValidationException("DPoP token replay detected: " + tokenId);
        }
        
        // Store token ID to prevent future replay
        nonceStore.storeTokenId(tokenId, dpopToken.getExpiresAt());
        
        log.debug("Token uniqueness validation successful for DPoP token: {}", dpopToken.getTokenId());
    }
    
    /**
     * Generate DPoP nonce for challenge-response
     */
    public String generateDPoPNonce() {
        String nonce = nonceStore.generateNonce();
        log.debug("Generated DPoP nonce: {}", nonce);
        return nonce;
    }
    
    /**
     * Check if DPoP is required for the given request
     */
    public boolean isDPoPRequired(String httpMethod, String httpUri, String clientId) {
        // Define DPoP requirements based on:
        // - Sensitive endpoints
        // - Client registration settings
        // - Security policies
        
        boolean required = isHighSecurityEndpoint(httpUri) || 
                          isClientRequiresDPoP(clientId) ||
                          isPaymentOrTransferEndpoint(httpUri);
        
        log.debug("DPoP required for {} {}: {}", httpMethod, httpUri, required);
        return required;
    }
    
    /**
     * Create DPoP error response
     */
    public DPoPErrorResponse createErrorResponse(String error, String description, String nonce) {
        return DPoPErrorResponse.builder()
            .error(error)
            .errorDescription(description)
            .nonce(nonce)
            .build();
    }
    
    // Helper methods
    
    private boolean isHighSecurityEndpoint(String httpUri) {
        return httpUri.contains("/admin/") ||
               httpUri.contains("/compliance/") ||
               httpUri.contains("/regulatory/") ||
               httpUri.contains("/cbdc/");
    }
    
    private boolean isClientRequiresDPoP(String clientId) {
        // Check client registration to see if DPoP is required
        // This would typically involve looking up client configuration
        return false; // Simplified for now
    }
    
    private boolean isPaymentOrTransferEndpoint(String httpUri) {
        return httpUri.contains("/payments/") ||
               httpUri.contains("/transfers/") ||
               httpUri.contains("/islamic-finance/");
    }
    
    /**
     * DPoP error response
     */
    @lombok.Value
    @lombok.Builder
    public static class DPoPErrorResponse {
        String error;
        String errorDescription;
        String nonce;
    }
    
    /**
     * Exception for DPoP validation failures
     */
    public static class DPoPValidationException extends RuntimeException {
        public DPoPValidationException(String message) {
            super(message);
        }
        
        public DPoPValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}