package com.amanahfi.platform.regulatory.port.in;

import com.amanahfi.platform.regulatory.domain.AssessmentResult;
import com.amanahfi.platform.regulatory.domain.ComplianceId;
import com.amanahfi.platform.regulatory.domain.RegulatoryAuthority;
import com.amanahfi.platform.shared.command.Command;
import com.amanahfi.platform.shared.command.CommandMetadata;
import com.amanahfi.platform.shared.idempotence.IdempotencyKey;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Command to perform a compliance assessment
 */
@Value
@Builder
public class PerformAssessmentCommand implements Command {
    UUID commandId = UUID.randomUUID();
    IdempotencyKey idempotencyKey;
    CommandMetadata metadata;
    
    ComplianceId complianceId;
    RegulatoryAuthority authority;
    AssessmentResult result;
    Double score; // 0.0 to 100.0
    String assessorId;
    List<String> findings;
    List<String> recommendations;
    List<String> pendingRequirements;
    
    @Override
    public void validate() {
        Objects.requireNonNull(idempotencyKey, "Idempotency key cannot be null");
        Objects.requireNonNull(complianceId, "Compliance ID cannot be null");
        Objects.requireNonNull(authority, "Authority cannot be null");
        Objects.requireNonNull(result, "Assessment result cannot be null");
        Objects.requireNonNull(assessorId, "Assessor ID cannot be null");
        
        if (score != null && (score < 0.0 || score > 100.0)) {
            throw new IllegalArgumentException("Score must be between 0.0 and 100.0");
        }
    }
}