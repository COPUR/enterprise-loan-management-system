package com.bank.loanmanagement.loan.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Standardized error response structure for banking APIs
 * Compliant with Berlin Group PSD2 error response format
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BankingErrorResponse {
    
    /**
     * Unique error code identifying the specific error type
     */
    private String errorCode;
    
    /**
     * Human-readable error message in the requested language
     */
    private String message;
    
    /**
     * Additional error details for debugging (not exposed in production)
     */
    private String details;
    
    /**
     * HTTP status code
     */
    private int httpStatus;
    
    /**
     * Timestamp when the error occurred (Unix timestamp)
     */
    private long timestamp;
    
    /**
     * Language/locale of the error message
     */
    private String locale;
    
    /**
     * Request trace ID for correlation
     */
    private String traceId;
    
    /**
     * Span ID for distributed tracing
     */
    private String spanId;
    
    /**
     * List of validation errors (for validation failures)
     */
    private List<ValidationError> validationErrors;
    
    /**
     * Additional context information
     */
    private Map<String, Object> context;
    
    /**
     * Berlin Group PSD2 specific error information
     */
    private PSD2ErrorInfo psd2Error;
    
    /**
     * AI-related error information
     */
    private AIErrorInfo aiError;
    
    @Data
    @Builder
    public static class ValidationError {
        private String field;
        private String code;
        private String message;
        private Object rejectedValue;
    }
    
    @Builder
    @Data
    public static class PSD2ErrorInfo {
        private String tppId;
        private String aspspId;
        private String paymentId;
        private String consentId;
        private String resourceId;
    }
    
    @Builder
    @Data
    public static class AIErrorInfo {
        private String modelName;
        private String modelVersion;
        private String confidence;
        private String reasoning;
        private long processingTimeMs;
    }
    
    /**
     * Create a simple error response
     */
    public static BankingErrorResponse of(String errorCode, String message, int httpStatus) {
        return BankingErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .httpStatus(httpStatus)
                .timestamp(Instant.now().toEpochMilli())
                .build();
    }
    
    /**
     * Create error response with trace information
     */
    public static BankingErrorResponse withTrace(String errorCode, String message, int httpStatus, 
                                               String traceId, String spanId) {
        return BankingErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .httpStatus(httpStatus)
                .timestamp(Instant.now().toEpochMilli())
                .traceId(traceId)
                .spanId(spanId)
                .build();
    }
}