package com.bank.loan.loan.security.dpop.model;

import com.nimbusds.jwt.JWTClaimsSet;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;

public class DPoPBoundToken {
    
    private final String accessToken;
    private final JWTClaimsSet jwtClaimsSet;
    private final String jktThumbprint;
    
    public DPoPBoundToken(String accessToken, JWTClaimsSet jwtClaimsSet, String jktThumbprint) {
        this.accessToken = accessToken;
        this.jwtClaimsSet = jwtClaimsSet;
        this.jktThumbprint = jktThumbprint;
    }
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public JWTClaimsSet getJwtClaimsSet() {
        return jwtClaimsSet;
    }
    
    public String getJktThumbprint() {
        return jktThumbprint;
    }
    
    public String getIssuer() {
        return jwtClaimsSet.getIssuer();
    }
    
    public String getSubject() {
        return jwtClaimsSet.getSubject();
    }
    
    public List<String> getAudience() {
        try {
            List<String> audiences = jwtClaimsSet.getAudience();
            return audiences != null ? audiences : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
    
    public String getTokenId() {
        return jwtClaimsSet.getJWTID();
    }
    
    public Instant getExpiresAt() {
        return jwtClaimsSet.getExpirationTime() != null ? 
            jwtClaimsSet.getExpirationTime().toInstant() : null;
    }
    
    public Instant getIssuedAt() {
        return jwtClaimsSet.getIssueTime() != null ? 
            jwtClaimsSet.getIssueTime().toInstant() : null;
    }
    
    public List<String> getScopes() {
        try {
            String scope = jwtClaimsSet.getStringClaim("scope");
            if (scope != null && !scope.trim().isEmpty()) {
                return Arrays.asList(scope.trim().split("\\s+"));
            }
            return Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
    
    public boolean isActive() {
        Instant now = Instant.now();
        Instant expiresAt = getExpiresAt();
        
        // If no expiration time, consider it active
        if (expiresAt == null) {
            return true;
        }
        
        return now.isBefore(expiresAt);
    }
    
    public boolean hasScope(String scope) {
        return getScopes().contains(scope);
    }
    
    public boolean hasAnyScope(String... scopes) {
        List<String> tokenScopes = getScopes();
        return Arrays.stream(scopes).anyMatch(tokenScopes::contains);
    }
    
    public Object getClaim(String claimName) {
        try {
            return jwtClaimsSet.getClaim(claimName);
        } catch (Exception e) {
            return null;
        }
    }
    
    public String getStringClaim(String claimName) {
        try {
            return jwtClaimsSet.getStringClaim(claimName);
        } catch (Exception e) {
            return null;
        }
    }
    
    @Override
    public String toString() {
        return "DPoPBoundToken{" +
                "subject='" + getSubject() + '\'' +
                ", issuer='" + getIssuer() + '\'' +
                ", tokenId='" + getTokenId() + '\'' +
                ", jktThumbprint='" + jktThumbprint + '\'' +
                ", scopes=" + getScopes() +
                ", isActive=" + isActive() +
                ", expiresAt=" + getExpiresAt() +
                '}';
    }
}