package com.amanahfi.platform.regulatory.port.in;

import com.amanahfi.platform.regulatory.domain.ComplianceId;
import com.amanahfi.platform.shared.command.Command;
import com.amanahfi.platform.shared.command.CommandMetadata;
import com.amanahfi.platform.shared.idempotence.IdempotencyKey;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Command to remediate a compliance violation
 */
@Value
@Builder
public class RemediateViolationCommand implements Command {
    UUID commandId = UUID.randomUUID();
    IdempotencyKey idempotencyKey;
    CommandMetadata metadata;
    
    ComplianceId complianceId;
    String violationId;
    String remediatedBy;
    String description;
    List<String> actionsTaken;
    List<String> preventiveMeasures;
    String evidenceReference;
    boolean regulatoryNotificationRequired;
    
    @Override
    public void validate() {
        Objects.requireNonNull(idempotencyKey, "Idempotency key cannot be null");
        Objects.requireNonNull(complianceId, "Compliance ID cannot be null");
        Objects.requireNonNull(violationId, "Violation ID cannot be null");
        Objects.requireNonNull(remediatedBy, "Remediated by cannot be null");
        Objects.requireNonNull(description, "Description cannot be null");
        Objects.requireNonNull(actionsTaken, "Actions taken cannot be null");
        Objects.requireNonNull(preventiveMeasures, "Preventive measures cannot be null");
        
        if (violationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Violation ID cannot be empty");
        }
        if (description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be empty");
        }
        if (actionsTaken.isEmpty()) {
            throw new IllegalArgumentException("At least one action taken must be specified");
        }
    }
}