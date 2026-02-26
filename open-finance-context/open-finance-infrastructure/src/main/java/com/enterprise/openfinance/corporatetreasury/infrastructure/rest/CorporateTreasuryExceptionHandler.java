package com.enterprise.openfinance.corporatetreasury.infrastructure.rest;

import com.enterprise.openfinance.corporatetreasury.domain.exception.ForbiddenException;
import com.enterprise.openfinance.corporatetreasury.domain.exception.ResourceNotFoundException;
import com.enterprise.openfinance.corporatetreasury.infrastructure.rest.dto.CorporateErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.enterprise.openfinance.corporatetreasury.infrastructure.rest")
public class CorporateTreasuryExceptionHandler {

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<CorporateErrorResponse> handleForbidden(ForbiddenException exception,
                                                                  HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(CorporateErrorResponse.of("FORBIDDEN", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<CorporateErrorResponse> handleNotFound(ResourceNotFoundException exception,
                                                                 HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(CorporateErrorResponse.of("NOT_FOUND", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CorporateErrorResponse> handleBadRequest(IllegalArgumentException exception,
                                                                   HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(CorporateErrorResponse.of("INVALID_REQUEST", exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CorporateErrorResponse> handleUnexpected(Exception exception,
                                                                   HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CorporateErrorResponse.of("INTERNAL_ERROR", "Unexpected error occurred", interactionId(request)));
    }

    private static String interactionId(HttpServletRequest request) {
        return request.getHeader("X-FAPI-Interaction-ID");
    }
}
