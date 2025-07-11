package com.masrufi.framework.domain.exception;

/**
 * Exception thrown when an operation violates Sharia principles
 * 
 * This exception is thrown when any Islamic finance operation
 * violates fundamental Sharia principles such as:
 * - Riba (interest/usury)
 * - Gharar (uncertainty/speculation)
 * - Asset permissibility
 * - Profit sharing requirements
 * - Other Islamic finance rules
 * 
 * @author MasruFi Development Team
 * @version 1.0.0
 */
public class ShariaViolationException extends RuntimeException {

    private final String violationType;
    private final String shariaRule;

    public ShariaViolationException(String message) {
        super(message);
        this.violationType = "GENERAL";
        this.shariaRule = "UNKNOWN";
    }

    public ShariaViolationException(String message, String violationType) {
        super(message);
        this.violationType = violationType;
        this.shariaRule = "UNKNOWN";
    }

    public ShariaViolationException(String message, String violationType, String shariaRule) {
        super(message);
        this.violationType = violationType;
        this.shariaRule = shariaRule;
    }

    public ShariaViolationException(String message, Throwable cause) {
        super(message, cause);
        this.violationType = "GENERAL";
        this.shariaRule = "UNKNOWN";
    }

    public String getViolationType() {
        return violationType;
    }

    public String getShariaRule() {
        return shariaRule;
    }

    @Override
    public String toString() {
        return String.format("ShariaViolationException[type=%s, rule=%s, message=%s]", 
            violationType, shariaRule, getMessage());
    }
}