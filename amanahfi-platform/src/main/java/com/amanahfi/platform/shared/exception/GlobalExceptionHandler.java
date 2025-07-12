package com.amanahfi.platform.shared.exception;

import com.amanahfi.platform.shared.i18n.MessageService;
import com.amanahfi.platform.tenant.application.TenantContextManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for AmanahFi platform
 * Provides consistent error responses with internationalization support
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {
    
    private final MessageService messageService;
    private final TenantContextManager tenantContextManager;
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        
        log.warn("Business exception: {} - {}", ex.getErrorCode(), ex.getTechnicalMessage());
        
        String languageCode = getLanguageFromRequest(request);
        String localizedMessage = ex.getLocalizedMessage(languageCode);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Business Rule Violation")
            .errorCode(ex.getErrorCode())
            .message(localizedMessage)
            .path(request.getRequestURI())
            .correlationId(ex.getCorrelationId())
            .severity(ex.getSeverity())
            .category(ex.getCategory())
            .retryable(ex.isRetryable())
            .details(ex.getErrorContext())
            .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex, HttpServletRequest request) {
        
        log.warn("Validation exception: {} - {}", ex.getErrorCode(), ex.getTechnicalMessage());
        
        String languageCode = getLanguageFromRequest(request);
        String localizedMessage = ex.getLocalizedMessage(languageCode);
        List<String> localizedValidationMessages = ex.getLocalizedValidationMessages(languageCode);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .errorCode(ex.getErrorCode())
            .message(localizedMessage)
            .path(request.getRequestURI())
            .correlationId(ex.getCorrelationId())
            .severity(ex.getSeverity())
            .category(ex.getCategory())
            .retryable(false)
            .details(Map.of(
                "fieldName", ex.getFieldName(),
                "invalidValue", ex.getInvalidValue(),
                "validationErrors", localizedValidationMessages
            ))
            .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(
            SecurityException ex, HttpServletRequest request) {
        
        log.error("Security exception: {} - {} - User: {}, IP: {}", 
            ex.getErrorCode(), ex.getTechnicalMessage(), ex.getUserId(), ex.getClientIp());
        
        String languageCode = getLanguageFromRequest(request);
        String localizedMessage = ex.getLocalizedMessage(languageCode);
        
        // Determine HTTP status based on error code
        HttpStatus status = switch (ex.getErrorCode()) {
            case "AUTHENTICATION_REQUIRED", "INVALID_CREDENTIALS", "SESSION_EXPIRED" -> HttpStatus.UNAUTHORIZED;
            case "ACCESS_DENIED", "INSUFFICIENT_PRIVILEGES" -> HttpStatus.FORBIDDEN;
            case "RATE_LIMIT_EXCEEDED" -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.FORBIDDEN;
        };
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(status.value())
            .error("Security Violation")
            .errorCode(ex.getErrorCode())
            .message(localizedMessage)
            .path(request.getRequestURI())
            .correlationId(ex.getCorrelationId())
            .severity(ex.getSeverity())
            .category(ex.getCategory())
            .retryable(ex.isRetryable())
            .details(sanitizeSecurityDetails(ex.getErrorContext()))
            .build();
        
        return ResponseEntity.status(status).body(errorResponse);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        log.warn("Method argument validation failed: {}", ex.getMessage());
        
        String languageCode = getLanguageFromRequest(request);
        
        List<ValidationException.ValidationError> validationErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(fieldError -> new ValidationException.ValidationError(
                "FIELD_VALIDATION_ERROR",
                fieldError.getField(),
                fieldError.getDefaultMessage(),
                new Object[]{fieldError.getField(), fieldError.getRejectedValue()}
            ))
            .toList();
        
        String message = messageService.getMessage("validation.multiple.errors", languageCode, validationErrors.size());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .errorCode("METHOD_ARGUMENT_NOT_VALID")
            .message(message)
            .path(request.getRequestURI())
            .correlationId(UUID.randomUUID().toString())
            .severity(AmanahFiException.ErrorSeverity.LOW)
            .category(AmanahFiException.ErrorCategory.VALIDATION)
            .retryable(false)
            .details(Map.of("validationErrors", validationErrors))
            .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        
        log.warn("Access denied: {} for path: {}", ex.getMessage(), request.getRequestURI());
        
        String languageCode = getLanguageFromRequest(request);
        String message = messageService.getMessage("security.access.denied", languageCode);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.FORBIDDEN.value())
            .error("Access Denied")
            .errorCode("ACCESS_DENIED")
            .message(message)
            .path(request.getRequestURI())
            .correlationId(UUID.randomUUID().toString())
            .severity(AmanahFiException.ErrorSeverity.MEDIUM)
            .category(AmanahFiException.ErrorCategory.AUTHORIZATION)
            .retryable(false)
            .details(Map.of())
            .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        
        log.warn("Illegal argument exception: {}", ex.getMessage());
        
        String languageCode = getLanguageFromRequest(request);
        String message = messageService.getMessage("validation.illegal.argument", languageCode);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Invalid Argument")
            .errorCode("ILLEGAL_ARGUMENT")
            .message(message)
            .path(request.getRequestURI())
            .correlationId(UUID.randomUUID().toString())
            .severity(AmanahFiException.ErrorSeverity.LOW)
            .category(AmanahFiException.ErrorCategory.VALIDATION)
            .retryable(false)
            .details(Map.of("originalMessage", ex.getMessage()))
            .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        log.error("Unexpected error occurred", ex);
        
        String languageCode = getLanguageFromRequest(request);
        String message = messageService.getMessage("error.internal.server", languageCode);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .errorCode("INTERNAL_SERVER_ERROR")
            .message(message)
            .path(request.getRequestURI())
            .correlationId(UUID.randomUUID().toString())
            .severity(AmanahFiException.ErrorSeverity.HIGH)
            .category(AmanahFiException.ErrorCategory.INFRASTRUCTURE)
            .retryable(true)
            .details(Map.of("exceptionType", ex.getClass().getSimpleName()))
            .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    // Helper methods
    
    private String getLanguageFromRequest(HttpServletRequest request) {
        // Try to get language from various sources
        
        // 1. Accept-Language header
        String acceptLanguage = request.getHeader("Accept-Language");
        if (acceptLanguage != null && !acceptLanguage.isEmpty()) {
            String[] languages = acceptLanguage.split(",");
            for (String lang : languages) {
                String langCode = lang.split(";")[0].split("-")[0].trim();
                if (messageService.isLanguageSupported(langCode)) {
                    return langCode;
                }
            }
        }
        
        // 2. X-Language header
        String xLanguage = request.getHeader("X-Language");
        if (xLanguage != null && messageService.isLanguageSupported(xLanguage)) {
            return xLanguage;
        }
        
        // 3. Query parameter
        String langParam = request.getParameter("lang");
        if (langParam != null && messageService.isLanguageSupported(langParam)) {
            return langParam;
        }
        
        // 4. Tenant context
        if (tenantContextManager.getCurrentTenant().isPresent()) {
            String tenantLang = tenantContextManager.getCurrentTenant().get().getDefaultLanguage();
            if (tenantLang != null && messageService.isLanguageSupported(tenantLang)) {
                return tenantLang;
            }
        }
        
        // 5. Default fallback
        return messageService.getDefaultLanguage();
    }
    
    private Map<String, Object> sanitizeSecurityDetails(Map<String, Object> errorContext) {
        // Remove sensitive information from security error details
        Map<String, Object> sanitized = Map.copyOf(errorContext);
        
        // Remove sensitive keys
        sanitized.entrySet().removeIf(entry -> 
            entry.getKey().toLowerCase().contains("password") ||
            entry.getKey().toLowerCase().contains("token") ||
            entry.getKey().toLowerCase().contains("secret") ||
            entry.getKey().toLowerCase().contains("key")
        );
        
        return sanitized;
    }
    
    /**
     * Standard error response format
     */
    @lombok.Value
    @lombok.Builder
    public static class ErrorResponse {
        Instant timestamp;
        int status;
        String error;
        String errorCode;
        String message;
        String path;
        String correlationId;
        AmanahFiException.ErrorSeverity severity;
        AmanahFiException.ErrorCategory category;
        boolean retryable;
        Map<String, Object> details;
    }
}