package com.bank.loan.loan.security.par.model;

import com.nimbusds.jwt.JWTClaimsSet;

import java.time.Instant;
import java.util.Map;

public class PARRequest {
    
    private final String clientId;
    private final String redirectUri;
    private final String responseType;
    private final String scope;
    private final String state;
    private final String codeChallenge;
    private final String codeChallengeMethod;
    private final String dpopJktThumbprint;
    private final Map<String, Object> additionalParameters;
    private final Instant createdAt;
    
    public PARRequest(String clientId, String redirectUri, String responseType, String scope, 
                     String state, String codeChallenge, String codeChallengeMethod, 
                     String dpopJktThumbprint, Map<String, Object> additionalParameters) {
        this.clientId = clientId;
        this.redirectUri = redirectUri;
        this.responseType = responseType;
        this.scope = scope;
        this.state = state;
        this.codeChallenge = codeChallenge;
        this.codeChallengeMethod = codeChallengeMethod;
        this.dpopJktThumbprint = dpopJktThumbprint;
        this.additionalParameters = additionalParameters != null ? additionalParameters : Map.of();
        this.createdAt = Instant.now();
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public String getRedirectUri() {
        return redirectUri;
    }
    
    public String getResponseType() {
        return responseType;
    }
    
    public String getScope() {
        return scope;
    }
    
    public String getState() {
        return state;
    }
    
    public String getCodeChallenge() {
        return codeChallenge;
    }
    
    public String getCodeChallengeMethod() {
        return codeChallengeMethod;
    }
    
    public String getDpopJktThumbprint() {
        return dpopJktThumbprint;
    }
    
    public Map<String, Object> getAdditionalParameters() {
        return additionalParameters;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public boolean isExpired(long expirationSeconds) {
        return Instant.now().isAfter(createdAt.plusSeconds(expirationSeconds));
    }
    
    public JWTClaimsSet toJWTClaimsSet() {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .claim("client_id", clientId)
                .claim("redirect_uri", redirectUri)
                .claim("response_type", responseType)
                .claim("scope", scope)
                .claim("state", state)
                .claim("code_challenge", codeChallenge)
                .claim("code_challenge_method", codeChallengeMethod)
                .issueTime(java.util.Date.from(createdAt));
        
        if (dpopJktThumbprint != null) {
            builder.claim("dpop_jkt", dpopJktThumbprint);
        }
        
        // Add additional parameters
        for (Map.Entry<String, Object> entry : additionalParameters.entrySet()) {
            builder.claim(entry.getKey(), entry.getValue());
        }
        
        return builder.build();
    }
    
    @Override
    public String toString() {
        return "PARRequest{" +
                "clientId='" + clientId + '\'' +
                ", redirectUri='" + redirectUri + '\'' +
                ", responseType='" + responseType + '\'' +
                ", scope='" + scope + '\'' +
                ", state='" + state + '\'' +
                ", codeChallengeMethod='" + codeChallengeMethod + '\'' +
                ", hasDpopJkt=" + (dpopJktThumbprint != null) +
                ", createdAt=" + createdAt +
                '}';
    }
}