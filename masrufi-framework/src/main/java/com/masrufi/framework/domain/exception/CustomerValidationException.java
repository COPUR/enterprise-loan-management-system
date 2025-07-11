package com.masrufi.framework.domain.exception;

/**
 * Exception thrown when customer validation fails
 * 
 * @author MasruFi Development Team
 * @version 1.0.0
 */
public class CustomerValidationException extends RuntimeException {

    public CustomerValidationException(String message) {
        super(message);
    }

    public CustomerValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}