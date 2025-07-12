package com.amanahfi.platform.tenant.domain;

import com.amanahfi.platform.shared.domain.DomainId;
import lombok.Value;

import java.util.UUID;

/**
 * Tenant identifier value object
 */
@Value
public class TenantId implements DomainId {
    
    UUID value;
    
    public static TenantId of(UUID value) {
        return new TenantId(value);
    }
    
    public static TenantId of(String value) {
        return new TenantId(UUID.fromString(value));
    }
    
    public static TenantId generate() {
        return new TenantId(UUID.randomUUID());
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
    
    @Override
    public String asString() {
        return value.toString();
    }
}