package com.amanahfi.platform.tenant.domain.events;

import com.amanahfi.platform.shared.domain.DomainEvent;
import com.amanahfi.platform.shared.domain.EventMetadata;
import com.amanahfi.platform.tenant.domain.TenantConfiguration;
import com.amanahfi.platform.tenant.domain.TenantId;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Event raised when tenant configuration is updated
 */
@Value
@Builder
public class TenantConfigurationUpdatedEvent extends DomainEvent {
    
    TenantId tenantId;
    TenantConfiguration previousConfiguration;
    TenantConfiguration newConfiguration;
    String updatedBy;
    Instant updatedAt;
    
    public TenantConfigurationUpdatedEvent(
            TenantId tenantId,
            TenantConfiguration previousConfiguration,
            TenantConfiguration newConfiguration,
            String updatedBy,
            Instant updatedAt
    ) {
        super(
            tenantId.toString(),
            "Tenant",
            EventMetadata.builder()
                .correlationId(java.util.UUID.randomUUID().toString())
                .causationId(java.util.UUID.randomUUID().toString())
                .userId(updatedBy)
                .build()
        );
        
        this.tenantId = tenantId;
        this.previousConfiguration = previousConfiguration;
        this.newConfiguration = newConfiguration;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }
}