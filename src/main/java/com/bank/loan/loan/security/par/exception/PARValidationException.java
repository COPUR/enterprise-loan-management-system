package com.bank.loan.loan.security.par.exception;

public class PARValidationException extends RuntimeException {
    
    private final String errorCode;
    private final String errorDescription;
    
    public PARValidationException(String message) {
        super(message);
        this.errorCode = "invalid_request";
        this.errorDescription = message;
    }
    
    public PARValidationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "invalid_request";
        this.errorDescription = message;
    }
    
    public PARValidationException(String errorCode, String errorDescription) {
        super(errorDescription);
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }
    
    public PARValidationException(String errorCode, String errorDescription, Throwable cause) {
        super(errorDescription, cause);
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getErrorDescription() {
        return errorDescription;
    }
}