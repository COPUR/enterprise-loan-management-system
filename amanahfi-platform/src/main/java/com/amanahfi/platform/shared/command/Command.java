package com.amanahfi.platform.shared.command;

import com.amanahfi.platform.shared.idempotence.IdempotencyKey;

import java.time.Instant;
import java.util.UUID;

/**
 * Base interface for all commands in the AmanahFi Platform
 * 
 * Commands represent the intention to change system state and must
 * be idempotent to handle retries, network failures, and saga
 * compensations without creating duplicate side effects.
 * 
 * Idempotence Requirements:
 * - Every command includes a unique commandId for deduplication
 * - Commands can be safely replayed without changing final state
 * - Command bus filters duplicates using outbox pattern
 * 
 * Islamic Finance Context:
 * - Commands must preserve Sharia compliance during retries
 * - Audit trail must remain intact even with replay scenarios
 * - Financial state changes must be exactly-once
 * 
 * Design Principles:
 * - Immutable command objects
 * - Rich validation at command creation
 * - Clear intent through naming
 * - Comprehensive audit information
 * 
 * @author AmanahFi Development Team
 * @version 1.0.0
 * @since 2024
 */
public interface Command {

    /**
     * Gets the unique identifier for this command
     * 
     * This ID serves as the idempotency key for command processing.
     * The command bus uses this ID to prevent duplicate execution.
     * 
     * @return Unique command identifier
     */
    UUID getCommandId();

    /**
     * Gets the idempotency key for this command
     * 
     * This key is used by the command bus and outbox pattern
     * to ensure exactly-once processing semantics.
     * 
     * @return Idempotency key for duplicate prevention
     */
    IdempotencyKey getIdempotencyKey();

    /**
     * Gets the timestamp when this command was created
     * 
     * @return Command creation timestamp
     */
    Instant getCreatedAt();

    /**
     * Gets the identifier of the user or system that issued this command
     * 
     * @return Command issuer identifier
     */
    String getIssuedBy();

    /**
     * Gets the correlation ID for distributed tracing
     * 
     * @return Correlation ID for request tracing
     */
    UUID getCorrelationId();

    /**
     * Gets the causation ID linking to the triggering event
     * 
     * @return Causation ID for event sourcing
     */
    UUID getCausationId();

    /**
     * Gets the expected version for optimistic locking
     * 
     * @return Expected aggregate version, null if not applicable
     */
    Long getExpectedVersion();

    /**
     * Gets the jurisdiction for regulatory compliance
     * 
     * @return Jurisdiction code (e.g., "AE", "SA", "TR")
     */
    String getJurisdiction();

    /**
     * Validates this command's business rules
     * 
     * This method should check all business invariants and
     * throw appropriate exceptions for violations.
     * 
     * @throws CommandValidationException if validation fails
     */
    void validate();

    /**
     * Checks if this command requires Sharia compliance validation
     * 
     * @return true if Sharia compliance checking is required
     */
    default boolean requiresShariaCompliance() {
        return false;
    }

    /**
     * Checks if this command requires regulatory reporting
     * 
     * @return true if regulatory reporting is required
     */
    default boolean requiresRegulatoryReporting() {
        return false;
    }

    /**
     * Checks if this command is financially sensitive
     * 
     * Financially sensitive commands require enhanced validation,
     * audit logging, and compliance checking.
     * 
     * @return true if command affects financial state
     */
    default boolean isFinanciallySensitive() {
        return false;
    }

    /**
     * Gets the timeout for this command in seconds
     * 
     * @return Command timeout, or null for default timeout
     */
    default Long getTimeoutSeconds() {
        return null;
    }

    /**
     * Gets the priority of this command for processing
     * 
     * @return Command priority
     */
    default CommandPriority getPriority() {
        return CommandPriority.NORMAL;
    }

    /**
     * Gets additional metadata for this command
     * 
     * @return Command metadata
     */
    default CommandMetadata getMetadata() {
        return CommandMetadata.empty();
    }

    /**
     * Command priority levels
     */
    enum CommandPriority {
        /**
         * Critical financial operations (payments, disbursements)
         */
        CRITICAL(1),
        
        /**
         * High priority operations (approvals, activations)
         */
        HIGH(2),
        
        /**
         * Normal priority operations (standard processing)
         */
        NORMAL(3),
        
        /**
         * Low priority operations (reporting, analytics)
         */
        LOW(4),
        
        /**
         * Background operations (cleanup, maintenance)
         */
        BACKGROUND(5);

        private final int level;

        CommandPriority(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }

        /**
         * Compares priority levels
         * 
         * @param other Priority to compare with
         * @return true if this priority is higher than other
         */
        public boolean isHigherThan(CommandPriority other) {
            return this.level < other.level;
        }
    }

    /**
     * Exception thrown when command validation fails
     */
    class CommandValidationException extends RuntimeException {
        
        private final String commandType;
        private final UUID commandId;

        public CommandValidationException(String message, String commandType, UUID commandId) {
            super(message);
            this.commandType = commandType;
            this.commandId = commandId;
        }

        public CommandValidationException(String message, Throwable cause, String commandType, UUID commandId) {
            super(message, cause);
            this.commandType = commandType;
            this.commandId = commandId;
        }

        public String getCommandType() {
            return commandType;
        }

        public UUID getCommandId() {
            return commandId;
        }
    }
}