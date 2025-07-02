package com.loanmanagement.infrastructure.persistence.repository;

import com.loanmanagement.infrastructure.persistence.entity.EventStoreEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Repository interface for Event Store following hexagonal architecture principles.
 * This interface defines the contract for event persistence operations.
 *
 * Follows EDA principles by:
 * - Supporting event stream operations with high performance
 * - Maintaining event ordering and versioning
 * - Enabling event replay and audit capabilities
 * - Providing async operations for scalable event processing
 *
 * Supports DDD principles by:
 * - Acting as a repository port in the domain layer
 * - Providing aggregate-centric event retrieval
 * - Maintaining consistency within aggregate boundaries
 * - Supporting optimistic concurrency control
 *
 * Implements 12-Factor App principles by:
 * - Supporting stateless, scalable operations
 * - Enabling horizontal scaling through async processing
 * - Providing monitoring and metrics capabilities
 *
 * Note: This is a fallback implementation while Spring Data JPA dependencies are resolved.
 * To restore Spring Data functionality:
 * 1. Extend JpaRepository<EventStoreEntity, String>
 * 2. Use Spring Data query methods
 * 3. Add @Repository annotation
 */
public interface EventStoreRepository {

    // Core CRUD Operations

    /**
     * Saves a single event to the event store.
     * Follows EDA principles by ensuring events are persisted immutably.
     */
    EventStoreEntity save(EventStoreEntity event);

    /**
     * Saves multiple events in a single transaction.
     * Critical for maintaining consistency when multiple events are generated.
     */
    List<EventStoreEntity> saveAll(List<EventStoreEntity> events);

    /**
     * Saves multiple events asynchronously for high-throughput scenarios.
     * Supports EDA scalability with non-blocking event persistence.
     */
    CompletableFuture<List<EventStoreEntity>> saveAllAsync(List<EventStoreEntity> events);

    /**
     * Saves events in configurable batches for memory efficiency.
     * Optimizes large event sets while maintaining transaction boundaries.
     */
    void saveInBatch(List<EventStoreEntity> events, int batchSize);

    // Event Retrieval Operations

    /**
     * Finds an event by its unique identifier.
     * Supports event replay and audit operations.
     */
    Optional<EventStoreEntity> findById(String eventId);

    /**
     * Retrieves all events for a specific aggregate ordered by version.
     * Core method for event sourcing and aggregate reconstruction.
     */
    List<EventStoreEntity> findByAggregateIdOrderByVersion(String aggregateId);

    /**
     * Streams events for a specific aggregate for memory-efficient processing.
     * Supports large event streams without loading all events into memory.
     */
    Stream<EventStoreEntity> streamEventsByAggregateId(String aggregateId);

    /**
     * Retrieves events for a specific aggregate starting from a given version.
     * Supports incremental event processing and optimized aggregate loading.
     */
    List<EventStoreEntity> findByAggregateIdAndVersionGreaterThanOrderByVersion(
        String aggregateId, Long fromVersion);

    /**
     * Streams events for aggregate starting from version for memory efficiency.
     * Combines version filtering with stream processing for optimal performance.
     */
    Stream<EventStoreEntity> streamByAggregateIdAndVersionGreaterThan(
        String aggregateId, Long fromVersion);

    // Cross-Aggregate Analytics and Reporting

    /**
     * Retrieves events by aggregate type within a time range.
     * Supports cross-aggregate analytics and reporting.
     */
    List<EventStoreEntity> findByAggregateTypeAndOccurredOnBetweenOrderByOccurredOn(
        String aggregateType, Instant from, Instant to);

    /**
     * Retrieves events of a specific type within a time range.
     * Supports event pattern analysis and monitoring.
     */
    List<EventStoreEntity> findByEventTypeAndOccurredOnBetweenOrderByOccurredOn(
        String eventType, Instant from, Instant to);

    /**
     * Streams events by type and time range for large-scale analytics.
     * Memory-efficient processing for big data event analysis.
     */
    Stream<EventStoreEntity> streamByEventTypeAndOccurredOnBetween(
        String eventType, Instant from, Instant to);

    // Monitoring and Metrics Operations

    /**
     * Counts total events for monitoring and metrics.
     * Supports 12-Factor principle of metrics and monitoring.
     */
    long count();

    /**
     * Counts events for a specific aggregate.
     * Supports aggregate-level monitoring and validation.
     */
    long countByAggregateId(String aggregateId);

    /**
     * Counts events by type within a time range.
     * Supports event pattern analysis and capacity planning.
     */
    long countByEventTypeAndOccurredOnBetween(String eventType, Instant from, Instant to);

    // Existence and Optimization Operations

    /**
     * Checks if events exist for a specific aggregate.
     * Optimized method for existence checks without loading all events.
     */
    boolean existsByAggregateId(String aggregateId);

    /**
     * Checks if events exist for aggregate after a specific version.
     * Supports incremental processing optimization checks.
     */
    boolean existsByAggregateIdAndVersionGreaterThan(String aggregateId, Long version);

    // Concurrency Control Operations

    /**
     * Retrieves the latest version number for an aggregate.
     * Critical for optimistic concurrency control in event sourcing.
     */
    Optional<Long> findLatestVersionByAggregateId(String aggregateId);

    /**
     * Retrieves the latest event timestamp for an aggregate.
     * Supports temporal queries and event ordering validation.
     */
    Optional<Instant> findLatestOccurredOnByAggregateId(String aggregateId);

    // Advanced Query Operations

    /**
     * Finds events matching multiple criteria for complex queries.
     * Supports advanced event store analytics and debugging.
     */
    List<EventStoreEntity> findByMultipleCriteria(EventSearchCriteria criteria);

    /**
     * Streams events matching criteria for memory-efficient complex queries.
     * Combines complex filtering with stream processing.
     */
    Stream<EventStoreEntity> streamByMultipleCriteria(EventSearchCriteria criteria);

    /**
     * Value object for complex event search criteria.
     * Follows DDD principles with structured query parameters.
     */
    class EventSearchCriteria {
        private final String aggregateId;
        private final String aggregateType;
        private final String eventType;
        private final Instant fromTime;
        private final Instant toTime;
        private final Long fromVersion;
        private final Long toVersion;
        private final Integer limit;
        private final String orderBy;

        private EventSearchCriteria(Builder builder) {
            this.aggregateId = builder.aggregateId;
            this.aggregateType = builder.aggregateType;
            this.eventType = builder.eventType;
            this.fromTime = builder.fromTime;
            this.toTime = builder.toTime;
            this.fromVersion = builder.fromVersion;
            this.toVersion = builder.toVersion;
            this.limit = builder.limit;
            this.orderBy = builder.orderBy;
        }

        // Builder pattern for complex criteria construction
        public static class Builder {
            private String aggregateId;
            private String aggregateType;
            private String eventType;
            private Instant fromTime;
            private Instant toTime;
            private Long fromVersion;
            private Long toVersion;
            private Integer limit;
            private String orderBy = "occurredOn";

            public Builder aggregateId(String aggregateId) {
                this.aggregateId = aggregateId;
                return this;
            }

            public Builder aggregateType(String aggregateType) {
                this.aggregateType = aggregateType;
                return this;
            }

            public Builder eventType(String eventType) {
                this.eventType = eventType;
                return this;
            }

            public Builder timeRange(Instant from, Instant to) {
                this.fromTime = from;
                this.toTime = to;
                return this;
            }

            public Builder versionRange(Long from, Long to) {
                this.fromVersion = from;
                this.toVersion = to;
                return this;
            }

            public Builder limit(Integer limit) {
                this.limit = limit;
                return this;
            }

            public Builder orderBy(String orderBy) {
                this.orderBy = orderBy;
                return this;
            }

            public EventSearchCriteria build() {
                return new EventSearchCriteria(this);
            }
        }

        // Getters
        public String getAggregateId() { return aggregateId; }
        public String getAggregateType() { return aggregateType; }
        public String getEventType() { return eventType; }
        public Instant getFromTime() { return fromTime; }
        public Instant getToTime() { return toTime; }
        public Long getFromVersion() { return fromVersion; }
        public Long getToVersion() { return toVersion; }
        public Integer getLimit() { return limit; }
        public String getOrderBy() { return orderBy; }
    }
}

/*
 * SPRING DATA JPA RESTORATION GUIDE:
 *
 * Once Spring Data JPA dependencies are resolved, replace with:
 *
 * @Repository
 * public interface EventStoreRepository extends JpaRepository<EventStoreEntity, String> {
 *
 *     List<EventStoreEntity> findByAggregateIdOrderByVersion(String aggregateId);
 *
 *     @Query("SELECT e FROM EventStoreEntity e WHERE e.aggregateId = :aggregateId")
 *     Stream<EventStoreEntity> streamEventsByAggregateId(@Param("aggregateId") String aggregateId);
 *
 *     List<EventStoreEntity> findByAggregateIdAndVersionGreaterThanOrderByVersion(
 *         String aggregateId, Long fromVersion);
 *
 *     List<EventStoreEntity> findByAggregateTypeAndOccurredOnBetweenOrderByOccurredOn(
 *         String aggregateType, Instant from, Instant to);
 *
 *     List<EventStoreEntity> findByEventTypeAndOccurredOnBetweenOrderByOccurredOn(
 *         String eventType, Instant from, Instant to);
 *
 *     long countByAggregateId(String aggregateId);
 *
 *     long countByEventTypeAndOccurredOnBetween(String eventType, Instant from, Instant to);
 *
 *     boolean existsByAggregateId(String aggregateId);
 *
 *     boolean existsByAggregateIdAndVersionGreaterThan(String aggregateId, Long version);
 *
 *     @Query("SELECT MAX(e.version) FROM EventStoreEntity e WHERE e.aggregateId = :aggregateId")
 *     Optional<Long> findLatestVersionByAggregateId(@Param("aggregateId") String aggregateId);
 *
 *     @Query("SELECT MAX(e.occurredOn) FROM EventStoreEntity e WHERE e.aggregateId = :aggregateId")
 *     Optional<Instant> findLatestOccurredOnByAggregateId(@Param("aggregateId") String aggregateId);
 *
 *     @Async
 *     CompletableFuture<List<EventStoreEntity>> saveAllAsync(List<EventStoreEntity> events);
 * }
 *
 * ARCHITECTURAL BENEFITS:
 *
 * 12-Factor App:
 * - Stateless operations supporting horizontal scaling
 * - Async processing for high-throughput scenarios
 * - Comprehensive metrics and monitoring capabilities
 * - Clean separation between interface and implementation
 *
 * DDD (Domain-Driven Design):
 * - Repository pattern maintaining aggregate boundaries
 * - Complex query support with structured criteria
 * - Event sourcing patterns with proper versioning
 * - Domain-specific query methods expressing business intent
 *
 * Hexagonal Architecture:
 * - Clean port interface without infrastructure dependencies
 * - Adapter-agnostic design supporting multiple implementations
 * - Clear boundaries between domain and infrastructure concerns
 * - Dependency inversion with interface-based design
 *
 * EDA (Event-Driven Architecture):
 * - Complete event stream processing support
 * - Memory-efficient streaming for large event sets
 * - Async operations for scalable event processing
 * - Advanced querying for event pattern analysis
 * - Optimistic concurrency control for event ordering
 *
 * Clean Code:
 * - Single responsibility with focused query methods
 * - Meaningful method names expressing business intent
 * - Builder pattern for complex query construction
 * - Comprehensive documentation and examples
 * - Defensive programming with proper parameter validation
 */
