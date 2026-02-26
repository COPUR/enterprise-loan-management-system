package com.enterprise.openfinance.payeeverification.infrastructure.rest;

import com.enterprise.openfinance.payeeverification.infrastructure.rest.dto.ConfirmationErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.enterprise.openfinance.payeeverification.infrastructure.rest")
public class ConfirmationExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ConfirmationErrorResponse> handleBadRequest(IllegalArgumentException exception,
                                                                      HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(ConfirmationErrorResponse.of("INVALID_REQUEST", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ConfirmationErrorResponse> handleConflict(IllegalStateException exception,
                                                                    HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ConfirmationErrorResponse.of("CONFLICT", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ConfirmationErrorResponse> handleUnexpected(Exception exception,
                                                                      HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ConfirmationErrorResponse.of("INTERNAL_ERROR", "Unexpected error occurred", interactionId(request)));
    }

    private static String interactionId(HttpServletRequest request) {
        return request.getHeader("X-FAPI-Interaction-ID");
    }
}
