package com.enterprise.openfinance.consentauthorization.infrastructure.rest;

import com.enterprise.openfinance.consentauthorization.infrastructure.rest.dto.ErrorResponse;
import com.enterprise.openfinance.consentauthorization.infrastructure.rest.dto.OAuthErrorResponse;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.exception.InternalAuthenticationException;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.exception.InternalTokenUnauthorizedException;
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

    @ExceptionHandler(OAuthException.class)
    public ResponseEntity<OAuthErrorResponse> handleOAuthException(OAuthException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(OAuthErrorResponse.of(exception.getError(), exception.getMessage()));
    }

    @ExceptionHandler(InternalAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleInternalAuthentication(InternalAuthenticationException exception,
                                                                      HttpServletRequest request) {
        HttpStatus status = exception.isThrottled() ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.UNAUTHORIZED;
        String code = exception.isThrottled() ? "AUTH_RATE_LIMITED" : "INVALID_CREDENTIALS";
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(code, exception.getMessage(), interactionId(request)));
    }

    @ExceptionHandler(InternalTokenUnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleTokenUnauthorized(InternalTokenUnauthorizedException exception,
                                                                 HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("UNAUTHORIZED", exception.getMessage(), interactionId(request)));
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
