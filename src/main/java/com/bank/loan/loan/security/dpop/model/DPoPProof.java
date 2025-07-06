package com.bank.loan.loan.security.dpop.model;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;

import java.time.Instant;

public class DPoPProof {
    
    private final String jwtString;
    private final JWTClaimsSet claimsSet;
    private final JWK publicKey;
    private final String algorithm;
    
    public DPoPProof(String jwtString, JWTClaimsSet claimsSet, JWK publicKey, String algorithm) {
        this.jwtString = jwtString;
        this.claimsSet = claimsSet;
        this.publicKey = publicKey;
        this.algorithm = algorithm;
    }
    
    public String getJwtString() {
        return jwtString;
    }
    
    public JWTClaimsSet getClaimsSet() {
        return claimsSet;
    }
    
    public JWK getPublicKey() {
        return publicKey;
    }
    
    public String getAlgorithm() {
        return algorithm;
    }
    
    public String getJti() {
        return claimsSet.getJWTID();
    }
    
    public String getHttpMethod() {
        return claimsSet.getStringClaim("htm");
    }
    
    public String getHttpUri() {
        return claimsSet.getStringClaim("htu");
    }
    
    public Instant getIssuedAt() {
        return claimsSet.getIssueTime() != null ? 
            claimsSet.getIssueTime().toInstant() : null;
    }
    
    public String getAccessTokenHash() {
        return claimsSet.getStringClaim("ath");
    }
    
    public String getNonce() {
        return claimsSet.getStringClaim("nonce");
    }
    
    public boolean hasAccessTokenHash() {
        return getAccessTokenHash() != null && !getAccessTokenHash().isEmpty();
    }
    
    public boolean hasNonce() {
        return getNonce() != null && !getNonce().isEmpty();
    }
    
    @Override
    public String toString() {
        return "DPoPProof{" +
                "jti='" + getJti() + '\'' +
                ", htm='" + getHttpMethod() + '\'' +
                ", htu='" + getHttpUri() + '\'' +
                ", iat=" + getIssuedAt() +
                ", hasAth=" + hasAccessTokenHash() +
                ", hasNonce=" + hasNonce() +
                '}';
    }
}