package com.amanahfi.platform.tenant.port.in;

import com.amanahfi.platform.regulatory.domain.Jurisdiction;
import com.amanahfi.platform.shared.command.Command;
import lombok.Builder;
import lombok.Value;

/**
 * Query to get tenants by jurisdiction
 */
@Value
@Builder
public class GetTenantsByJurisdictionQuery implements Command {
    
    Jurisdiction jurisdiction;
    
    @Override
    public void validate() {
        if (jurisdiction == null) {
            throw new IllegalArgumentException("Jurisdiction cannot be null");
        }
    }
}