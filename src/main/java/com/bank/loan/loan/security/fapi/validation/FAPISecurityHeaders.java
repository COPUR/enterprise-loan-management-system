package com.bank.loan.loan.security.fapi.validation;

import org.springframework.util.StringUtils;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * FAPI Security Headers Validation
 * 
 * Validates FAPI 2.0 required security headers according to specification.
 */
public class FAPISecurityHeaders {
    
    private static final Pattern FAPI_INTERACTION_ID_PATTERN = 
        Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    
    private static final Pattern IP_ADDRESS_PATTERN = 
        Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
    
    /**
     * Validates FAPI security headers
     * 
     * @param fiapiInteractionId X-FAPI-Interaction-ID header
     * @param fapiAuthDate X-FAPI-Auth-Date header
     * @param customerIpAddress X-FAPI-Customer-IP-Address header
     * @throws FAPISecurityException if validation fails
     */
    public static void validateHeaders(String fiapiInteractionId, String fapiAuthDate, String customerIpAddress) {
        validateFAPIInteractionId(fiapiInteractionId);
        validateFAPIAuthDate(fapiAuthDate);
        validateCustomerIpAddress(customerIpAddress);
    }
    
    /**
     * Validates X-FAPI-Interaction-ID header
     * Must be a valid UUID format
     */
    public static void validateFAPIInteractionId(String fiapiInteractionId) {
        if (!StringUtils.hasText(fiapiInteractionId)) {
            throw new FAPISecurityException("X-FAPI-Interaction-ID header is required");
        }
        
        if (!FAPI_INTERACTION_ID_PATTERN.matcher(fiapiInteractionId).matches()) {
            throw new FAPISecurityException("X-FAPI-Interaction-ID must be a valid UUID format");
        }
    }
    
    /**
     * Validates X-FAPI-Auth-Date header
     * Must be a valid RFC 1123 date format
     */
    public static void validateFAPIAuthDate(String fapiAuthDate) {
        if (!StringUtils.hasText(fapiAuthDate)) {
            // Optional header - don't validate if not present
            return;
        }
        
        try {
            ZonedDateTime.parse(fapiAuthDate, DateTimeFormatter.RFC_1123_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new FAPISecurityException("X-FAPI-Auth-Date must be in RFC 1123 format");
        }
    }
    
    /**
     * Validates X-FAPI-Customer-IP-Address header
     * Must be a valid IPv4 address
     */
    public static void validateCustomerIpAddress(String customerIpAddress) {
        if (!StringUtils.hasText(customerIpAddress)) {
            // Optional header - don't validate if not present
            return;
        }
        
        if (!IP_ADDRESS_PATTERN.matcher(customerIpAddress).matches()) {
            throw new FAPISecurityException("X-FAPI-Customer-IP-Address must be a valid IPv4 address");
        }
    }
    
    /**
     * Generates a new FAPI Interaction ID
     */
    public static String generateFAPIInteractionId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Generates current FAPI Auth Date
     */
    public static String generateFAPIAuthDate() {
        return ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }
    
    /**
     * FAPI Security Exception
     */
    public static class FAPISecurityException extends RuntimeException {
        public FAPISecurityException(String message) {
            super(message);
        }
        
        public FAPISecurityException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}