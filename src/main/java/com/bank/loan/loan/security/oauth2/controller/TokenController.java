package com.bank.loan.loan.security.oauth2.controller;

import com.bank.loan.loan.security.dpop.exception.InvalidDPoPProofException;
import com.bank.loan.loan.security.dpop.service.DPoPProofValidationService;
import com.bank.loan.loan.security.dpop.service.DPoPTokenBindingService;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/oauth2")
public class TokenController {
    
    private final DPoPProofValidationService dpopProofValidationService;
    private final DPoPTokenBindingService dpopTokenBindingService;
    
    public TokenController(DPoPProofValidationService dpopProofValidationService,
                          DPoPTokenBindingService dpopTokenBindingService) {
        this.dpopProofValidationService = dpopProofValidationService;
        this.dpopTokenBindingService = dpopTokenBindingService;
    }
    
    @PostMapping(value = "/token",
                consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> token(@RequestParam MultiValueMap<String, String> parameters,
                                 HttpServletRequest request) {
        
        try {
            // Convert MultiValueMap to Map
            Map<String, String> tokenParams = new HashMap<>();
            parameters.forEach((key, values) -> {
                if (values != null && !values.isEmpty()) {
                    tokenParams.put(key, values.get(0));
                }
            });
            
            // Validate grant type
            String grantType = tokenParams.get("grant_type");
            if (!"authorization_code".equals(grantType) && !"refresh_token".equals(grantType)) {
                return createErrorResponse("unsupported_grant_type", 
                    "Only authorization_code and refresh_token grant types are supported");
            }
            
            // Get DPoP proof from header
            String dpopProof = request.getHeader("DPoP");
            if (dpopProof == null || dpopProof.trim().isEmpty()) {
                return createErrorResponse("invalid_dpop_proof", "Missing DPoP header");
            }
            
            // Validate DPoP proof for token endpoint
            String tokenEndpointUrl = constructTokenEndpointUrl(request);
            dpopProofValidationService.validateDPoPProof(dpopProof, "POST", tokenEndpointUrl, null);
            
            // Extract DPoP key from proof
            JWK dpopKey = extractDPoPKeyFromProof(dpopProof);
            
            if ("authorization_code".equals(grantType)) {
                return handleAuthorizationCodeGrant(tokenParams, dpopKey);
            } else {
                return handleRefreshTokenGrant(tokenParams, dpopKey);
            }
            
        } catch (InvalidDPoPProofException e) {
            return createDPoPErrorResponse(e);
        } catch (Exception e) {
            return createErrorResponse("server_error", "Internal server error occurred");
        }
    }
    
    private ResponseEntity<?> handleAuthorizationCodeGrant(Map<String, String> params, JWK dpopKey) {
        try {
            // Validate required parameters
            String code = params.get("code");
            String redirectUri = params.get("redirect_uri");
            String codeVerifier = params.get("code_verifier");
            String clientAssertion = params.get("client_assertion");
            String clientAssertionType = params.get("client_assertion_type");
            
            if (code == null || code.trim().isEmpty()) {
                return createErrorResponse("invalid_request", "Missing authorization code");
            }
            
            if (redirectUri == null || redirectUri.trim().isEmpty()) {
                return createErrorResponse("invalid_request", "Missing redirect_uri");
            }
            
            if (codeVerifier == null || codeVerifier.trim().isEmpty()) {
                return createErrorResponse("invalid_request", "Missing code_verifier (PKCE required)");
            }
            
            // Validate client assertion for private_key_jwt
            if (clientAssertion == null || clientAssertionType == null ||
                !"urn:ietf:params:oauth:client-assertion-type:jwt-bearer".equals(clientAssertionType)) {
                return createErrorResponse("invalid_client", 
                    "Client authentication must use private_key_jwt");
            }
            
            // In a real implementation, you would:
            // 1. Validate the authorization code
            // 2. Verify PKCE code_verifier
            // 3. Validate client assertion JWT
            // 4. Generate access and refresh tokens
            
            // For this implementation, we'll create DPoP-bound tokens
            return createTokenResponse(dpopKey, "user123", "enterprise-banking-app", 
                                     "loans payments accounts");
            
        } catch (Exception e) {
            return createErrorResponse("invalid_grant", "Authorization code validation failed");
        }
    }
    
    private ResponseEntity<?> handleRefreshTokenGrant(Map<String, String> params, JWK dpopKey) {
        try {
            String refreshToken = params.get("refresh_token");
            
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                return createErrorResponse("invalid_request", "Missing refresh_token");
            }
            
            // In a real implementation, you would:
            // 1. Validate the refresh token
            // 2. Verify it's bound to the same DPoP key
            // 3. Check if it's expired or revoked
            // 4. Generate new access token (and optionally new refresh token)
            
            // For this implementation, we'll create new DPoP-bound tokens
            return createTokenResponse(dpopKey, "user123", "enterprise-banking-app", 
                                     "loans payments accounts");
            
        } catch (Exception e) {
            return createErrorResponse("invalid_grant", "Refresh token validation failed");
        }
    }
    
    private ResponseEntity<?> createTokenResponse(JWK dpopKey, String subject, String clientId, String scope) {
        try {
            // Create access token claims
            Instant now = Instant.now();
            Instant expiry = now.plusSeconds(300); // 5 minutes
            
            JWTClaimsSet accessTokenClaims = new JWTClaimsSet.Builder()
                    .issuer("https://auth.example.com")
                    .subject(subject)
                    .audience("https://api.example.com")
                    .expirationTime(Date.from(expiry))
                    .issueTime(Date.from(now))
                    .jwtID(generateJti())
                    .claim("client_id", clientId)
                    .claim("scope", scope)
                    .build();
            
            // Create DPoP-bound access token
            String accessToken = dpopTokenBindingService.createDPoPBoundAccessToken(accessTokenClaims, dpopKey);
            
            // Create refresh token (also DPoP-bound)
            JWTClaimsSet refreshTokenClaims = new JWTClaimsSet.Builder()
                    .issuer("https://auth.example.com")
                    .subject(subject)
                    .audience("https://auth.example.com")
                    .expirationTime(Date.from(now.plusSeconds(28800))) // 8 hours
                    .issueTime(Date.from(now))
                    .jwtID(generateJti())
                    .claim("client_id", clientId)
                    .claim("token_type", "refresh_token")
                    .build();
            
            String refreshToken = dpopTokenBindingService.createDPoPBoundAccessToken(refreshTokenClaims, dpopKey);
            
            // Create response
            Map<String, Object> tokenResponse = new HashMap<>();
            tokenResponse.put("access_token", accessToken);
            tokenResponse.put("token_type", "DPoP");
            tokenResponse.put("expires_in", 300);
            tokenResponse.put("refresh_token", refreshToken);
            tokenResponse.put("scope", scope);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(tokenResponse);
            
        } catch (Exception e) {
            return createErrorResponse("server_error", "Failed to create tokens");
        }
    }
    
    private JWK extractDPoPKeyFromProof(String dpopProof) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(dpopProof);
            return signedJWT.getHeader().getJWK();
        } catch (Exception e) {
            throw new InvalidDPoPProofException("Failed to extract DPoP key from proof", e);
        }
    }
    
    private String constructTokenEndpointUrl(HttpServletRequest request) {
        StringBuilder url = new StringBuilder();
        
        String scheme = request.getHeader("X-Forwarded-Proto");
        if (scheme == null) {
            scheme = request.getScheme();
        }
        
        String host = request.getHeader("X-Forwarded-Host");
        if (host == null) {
            host = request.getServerName();
        }
        
        url.append(scheme).append("://").append(host);
        
        int port = request.getServerPort();
        if (("http".equals(scheme) && port != 80) || ("https".equals(scheme) && port != 443)) {
            url.append(":").append(port);
        }
        
        url.append("/oauth2/token");
        
        return url.toString();
    }
    
    private String generateJti() {
        java.security.SecureRandom secureRandom = new java.security.SecureRandom();
        byte[] jtiBytes = new byte[16];
        secureRandom.nextBytes(jtiBytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(jtiBytes);
    }
    
    private ResponseEntity<Map<String, Object>> createErrorResponse(String errorCode, String errorDescription) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", errorCode);
        errorResponse.put("error_description", errorDescription);
        
        HttpStatus status;
        switch (errorCode) {
            case "invalid_client":
                status = HttpStatus.UNAUTHORIZED;
                break;
            case "invalid_grant":
            case "unsupported_grant_type":
                status = HttpStatus.BAD_REQUEST;
                break;
            case "server_error":
                status = HttpStatus.INTERNAL_SERVER_ERROR;
                break;
            default:
                status = HttpStatus.BAD_REQUEST;
        }
        
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }
    
    private ResponseEntity<Map<String, Object>> createDPoPErrorResponse(InvalidDPoPProofException e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", e.getErrorCode());
        errorResponse.put("error_description", e.getErrorDescription());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .header("WWW-Authenticate", "DPoP error=\"" + e.getErrorCode() + "\"")
                .body(errorResponse);
    }
}