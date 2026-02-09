package com.enterprise.openfinance.uc04.infrastructure.rest;

import com.enterprise.openfinance.uc04.domain.exception.ForbiddenException;
import com.enterprise.openfinance.uc04.domain.exception.ResourceNotFoundException;
import com.enterprise.openfinance.uc04.infrastructure.rest.dto.MetadataErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.enterprise.openfinance.uc04.infrastructure.rest")
public class MetadataExceptionHandler {

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<MetadataErrorResponse> handleForbidden(ForbiddenException exception,
                                                                 HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(MetadataErrorResponse.of("FORBIDDEN", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<MetadataErrorResponse> handleNotFound(ResourceNotFoundException exception,
                                                                HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(MetadataErrorResponse.of("NOT_FOUND", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MetadataErrorResponse> handleBadRequest(IllegalArgumentException exception,
                                                                  HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(MetadataErrorResponse.of("INVALID_REQUEST", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MetadataErrorResponse> handleUnexpected(Exception exception,
                                                                  HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(MetadataErrorResponse.of("INTERNAL_ERROR", "Unexpected error occurred", interactionId(request)));
    }

    private static String interactionId(HttpServletRequest request) {
        return request.getHeader("X-FAPI-Interaction-ID");
    }
}
