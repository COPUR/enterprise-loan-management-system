package com.amanahfi.platform.events.port.in;

import com.amanahfi.platform.shared.command.Command;
import com.amanahfi.platform.shared.domain.DomainEvent;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Command to store events for an aggregate
 */
@Value
@Builder
public class StoreEventsCommand implements Command {
    
    String aggregateId;
    String aggregateType;
    List<DomainEvent> events;
    long expectedVersion;
    
    @Override
    public void validate() {
        if (aggregateId == null || aggregateId.trim().isEmpty()) {
            throw new IllegalArgumentException("Aggregate ID cannot be null or empty");
        }
        if (aggregateType == null || aggregateType.trim().isEmpty()) {
            throw new IllegalArgumentException("Aggregate type cannot be null or empty");
        }
        if (events == null || events.isEmpty()) {
            throw new IllegalArgumentException("Events list cannot be null or empty");
        }
        if (expectedVersion < 0) {
            throw new IllegalArgumentException("Expected version cannot be negative");
        }
        
        // Validate all events belong to the same aggregate
        for (DomainEvent event : events) {
            if (!aggregateId.equals(event.getAggregateId())) {
                throw new IllegalArgumentException("All events must belong to the same aggregate");
            }
        }
    }
}