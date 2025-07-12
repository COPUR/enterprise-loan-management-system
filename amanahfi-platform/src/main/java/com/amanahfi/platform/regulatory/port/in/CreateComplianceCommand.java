package com.amanahfi.platform.regulatory.port.in;

import com.amanahfi.platform.regulatory.domain.ComplianceType;
import com.amanahfi.platform.regulatory.domain.Jurisdiction;
import com.amanahfi.platform.shared.command.Command;
import com.amanahfi.platform.shared.command.CommandMetadata;
import com.amanahfi.platform.shared.idempotence.IdempotencyKey;
import lombok.Builder;
import lombok.Value;

import java.util.Objects;
import java.util.UUID;

/**
 * Command to create a new regulatory compliance record
 */
@Value
@Builder
public class CreateComplianceCommand implements Command {
    UUID commandId = UUID.randomUUID();
    IdempotencyKey idempotencyKey;
    CommandMetadata metadata;
    
    String entityId;
    ComplianceType complianceType;
    Jurisdiction jurisdiction;
    
    @Override
    public void validate() {
        Objects.requireNonNull(idempotencyKey, "Idempotency key cannot be null");
        Objects.requireNonNull(entityId, "Entity ID cannot be null");
        Objects.requireNonNull(complianceType, "Compliance type cannot be null");
        Objects.requireNonNull(jurisdiction, "Jurisdiction cannot be null");
        
        if (entityId.trim().isEmpty()) {
            throw new IllegalArgumentException("Entity ID cannot be empty");
        }
    }
}