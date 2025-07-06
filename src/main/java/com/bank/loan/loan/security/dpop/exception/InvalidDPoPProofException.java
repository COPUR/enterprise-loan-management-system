package com.bank.loan.loan.security.dpop.exception;

public class InvalidDPoPProofException extends RuntimeException {
    
    private final String errorCode;
    private final String errorDescription;
    
    public InvalidDPoPProofException(String message) {
        super(message);
        this.errorCode = "invalid_dpop_proof";
        this.errorDescription = message;
    }
    
    public InvalidDPoPProofException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "invalid_dpop_proof";
        this.errorDescription = message;
    }
    
    public InvalidDPoPProofException(String errorCode, String errorDescription) {
        super(errorDescription);
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }
    
    public InvalidDPoPProofException(String errorCode, String errorDescription, Throwable cause) {
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