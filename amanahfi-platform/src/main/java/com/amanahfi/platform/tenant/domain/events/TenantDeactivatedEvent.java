package com.amanahfi.platform.tenant.domain.events;

import com.amanahfi.platform.shared.domain.DomainEvent;
import com.amanahfi.platform.shared.domain.EventMetadata;
import com.amanahfi.platform.tenant.domain.TenantId;
import com.amanahfi.platform.tenant.domain.TenantStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Event raised when a tenant is deactivated
 */
@Value
@Builder
public class TenantDeactivatedEvent extends DomainEvent {
    
    TenantId tenantId;
    TenantStatus previousStatus;
    String reason;
    String updatedBy;
    Instant updatedAt;
    
    public TenantDeactivatedEvent(
            TenantId tenantId,
            TenantStatus previousStatus,
            String reason,
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
        this.reason = reason;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }
}