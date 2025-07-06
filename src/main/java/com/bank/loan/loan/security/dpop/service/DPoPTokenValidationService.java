package com.bank.loan.loan.security.dpop.service;

import com.bank.loan.loan.security.dpop.exception.InvalidDPoPProofException;
import com.bank.loan.loan.security.dpop.exception.TokenBindingMismatchException;
import com.bank.loan.loan.security.dpop.model.DPoPBoundToken;
import com.bank.loan.loan.security.dpop.model.DPoPProof;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class DPoPTokenValidationService {
    
    private final JwtDecoder jwtDecoder;
    private final DPoPProofValidationService dpopProofValidationService;
    private final DPoPTokenBindingService dpopTokenBindingService;
    
    public DPoPTokenValidationService(JwtDecoder jwtDecoder, 
                                     DPoPProofValidationService dpopProofValidationService) {
        this.jwtDecoder = jwtDecoder;
        this.dpopProofValidationService = dpopProofValidationService;
        this.dpopTokenBindingService = new DPoPTokenBindingService(null); // Will be injected properly in configuration
    }
    
    public void validateDPoPBoundToken(String accessToken, String dpopProof, String httpMethod, String httpUri) {
        try {
            // First validate the DPoP proof
            dpopProofValidationService.validateDPoPProof(dpopProof, httpMethod, httpUri, accessToken);
            
            // Decode the access token
            Jwt jwt = jwtDecoder.decode(accessToken);
            
            // Validate token is not expired
            validateTokenExpiration(jwt);
            
            // Extract DPoP key from proof
            JWK dpopKey = extractDPoPKeyFromProof(dpopProof);
            
            // Validate token binding
            validateTokenBinding(jwt, dpopKey);
            
        } catch (Exception e) {
            if (e instanceof InvalidDPoPProofException || e instanceof TokenBindingMismatchException) {
                throw e;
            }
            throw new TokenBindingMismatchException("DPoP token validation failed", e);
        }
    }
    
    public DPoPBoundToken validateAndExtractDPoPBoundToken(String accessToken, String dpopProof, 
                                                          String httpMethod, String httpUri) {
        try {
            // Validate the DPoP-bound token
            validateDPoPBoundToken(accessToken, dpopProof, httpMethod, httpUri);
            
            // Decode the access token
            Jwt jwt = jwtDecoder.decode(accessToken);
            
            // Extract DPoP bound token information
            return dpopTokenBindingService.extractDPoPBoundToken(jwt);
            
        } catch (Exception e) {
            if (e instanceof InvalidDPoPProofException || e instanceof TokenBindingMismatchException) {
                throw e;
            }
            throw new TokenBindingMismatchException("Failed to extract DPoP bound token", e);
        }
    }
    
    public void validateTokenBinding(Jwt jwt, JWK dpopKey) {
        try {
            // Check if token is DPoP-bound
            Map<String, Object> cnfClaim = jwt.getClaim("cnf");
            if (cnfClaim == null) {
                throw new TokenBindingMismatchException("Token is not DPoP-bound");
            }
            
            // Extract jkt from cnf claim
            Object jktObj = cnfClaim.get("jkt");
            if (jktObj == null) {
                throw new TokenBindingMismatchException("Missing jkt claim");
            }
            
            String tokenJktThumbprint = jktObj.toString();
            if (tokenJktThumbprint.isEmpty()) {
                throw new TokenBindingMismatchException("Empty jkt claim");
            }
            
            // Calculate thumbprint from DPoP key
            String dpopJktThumbprint = dpopTokenBindingService.calculateJktThumbprint(dpopKey);
            
            // Compare thumbprints
            if (!tokenJktThumbprint.equals(dpopJktThumbprint)) {
                throw new TokenBindingMismatchException("JKT thumbprint mismatch");
            }
            
        } catch (Exception e) {
            if (e instanceof TokenBindingMismatchException) {
                throw e;
            }
            throw new TokenBindingMismatchException("Token binding validation failed", e);
        }
    }
    
    private void validateTokenExpiration(Jwt jwt) {
        Instant expiresAt = jwt.getExpiresAt();
        if (expiresAt != null && Instant.now().isAfter(expiresAt)) {
            throw new TokenBindingMismatchException("Token expired");
        }
    }
    
    private JWK extractDPoPKeyFromProof(String dpopProof) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(dpopProof);
            JWK publicKey = signedJWT.getHeader().getJWK();
            
            if (publicKey == null) {
                throw new InvalidDPoPProofException("Missing JWK in DPoP proof header");
            }
            
            return publicKey;
            
        } catch (Exception e) {
            throw new InvalidDPoPProofException("Failed to extract DPoP key from proof", e);
        }
    }
    
    public boolean isDPoPBoundToken(String accessToken) {
        try {
            Jwt jwt = jwtDecoder.decode(accessToken);
            Map<String, Object> cnfClaim = jwt.getClaim("cnf");
            
            if (cnfClaim == null) {
                return false;
            }
            
            Object jktObj = cnfClaim.get("jkt");
            return jktObj != null && !jktObj.toString().isEmpty();
            
        } catch (Exception e) {
            return false;
        }
    }
    
    public String extractJktThumbprint(String accessToken) {
        try {
            Jwt jwt = jwtDecoder.decode(accessToken);
            Map<String, Object> cnfClaim = jwt.getClaim("cnf");
            
            if (cnfClaim == null) {
                return null;
            }
            
            Object jktObj = cnfClaim.get("jkt");
            return jktObj != null ? jktObj.toString() : null;
            
        } catch (Exception e) {
            return null;
        }
    }
    
    public DPoPBoundToken introspectDPoPBoundToken(String accessToken) {
        try {
            Jwt jwt = jwtDecoder.decode(accessToken);
            
            if (!isDPoPBoundToken(accessToken)) {
                throw new TokenBindingMismatchException("Token is not DPoP-bound");
            }
            
            return dpopTokenBindingService.extractDPoPBoundToken(jwt);
            
        } catch (Exception e) {
            if (e instanceof TokenBindingMismatchException) {
                throw e;
            }
            throw new TokenBindingMismatchException("Failed to introspect DPoP bound token", e);
        }
    }
}