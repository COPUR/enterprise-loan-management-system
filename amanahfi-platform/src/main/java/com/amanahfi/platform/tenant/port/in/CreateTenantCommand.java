package com.amanahfi.platform.tenant.port.in;

import com.amanahfi.platform.shared.command.Command;
import com.amanahfi.platform.tenant.domain.TenantConfiguration;
import com.amanahfi.platform.tenant.domain.TenantType;
import lombok.Builder;
import lombok.Value;

/**
 * Command to create a new tenant
 */
@Value
@Builder
public class CreateTenantCommand implements Command {
    
    String name;
    String description;
    TenantType type;
    TenantConfiguration configuration;
    String contactEmail;
    String contactPhone;
    String administratorUserId;
    String createdBy;
    
    @Override
    public void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant name cannot be null or empty");
        }
        
        if (type == null) {
            throw new IllegalArgumentException("Tenant type cannot be null");
        }
        
        if (configuration == null) {
            throw new IllegalArgumentException("Tenant configuration cannot be null");
        }
        
        if (contactEmail == null || contactEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Contact email cannot be null or empty");
        }
        
        if (administratorUserId == null || administratorUserId.trim().isEmpty()) {
            throw new IllegalArgumentException("Administrator user ID cannot be null or empty");
        }
        
        if (createdBy == null || createdBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Created by cannot be null or empty");
        }
        
        // Validate configuration
        configuration.validate();
    }
}