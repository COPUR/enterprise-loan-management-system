package com.bank.infrastructure.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * DPoP Validator - TDD Implementation (REFACTORED)
 * 
 * REFACTOR Phase: Optimized implementation with production-ready features
 * 
 * Implements OAuth 2.1 DPoP validation according to RFC:
 * - Validates JWT structure and claims
 * - Prevents replay attacks with Redis cache
 * - Validates HTTP method/URI binding
 * - Validates access token hash
 * - Validates cryptographic signatures
 * - Comprehensive logging and monitoring
 * - Configurable validation parameters
 */
@Component
public class DPoPValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(DPoPValidator.class);
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY");
    
    // Redis cache for replay prevention (production-ready)
    private final RedisTemplate<String, String> redisTemplate;
    private final Set<String> fallbackNonceCache = ConcurrentHashMap.newKeySet();
    
    // Configuration constants (externalized)
    @Value("${banking.security.dpop.max-age-seconds:300}")
    private long maxAgeSeconds;
    
    @Value("${banking.security.dpop.max-clock-skew-seconds:60}")
    private long maxClockSkewSeconds;
    
    @Value("${banking.security.dpop.redis-enabled:true}")
    private boolean redisEnabled;
    
    private static final String DPOP_NONCE_PREFIX = "dpop:nonce:";
    private static final String HTM_CLAIM = "htm";
    private static final String HTU_CLAIM = "htu";
    private static final String ATH_CLAIM = "ath";
    
    @Autowired
    public DPoPValidator(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    // Fallback constructor for testing
    public DPoPValidator() {
        this.redisTemplate = null;
        this.maxAgeSeconds = 300;
        this.maxClockSkewSeconds = 60;
        this.redisEnabled = false;
    }
    
    /**
     * Validate DPoP proof
     * 
     * @param request HTTP request
     * @param dpopProof DPoP proof JWT
     * @param accessToken Bearer access token
     * @throws DPoPValidationException if validation fails
     */
    public void validate(HttpServletRequest request, String dpopProof, String accessToken) 
            throws DPoPValidationException {
        
        long startTime = System.currentTimeMillis();
        String clientIp = getClientIpAddress(request);
        
        try {
            // Step 1: Basic validation
            if (dpopProof == null || dpopProof.trim().isEmpty()) {
                logSecurityEvent("DPoP validation failed", "Empty DPoP proof", request, clientIp);
                throw new DPoPValidationException("DPoP proof is required");
            }
            
            // Step 2: Parse JWT
            SignedJWT signedJWT = SignedJWT.parse(dpopProof);
            JWSHeader header = signedJWT.getHeader();
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            
            // Step 3: Validate JWT structure
            validateJWTStructure(header);
            
            // Step 4: Validate required claims
            validateRequiredClaims(claims);
            
            // Step 5: Validate HTTP method and URI binding
            validateHTTPBinding(request, claims);
            
            // Step 6: Validate timestamp and replay protection
            validateTimestampAndReplay(claims);
            
            // Step 7: Validate access token hash (if access token provided)
            if (accessToken != null && accessToken.startsWith("Bearer ")) {
                validateAccessTokenHash(accessToken, claims);
            }
            
            // Step 8: Validate signature
            validateSignature(signedJWT, header);
            
            // Log successful validation
            long duration = System.currentTimeMillis() - startTime;
            logSecurityEvent("DPoP validation successful", 
                String.format("Validation completed in %dms", duration), request, clientIp);
            
        } catch (ParseException e) {
            logSecurityEvent("DPoP validation failed", "Invalid JWT format: " + e.getMessage(), request, clientIp);
            throw new DPoPValidationException("Invalid DPoP proof format", e);
        } catch (Exception e) {
            if (e instanceof DPoPValidationException) {
                logSecurityEvent("DPoP validation failed", e.getMessage(), request, clientIp);
                throw e;
            }
            logSecurityEvent("DPoP validation failed", "Unexpected error: " + e.getMessage(), request, clientIp);
            throw new DPoPValidationException("DPoP validation failed", e);
        }
    }
    
    private void validateJWTStructure(JWSHeader header) throws DPoPValidationException {
        // Validate JWT type
        if (header.getType() == null || !"dpop+jwt".equals(header.getType().getType())) {
            throw new DPoPValidationException("Invalid DPoP JWT type");
        }
        
        // Validate algorithm (must be asymmetric)
        JWSAlgorithm algorithm = header.getAlgorithm();
        if (isSymmetricAlgorithm(algorithm)) {
            throw new DPoPValidationException("Invalid DPoP algorithm: " + algorithm);
        }
        
        // Validate JWK presence
        if (header.getJWK() == null) {
            throw new DPoPValidationException("DPoP JWT missing JWK");
        }
    }
    
    private void validateRequiredClaims(JWTClaimsSet claims) throws DPoPValidationException {
        // Validate jti (JWT ID)
        String jti = claims.getJTIString();
        if (jti == null || jti.trim().isEmpty()) {
            throw new DPoPValidationException("Missing or empty jti claim");
        }
        
        // Validate htm (HTTP method)
        String htm = claims.getStringClaim("htm");
        if (htm == null || htm.trim().isEmpty()) {
            throw new DPoPValidationException("Missing or empty htm claim");
        }
        
        // Validate htu (HTTP URI)
        String htu = claims.getStringClaim("htu");
        if (htu == null || htu.trim().isEmpty()) {
            throw new DPoPValidationException("Missing or empty htu claim");
        }
        
        // Validate iat (issued at)
        Date iat = claims.getIssueTime();
        if (iat == null) {
            throw new DPoPValidationException("Missing or empty iat claim");
        }
    }
    
    private void validateHTTPBinding(HttpServletRequest request, JWTClaimsSet claims) 
            throws DPoPValidationException {
        
        // Validate HTTP method
        String htm = claims.getStringClaim("htm");
        if (!request.getMethod().equalsIgnoreCase(htm)) {
            throw new DPoPValidationException(
                String.format("HTTP method mismatch: expected %s, got %s", htm, request.getMethod()));
        }
        
        // Validate HTTP URI
        String htu = claims.getStringClaim("htu");
        String requestUri = buildRequestURI(request);
        
        if (!requestUri.equals(htu)) {
            throw new DPoPValidationException(
                String.format("HTTP URI mismatch: expected %s, got %s", htu, requestUri));
        }
    }
    
    private void validateTimestampAndReplay(JWTClaimsSet claims) throws DPoPValidationException {
        Date iat = claims.getIssueTime();
        Instant now = Instant.now();
        Instant issuedAt = iat.toInstant();
        
        // Check if token is too old
        if (issuedAt.isBefore(now.minusSeconds(maxAgeSeconds))) {
            throw new DPoPValidationException("DPoP proof too old");
        }
        
        // Check if token is from future (with clock skew tolerance)
        if (issuedAt.isAfter(now.plusSeconds(maxClockSkewSeconds))) {
            throw new DPoPValidationException("DPoP proof from future");
        }
        
        // Check for replay attack with Redis cache
        String jti = claims.getJTIString();
        if (!storeNonceIfNotExists(jti)) {
            throw new DPoPValidationException("DPoP proof replay detected");
        }
    }
    
    private boolean storeNonceIfNotExists(String jti) {
        try {
            if (redisEnabled && redisTemplate != null) {
                // Use Redis SETNX (SET if Not eXists) for atomic operation
                String key = DPOP_NONCE_PREFIX + jti;
                Boolean result = redisTemplate.opsForValue().setIfAbsent(key, "used", maxAgeSeconds, TimeUnit.SECONDS);
                return result != null && result;
            } else {
                // Fallback to in-memory cache
                return fallbackNonceCache.add(jti);
            }
        } catch (Exception e) {
            logger.warn("Failed to store nonce in Redis, using fallback cache", e);
            return fallbackNonceCache.add(jti);
        }
    }
    
    private void validateAccessTokenHash(String accessToken, JWTClaimsSet claims) 
            throws DPoPValidationException {
        
        try {
            // Extract access token (remove "Bearer " prefix)
            String token = accessToken.substring(7);
            
            // Calculate access token hash
            String calculatedHash = calculateAccessTokenHash(token);
            
            // Get expected hash from claims
            String expectedHash = claims.getStringClaim("ath");
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
    
    private void validateSignature(SignedJWT signedJWT, JWSHeader header) 
            throws DPoPValidationException {
        
        try {
            // Extract JWK from header
            JWK jwk = header.getJWK();
            if (jwk == null) {
                throw new DPoPValidationException("Missing JWK in header");
            }
            
            // Convert to RSA key and verify signature
            RSAKey rsaKey = jwk.toRSAKey();
            RSASSAVerifier verifier = new RSASSAVerifier(rsaKey);
            
            if (!signedJWT.verify(verifier)) {
                throw new DPoPValidationException("Invalid DPoP proof signature");
            }
            
        } catch (JOSEException e) {
            throw new DPoPValidationException("Failed to verify signature", e);
        }
    }
    
    private String buildRequestURI(HttpServletRequest request) {
        StringBuilder uri = new StringBuilder();
        
        // Add scheme
        uri.append(request.getScheme() != null ? request.getScheme() : "https");
        uri.append("://");
        
        // Add host
        uri.append(request.getServerName() != null ? request.getServerName() : "api.banking.example.com");
        
        // Add port if not default
        int port = request.getServerPort();
        if (port != 80 && port != 443 && port != -1) {
            uri.append(":").append(port);
        }
        
        // Add path
        uri.append(request.getRequestURI() != null ? request.getRequestURI() : "/");
        
        return uri.toString();
    }
    
    private String calculateAccessTokenHash(String accessToken) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(accessToken.getBytes());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes);
    }
    
    private boolean isSymmetricAlgorithm(JWSAlgorithm algorithm) {
        return algorithm.equals(JWSAlgorithm.HS256) ||
               algorithm.equals(JWSAlgorithm.HS384) ||
               algorithm.equals(JWSAlgorithm.HS512);
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
    
    private void logSecurityEvent(String event, String details, HttpServletRequest request, String clientIp) {
        securityLogger.info("DPoP Security Event: {} | URI: {} | Method: {} | IP: {} | Details: {}",
            event,
            request.getRequestURI(),
            request.getMethod(),
            clientIp,
            details != null ? details : "None"
        );
    }
}