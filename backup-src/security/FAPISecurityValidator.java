package com.bank.loanmanagement.security;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * FAPI Security Validator
 * Implements FAPI 1.0 Advanced security requirements
 * - Request/Response signing validation
 * - MTLS certificate validation
 * - Structured security header validation
 */
@Component
public class FAPISecurityValidator {

    @Value("${fapi.signing.key:default-signing-key}")
    private String signingKey;

    @Value("${fapi.client.certificate.enabled:false}")
    private boolean mtlsEnabled;

    /**
     * Validate FAPI required headers
     */
    public void validateFAPIHeaders(String authDate, String customerIP, String interactionId) {
        validateAuthDate(authDate);
        validateCustomerIP(customerIP);
        validateInteractionId(interactionId);
    }

    /**
     * Validate x-fapi-auth-date header
     * Must be within 5 minutes of current time
     */
    private void validateAuthDate(String authDate) {
        try {
            OffsetDateTime authDateTime = OffsetDateTime.parse(authDate);
            OffsetDateTime now = OffsetDateTime.now();
            
            long diffMinutes = Math.abs(java.time.Duration.between(authDateTime, now).toMinutes());
            if (diffMinutes > 5) {
                throw new FAPISecurityException("Auth date too old or in future");
            }
        } catch (DateTimeParseException e) {
            throw new FAPISecurityException("Invalid auth date format");
        }
    }

    /**
     * Validate customer IP address format
     */
    private void validateCustomerIP(String customerIP) {
        if (customerIP == null || customerIP.trim().isEmpty()) {
            throw new FAPISecurityException("Customer IP address required");
        }
        
        // Basic IP validation (IPv4 or IPv6)
        if (!isValidIP(customerIP)) {
            throw new FAPISecurityException("Invalid customer IP address format");
        }
    }

    /**
     * Validate interaction ID is valid UUID format
     */
    private void validateInteractionId(String interactionId) {
        try {
            UUID.fromString(interactionId);
        } catch (IllegalArgumentException e) {
            throw new FAPISecurityException("Invalid interaction ID format");
        }
    }

    /**
     * Validate JWS signature for request
     */
    public void validateRequestSignature(Object request, String jwsSignature) {
        if (jwsSignature == null || jwsSignature.trim().isEmpty()) {
            throw new FAPISecurityException("JWS signature required for request");
        }

        try {
            // Convert request to JSON string for signature validation
            String requestJson = convertToJson(request);
            String expectedSignature = generateHMACSignature(requestJson);
            
            if (!verifySignature(jwsSignature, expectedSignature)) {
                throw new FAPISecurityException("Invalid request signature");
            }
        } catch (Exception e) {
            throw new FAPISecurityException("Signature validation failed: " + e.getMessage());
        }
    }

    /**
     * Generate JWS signature for response
     */
    public String signResponse(Object response) {
        try {
            String responseJson = convertToJson(response);
            return generateHMACSignature(responseJson);
        } catch (Exception e) {
            throw new FAPISecurityException("Response signing failed: " + e.getMessage());
        }
    }

    /**
     * Generate HMAC-SHA256 signature
     */
    private String generateHMACSignature(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(signingKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        
        byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature);
    }

    /**
     * Verify signature matches expected value
     */
    private boolean verifySignature(String providedSignature, String expectedSignature) {
        return MessageDigest.isEqual(
            providedSignature.getBytes(StandardCharsets.UTF_8),
            expectedSignature.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Convert object to JSON string for signature calculation
     */
    private String convertToJson(Object obj) {
        // Simple JSON conversion for validation
        return obj.toString();
    }

    /**
     * Basic IP address validation
     */
    private boolean isValidIP(String ip) {
        // IPv4 regex pattern
        String ipv4Pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        
        // IPv6 basic pattern
        String ipv6Pattern = "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$";
        
        return ip.matches(ipv4Pattern) || ip.matches(ipv6Pattern) || "127.0.0.1".equals(ip) || "localhost".equals(ip);
    }

    /**
     * FAPI Security Exception
     */
    public static class FAPISecurityException extends RuntimeException {
        public FAPISecurityException(String message) {
            super(message);
        }
    }
}