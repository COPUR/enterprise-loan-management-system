package com.amanahfi.platform.events.port.in;

import com.amanahfi.platform.shared.command.Command;
import lombok.Builder;
import lombok.Value;

/**
 * Query to get snapshot of event store
 */
@Value
@Builder
public class GetSnapshotQuery implements Command {
    
    String aggregateId;
    
    @Override
    public void validate() {
        if (aggregateId == null || aggregateId.trim().isEmpty()) {
            throw new IllegalArgumentException("Aggregate ID cannot be null or empty");
        }
    }
}