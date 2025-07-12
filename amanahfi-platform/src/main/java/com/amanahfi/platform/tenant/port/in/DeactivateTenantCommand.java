package com.amanahfi.platform.tenant.port.in;

import com.amanahfi.platform.shared.command.Command;
import com.amanahfi.platform.tenant.domain.TenantId;
import lombok.Builder;
import lombok.Value;

/**
 * Command to deactivate a tenant
 */
@Value
@Builder
public class DeactivateTenantCommand implements Command {
    
    TenantId tenantId;
    String reason;
    String updatedBy;
    
    @Override
    public void validate() {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason cannot be null or empty");
        }
        
        if (updatedBy == null || updatedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Updated by cannot be null or empty");
        }
    }
}