package com.amanahfi.platform.tenant.port.in;

import com.amanahfi.platform.shared.command.Command;
import com.amanahfi.platform.tenant.domain.TenantId;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

/**
 * Command to create tenant context
 */
@Value
@Builder
public class CreateTenantContextCommand implements Command {
    
    TenantId tenantId;
    String userId;
    String sessionId;
    String correlationId;
    Instant requestTime;
    String clientIp;
    String userAgent;
    String language;
    String currency;
    String timezone;
    boolean isAdmin;
    boolean hasElevatedPrivileges;
    Map<String, String> attributes;
    
    @Override
    public void validate() {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        if (correlationId == null || correlationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Correlation ID cannot be null or empty");
        }
        
        if (requestTime == null) {
            throw new IllegalArgumentException("Request time cannot be null");
        }
    }
}