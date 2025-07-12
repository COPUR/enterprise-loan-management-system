package com.amanahfi.platform.events.port.in;

import com.amanahfi.platform.shared.command.Command;
import lombok.Builder;
import lombok.Value;

/**
 * Command to delete event store (dangerous operation)
 */
@Value
@Builder
public class DeleteEventStoreCommand implements Command {
    
    String aggregateId;
    boolean confirmed;
    
    @Override
    public void validate() {
        if (aggregateId == null || aggregateId.trim().isEmpty()) {
            throw new IllegalArgumentException("Aggregate ID cannot be null or empty");
        }
    }
}