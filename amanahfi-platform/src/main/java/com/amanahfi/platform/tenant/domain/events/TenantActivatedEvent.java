package com.amanahfi.platform.tenant.domain.events;

import com.amanahfi.platform.shared.domain.DomainEvent;
import com.amanahfi.platform.shared.domain.EventMetadata;
import com.amanahfi.platform.tenant.domain.TenantId;
import com.amanahfi.platform.tenant.domain.TenantStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Event raised when a tenant is activated
 */
@Value
@Builder
public class TenantActivatedEvent extends DomainEvent {
    
    TenantId tenantId;
    TenantStatus previousStatus;
    TenantStatus newStatus;
    String updatedBy;
    Instant updatedAt;
    
    public TenantActivatedEvent(
            TenantId tenantId,
            TenantStatus previousStatus,
            TenantStatus newStatus,
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
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }
}