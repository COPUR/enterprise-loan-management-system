package com.bank.loanmanagement.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * FAPI-compliant request validation filter
 * Validates FAPI-specific headers and request structure
 * Following OpenID Foundation FAPI specifications
 */
@Component
@Slf4j
public class FAPIRequestValidationFilter extends OncePerRequestFilter {

    private static final String FAPI_INTERACTION_ID_HEADER = "X-FAPI-Interaction-ID";
    private static final String FAPI_AUTH_DATE_HEADER = "X-FAPI-Auth-Date";
    private static final String FAPI_CUSTOMER_IP_HEADER = "X-FAPI-Customer-IP-Address";
    
    // UUID pattern for interaction ID validation
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );
    
    // IP address pattern (IPv4 and IPv6)
    private static final Pattern IP_PATTERN = Pattern.compile(
        "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$|" +
        "^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // Validate FAPI-specific headers for protected endpoints
            if (isFAPIProtectedEndpoint(request)) {
                ValidationResult validationResult = validateFAPIHeaders(request);
                
                if (!validationResult.isValid()) {
                    handleValidationError(response, validationResult);
                    return;
                }
            }
            
            // Validate request structure
            ValidationResult structureValidation = validateRequestStructure(request);
            if (!structureValidation.isValid()) {
                handleValidationError(response, structureValidation);
                return;
            }
            
            // Add interaction ID if not present
            addInteractionIdIfMissing(response);
            
            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            log.error("Error in FAPI request validation filter", e);
            handleInternalError(response);
        }
    }

    private boolean isFAPIProtectedEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // FAPI protection applies to financial APIs
        return path.startsWith("/api/v1/fapi/") ||
               path.startsWith("/api/v1/ai/") ||
               path.startsWith("/api/v1/loans/") ||
               path.startsWith("/api/v1/payments/");
    }

    private ValidationResult validateFAPIHeaders(HttpServletRequest request) {
        // Validate X-FAPI-Interaction-ID
        String interactionId = request.getHeader(FAPI_INTERACTION_ID_HEADER);
        if (interactionId != null && !UUID_PATTERN.matcher(interactionId).matches()) {
            return ValidationResult.invalid("Invalid X-FAPI-Interaction-ID format. Must be a valid UUID.");
        }
        
        // Validate X-FAPI-Auth-Date
        String authDate = request.getHeader(FAPI_AUTH_DATE_HEADER);
        if (authDate != null) {
            try {
                Instant.parse(authDate);
                
                // Check if auth date is too old (more than 1 hour)
                Instant authInstant = Instant.parse(authDate);
                if (authInstant.isBefore(Instant.now().minusSeconds(3600))) {
                    return ValidationResult.invalid("X-FAPI-Auth-Date is too old. Maximum age is 1 hour.");
                }
                
                // Check if auth date is in the future
                if (authInstant.isAfter(Instant.now().plusSeconds(300))) { // 5 minutes tolerance
                    return ValidationResult.invalid("X-FAPI-Auth-Date cannot be in the future.");
                }
                
            } catch (DateTimeParseException e) {
                return ValidationResult.invalid("Invalid X-FAPI-Auth-Date format. Must be ISO 8601 format.");
            }
        }
        
        // Validate X-FAPI-Customer-IP-Address
        String customerIp = request.getHeader(FAPI_CUSTOMER_IP_HEADER);
        if (customerIp != null && !IP_PATTERN.matcher(customerIp).matches()) {
            return ValidationResult.invalid("Invalid X-FAPI-Customer-IP-Address format.");
        }
        
        return ValidationResult.valid();
    }

    private ValidationResult validateRequestStructure(HttpServletRequest request) {
        // Validate Content-Type for POST/PUT requests
        if ("POST".equals(request.getMethod()) || "PUT".equals(request.getMethod())) {
            String contentType = request.getContentType();
            if (contentType == null || 
                (!contentType.startsWith("application/json") && 
                 !contentType.startsWith("application/x-www-form-urlencoded"))) {
                return ValidationResult.invalid("Invalid Content-Type. Must be application/json or application/x-www-form-urlencoded.");
            }
        }
        
        // Validate Authorization header format
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && !authHeader.startsWith("Bearer ")) {
            return ValidationResult.invalid("Invalid Authorization header format. Must use Bearer token.");
        }
        
        // Validate request size (prevent DoS attacks)
        int contentLength = request.getContentLength();
        if (contentLength > 1024 * 1024) { // 1MB limit
            return ValidationResult.invalid("Request too large. Maximum size is 1MB.");
        }
        
        return ValidationResult.valid();
    }

    private void addInteractionIdIfMissing(HttpServletResponse response) {
        if (response.getHeader(FAPI_INTERACTION_ID_HEADER) == null) {
            response.setHeader(FAPI_INTERACTION_ID_HEADER, java.util.UUID.randomUUID().toString());
        }
    }

    private void handleValidationError(HttpServletResponse response, ValidationResult validationResult) 
            throws IOException {
        
        log.warn("FAPI validation failed: {}", validationResult.getErrorMessage());
        
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType("application/json");
        response.setHeader(FAPI_INTERACTION_ID_HEADER, java.util.UUID.randomUUID().toString());
        
        String errorResponse = """
            {
                "error": "invalid_request",
                "error_description": "%s",
                "fapi_compliance": true
            }
            """.formatted(validationResult.getErrorMessage());
        
        response.getWriter().write(errorResponse);
        response.getWriter().flush();
    }

    private void handleInternalError(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.setContentType("application/json");
        response.setHeader(FAPI_INTERACTION_ID_HEADER, java.util.UUID.randomUUID().toString());
        
        String errorResponse = """
            {
                "error": "server_error",
                "error_description": "Internal server error during request validation",
                "fapi_compliance": true
            }
            """;
        
        response.getWriter().write(errorResponse);
        response.getWriter().flush();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Skip validation for public endpoints
        return path.startsWith("/actuator/") || 
               path.startsWith("/api/v1/public/") ||
               path.endsWith(".css") ||
               path.endsWith(".js") ||
               path.endsWith(".ico") ||
               path.equals("/");
    }

    // Inner class for validation results
    private static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}