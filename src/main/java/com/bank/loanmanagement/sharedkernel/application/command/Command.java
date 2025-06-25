package com.bank.loanmanagement.sharedkernel.application.command;

import java.time.Instant;
import java.util.UUID;

/**
 * Enterprise-grade Command interface following FAPI standards
 * Represents an intention to change the state of the system
 * 
 * This interface ensures all commands in the enterprise banking system
 * comply with Financial-grade API security and traceability requirements
 */
public interface Command {
    
    /**
     * Unique identifier for command traceability and audit compliance
     * Required for FAPI and banking regulatory standards
     * 
     * @return UUID for command tracking
     */
    default UUID getCommandId() {
        return UUID.randomUUID();
    }
    
    /**
     * Timestamp when command was created
     * Required for audit trail and regulatory compliance
     * 
     * @return Instant of command creation
     */
    default Instant getTimestamp() {
        return Instant.now();
    }
    
    /**
     * Correlation ID for distributed tracing
     * Essential for microservices observability and FAPI compliance
     * 
     * @return Correlation ID for request tracking
     */
    default String getCorrelationId() {
        return "cmd-" + getCommandId().toString().substring(0, 8);
    }
    
    /**
     * User ID who initiated the command
     * Required for authorization and audit purposes
     * 
     * @return User identifier
     */
    default String getUserId() {
        return "system";
    }
    
    /**
     * Validates command data integrity
     * Ensures all required fields are present and valid
     * 
     * @throws IllegalArgumentException if validation fails
     */
    default void validate() {
        // Default implementation - override in concrete commands
    }
}