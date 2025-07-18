package com.bank.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DPoP (Demonstrating Proof-of-Possession) Validation Filter
 * 
 * Implements OAuth 2.1 DPoP specification for enhanced security:
 * - Validates DPoP proof JWT tokens
 * - Prevents token replay attacks with nonce validation
 * - Ensures HTTP method and URI binding
 * - Validates cryptographic binding between access token and DPoP proof
 * - Provides comprehensive audit logging for security events
 */
public class DPoPValidationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(DPoPValidationFilter.class);
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY");
    
    private final boolean enabled;
    private final ObjectMapper objectMapper;
    private final Map<String, Instant> nonceCache;
    private final long nonceValiditySeconds;
    private final long maxClockSkewSeconds;
    
    // DPoP header constants
    private static final String DPOP_HEADER = "DPoP";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String DPOP_NONCE_HEADER = "DPoP-Nonce";
    
    // DPoP JWT claims
    private static final String JTI_CLAIM = "jti";
    private static final String HTM_CLAIM = "htm";
    private static final String HTU_CLAIM = "htu";
    private static final String IAT_CLAIM = "iat";
    private static final String ATH_CLAIM = "ath";
    private static final String NONCE_CLAIM = "nonce";
    
    public DPoPValidationFilter(boolean enabled) {
        this.enabled = enabled;
        this.objectMapper = new ObjectMapper();
        this.nonceCache = new ConcurrentHashMap<>();
        this.nonceValiditySeconds = 300; // 5 minutes
        this.maxClockSkewSeconds = 60; // 1 minute
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Skip DPoP validation for non-API requests
        if (!request.getRequestURI().startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            // Validate DPoP proof if present
            String dpopHeader = request.getHeader(DPOP_HEADER);
            String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
            
            if (dpopHeader != null && authorizationHeader != null) {
                validateDPoPProof(request, dpopHeader, authorizationHeader);
            }
            
            filterChain.doFilter(request, response);
            
        } catch (DPoPValidationException e) {
            handleDPoPValidationError(request, response, e);
        } catch (Exception e) {
            logger.error("Unexpected error during DPoP validation", e);
            handleDPoPValidationError(request, response, 
                new DPoPValidationException("DPoP validation failed", e));
        }
    }
    
    private void validateDPoPProof(HttpServletRequest request, String dpopHeader, 
                                  String authorizationHeader) throws DPoPValidationException {
        
        try {
            // Parse DPoP proof JWT
            SignedJWT dpopJWT = SignedJWT.parse(dpopHeader);
            JWTClaimsSet claims = dpopJWT.getJWTClaimsSet();
            
            // Validate JWT structure and algorithm
            validateJWTStructure(dpopJWT);
            
            // Validate required claims
            validateRequiredClaims(claims);
            
            // Validate HTTP method and URI binding
            validateHTTPBinding(request, claims);
            
            // Validate timestamp and prevent replay
            validateTimestamp(claims);
            
            // Validate nonce (if present)
            validateNonce(claims);
            
            // Validate access token hash binding
            validateAccessTokenBinding(authorizationHeader, claims);
            
            // Validate cryptographic signature
            validateSignature(dpopJWT);
            
            // Log successful validation
            logSecurityEvent(request, "DPoP validation successful", null);
            
        } catch (ParseException e) {
            throw new DPoPValidationException("Invalid DPoP proof format", e);
        } catch (Exception e) {
            throw new DPoPValidationException("DPoP validation failed", e);
        }
    }
    
    private void validateJWTStructure(SignedJWT dpopJWT) throws DPoPValidationException {
        try {
            // Validate JWT header
            var header = dpopJWT.getHeader();
            
            // Check algorithm (must be asymmetric)
            JWSAlgorithm algorithm = header.getAlgorithm();
            if (!isValidDPoPAlgorithm(algorithm)) {
                throw new DPoPValidationException("Invalid DPoP algorithm: " + algorithm);
            }
            
            // Check type
            if (!"dpop+jwt".equals(header.getType().getType())) {
                throw new DPoPValidationException("Invalid DPoP JWT type");
            }
            
            // Check JWK
            JWK jwk = header.getJWK();
            if (jwk == null) {
                throw new DPoPValidationException("DPoP JWT missing JWK");
            }
            
        } catch (ParseException e) {
            throw new DPoPValidationException("Failed to parse DPoP JWT header", e);
        }
    }
    
    private void validateRequiredClaims(JWTClaimsSet claims) throws DPoPValidationException {
        // Validate jti (JWT ID)
        String jti = claims.getJTIString();
        if (jti == null || jti.trim().isEmpty()) {
            throw new DPoPValidationException("Missing or empty jti claim");
        }
        
        // Validate htm (HTTP method)
        String htm = claims.getStringClaim(HTM_CLAIM);
        if (htm == null || htm.trim().isEmpty()) {
            throw new DPoPValidationException("Missing or empty htm claim");
        }
        
        // Validate htu (HTTP URI)
        String htu = claims.getStringClaim(HTU_CLAIM);
        if (htu == null || htu.trim().isEmpty()) {
            throw new DPoPValidationException("Missing or empty htu claim");
        }
        
        // Validate iat (issued at)
        Date iat = claims.getIssueTime();
        if (iat == null) {
            throw new DPoPValidationException("Missing iat claim");
        }
    }
    
    private void validateHTTPBinding(HttpServletRequest request, JWTClaimsSet claims) 
            throws DPoPValidationException {
        
        // Validate HTTP method
        String htm = claims.getStringClaim(HTM_CLAIM);
        if (!request.getMethod().equalsIgnoreCase(htm)) {
            throw new DPoPValidationException(
                String.format("HTTP method mismatch: expected %s, got %s", htm, request.getMethod()));
        }
        
        // Validate HTTP URI
        String htu = claims.getStringClaim(HTU_CLAIM);
        String requestUri = buildRequestURI(request);
        
        if (!requestUri.equals(htu)) {
            throw new DPoPValidationException(
                String.format("HTTP URI mismatch: expected %s, got %s", htu, requestUri));
        }
    }
    
    private void validateTimestamp(JWTClaimsSet claims) throws DPoPValidationException {
        Date iat = claims.getIssueTime();
        Instant now = Instant.now();
        Instant issuedAt = iat.toInstant();
        
        // Check if token is too old
        if (issuedAt.isBefore(now.minusSeconds(nonceValiditySeconds))) {
            throw new DPoPValidationException("DPoP proof too old");
        }
        
        // Check if token is from future (with clock skew tolerance)
        if (issuedAt.isAfter(now.plusSeconds(maxClockSkewSeconds))) {
            throw new DPoPValidationException("DPoP proof from future");
        }
        
        // Check for replay attack
        String jti = claims.getJTIString();
        if (nonceCache.containsKey(jti)) {
            throw new DPoPValidationException("DPoP proof replay detected");
        }
        
        // Store nonce to prevent replay
        nonceCache.put(jti, now.plusSeconds(nonceValiditySeconds));
        
        // Clean up expired nonces
        cleanupExpiredNonces();
    }
    
    private void validateNonce(JWTClaimsSet claims) throws DPoPValidationException {
        String nonce = claims.getStringClaim(NONCE_CLAIM);
        // Nonce validation logic would depend on your specific requirements
        // For now, we'll just validate format if present
        if (nonce != null && nonce.length() < 8) {
            throw new DPoPValidationException("Invalid nonce format");
        }
    }
    
    private void validateAccessTokenBinding(String authorizationHeader, JWTClaimsSet claims) 
            throws DPoPValidationException {
        
        try {
            // Extract access token from Authorization header
            String accessToken = extractAccessToken(authorizationHeader);
            if (accessToken == null) {
                throw new DPoPValidationException("Unable to extract access token");
            }
            
            // Calculate access token hash
            String calculatedHash = calculateAccessTokenHash(accessToken);
            
            // Get expected hash from DPoP proof
            String expectedHash = claims.getStringClaim(ATH_CLAIM);
            if (expectedHash == null) {
                throw new DPoPValidationException("Missing ath claim");
            }
            
            // Compare hashes
            if (!calculatedHash.equals(expectedHash)) {
                throw new DPoPValidationException("Access token hash mismatch");
            }
            
        } catch (NoSuchAlgorithmException e) {
            throw new DPoPValidationException("Failed to calculate access token hash", e);
        }
    }
    
    private void validateSignature(SignedJWT dpopJWT) throws DPoPValidationException {
        try {
            // Extract JWK from header
            JWK jwk = dpopJWT.getHeader().getJWK();
            
            // Verify signature using JWK
            boolean isValid = dpopJWT.verify(jwk.toRSAKey().toRSAPublicKey());
            
            if (!isValid) {
                throw new DPoPValidationException("Invalid DPoP proof signature");
            }
            
        } catch (JOSEException e) {
            throw new DPoPValidationException("Failed to verify DPoP proof signature", e);
        }
    }
    
    private String extractAccessToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }
    
    private String calculateAccessTokenHash(String accessToken) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(accessToken.getBytes());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes);
    }
    
    private String buildRequestURI(HttpServletRequest request) {
        StringBuilder uri = new StringBuilder();
        
        // Add scheme
        uri.append(request.getScheme()).append("://");
        
        // Add host
        uri.append(request.getServerName());
        
        // Add port if not default
        int port = request.getServerPort();
        if (port != 80 && port != 443) {
            uri.append(":").append(port);
        }
        
        // Add path
        uri.append(request.getRequestURI());
        
        // Note: Query parameters are typically not included in DPoP htu claim
        
        return uri.toString();
    }
    
    private boolean isValidDPoPAlgorithm(JWSAlgorithm algorithm) {
        // Allow common asymmetric algorithms
        return algorithm.equals(JWSAlgorithm.RS256) ||
               algorithm.equals(JWSAlgorithm.RS384) ||
               algorithm.equals(JWSAlgorithm.RS512) ||
               algorithm.equals(JWSAlgorithm.ES256) ||
               algorithm.equals(JWSAlgorithm.ES384) ||
               algorithm.equals(JWSAlgorithm.ES512) ||
               algorithm.equals(JWSAlgorithm.PS256) ||
               algorithm.equals(JWSAlgorithm.PS384) ||
               algorithm.equals(JWSAlgorithm.PS512);
    }
    
    private void cleanupExpiredNonces() {
        Instant now = Instant.now();
        nonceCache.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }
    
    private void handleDPoPValidationError(HttpServletRequest request, HttpServletResponse response, 
                                          DPoPValidationException e) throws IOException {
        
        // Log security event
        logSecurityEvent(request, "DPoP validation failed", e.getMessage());
        
        // Set response status and headers
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/problem+json");
        
        // Add DPoP-Nonce header for client retry
        response.setHeader(DPOP_NONCE_HEADER, generateDPoPNonce());
        
        // Create error response
        Map<String, Object> errorResponse = Map.of(
            "type", "https://banking.example.com/problems/dpop-validation-failed",
            "title", "DPoP Validation Failed",
            "status", HttpStatus.UNAUTHORIZED.value(),
            "detail", e.getMessage(),
            "instance", request.getRequestURI(),
            "timestamp", Instant.now().toString()
        );
        
        // Write error response
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
    
    private String generateDPoPNonce() {
        // Generate a secure random nonce
        byte[] nonceBytes = new byte[32];
        new java.security.SecureRandom().nextBytes(nonceBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(nonceBytes);
    }
    
    private void logSecurityEvent(HttpServletRequest request, String event, String details) {
        securityLogger.info("DPoP Security Event: {} | URI: {} | Method: {} | IP: {} | Details: {}",
            event,
            request.getRequestURI(),
            request.getMethod(),
            getClientIpAddress(request),
            details != null ? details : "None"
        );
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For", "X-Real-IP", "X-Originating-IP", "CF-Connecting-IP", "True-Client-IP"
        };
        
        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Custom exception for DPoP validation errors
     */
    public static class DPoPValidationException extends Exception {
        public DPoPValidationException(String message) {
            super(message);
        }
        
        public DPoPValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}