package com.amanahfi.platform.tenant.port.in;

import com.amanahfi.platform.shared.command.Command;
import com.amanahfi.platform.tenant.domain.TenantConfiguration;
import com.amanahfi.platform.tenant.domain.TenantId;
import lombok.Builder;
import lombok.Value;

/**
 * Command to update tenant configuration
 */
@Value
@Builder
public class UpdateTenantConfigurationCommand implements Command {
    
    TenantId tenantId;
    TenantConfiguration configuration;
    String updatedBy;
    
    @Override
    public void validate() {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        
        if (updatedBy == null || updatedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Updated by cannot be null or empty");
        }
        
        // Validate configuration
        configuration.validate();
    }
}