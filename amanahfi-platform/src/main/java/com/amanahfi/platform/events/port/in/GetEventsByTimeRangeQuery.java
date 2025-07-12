package com.amanahfi.platform.events.port.in;

import com.amanahfi.platform.shared.command.Command;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Query to get events by time range
 */
@Value
@Builder
public class GetEventsByTimeRangeQuery implements Command {
    
    String aggregateId;
    Instant fromTime;
    Instant toTime;
    
    @Override
    public void validate() {
        if (aggregateId == null || aggregateId.trim().isEmpty()) {
            throw new IllegalArgumentException("Aggregate ID cannot be null or empty");
        }
        if (fromTime == null) {
            throw new IllegalArgumentException("From time cannot be null");
        }
        if (toTime == null) {
            throw new IllegalArgumentException("To time cannot be null");
        }
        if (fromTime.isAfter(toTime)) {
            throw new IllegalArgumentException("From time must be before to time");
        }
    }
}