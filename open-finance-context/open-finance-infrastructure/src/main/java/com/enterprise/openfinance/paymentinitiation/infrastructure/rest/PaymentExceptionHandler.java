package com.enterprise.openfinance.paymentinitiation.infrastructure.rest;

import com.enterprise.openfinance.paymentinitiation.infrastructure.rest.dto.PaymentErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.enterprise.openfinance.paymentinitiation.infrastructure.rest")
public class PaymentExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<PaymentErrorResponse> handleBadRequest(IllegalArgumentException exception,
                                                                 HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(PaymentErrorResponse.of("INVALID_REQUEST", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<PaymentErrorResponse> handleIllegalState(IllegalStateException exception,
                                                                   HttpServletRequest request) {
        if ("Idempotency conflict".equals(exception.getMessage())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(PaymentErrorResponse.of("CONFLICT", exception.getMessage(), interactionId(request)));
        }
        if ("Insufficient funds".equals(exception.getMessage())
                || "Consent binding validation failed".equals(exception.getMessage())) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(PaymentErrorResponse.of("BUSINESS_RULE_VIOLATION", exception.getMessage(), interactionId(request)));
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(PaymentErrorResponse.of("CONFLICT", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<PaymentErrorResponse> handleUnexpected(Exception exception,
                                                                 HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(PaymentErrorResponse.of("INTERNAL_ERROR", "Unexpected error occurred", interactionId(request)));
    }

    private static String interactionId(HttpServletRequest request) {
        return request.getHeader("X-FAPI-Interaction-ID");
    }
}
