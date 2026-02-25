package com.loanmanagement.security.keycloak.fapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * FAPI 2.0 Security Profile compliance validator
 * Validates requests according to FAPI 2.0 specification
 */
@Component
public class FapiComplianceValidator {

    private static final Logger logger = LoggerFactory.getLogger(FapiComplianceValidator.class);
    
    private static final String FAPI_INTERACTION_ID_HEADER = "x-fapi-interaction-id";
    private static final String FAPI_AUTH_DATE_HEADER = "x-fapi-auth-date";
    private static final String FAPI_CUSTOMER_IP_HEADER = "x-fapi-customer-ip-address";
    private static final String FAPI_CUSTOMER_LAST_LOGGED_TIME_HEADER = "x-fapi-customer-last-logged-time";
    
    private static final long MAX_TOKEN_LIFETIME_SECONDS = 3600; // 1 hour for FAPI
    private static final long MAX_AUTH_AGE_SECONDS = 7200; // 2 hours
    
    /**
     * Validates FAPI 2.0 compliance for incoming requests
     */
    public FapiComplianceResult validateRequest(HttpServletRequest request) {
        try {
            // Check required HTTPS
            if (!isSecureRequest(request)) {
                return FapiComplianceResult.violation("FAPI requires HTTPS");
            }
            
            // Validate FAPI interaction ID
            String interactionId = request.getHeader(FAPI_INTERACTION_ID_HEADER);
            if (interactionId == null || !isValidUUID(interactionId)) {
                // Generate interaction ID if missing (for internal requests)
                interactionId = UUID.randomUUID().toString();
                logger.debug("Generated FAPI interaction ID: {}", interactionId);
            }
            
            // Validate auth date if present
            String authDate = request.getHeader(FAPI_AUTH_DATE_HEADER);
            if (authDate != null && !isValidAuthDate(authDate)) {
                return FapiComplianceResult.violation("Invalid x-fapi-auth-date format");
            }
            
            // Validate customer IP if present
            String customerIp = request.getHeader(FAPI_CUSTOMER_IP_HEADER);
            if (customerIp != null && !isValidIpAddress(customerIp)) {
                return FapiComplianceResult.violation("Invalid x-fapi-customer-ip-address format");
            }
            
            // Check for dangerous HTTP methods
            String method = request.getMethod();
            if ("TRACE".equals(method) || "TRACK".equals(method)) {
                return FapiComplianceResult.violation("HTTP method not allowed in FAPI");
            }
            
            logger.debug("FAPI compliance validation passed for interaction: {}", interactionId);
            
            return FapiComplianceResult.compliant(interactionId);
            
        } catch (Exception e) {
            logger.error("FAPI compliance validation error", e);
            return FapiComplianceResult.violation("FAPI compliance validation failed");
        }
    }
    
    /**
     * Validates token lifetime according to FAPI requirements
     */
    public FapiComplianceResult validateTokenLifetime(Instant issuedAt, Instant expiresAt) {
        if (issuedAt == null || expiresAt == null) {
            return FapiComplianceResult.violation("Token must have valid issued and expiry times");
        }
        
        Duration lifetime = Duration.between(issuedAt, expiresAt);
        if (lifetime.getSeconds() > MAX_TOKEN_LIFETIME_SECONDS) {
            return FapiComplianceResult.violation(
                String.format("Token lifetime (%d seconds) exceeds FAPI maximum (%d seconds)", 
                    lifetime.getSeconds(), MAX_TOKEN_LIFETIME_SECONDS));
        }
        
        return FapiComplianceResult.compliant();
    }
    
    /**
     * Validates authentication age according to FAPI requirements
     */
    public FapiComplianceResult validateAuthAge(Instant authTime) {
        if (authTime == null) {
            return FapiComplianceResult.compliant(); // Not required for all flows
        }
        
        Duration authAge = Duration.between(authTime, Instant.now());
        if (authAge.getSeconds() > MAX_AUTH_AGE_SECONDS) {
            return FapiComplianceResult.violation(
                String.format("Authentication is too old (%d seconds)", authAge.getSeconds()));
        }
        
        return FapiComplianceResult.compliant();
    }
    
    /**
     * Validates PKCE parameters for authorization requests
     */
    public FapiComplianceResult validatePKCE(String codeChallenge, String codeChallengeMethod) {
        if (codeChallenge == null || codeChallenge.trim().isEmpty()) {
            return FapiComplianceResult.violation("FAPI requires PKCE code_challenge");
        }
        
        if (!"S256".equals(codeChallengeMethod)) {
            return FapiComplianceResult.violation("FAPI requires PKCE code_challenge_method=S256");
        }
        
        // Validate code challenge format (Base64url encoded)
        if (!isValidBase64Url(codeChallenge)) {
            return FapiComplianceResult.violation("Invalid PKCE code_challenge format");
        }
        
        return FapiComplianceResult.compliant();
    }
    
    /**
     * Validates client assertion for private_key_jwt authentication
     */
    public FapiComplianceResult validateClientAssertion(String clientAssertionType, String clientAssertion) {
        if (!"urn:ietf:params:oauth:client-assertion-type:jwt-bearer".equals(clientAssertionType)) {
            return FapiComplianceResult.violation("FAPI requires private_key_jwt client authentication");
        }
        
        if (clientAssertion == null || clientAssertion.trim().isEmpty()) {
            return FapiComplianceResult.violation("Client assertion is required for FAPI");
        }
        
        // Basic JWT format validation
        String[] parts = clientAssertion.split("\\.");
        if (parts.length != 3) {
            return FapiComplianceResult.violation("Invalid client assertion JWT format");
        }
        
        return FapiComplianceResult.compliant();
    }
    
    private boolean isSecureRequest(HttpServletRequest request) {
        return request.isSecure() || 
               "https".equals(request.getHeader("x-forwarded-proto")) ||
               "on".equals(request.getHeader("x-forwarded-ssl"));
    }
    
    private boolean isValidUUID(String uuid) {
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    private boolean isValidAuthDate(String authDate) {
        try {
            // Validate RFC 3339 format (basic check)
            return authDate.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*");
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isValidIpAddress(String ip) {
        // Basic IP address validation
        return ip.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$") || 
               ip.matches("^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$");
    }
    
    private boolean isValidBase64Url(String value) {
        return value.matches("^[A-Za-z0-9_-]+$");
    }
    
    /**
     * FAPI compliance validation result
     */
    public static class FapiComplianceResult {
        private final boolean compliant;
        private final String violationMessage;
        private final String interactionId;
        
        private FapiComplianceResult(boolean compliant, String violationMessage, String interactionId) {
            this.compliant = compliant;
            this.violationMessage = violationMessage;
            this.interactionId = interactionId;
        }
        
        public static FapiComplianceResult compliant() {
            return new FapiComplianceResult(true, null, null);
        }
        
        public static FapiComplianceResult compliant(String interactionId) {
            return new FapiComplianceResult(true, null, interactionId);
        }
        
        public static FapiComplianceResult violation(String message) {
            return new FapiComplianceResult(false, message, null);
        }
        
        public boolean isCompliant() { return compliant; }
        public String getViolationMessage() { return violationMessage; }
        public String getInteractionId() { return interactionId; }
    }
}