package com.amanahfi.platform.tenant.domain.events;

import com.amanahfi.platform.regulatory.domain.Jurisdiction;
import com.amanahfi.platform.shared.domain.DomainEvent;
import com.amanahfi.platform.shared.domain.EventMetadata;
import com.amanahfi.platform.tenant.domain.TenantId;
import com.amanahfi.platform.tenant.domain.TenantType;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Event raised when a new tenant is created
 */
@Value
@Builder
public class TenantCreatedEvent extends DomainEvent {
    
    TenantId tenantId;
    String name;
    TenantType type;
    Jurisdiction primaryJurisdiction;
    String contactEmail;
    String administratorUserId;
    String createdBy;
    Instant createdAt;
    
    public TenantCreatedEvent(
            TenantId tenantId,
            String name,
            TenantType type,
            Jurisdiction primaryJurisdiction,
            String contactEmail,
            String administratorUserId,
            String createdBy,
            Instant createdAt
    ) {
        super(
            tenantId.toString(),
            "Tenant",
            EventMetadata.builder()
                .correlationId(java.util.UUID.randomUUID().toString())
                .causationId(java.util.UUID.randomUUID().toString())
                .userId(createdBy)
                .build()
        );
        
        this.tenantId = tenantId;
        this.name = name;
        this.type = type;
        this.primaryJurisdiction = primaryJurisdiction;
        this.contactEmail = contactEmail;
        this.administratorUserId = administratorUserId;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }
}