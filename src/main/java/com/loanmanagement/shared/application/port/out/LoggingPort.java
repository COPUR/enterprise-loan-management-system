package com.loanmanagement.shared.application.port.out;

/**
 * Outbound Port for Logging
 * Abstracts logging framework from the application layer
 */
public interface LoggingPort {
    
    /**
     * Log a debug message
     */
    void debug(String message, Object... args);
    
    /**
     * Log an info message
     */
    void info(String message, Object... args);
    
    /**
     * Log a warning message
     */
    void warn(String message, Object... args);
    
    /**
     * Log an error message
     */
    void error(String message, Object... args);
    
    /**
     * Log an error message with exception
     */
    void error(String message, Throwable throwable, Object... args);
    
    /**
     * Check if debug logging is enabled
     */
    boolean isDebugEnabled();
    
    /**
     * Check if info logging is enabled
     */
    boolean isInfoEnabled();
    
    /**
     * Check if warn logging is enabled
     */
    boolean isWarnEnabled();
    
    /**
     * Check if error logging is enabled
     */
    boolean isErrorEnabled();
}