package com.enterprise.openfinance.uc02.infrastructure.rest;

import com.enterprise.openfinance.uc02.domain.exception.ForbiddenException;
import com.enterprise.openfinance.uc02.domain.exception.ResourceNotFoundException;
import com.enterprise.openfinance.uc02.infrastructure.rest.dto.AisErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.enterprise.openfinance.uc02.infrastructure.rest")
public class AccountInformationExceptionHandler {

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<AisErrorResponse> handleForbidden(ForbiddenException exception,
                                                            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(AisErrorResponse.of("FORBIDDEN", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<AisErrorResponse> handleNotFound(ResourceNotFoundException exception,
                                                           HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(AisErrorResponse.of("NOT_FOUND", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<AisErrorResponse> handleBadRequest(IllegalArgumentException exception,
                                                             HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(AisErrorResponse.of("INVALID_REQUEST", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AisErrorResponse> handleUnexpected(Exception exception,
                                                             HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AisErrorResponse.of("INTERNAL_ERROR", "Unexpected error occurred", interactionId(request)));
    }

    private static String interactionId(HttpServletRequest request) {
        return request.getHeader("X-FAPI-Interaction-ID");
    }
}
