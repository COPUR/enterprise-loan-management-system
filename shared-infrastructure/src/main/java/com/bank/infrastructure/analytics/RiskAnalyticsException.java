package com.bank.infrastructure.analytics;

/**
 * Exception thrown when risk analytics operations fail
 * 
 * Used for all risk analytics related errors including:
 * - Database query failures
 * - Calculation errors
 * - Data validation failures
 * - External service integration failures
 */
public class RiskAnalyticsException extends RuntimeException {
    
    private final String errorCode;
    
    public RiskAnalyticsException(String message) {
        super(message);
        this.errorCode = "ANALYTICS_ERROR";
    }
    
    public RiskAnalyticsException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public RiskAnalyticsException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "ANALYTICS_ERROR";
    }
    
    public RiskAnalyticsException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}