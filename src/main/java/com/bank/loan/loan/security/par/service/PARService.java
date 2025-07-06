package com.bank.loan.loan.security.par.service;

import com.bank.loan.loan.security.par.exception.PARValidationException;
import com.bank.loan.loan.security.par.model.PARRequest;
import com.bank.loan.loan.security.par.model.PARResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class PARService {
    
    private static final String PAR_REQUEST_PREFIX = "par:request:";
    private static final int DEFAULT_EXPIRATION_SECONDS = 300; // 5 minutes
    private static final int MIN_EXPIRATION_SECONDS = 60;      // 1 minute
    private static final int MAX_EXPIRATION_SECONDS = 600;     // 10 minutes
    private static final int REQUEST_URI_LENGTH = 32;
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final SecureRandom secureRandom;
    
    public PARService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.secureRandom = new SecureRandom();
    }
    
    public PARResponse createPARRequest(Map<String, String> parameters) {
        try {
            // Validate the PAR request parameters
            validatePARParameters(parameters);
            
            // Create PAR request object
            PARRequest parRequest = createPARRequestFromParameters(parameters);
            
            // Generate request URI
            String requestUri = generateRequestUri();
            
            // Store PAR request in Redis
            String cacheKey = PAR_REQUEST_PREFIX + requestUri;
            redisTemplate.opsForValue().set(cacheKey, parRequest, DEFAULT_EXPIRATION_SECONDS, TimeUnit.SECONDS);
            
            // Return PAR response
            return new PARResponse("urn:ietf:params:oauth:request_uri:" + requestUri, DEFAULT_EXPIRATION_SECONDS);
            
        } catch (Exception e) {
            if (e instanceof PARValidationException) {
                throw e;
            }
            throw new PARValidationException("Failed to create PAR request", e);
        }
    }
    
    public PARRequest retrievePARRequest(String requestUri) {
        try {
            // Extract URI identifier
            String uriIdentifier = extractUriIdentifier(requestUri);
            
            // Retrieve PAR request from Redis
            String cacheKey = PAR_REQUEST_PREFIX + uriIdentifier;
            Object cachedRequest = redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedRequest == null) {
                throw new PARValidationException("invalid_request_uri", "Request URI not found or expired");
            }
            
            if (!(cachedRequest instanceof PARRequest)) {
                throw new PARValidationException("invalid_request_uri", "Invalid request URI format");
            }
            
            return (PARRequest) cachedRequest;
            
        } catch (Exception e) {
            if (e instanceof PARValidationException) {
                throw e;
            }
            throw new PARValidationException("Failed to retrieve PAR request", e);
        }
    }
    
    public void consumePARRequest(String requestUri) {
        try {
            // Extract URI identifier
            String uriIdentifier = extractUriIdentifier(requestUri);
            
            // Remove PAR request from Redis (one-time use)
            String cacheKey = PAR_REQUEST_PREFIX + uriIdentifier;
            Boolean deleted = redisTemplate.delete(cacheKey);
            
            if (!Boolean.TRUE.equals(deleted)) {
                throw new PARValidationException("invalid_request_uri", "Request URI already used or expired");
            }
            
        } catch (Exception e) {
            if (e instanceof PARValidationException) {
                throw e;
            }
            throw new PARValidationException("Failed to consume PAR request", e);
        }
    }
    
    private void validatePARParameters(Map<String, String> parameters) {
        // Validate required parameters
        validateRequiredParameter(parameters, "client_id", "Missing client_id parameter");
        validateRequiredParameter(parameters, "redirect_uri", "Missing redirect_uri parameter");
        validateRequiredParameter(parameters, "response_type", "Missing response_type parameter");
        
        // Validate response_type using FAPI 2.0 compliance validator
        String responseType = parameters.get("response_type");
        if (!"code".equals(responseType)) {
            throw new PARValidationException("unsupported_response_type", 
                "Only 'code' response type is supported in FAPI 2.0. " +
                "Hybrid flows (code id_token, code token, code id_token token) and " +
                "implicit flows (token, id_token) are not allowed.");
        }
        
        // Validate PKCE parameters
        String codeChallenge = parameters.get("code_challenge");
        String codeChallengeMethod = parameters.get("code_challenge_method");
        
        if (codeChallenge == null || codeChallenge.trim().isEmpty()) {
            throw new PARValidationException("invalid_request", "code_challenge is required");
        }
        
        if (codeChallengeMethod == null || codeChallengeMethod.trim().isEmpty()) {
            throw new PARValidationException("invalid_request", "code_challenge_method is required");
        }
        
        if (!"S256".equals(codeChallengeMethod)) {
            throw new PARValidationException("invalid_request", "Only S256 code_challenge_method is supported");
        }
        
        // Validate redirect URI format
        String redirectUri = parameters.get("redirect_uri");
        if (!isValidRedirectUri(redirectUri)) {
            throw new PARValidationException("invalid_redirect_uri", "Invalid redirect URI format");
        }
        
        // Validate scope parameter
        String scope = parameters.get("scope");
        if (scope != null && !isValidScope(scope)) {
            throw new PARValidationException("invalid_scope", "Invalid scope parameter");
        }
    }
    
    private void validateRequiredParameter(Map<String, String> parameters, String paramName, String errorMessage) {
        String value = parameters.get(paramName);
        if (value == null || value.trim().isEmpty()) {
            throw new PARValidationException("invalid_request", errorMessage);
        }
    }
    
    private boolean isValidRedirectUri(String redirectUri) {
        try {
            java.net.URI uri = java.net.URI.create(redirectUri);
            String scheme = uri.getScheme();
            
            // Must be HTTPS for production or HTTP for localhost in development
            if ("https".equals(scheme)) {
                return true;
            }
            
            if ("http".equals(scheme)) {
                String host = uri.getHost();
                return "localhost".equals(host) || "127.0.0.1".equals(host);
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isValidScope(String scope) {
        // Validate scope contains only allowed characters and values
        if (scope.trim().isEmpty()) {
            return false;
        }
        
        String[] scopes = scope.split("\\s+");
        for (String s : scopes) {
            if (!isAllowedScope(s)) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean isAllowedScope(String scope) {
        // Define allowed scopes for the banking system
        return "openid".equals(scope) || 
               "loans".equals(scope) || 
               "payments".equals(scope) || 
               "accounts".equals(scope) ||
               "profile".equals(scope) ||
               "banking-scope".equals(scope) ||
               "banking-loans".equals(scope) ||
               "banking-payments".equals(scope);
    }
    
    private PARRequest createPARRequestFromParameters(Map<String, String> parameters) {
        String clientId = parameters.get("client_id");
        String redirectUri = parameters.get("redirect_uri");
        String responseType = parameters.get("response_type");
        String scope = parameters.get("scope");
        String state = parameters.get("state");
        String codeChallenge = parameters.get("code_challenge");
        String codeChallengeMethod = parameters.get("code_challenge_method");
        String dpopJkt = parameters.get("dpop_jkt");
        
        // Extract additional parameters
        Map<String, Object> additionalParams = parameters.entrySet().stream()
                .filter(entry -> !isStandardParameter(entry.getKey()))
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> (Object) entry.getValue()
                ));
        
        return new PARRequest(clientId, redirectUri, responseType, scope, state, 
                             codeChallenge, codeChallengeMethod, dpopJkt, additionalParams);
    }
    
    private boolean isStandardParameter(String paramName) {
        return "client_id".equals(paramName) ||
               "redirect_uri".equals(paramName) ||
               "response_type".equals(paramName) ||
               "scope".equals(paramName) ||
               "state".equals(paramName) ||
               "code_challenge".equals(paramName) ||
               "code_challenge_method".equals(paramName) ||
               "dpop_jkt".equals(paramName);
    }
    
    private String generateRequestUri() {
        byte[] randomBytes = new byte[REQUEST_URI_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
    
    private String extractUriIdentifier(String requestUri) {
        if (requestUri == null || requestUri.trim().isEmpty()) {
            throw new PARValidationException("invalid_request_uri", "Request URI is empty");
        }
        
        String prefix = "urn:ietf:params:oauth:request_uri:";
        if (!requestUri.startsWith(prefix)) {
            throw new PARValidationException("invalid_request_uri", "Invalid request URI format");
        }
        
        String identifier = requestUri.substring(prefix.length());
        if (identifier.isEmpty()) {
            throw new PARValidationException("invalid_request_uri", "Empty request URI identifier");
        }
        
        return identifier;
    }
}