package com.enterprise.openfinance.insurancequotes.infrastructure.rest;

import com.enterprise.openfinance.insurancequotes.domain.exception.BusinessRuleViolationException;
import com.enterprise.openfinance.insurancequotes.domain.exception.ForbiddenException;
import com.enterprise.openfinance.insurancequotes.domain.exception.IdempotencyConflictException;
import com.enterprise.openfinance.insurancequotes.domain.exception.ResourceNotFoundException;
import com.enterprise.openfinance.insurancequotes.infrastructure.rest.dto.InsuranceQuoteErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.enterprise.openfinance.insurancequotes.infrastructure.rest")
public class InsuranceQuoteExceptionHandler {

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<InsuranceQuoteErrorResponse> handleForbidden(ForbiddenException exception,
                                                                       HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(InsuranceQuoteErrorResponse.of("FORBIDDEN", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<InsuranceQuoteErrorResponse> handleNotFound(ResourceNotFoundException exception,
                                                                      HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(InsuranceQuoteErrorResponse.of("NOT_FOUND", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<InsuranceQuoteErrorResponse> handleConflict(IdempotencyConflictException exception,
                                                                      HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(InsuranceQuoteErrorResponse.of("IDEMPOTENCY_CONFLICT", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<InsuranceQuoteErrorResponse> handleBusinessRule(BusinessRuleViolationException exception,
                                                                          HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(InsuranceQuoteErrorResponse.of("BUSINESS_RULE_VIOLATION", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<InsuranceQuoteErrorResponse> handleBadRequest(IllegalArgumentException exception,
                                                                        HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(InsuranceQuoteErrorResponse.of("INVALID_REQUEST", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<InsuranceQuoteErrorResponse> handleUnexpected(Exception exception,
                                                                        HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(InsuranceQuoteErrorResponse.of("INTERNAL_ERROR", "Unexpected error occurred", interactionId(request)));
    }

    private static String interactionId(HttpServletRequest request) {
        return request.getHeader("X-FAPI-Interaction-ID");
    }
}
