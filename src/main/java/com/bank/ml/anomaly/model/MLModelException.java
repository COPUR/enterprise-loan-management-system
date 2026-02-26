package com.bank.ml.anomaly.model;

/**
 * ML Model Exception
 * Custom exception for ML model-related errors
 */
public class MLModelException extends RuntimeException {
    
    private final String modelName;
    private final String errorCode;
    
    public MLModelException(String message) {
        super(message);
        this.modelName = null;
        this.errorCode = "GENERAL_ERROR";
    }
    
    public MLModelException(String message, Throwable cause) {
        super(message, cause);
        this.modelName = null;
        this.errorCode = "GENERAL_ERROR";
    }
    
    public MLModelException(String message, String modelName, String errorCode) {
        super(message);
        this.modelName = modelName;
        this.errorCode = errorCode;
    }
    
    public MLModelException(String message, String modelName, String errorCode, Throwable cause) {
        super(message, cause);
        this.modelName = modelName;
        this.errorCode = errorCode;
    }
    
    // Getters
    public String getModelName() { return modelName; }
    public String getErrorCode() { return errorCode; }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MLModelException");
        
        if (modelName != null) {
            sb.append(" [Model: ").append(modelName).append("]");
        }
        
        if (errorCode != null) {
            sb.append(" [Code: ").append(errorCode).append("]");
        }
        
        sb.append(": ").append(getMessage());
        
        return sb.toString();
    }
}