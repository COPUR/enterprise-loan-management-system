package com.bank.shared.domain.exception;

/**
 * Base class for business exceptions
 * 
 * Represents exceptions that occur due to business rule violations
 * or invalid business operations.
 */
public abstract class BusinessException extends RuntimeException {
    
    private final String errorCode;
    
    protected BusinessException(String message) {
        super(message);
        this.errorCode = this.getClass().getSimpleName();
    }
    
    protected BusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    protected BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = this.getClass().getSimpleName();
    }
    
    protected BusinessException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}