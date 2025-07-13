package com.amanahfi.murabaha.domain.contract;

/**
 * Exception thrown when contract amount is outside valid limits
 */
public class InvalidContractAmountException extends RuntimeException {
    
    public InvalidContractAmountException(String message) {
        super(message);
    }
    
    public InvalidContractAmountException(String message, Throwable cause) {
        super(message, cause);
    }
}