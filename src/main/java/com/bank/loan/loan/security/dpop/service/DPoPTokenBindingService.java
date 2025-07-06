package com.bank.loan.loan.security.dpop.service;

import com.bank.loan.loan.security.dpop.exception.TokenBindingMismatchException;
import com.bank.loan.loan.security.dpop.model.DPoPBoundToken;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class DPoPTokenBindingService {
    
    private final JwtEncoder jwtEncoder;
    
    public DPoPTokenBindingService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }
    
    public String createDPoPBoundAccessToken(JWTClaimsSet claimsSet, JWK dpopKey) {
        try {
            JWTClaimsSet boundClaimsSet = addDPoPBinding(claimsSet, dpopKey);
            
            JwsHeader header = JwsHeader.with(SignatureAlgorithm.PS256)
                    .type("JWT")
                    .build();
            
            org.springframework.security.oauth2.jwt.JwtClaimsSet springClaimsSet = 
                convertToSpringClaimsSet(boundClaimsSet);
            
            JwtEncoderParameters parameters = JwtEncoderParameters.from(header, springClaimsSet);
            Jwt jwt = jwtEncoder.encode(parameters);
            
            return jwt.getTokenValue();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create DPoP-bound access token", e);
        }
    }
    
    public JWTClaimsSet addDPoPBinding(JWTClaimsSet originalClaimsSet, JWK dpopKey) {
        try {
            String jktThumbprint = calculateJktThumbprint(dpopKey);
            
            JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder(originalClaimsSet);
            
            // Add cnf claim with jkt
            Map<String, Object> cnfClaim = Map.of("jkt", jktThumbprint);
            builder.claim("cnf", cnfClaim);
            
            return builder.build();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to add DPoP binding", e);
        }
    }
    
    public void validateTokenBinding(Jwt jwt, JWK dpopKey) {
        try {
            // Extract cnf claim
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
            String dpopJktThumbprint = calculateJktThumbprint(dpopKey);
            
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
    
    public String calculateJktThumbprint(JWK key) {
        try {
            if (key instanceof ECKey) {
                return ((ECKey) key).computeThumbprint().toString();
            } else if (key instanceof RSAKey) {
                return ((RSAKey) key).computeThumbprint().toString();
            } else {
                throw new IllegalArgumentException("Unsupported key type");
            }
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to calculate JKT thumbprint", e);
        }
    }
    
    public DPoPBoundToken extractDPoPBoundToken(Jwt jwt) {
        try {
            // Validate that token is DPoP-bound
            Map<String, Object> cnfClaim = jwt.getClaim("cnf");
            if (cnfClaim == null) {
                throw new TokenBindingMismatchException("Token is not DPoP-bound");
            }
            
            Object jktObj = cnfClaim.get("jkt");
            if (jktObj == null) {
                throw new TokenBindingMismatchException("Missing jkt claim");
            }
            
            String jktThumbprint = jktObj.toString();
            
            // Convert JWT to JWTClaimsSet
            JWTClaimsSet claimsSet = convertToNimbusClaimsSet(jwt);
            
            return new DPoPBoundToken(jwt.getTokenValue(), claimsSet, jktThumbprint);
            
        } catch (Exception e) {
            if (e instanceof TokenBindingMismatchException) {
                throw e;
            }
            throw new TokenBindingMismatchException("Failed to extract DPoP bound token", e);
        }
    }
    
    private org.springframework.security.oauth2.jwt.JwtClaimsSet convertToSpringClaimsSet(JWTClaimsSet nimbusClaimsSet) {
        org.springframework.security.oauth2.jwt.JwtClaimsSet.Builder builder = 
            org.springframework.security.oauth2.jwt.JwtClaimsSet.builder();
        
        // Copy standard claims
        if (nimbusClaimsSet.getIssuer() != null) {
            builder.issuer(nimbusClaimsSet.getIssuer());
        }
        if (nimbusClaimsSet.getSubject() != null) {
            builder.subject(nimbusClaimsSet.getSubject());
        }
        if (nimbusClaimsSet.getAudience() != null) {
            builder.audience(nimbusClaimsSet.getAudience());
        }
        if (nimbusClaimsSet.getExpirationTime() != null) {
            builder.expiresAt(nimbusClaimsSet.getExpirationTime().toInstant());
        }
        if (nimbusClaimsSet.getIssueTime() != null) {
            builder.issuedAt(nimbusClaimsSet.getIssueTime().toInstant());
        }
        if (nimbusClaimsSet.getJWTID() != null) {
            builder.id(nimbusClaimsSet.getJWTID());
        }
        
        // Copy all other claims
        for (String claimName : nimbusClaimsSet.getClaims().keySet()) {
            if (!isStandardClaim(claimName)) {
                try {
                    Object claimValue = nimbusClaimsSet.getClaim(claimName);
                    builder.claim(claimName, claimValue);
                } catch (Exception e) {
                    // Skip claims that can't be copied
                }
            }
        }
        
        return builder.build();
    }
    
    private JWTClaimsSet convertToNimbusClaimsSet(Jwt jwt) {
        try {
            JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();
            
            // Copy standard claims
            if (jwt.getIssuer() != null) {
                builder.issuer(jwt.getIssuer().toString());
            }
            if (jwt.getSubject() != null) {
                builder.subject(jwt.getSubject());
            }
            if (jwt.getAudience() != null) {
                builder.audience(jwt.getAudience());
            }
            if (jwt.getExpiresAt() != null) {
                builder.expirationTime(Date.from(jwt.getExpiresAt()));
            }
            if (jwt.getIssuedAt() != null) {
                builder.issueTime(Date.from(jwt.getIssuedAt()));
            }
            if (jwt.getId() != null) {
                builder.jwtID(jwt.getId());
            }
            
            // Copy all other claims
            for (String claimName : jwt.getClaims().keySet()) {
                if (!isStandardClaim(claimName)) {
                    Object claimValue = jwt.getClaim(claimName);
                    builder.claim(claimName, claimValue);
                }
            }
            
            return builder.build();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JWT to JWTClaimsSet", e);
        }
    }
    
    private boolean isStandardClaim(String claimName) {
        return "iss".equals(claimName) || 
               "sub".equals(claimName) || 
               "aud".equals(claimName) || 
               "exp".equals(claimName) || 
               "iat".equals(claimName) || 
               "jti".equals(claimName);
    }
}