package com.loanmanagement.shared.application.port.out;

/**
 * Factory for creating LoggingPort instances
 * Allows for framework-agnostic logging in application services
 */
public interface LoggingFactory {
    
    /**
     * Create a logger for a specific class
     */
    LoggingPort getLogger(Class<?> clazz);
    
    /**
     * Create a logger with a specific name
     */
    LoggingPort getLogger(String name);
}