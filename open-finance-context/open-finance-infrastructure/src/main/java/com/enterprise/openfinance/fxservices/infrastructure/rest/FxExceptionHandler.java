package com.enterprise.openfinance.fxservices.infrastructure.rest;

import com.enterprise.openfinance.fxservices.domain.exception.BusinessRuleViolationException;
import com.enterprise.openfinance.fxservices.domain.exception.ForbiddenException;
import com.enterprise.openfinance.fxservices.domain.exception.IdempotencyConflictException;
import com.enterprise.openfinance.fxservices.domain.exception.ResourceNotFoundException;
import com.enterprise.openfinance.fxservices.domain.exception.ServiceUnavailableException;
import com.enterprise.openfinance.fxservices.infrastructure.rest.dto.FxErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.enterprise.openfinance.fxservices.infrastructure.rest")
public class FxExceptionHandler {

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<FxErrorResponse> handleForbidden(ForbiddenException exception,
                                                           HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(FxErrorResponse.of("FORBIDDEN", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<FxErrorResponse> handleNotFound(ResourceNotFoundException exception,
                                                          HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(FxErrorResponse.of("NOT_FOUND", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<FxErrorResponse> handleConflict(IdempotencyConflictException exception,
                                                          HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(FxErrorResponse.of("IDEMPOTENCY_CONFLICT", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<FxErrorResponse> handleBusinessRule(BusinessRuleViolationException exception,
                                                              HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(FxErrorResponse.of("BUSINESS_RULE_VIOLATION", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<FxErrorResponse> handleUnavailable(ServiceUnavailableException exception,
                                                             HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(FxErrorResponse.of("SERVICE_UNAVAILABLE", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<FxErrorResponse> handleBadRequest(IllegalArgumentException exception,
                                                            HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(FxErrorResponse.of("INVALID_REQUEST", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<FxErrorResponse> handleUnexpected(Exception exception,
                                                            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(FxErrorResponse.of("INTERNAL_ERROR", "Unexpected error occurred", interactionId(request)));
    }

    private static String interactionId(HttpServletRequest request) {
        return request.getHeader("X-FAPI-Interaction-ID");
    }
}
