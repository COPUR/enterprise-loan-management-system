package com.bank.infrastructure.security;

/**
 * Exception thrown when DPoP proof validation fails.
 */
public class DPoPValidationException extends Exception {

    public DPoPValidationException(String message) {
        super(message);
    }

    public DPoPValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
