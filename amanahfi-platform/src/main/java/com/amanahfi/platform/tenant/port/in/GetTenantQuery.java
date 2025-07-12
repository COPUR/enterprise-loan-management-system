package com.amanahfi.platform.tenant.port.in;

import com.amanahfi.platform.shared.command.Command;
import com.amanahfi.platform.tenant.domain.TenantId;
import lombok.Builder;
import lombok.Value;

/**
 * Query to get a tenant by ID
 */
@Value
@Builder
public class GetTenantQuery implements Command {
    
    TenantId tenantId;
    
    @Override
    public void validate() {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
    }
}