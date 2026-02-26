package com.enterprise.openfinance.atmdata.infrastructure.rest;

import com.enterprise.openfinance.atmdata.infrastructure.rest.dto.AtmErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AtmDataController.class)
public class AtmDataExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<AtmErrorResponse> handleInvalidRequest(IllegalArgumentException exception,
                                                                 HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(AtmErrorResponse.of("INVALID_REQUEST", exception.getMessage(), resolveInteractionId(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AtmErrorResponse> handleUnexpected(Exception exception,
                                                             HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AtmErrorResponse.of("INTERNAL_ERROR", "Unexpected server error", resolveInteractionId(request)));
    }

    private static String resolveInteractionId(HttpServletRequest request) {
        String interactionId = request == null ? null : request.getHeader("X-FAPI-Interaction-ID");
        if (interactionId == null || interactionId.isBlank()) {
            return "UNKNOWN";
        }
        return interactionId.trim();
    }
}
