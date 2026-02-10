package com.enterprise.openfinance.uc12.infrastructure.rest;

import com.enterprise.openfinance.uc12.domain.exception.ComplianceViolationException;
import com.enterprise.openfinance.uc12.domain.exception.DecryptionFailedException;
import com.enterprise.openfinance.uc12.domain.exception.ForbiddenException;
import com.enterprise.openfinance.uc12.domain.exception.IdempotencyConflictException;
import com.enterprise.openfinance.uc12.domain.exception.ResourceNotFoundException;
import com.enterprise.openfinance.uc12.infrastructure.rest.dto.OnboardingErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.enterprise.openfinance.uc12.infrastructure.rest")
public class OnboardingExceptionHandler {

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<OnboardingErrorResponse> handleForbidden(ForbiddenException exception,
                                                                   HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(OnboardingErrorResponse.of("FORBIDDEN", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<OnboardingErrorResponse> handleNotFound(ResourceNotFoundException exception,
                                                                  HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(OnboardingErrorResponse.of("NOT_FOUND", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<OnboardingErrorResponse> handleConflict(IdempotencyConflictException exception,
                                                                  HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(OnboardingErrorResponse.of("IDEMPOTENCY_CONFLICT", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(DecryptionFailedException.class)
    public ResponseEntity<OnboardingErrorResponse> handleDecryptionFailure(DecryptionFailedException exception,
                                                                           HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(OnboardingErrorResponse.of("DECRYPTION_FAILED", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(ComplianceViolationException.class)
    public ResponseEntity<OnboardingErrorResponse> handleCompliance(ComplianceViolationException exception,
                                                                    HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(OnboardingErrorResponse.of("COMPLIANCE_VIOLATION", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<OnboardingErrorResponse> handleBadRequest(IllegalArgumentException exception,
                                                                    HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(OnboardingErrorResponse.of("INVALID_REQUEST", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<OnboardingErrorResponse> handleUnexpected(Exception exception,
                                                                    HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(OnboardingErrorResponse.of("INTERNAL_ERROR", "Unexpected error occurred", interactionId(request)));
    }

    private static String interactionId(HttpServletRequest request) {
        return request.getHeader("X-FAPI-Interaction-ID");
    }
}
