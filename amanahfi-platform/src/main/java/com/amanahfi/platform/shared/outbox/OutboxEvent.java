package com.amanahfi.platform.shared.outbox;

import com.amanahfi.platform.shared.idempotence.IdempotencyKey;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbox Event for transactional event publishing with idempotency
 * 
 * This class implements the Outbox Pattern to ensure reliable event
 * publishing with exactly-once semantics. Events are stored in the
 * same database transaction as business state changes, then published
 * asynchronously by a separate process.
 * 
 * Pattern Implementation:
 * 1. Write (commandId, payload, status='NEW') in same DB tx as business state
 * 2. Async publisher dequeues NEW rows and marks SENT
 * 3. Duplicates filtered at INSERT (PK = commandId)
 * 
 * Islamic Finance Context:
 * - Ensures Sharia-compliant transactions are reliably processed
 * - Maintains audit trail integrity for regulatory compliance
 * - Prevents duplicate financial events that could violate Islamic principles
 * 
 * @author AmanahFi Development Team
 * @version 1.0.0
 * @since 2024
 */
@Value
@Builder
public class OutboxEvent {

    /**
     * Unique identifier for this outbox event (serves as idempotency key)
     */
    UUID id;

    /**
     * The aggregate ID that produced this event
     */
    String aggregateId;

    /**
     * The type of aggregate that produced this event
     */
    String aggregateType;

    /**
     * The version of the aggregate when this event was produced
     */
    Long aggregateVersion;

    /**
     * The type of event
     */
    String eventType;

    /**
     * The serialized event payload
     */
    String eventPayload;

    /**
     * Content type of the payload (e.g., application/json)
     */
    String contentType;

    /**
     * Current status of the event
     */
    EventStatus status;

    /**
     * When this event was created
     */
    Instant createdAt;

    /**
     * When this event was last processed
     */
    Instant processedAt;

    /**
     * When this event should be processed (for delayed events)
     */
    Instant scheduleAt;

    /**
     * Number of processing attempts
     */
    Integer attempts;

    /**
     * Maximum number of retry attempts
     */
    Integer maxAttempts;

    /**
     * Error message from last failed attempt
     */
    String lastError;

    /**
     * Metadata associated with the event
     */
    String metadata;

    /**
     * The topic/destination for this event
     */
    String destination;

    /**
     * Partition key for ordered processing
     */
    String partitionKey;

    /**
     * Correlation ID for tracing
     */
    UUID correlationId;

    /**
     * Causation ID linking to triggering event
     */
    UUID causationId;

    /**
     * Idempotency key for duplicate prevention
     */
    IdempotencyKey idempotencyKey;

    /**
     * Creates a new outbox event for immediate processing
     * 
     * @param aggregateId The aggregate identifier
     * @param aggregateType The aggregate type
     * @param aggregateVersion The aggregate version
     * @param eventType The event type
     * @param eventPayload The serialized event payload
     * @param destination The target topic/queue
     * @return New outbox event
     */
    public static OutboxEvent create(
            String aggregateId,
            String aggregateType,
            Long aggregateVersion,
            String eventType,
            String eventPayload,
            String destination) {
        
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.now();
        
        return OutboxEvent.builder()
                .id(eventId)
                .aggregateId(aggregateId)
                .aggregateType(aggregateType)
                .aggregateVersion(aggregateVersion)
                .eventType(eventType)
                .eventPayload(eventPayload)
                .contentType("application/json")
                .status(EventStatus.NEW)
                .createdAt(now)
                .scheduleAt(now)
                .attempts(0)
                .maxAttempts(3)
                .destination(destination)
                .partitionKey(aggregateId)
                .correlationId(eventId)
                .idempotencyKey(IdempotencyKey.of(eventId))
                .build();
    }

    /**
     * Creates a delayed outbox event
     * 
     * @param aggregateId The aggregate identifier
     * @param aggregateType The aggregate type
     * @param aggregateVersion The aggregate version
     * @param eventType The event type
     * @param eventPayload The serialized event payload
     * @param destination The target topic/queue
     * @param delaySeconds Delay before processing
     * @return New delayed outbox event
     */
    public static OutboxEvent createDelayed(
            String aggregateId,
            String aggregateType,
            Long aggregateVersion,
            String eventType,
            String eventPayload,
            String destination,
            long delaySeconds) {
        
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.now();
        
        return OutboxEvent.builder()
                .id(eventId)
                .aggregateId(aggregateId)
                .aggregateType(aggregateType)
                .aggregateVersion(aggregateVersion)
                .eventType(eventType)
                .eventPayload(eventPayload)
                .contentType("application/json")
                .status(EventStatus.SCHEDULED)
                .createdAt(now)
                .scheduleAt(now.plusSeconds(delaySeconds))
                .attempts(0)
                .maxAttempts(3)
                .destination(destination)
                .partitionKey(aggregateId)
                .correlationId(eventId)
                .idempotencyKey(IdempotencyKey.of(eventId))
                .build();
    }

    /**
     * Marks this event as being processed
     * 
     * @return Updated outbox event
     */
    public OutboxEvent markAsProcessing() {
        return this.toBuilder()
                .status(EventStatus.PROCESSING)
                .processedAt(Instant.now())
                .attempts(attempts + 1)
                .build();
    }

    /**
     * Marks this event as successfully sent
     * 
     * @return Updated outbox event
     */
    public OutboxEvent markAsSent() {
        return this.toBuilder()
                .status(EventStatus.SENT)
                .processedAt(Instant.now())
                .build();
    }

    /**
     * Marks this event as failed with error
     * 
     * @param error The error message
     * @return Updated outbox event
     */
    public OutboxEvent markAsFailed(String error) {
        EventStatus newStatus = attempts >= maxAttempts ? 
                EventStatus.DEAD_LETTER : EventStatus.FAILED;
                
        return this.toBuilder()
                .status(newStatus)
                .lastError(error)
                .processedAt(Instant.now())
                .build();
    }

    /**
     * Schedules this event for retry
     * 
     * @param retryDelaySeconds Delay before next retry
     * @return Updated outbox event
     */
    public OutboxEvent scheduleRetry(long retryDelaySeconds) {
        return this.toBuilder()
                .status(EventStatus.RETRY)
                .scheduleAt(Instant.now().plusSeconds(retryDelaySeconds))
                .processedAt(Instant.now())
                .build();
    }

    /**
     * Checks if this event is ready for processing
     * 
     * @return true if event should be processed now
     */
    public boolean isReadyForProcessing() {
        return (status == EventStatus.NEW || status == EventStatus.RETRY) &&
               Instant.now().isAfter(scheduleAt);
    }

    /**
     * Checks if this event can be retried
     * 
     * @return true if retry attempts remaining
     */
    public boolean canRetry() {
        return attempts < maxAttempts && status != EventStatus.DEAD_LETTER;
    }

    /**
     * Checks if this event is terminal (final state)
     * 
     * @return true if event is in terminal state
     */
    public boolean isTerminal() {
        return status == EventStatus.SENT || 
               status == EventStatus.DEAD_LETTER ||
               status == EventStatus.CANCELLED;
    }

    /**
     * Gets the age of this event in seconds
     * 
     * @return Age in seconds since creation
     */
    public long getAgeInSeconds() {
        return java.time.Duration.between(createdAt, Instant.now()).getSeconds();
    }

    /**
     * Checks if this event is stale (older than threshold)
     * 
     * @param maxAgeSeconds Maximum acceptable age
     * @return true if event is stale
     */
    public boolean isStale(long maxAgeSeconds) {
        return getAgeInSeconds() > maxAgeSeconds;
    }

    /**
     * Status of an outbox event
     */
    public enum EventStatus {
        
        /**
         * Event is new and ready for processing
         */
        NEW("New"),
        
        /**
         * Event is scheduled for future processing
         */
        SCHEDULED("Scheduled"),
        
        /**
         * Event is currently being processed
         */
        PROCESSING("Processing"),
        
        /**
         * Event was successfully sent
         */
        SENT("Sent"),
        
        /**
         * Event processing failed, will retry
         */
        FAILED("Failed"),
        
        /**
         * Event is scheduled for retry
         */
        RETRY("Retry"),
        
        /**
         * Event failed max attempts, moved to dead letter
         */
        DEAD_LETTER("Dead Letter"),
        
        /**
         * Event was cancelled
         */
        CANCELLED("Cancelled");

        private final String description;

        EventStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        /**
         * Checks if this status indicates a processable state
         * 
         * @return true if event can be processed
         */
        public boolean isProcessable() {
            return this == NEW || this == RETRY;
        }

        /**
         * Checks if this status indicates a terminal state
         * 
         * @return true if event is in terminal state
         */
        public boolean isTerminal() {
            return this == SENT || this == DEAD_LETTER || this == CANCELLED;
        }

        /**
         * Checks if this status indicates a failure state
         * 
         * @return true if event has failed
         */
        public boolean isFailure() {
            return this == FAILED || this == DEAD_LETTER;
        }
    }

    /**
     * Builder class with enhanced validation
     */
    public static class OutboxEventBuilder {
        
        public OutboxEvent build() {
            // Validate required fields
            if (id == null) {
                id = UUID.randomUUID();
            }
            if (aggregateId == null || aggregateId.trim().isEmpty()) {
                throw new IllegalArgumentException("Aggregate ID is required");
            }
            if (aggregateType == null || aggregateType.trim().isEmpty()) {
                throw new IllegalArgumentException("Aggregate type is required");
            }
            if (eventType == null || eventType.trim().isEmpty()) {
                throw new IllegalArgumentException("Event type is required");
            }
            if (eventPayload == null || eventPayload.trim().isEmpty()) {
                throw new IllegalArgumentException("Event payload is required");
            }
            if (status == null) {
                status = EventStatus.NEW;
            }
            if (createdAt == null) {
                createdAt = Instant.now();
            }
            if (scheduleAt == null) {
                scheduleAt = createdAt;
            }
            if (attempts == null) {
                attempts = 0;
            }
            if (maxAttempts == null) {
                maxAttempts = 3;
            }
            if (contentType == null) {
                contentType = "application/json";
            }
            if (partitionKey == null) {
                partitionKey = aggregateId;
            }
            if (correlationId == null) {
                correlationId = id;
            }
            if (idempotencyKey == null) {
                idempotencyKey = IdempotencyKey.of(id);
            }

            return new OutboxEvent(
                id, aggregateId, aggregateType, aggregateVersion, eventType,
                eventPayload, contentType, status, createdAt, processedAt,
                scheduleAt, attempts, maxAttempts, lastError, metadata,
                destination, partitionKey, correlationId, causationId, idempotencyKey
            );
        }
    }
}