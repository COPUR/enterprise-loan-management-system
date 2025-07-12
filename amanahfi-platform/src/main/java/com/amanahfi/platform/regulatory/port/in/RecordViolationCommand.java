package com.amanahfi.platform.regulatory.port.in;

import com.amanahfi.platform.regulatory.domain.ComplianceId;
import com.amanahfi.platform.regulatory.domain.RegulatoryAuthority;
import com.amanahfi.platform.regulatory.domain.ViolationSeverity;
import com.amanahfi.platform.shared.command.Command;
import com.amanahfi.platform.shared.command.CommandMetadata;
import com.amanahfi.platform.shared.idempotence.IdempotencyKey;
import lombok.Builder;
import lombok.Value;

import java.util.Objects;
import java.util.UUID;

/**
 * Command to record a compliance violation
 */
@Value
@Builder
public class RecordViolationCommand implements Command {
    UUID commandId = UUID.randomUUID();
    IdempotencyKey idempotencyKey;
    CommandMetadata metadata;
    
    ComplianceId complianceId;
    RegulatoryAuthority authority;
    ViolationSeverity severity;
    String violationCode;
    String description;
    String regulatoryReference;
    String detectedBy;
    
    @Override
    public void validate() {
        Objects.requireNonNull(idempotencyKey, "Idempotency key cannot be null");
        Objects.requireNonNull(complianceId, "Compliance ID cannot be null");
        Objects.requireNonNull(authority, "Authority cannot be null");
        Objects.requireNonNull(severity, "Severity cannot be null");
        Objects.requireNonNull(violationCode, "Violation code cannot be null");
        Objects.requireNonNull(description, "Description cannot be null");
        Objects.requireNonNull(detectedBy, "Detected by cannot be null");
        
        if (violationCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Violation code cannot be empty");
        }
        if (description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be empty");
        }
    }
}