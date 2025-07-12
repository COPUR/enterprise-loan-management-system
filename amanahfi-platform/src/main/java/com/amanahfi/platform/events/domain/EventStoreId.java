package com.amanahfi.platform.events.domain;

import lombok.Value;
import java.util.Objects;
import java.util.UUID;

/**
 * Strongly-typed identifier for Event Store
 */
@Value
public class EventStoreId {
    String value;
    
    private EventStoreId(String value) {
        this.value = Objects.requireNonNull(value, "Event Store ID cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Event Store ID cannot be empty");
        }
    }
    
    public static EventStoreId of(String value) {
        return new EventStoreId(value);
    }
    
    public static EventStoreId generate() {
        return new EventStoreId("ES-" + UUID.randomUUID().toString());
    }
    
    public static EventStoreId fromAggregate(String aggregateId, String aggregateType) {
        return new EventStoreId("ES-" + aggregateType + "-" + aggregateId);
    }
}