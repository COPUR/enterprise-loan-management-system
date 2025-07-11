package com.masrufi.framework.domain.exception;

/**
 * Exception thrown when asset validation fails
 * 
 * @author MasruFi Development Team
 * @version 1.0.0
 */
public class AssetValidationException extends RuntimeException {

    public AssetValidationException(String message) {
        super(message);
    }

    public AssetValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}