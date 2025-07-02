package com.bank.loanmanagement.loan.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standard error response DTO for API error handling
 * Follows RFC 7807 Problem Details for HTTP APIs
 */
@Schema(
    description = "Standard error response following RFC 7807 Problem Details for HTTP APIs",
    example = """
    {
      "error": "VALIDATION_ERROR",
      "message": "Invalid request parameters",
      "details": ["Credit score must be between 300 and 850"],
      "timestamp": "2024-12-20T10:30:00Z",
      "path": "/api/v1/loans/recommendations",
      "traceId": "abc123def456"
    }
    """
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponseDto(
        @Schema(description = "Error code identifying the type of error", example = "VALIDATION_ERROR")
        String error,
        
        @Schema(description = "Human-readable error message", example = "Invalid request parameters")
        String message,
        
        @Schema(description = "Additional error details or validation failures")
        List<String> details,
        
        @Schema(description = "Timestamp when the error occurred", example = "2024-12-20T10:30:00Z")
        LocalDateTime timestamp,
        
        @Schema(description = "API path where the error occurred", example = "/api/v1/loans/recommendations")
        String path,
        
        @Schema(description = "Trace ID for debugging and support", example = "abc123def456")
        String traceId
) {
    
    public static ErrorResponseDto of(String error, String message) {
        return new ErrorResponseDto(
                error,
                message,
                null,
                LocalDateTime.now(),
                null,
                null
        );
    }
    
    public static ErrorResponseDto withDetails(String error, String message, List<String> details) {
        return new ErrorResponseDto(
                error,
                message,
                details,
                LocalDateTime.now(),
                null,
                null
        );
    }
}