package com.amanahfi.murabaha.domain.contract;

/**
 * Exception thrown when profit rate exceeds Islamic finance guidelines
 */
public class ExcessiveProfitRateException extends RuntimeException {
    
    public ExcessiveProfitRateException(String message) {
        super(message);
    }
    
    public ExcessiveProfitRateException(String message, Throwable cause) {
        super(message, cause);
    }
}