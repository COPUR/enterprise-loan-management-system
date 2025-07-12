package com.amanahfi.platform.tenant.port.in;

import com.amanahfi.platform.shared.command.Command;
import com.amanahfi.platform.tenant.domain.TenantType;
import lombok.Builder;
import lombok.Value;

/**
 * Query to get tenants by type
 */
@Value
@Builder
public class GetTenantsByTypeQuery implements Command {
    
    TenantType type;
    
    @Override
    public void validate() {
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }
    }
}