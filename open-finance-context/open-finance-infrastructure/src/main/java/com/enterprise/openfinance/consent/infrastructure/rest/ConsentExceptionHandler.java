package com.enterprise.openfinance.consent.infrastructure.rest;

import com.enterprise.openfinance.consent.infrastructure.rest.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ConsentExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException exception,
                                                          HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("INVALID_REQUEST", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleConflict(IllegalStateException exception,
                                                        HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("CONFLICT", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception,
                                                          HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "Unexpected error occurred", interactionId(request)));
    }

    private static String interactionId(HttpServletRequest request) {
        return request.getHeader("X-FAPI-Interaction-ID");
    }
}
