package com.amanahfi.platform.events.port.in;

import com.amanahfi.platform.events.domain.EventCategory;
import com.amanahfi.platform.shared.command.Command;
import lombok.Builder;
import lombok.Value;

/**
 * Query to get events by category
 */
@Value
@Builder
public class GetEventsByCategoryQuery implements Command {
    
    String aggregateId;
    EventCategory eventCategory;
    
    @Override
    public void validate() {
        if (aggregateId == null || aggregateId.trim().isEmpty()) {
            throw new IllegalArgumentException("Aggregate ID cannot be null or empty");
        }
        if (eventCategory == null) {
            throw new IllegalArgumentException("Event category cannot be null");
        }
    }
}