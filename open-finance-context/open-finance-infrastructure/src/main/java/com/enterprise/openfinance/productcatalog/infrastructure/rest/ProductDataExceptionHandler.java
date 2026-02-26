package com.enterprise.openfinance.productcatalog.infrastructure.rest;

import com.enterprise.openfinance.productcatalog.infrastructure.rest.dto.ProductErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ProductDataController.class)
public class ProductDataExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProductErrorResponse> handleInvalidRequest(IllegalArgumentException exception,
                                                                     HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ProductErrorResponse.of("INVALID_REQUEST", exception.getMessage(), resolveInteractionId(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProductErrorResponse> handleUnexpected(Exception exception,
                                                                 HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ProductErrorResponse.of("INTERNAL_ERROR", "Unexpected server error", resolveInteractionId(request)));
    }

    private static String resolveInteractionId(HttpServletRequest request) {
        String interactionId = request == null ? null : request.getHeader("X-FAPI-Interaction-ID");
        if (interactionId == null || interactionId.isBlank()) {
            return "UNKNOWN";
        }
        return interactionId.trim();
    }
}
