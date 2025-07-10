package com.loanmanagement.security.keycloak.dpop;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.security.MessageDigest;
import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * RFC 9449 compliant DPoP (Demonstrating Proof-of-Possession) validator
 * Validates DPoP proofs for OAuth 2.1 token binding
 */
@Component
public class DPoPValidator {

    private static final Logger logger = LoggerFactory.getLogger(DPoPValidator.class);
    
    private static final String DPOP_JWT_TYPE = "dpop+jwt";
    private static final long MAX_CLOCK_SKEW_SECONDS = 300; // 5 minutes
    private static final String JTI_CACHE_PREFIX = "dpop:jti:";
    private static final long JTI_CACHE_TTL_SECONDS = 3600; // 1 hour
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    /**
     * Validates a DPoP proof according to RFC 9449
     */
    public DPoPValidationResult validateDPoPProof(
            String dpopProof, 
            String httpMethod, 
            String httpUri, 
            String accessToken) {
        
        try {
            // Parse the DPoP JWT
            SignedJWT signedJWT = SignedJWT.parse(dpopProof);
            
            // Validate JWT header
            DPoPValidationResult headerResult = validateHeader(signedJWT.getHeader());
            if (!headerResult.isValid()) {
                return headerResult;
            }
            
            // Extract public key from header
            JWK publicKey = signedJWT.getHeader().getJWK();
            
            // Verify signature
            if (!verifySignature(signedJWT, publicKey)) {
                return DPoPValidationResult.invalid("Invalid DPoP proof signature");
            }
            
            // Validate claims
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            DPoPValidationResult claimsResult = validateClaims(claims, httpMethod, httpUri, accessToken);
            if (!claimsResult.isValid()) {
                return claimsResult;
            }
            
            // Check JTI for replay prevention
            String jti = claims.getJWTID();
            if (isJTIUsed(jti)) {
                return DPoPValidationResult.invalid("DPoP proof replay detected (JTI already used)");
            }
            
            // Store JTI to prevent replay
            storeJTI(jti);
            
            // Calculate JKT thumbprint
            String jktThumbprint = calculateJKTThumbprint(publicKey);
            
            logger.debug("DPoP proof validation successful for JTI: {}", jti);
            
            return DPoPValidationResult.valid(jktThumbprint, publicKey);
            
        } catch (ParseException e) {
            logger.warn("Failed to parse DPoP proof: {}", e.getMessage());
            return DPoPValidationResult.invalid("Invalid DPoP proof format");
        } catch (Exception e) {
            logger.error("DPoP validation error", e);
            return DPoPValidationResult.invalid("DPoP validation failed");
        }
    }
    
    private DPoPValidationResult validateHeader(JWSHeader header) {
        // Check JWT type
        if (!DPOP_JWT_TYPE.equals(header.getType().toString())) {
            return DPoPValidationResult.invalid("Invalid JWT type, expected: " + DPOP_JWT_TYPE);
        }
        
        // Check public key presence
        if (header.getJWK() == null) {
            return DPoPValidationResult.invalid("DPoP proof must contain public key in header");
        }
        
        // Validate algorithm
        String algorithm = header.getAlgorithm().getName();
        if (!"RS256".equals(algorithm) && !"ES256".equals(algorithm)) {
            return DPoPValidationResult.invalid("Unsupported algorithm: " + algorithm);
        }
        
        return DPoPValidationResult.valid();
    }
    
    private boolean verifySignature(SignedJWT signedJWT, JWK publicKey) {
        try {
            JWSVerifier verifier;
            
            if (publicKey instanceof RSAKey) {
                verifier = new RSASSAVerifier((RSAKey) publicKey);
            } else if (publicKey instanceof ECKey) {
                verifier = new ECDSAVerifier((ECKey) publicKey);
            } else {
                logger.warn("Unsupported key type: {}", publicKey.getKeyType());
                return false;
            }
            
            return signedJWT.verify(verifier);
            
        } catch (JOSEException e) {
            logger.warn("Signature verification failed: {}", e.getMessage());
            return false;
        }
    }
    
    private DPoPValidationResult validateClaims(
            JWTClaimsSet claims, 
            String httpMethod, 
            String httpUri, 
            String accessToken) {
        
        try {
            // Validate JTI
            String jti = claims.getJWTID();
            if (jti == null || jti.trim().isEmpty()) {
                return DPoPValidationResult.invalid("DPoP proof must contain jti claim");
            }
            
            // Validate HTTP method
            String htm = (String) claims.getClaim("htm");
            if (!httpMethod.equals(htm)) {
                return DPoPValidationResult.invalid(
                    String.format("HTTP method mismatch: expected %s, got %s", httpMethod, htm));
            }
            
            // Validate HTTP URI
            String htu = (String) claims.getClaim("htu");
            if (!normalizeUri(httpUri).equals(normalizeUri(htu))) {
                return DPoPValidationResult.invalid(
                    String.format("HTTP URI mismatch: expected %s, got %s", httpUri, htu));
            }
            
            // Validate timestamp
            Date iat = claims.getIssueTime();
            if (iat == null) {
                return DPoPValidationResult.invalid("DPoP proof must contain iat claim");
            }
            
            Instant now = Instant.now();
            Instant issuedAt = iat.toInstant();
            
            if (issuedAt.isAfter(now.plusSeconds(MAX_CLOCK_SKEW_SECONDS))) {
                return DPoPValidationResult.invalid("DPoP proof issued in the future");
            }
            
            if (issuedAt.isBefore(now.minusSeconds(MAX_CLOCK_SKEW_SECONDS))) {
                return DPoPValidationResult.invalid("DPoP proof is too old");
            }
            
            // Validate access token hash (if provided)
            if (accessToken != null) {
                String ath = (String) claims.getClaim("ath");
                if (ath != null) {
                    String expectedAth = calculateAccessTokenHash(accessToken);
                    if (!expectedAth.equals(ath)) {
                        return DPoPValidationResult.invalid("Access token hash mismatch");
                    }
                }
            }
            
            return DPoPValidationResult.valid();
            
        } catch (Exception e) {
            logger.warn("Claims validation failed: {}", e.getMessage());
            return DPoPValidationResult.invalid("Invalid DPoP proof claims");
        }
    }
    
    private String normalizeUri(String uri) {
        try {
            URI parsed = URI.create(uri);
            // Remove query parameters and fragments for comparison
            return new URI(parsed.getScheme(), null, parsed.getHost(), 
                          parsed.getPort(), parsed.getPath(), null, null).toString();
        } catch (Exception e) {
            return uri; // Return as-is if parsing fails
        }
    }
    
    private boolean isJTIUsed(String jti) {
        String key = JTI_CACHE_PREFIX + jti;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    private void storeJTI(String jti) {
        String key = JTI_CACHE_PREFIX + jti;
        redisTemplate.opsForValue().set(key, "used", JTI_CACHE_TTL_SECONDS, TimeUnit.SECONDS);
    }
    
    private String calculateAccessTokenHash(String accessToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(accessToken.getBytes("UTF-8"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate access token hash", e);
        }
    }
    
    /**
     * Calculates JKT thumbprint according to RFC 7638
     */
    public String calculateJKTThumbprint(JWK publicKey) {
        try {
            byte[] thumbprint = publicKey.computeThumbprint("SHA-256").decode();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(thumbprint);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate JKT thumbprint", e);
        }
    }
    
    /**
     * DPoP validation result
     */
    public static class DPoPValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final String jktThumbprint;
        private final JWK publicKey;
        
        private DPoPValidationResult(boolean valid, String errorMessage, 
                                   String jktThumbprint, JWK publicKey) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.jktThumbprint = jktThumbprint;
            this.publicKey = publicKey;
        }
        
        public static DPoPValidationResult valid() {
            return new DPoPValidationResult(true, null, null, null);
        }
        
        public static DPoPValidationResult valid(String jktThumbprint, JWK publicKey) {
            return new DPoPValidationResult(true, null, jktThumbprint, publicKey);
        }
        
        public static DPoPValidationResult invalid(String errorMessage) {
            return new DPoPValidationResult(false, errorMessage, null, null);
        }
        
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
        public String getJktThumbprint() { return jktThumbprint; }
        public JWK getPublicKey() { return publicKey; }
    }
}