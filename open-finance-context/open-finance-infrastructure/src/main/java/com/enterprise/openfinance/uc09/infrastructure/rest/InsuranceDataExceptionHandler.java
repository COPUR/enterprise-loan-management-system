package com.enterprise.openfinance.uc09.infrastructure.rest;

import com.enterprise.openfinance.uc09.domain.exception.ForbiddenException;
import com.enterprise.openfinance.uc09.domain.exception.ResourceNotFoundException;
import com.enterprise.openfinance.uc09.infrastructure.rest.dto.InsuranceErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.enterprise.openfinance.uc09.infrastructure.rest")
public class InsuranceDataExceptionHandler {

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<InsuranceErrorResponse> handleForbidden(ForbiddenException exception,
                                                                  HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(InsuranceErrorResponse.of("FORBIDDEN", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<InsuranceErrorResponse> handleNotFound(ResourceNotFoundException exception,
                                                                 HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(InsuranceErrorResponse.of("NOT_FOUND", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<InsuranceErrorResponse> handleBadRequest(IllegalArgumentException exception,
                                                                   HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(InsuranceErrorResponse.of("INVALID_REQUEST", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<InsuranceErrorResponse> handleUnexpected(Exception exception,
                                                                   HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(InsuranceErrorResponse.of("INTERNAL_ERROR", "Unexpected error occurred", interactionId(request)));
    }

    private static String interactionId(HttpServletRequest request) {
        return request.getHeader("X-FAPI-Interaction-ID");
    }
}
