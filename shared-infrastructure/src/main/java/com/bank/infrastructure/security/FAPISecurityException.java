package com.bank.infrastructure.security;

/**
 * Exception thrown when FAPI security validation fails
 * 
 * Used for all FAPI 1.0 Advanced security requirement violations including:
 * - Invalid headers (X-FAPI-Interaction-ID, X-FAPI-Auth-Date, X-FAPI-Customer-IP-Address)
 * - Request signature validation failures
 * - TLS requirement violations
 * - Authentication and authorization failures
 */
public class FAPISecurityException extends RuntimeException {
    
    private final String errorCode;
    private final String fapiInteractionId;
    
    public FAPISecurityException(String message) {
        super(message);
        this.errorCode = "invalid_request";
        this.fapiInteractionId = null;
    }
    
    public FAPISecurityException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.fapiInteractionId = null;
    }
    
    public FAPISecurityException(String message, String errorCode, String fapiInteractionId) {
        super(message);
        this.errorCode = errorCode;
        this.fapiInteractionId = fapiInteractionId;
    }
    
    public FAPISecurityException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "invalid_request";
        this.fapiInteractionId = null;
    }
    
    public FAPISecurityException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.fapiInteractionId = null;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getFapiInteractionId() {
        return fapiInteractionId;
    }
}