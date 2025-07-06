package com.bank.loan.loan.security.dpop.service;

import com.bank.loan.loan.security.dpop.exception.InvalidDPoPProofException;
import com.bank.loan.loan.security.dpop.model.DPoPProof;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
<<<<<<< Updated upstream
import com.nimbusds.jose.jwk.Curve;
=======
>>>>>>> Stashed changes
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DPoPProofValidationService {
    
    private static final String DPOP_JWT_TYPE = "dpop+jwt";
    private static final List<String> SUPPORTED_ALGORITHMS = Arrays.asList("ES256", "ES384", "ES512", "RS256", "RS384", "RS512", "PS256", "PS384", "PS512");
    private static final long PROOF_EXPIRATION_SECONDS = 60;
    private static final long CLOCK_SKEW_TOLERANCE_SECONDS = 30;
    private static final String JTI_CACHE_PREFIX = "dpop:jti:";
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    public DPoPProofValidationService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    public void validateDPoPProof(String dpopProofJwt, String httpMethod, String httpUri, String accessToken) {
        try {
            DPoPProof dpopProof = parseDPoPProof(dpopProofJwt);
            
            validateProofStructure(dpopProof);
            validateSignature(dpopProof);
            validateTimestamp(dpopProof);
            validateHttpBinding(dpopProof, httpMethod, httpUri);
            validateAccessTokenHash(dpopProof, accessToken);
            validateReplayPrevention(dpopProof);
            
        } catch (Exception e) {
            if (e instanceof InvalidDPoPProofException) {
                throw e;
            }
            throw new InvalidDPoPProofException("Failed to validate DPoP proof", e);
        }
    }
    
    public void validateDPoPProofWithNonce(String dpopProofJwt, String httpMethod, String httpUri, 
                                          String accessToken, String expectedNonce) {
        try {
            DPoPProof dpopProof = parseDPoPProof(dpopProofJwt);
            
            validateProofStructure(dpopProof);
            validateSignature(dpopProof);
            validateTimestamp(dpopProof);
            validateHttpBinding(dpopProof, httpMethod, httpUri);
            validateAccessTokenHash(dpopProof, accessToken);
            validateNonce(dpopProof, expectedNonce);
            validateReplayPrevention(dpopProof);
            
        } catch (Exception e) {
            if (e instanceof InvalidDPoPProofException) {
                throw e;
            }
            throw new InvalidDPoPProofException("Failed to validate DPoP proof with nonce", e);
        }
    }
    
    private DPoPProof parseDPoPProof(String dpopProofJwt) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(dpopProofJwt);
            JWSHeader header = signedJWT.getHeader();
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            
            // Extract JWK from header
            JWK publicKey = header.getJWK();
            if (publicKey == null) {
                throw new InvalidDPoPProofException("Missing JWK in header");
            }
            
            String algorithm = header.getAlgorithm().getName();
            
            return new DPoPProof(dpopProofJwt, claimsSet, publicKey, algorithm);
            
        } catch (Exception e) {
            throw new InvalidDPoPProofException("Failed to parse DPoP proof", e);
        }
    }
    
    private void validateProofStructure(DPoPProof dpopProof) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(dpopProof.getJwtString());
            JWSHeader header = signedJWT.getHeader();
            JWTClaimsSet claimsSet = dpopProof.getClaimsSet();
            
            // Validate typ header
            JOSEObjectType type = header.getType();
            if (type == null || !DPOP_JWT_TYPE.equals(type.getType())) {
                throw new InvalidDPoPProofException("Invalid typ header");
            }
            
            // Validate algorithm
            String algorithm = header.getAlgorithm().getName();
            if (!SUPPORTED_ALGORITHMS.contains(algorithm)) {
                throw new InvalidDPoPProofException("Unsupported algorithm");
            }
            
            // Validate required claims
            if (claimsSet.getJWTID() == null || claimsSet.getJWTID().trim().isEmpty()) {
                throw new InvalidDPoPProofException("Missing required claims");
            }
            
            if (claimsSet.getStringClaim("htm") == null) {
                throw new InvalidDPoPProofException("Missing required claims");
            }
            
            if (claimsSet.getStringClaim("htu") == null) {
                throw new InvalidDPoPProofException("Missing required claims");
            }
            
            if (claimsSet.getIssueTime() == null) {
                throw new InvalidDPoPProofException("Missing required claims");
            }
            
        } catch (Exception e) {
            if (e instanceof InvalidDPoPProofException) {
                throw e;
            }
            throw new InvalidDPoPProofException("Invalid proof structure", e);
        }
    }
    
    private void validateSignature(DPoPProof dpopProof) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(dpopProof.getJwtString());
            JWK publicKey = dpopProof.getPublicKey();
            
            JWSVerifier verifier;
            if (publicKey instanceof ECKey) {
                ECKey ecKey = (ECKey) publicKey;
                validateKeyStrength(ecKey);
                verifier = new ECDSAVerifier(ecKey);
            } else if (publicKey instanceof RSAKey) {
                RSAKey rsaKey = (RSAKey) publicKey;
                validateKeyStrength(rsaKey);
                verifier = new RSASSAVerifier(rsaKey);
            } else {
                throw new InvalidDPoPProofException("Unsupported key type");
            }
            
            if (!signedJWT.verify(verifier)) {
                throw new InvalidDPoPProofException("Invalid signature");
            }
            
        } catch (Exception e) {
            if (e instanceof InvalidDPoPProofException) {
                throw e;
            }
            throw new InvalidDPoPProofException("Signature validation failed", e);
        }
    }
    
    private void validateKeyStrength(ECKey ecKey) {
        try {
            Curve curve = ecKey.getCurve();
            if (curve == null) {
                throw new InvalidDPoPProofException("Missing curve information");
            }
            
            // Accept P-256, P-384, P-521
            if (!Arrays.asList(Curve.P_256, Curve.P_384, Curve.P_521).contains(curve)) {
                throw new InvalidDPoPProofException("Insufficient key strength");
            }
        } catch (Exception e) {
            throw new InvalidDPoPProofException("Key validation failed", e);
        }
    }
    
    private void validateKeyStrength(RSAKey rsaKey) {
        try {
            int keySize = rsaKey.size();
            if (keySize < 2048) {
                throw new InvalidDPoPProofException("Insufficient key strength");
            }
        } catch (Exception e) {
            throw new InvalidDPoPProofException("Key validation failed", e);
        }
    }
    
    private void validateTimestamp(DPoPProof dpopProof) {
        Instant now = Instant.now();
        Instant issuedAt = dpopProof.getIssuedAt();
        
        if (issuedAt == null) {
            throw new InvalidDPoPProofException("Missing issued at time");
        }
        
        // Check if proof is too old
        if (Duration.between(issuedAt, now).getSeconds() > PROOF_EXPIRATION_SECONDS + CLOCK_SKEW_TOLERANCE_SECONDS) {
            throw new InvalidDPoPProofException("Proof too old");
        }
        
        // Check if proof is from the future
        if (Duration.between(now, issuedAt).getSeconds() > CLOCK_SKEW_TOLERANCE_SECONDS) {
            throw new InvalidDPoPProofException("Proof too far in future");
        }
    }
    
    private void validateHttpBinding(DPoPProof dpopProof, String httpMethod, String httpUri) {
        String proofMethod = dpopProof.getHttpMethod();
        String proofUri = dpopProof.getHttpUri();
        
        if (!httpMethod.equals(proofMethod)) {
            throw new InvalidDPoPProofException("Method mismatch");
        }
        
        if (!httpUri.equals(proofUri)) {
            throw new InvalidDPoPProofException("URI mismatch");
        }
    }
    
    private void validateAccessTokenHash(DPoPProof dpopProof, String accessToken) {
        if (accessToken == null) {
            return; // No access token to validate
        }
        
        String proofAccessTokenHash = dpopProof.getAccessTokenHash();
        
        if (proofAccessTokenHash == null || proofAccessTokenHash.isEmpty()) {
            throw new InvalidDPoPProofException("Missing access token hash");
        }
        
        try {
            String expectedHash = calculateAccessTokenHash(accessToken);
            if (!expectedHash.equals(proofAccessTokenHash)) {
                throw new InvalidDPoPProofException("Access token binding mismatch");
            }
        } catch (Exception e) {
            throw new InvalidDPoPProofException("Access token hash validation failed", e);
        }
    }
    
    private void validateNonce(DPoPProof dpopProof, String expectedNonce) {
        if (expectedNonce == null) {
            return; // No nonce required
        }
        
        String proofNonce = dpopProof.getNonce();
        
        if (proofNonce == null || proofNonce.isEmpty()) {
            throw new InvalidDPoPProofException("use_dpop_nonce", "Missing nonce in DPoP proof");
        }
        
        if (!expectedNonce.equals(proofNonce)) {
            throw new InvalidDPoPProofException("use_dpop_nonce", "Invalid nonce in DPoP proof");
        }
    }
    
    private void validateReplayPrevention(DPoPProof dpopProof) {
        String jti = dpopProof.getJti();
        String cacheKey = JTI_CACHE_PREFIX + jti;
        
        // Check if JTI has been used before
        Boolean exists = redisTemplate.hasKey(cacheKey);
        if (Boolean.TRUE.equals(exists)) {
            throw new InvalidDPoPProofException("Proof replay detected");
        }
        
        // Store JTI to prevent replay
        try {
            redisTemplate.opsForValue().set(cacheKey, "used", PROOF_EXPIRATION_SECONDS + CLOCK_SKEW_TOLERANCE_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            // Log error but don't fail validation if Redis is temporarily unavailable
            // In production, you might want to fail-safe differently
            throw new InvalidDPoPProofException("Replay prevention storage failed", e);
        }
    }
    
    private String calculateAccessTokenHash(String accessToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(accessToken.getBytes(StandardCharsets.UTF_8));
            return Base64URL.encode(hash).toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate access token hash", e);
        }
    }
}