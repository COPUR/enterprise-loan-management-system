package com.amanahfi.platform.events.port.out;

import com.amanahfi.platform.shared.domain.DomainEvent;

import java.util.List;

/**
 * Interface for publishing events to external streams (e.g., Kafka)
 */
public interface EventStreamPublisher {
    
    /**
     * Publish a single event to the specified stream
     */
    void publish(String streamName, DomainEvent event);
    
    /**
     * Publish multiple events to the specified stream
     */
    void publish(String streamName, List<DomainEvent> events);
    
    /**
     * Publish event with partition key for ordered processing
     */
    void publishWithPartitionKey(String streamName, DomainEvent event, String partitionKey);
    
    /**
     * Publish events with partition key for ordered processing
     */
    void publishWithPartitionKey(String streamName, List<DomainEvent> events, String partitionKey);
    
    /**
     * Check if stream is healthy
     */
    boolean isHealthy();
    
    /**
     * Get stream statistics
     */
    StreamStats getStreamStats(String streamName);
}