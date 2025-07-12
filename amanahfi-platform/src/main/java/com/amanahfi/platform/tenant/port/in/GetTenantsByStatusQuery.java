package com.amanahfi.platform.tenant.port.in;

import com.amanahfi.platform.shared.command.Command;
import com.amanahfi.platform.tenant.domain.TenantStatus;
import lombok.Builder;
import lombok.Value;

/**
 * Query to get tenants by status
 */
@Value
@Builder
public class GetTenantsByStatusQuery implements Command {
    
    TenantStatus status;
    
    @Override
    public void validate() {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
    }
}