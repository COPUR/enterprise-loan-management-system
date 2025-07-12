package com.amanahfi.platform.tenant.port.in;

import com.amanahfi.platform.shared.command.Command;
import com.amanahfi.platform.tenant.domain.TenantId;
import lombok.Builder;
import lombok.Value;

/**
 * Command to update tenant contact information
 */
@Value
@Builder
public class UpdateTenantContactInfoCommand implements Command {
    
    TenantId tenantId;
    String contactEmail;
    String contactPhone;
    String updatedBy;
    
    @Override
    public void validate() {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        
        if (contactEmail == null || contactEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Contact email cannot be null or empty");
        }
        
        if (!contactEmail.contains("@")) {
            throw new IllegalArgumentException("Contact email must be a valid email address");
        }
        
        if (updatedBy == null || updatedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Updated by cannot be null or empty");
        }
    }
}