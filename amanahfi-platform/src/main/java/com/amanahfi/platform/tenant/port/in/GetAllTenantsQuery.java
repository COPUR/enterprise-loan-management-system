package com.amanahfi.platform.tenant.port.in;

import com.amanahfi.platform.shared.command.Command;
import lombok.Builder;
import lombok.Value;

/**
 * Query to get all tenants
 */
@Value
@Builder
public class GetAllTenantsQuery implements Command {
    
    @Override
    public void validate() {
        // No validation needed for this query
    }
}