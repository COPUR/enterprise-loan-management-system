package com.bank.loan.loan.security.dpop.exception;

public class TokenBindingMismatchException extends RuntimeException {
    
    private final String errorCode;
    private final String errorDescription;
    
    public TokenBindingMismatchException(String message) {
        super(message);
        this.errorCode = "token_binding_mismatch";
        this.errorDescription = message;
    }
    
    public TokenBindingMismatchException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "token_binding_mismatch";
        this.errorDescription = message;
    }
    
    public TokenBindingMismatchException(String errorCode, String errorDescription) {
        super(errorDescription);
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }
    
    public TokenBindingMismatchException(String errorCode, String errorDescription, Throwable cause) {
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