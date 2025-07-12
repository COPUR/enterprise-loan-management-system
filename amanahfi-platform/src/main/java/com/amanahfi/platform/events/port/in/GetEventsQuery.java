package com.amanahfi.platform.events.port.in;

import com.amanahfi.platform.shared.command.Command;
import lombok.Builder;
import lombok.Value;

/**
 * Query to get events for an aggregate from a specific version
 */
@Value
@Builder
public class GetEventsQuery implements Command {
    
    String aggregateId;
    long fromVersion;
    
    @Override
    public void validate() {
        if (aggregateId == null || aggregateId.trim().isEmpty()) {
            throw new IllegalArgumentException("Aggregate ID cannot be null or empty");
        }
        if (fromVersion < 0) {
            throw new IllegalArgumentException("From version cannot be negative");
        }
    }
}