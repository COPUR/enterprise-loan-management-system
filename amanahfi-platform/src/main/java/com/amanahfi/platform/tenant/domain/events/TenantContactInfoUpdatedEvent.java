package com.amanahfi.platform.tenant.domain.events;

import com.amanahfi.platform.shared.domain.DomainEvent;
import com.amanahfi.platform.shared.domain.EventMetadata;
import com.amanahfi.platform.tenant.domain.TenantId;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Event raised when tenant contact information is updated
 */
@Value
@Builder
public class TenantContactInfoUpdatedEvent extends DomainEvent {
    
    TenantId tenantId;
    String previousEmail;
    String previousPhone;
    String newEmail;
    String newPhone;
    String updatedBy;
    Instant updatedAt;
    
    public TenantContactInfoUpdatedEvent(
            TenantId tenantId,
            String previousEmail,
            String previousPhone,
            String newEmail,
            String newPhone,
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
        this.previousEmail = previousEmail;
        this.previousPhone = previousPhone;
        this.newEmail = newEmail;
        this.newPhone = newPhone;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }
}