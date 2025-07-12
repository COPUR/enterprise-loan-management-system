package com.amanahfi.platform.regulatory.port.in;

import com.amanahfi.platform.regulatory.domain.ComplianceId;
import com.amanahfi.platform.regulatory.domain.ComplianceReport;
import com.amanahfi.platform.regulatory.domain.RegulatoryAuthority;
import com.amanahfi.platform.shared.command.Command;
import com.amanahfi.platform.shared.command.CommandMetadata;
import com.amanahfi.platform.shared.idempotence.IdempotencyKey;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Command to submit a regulatory compliance report
 */
@Value
@Builder
public class SubmitReportCommand implements Command {
    UUID commandId = UUID.randomUUID();
    IdempotencyKey idempotencyKey;
    CommandMetadata metadata;
    
    ComplianceId complianceId;
    ComplianceReport.ReportType reportType;
    RegulatoryAuthority authority;
    LocalDate periodStart;
    LocalDate periodEnd;
    String submittedBy;
    Map<String, Object> reportData;
    
    @Override
    public void validate() {
        Objects.requireNonNull(idempotencyKey, "Idempotency key cannot be null");
        Objects.requireNonNull(complianceId, "Compliance ID cannot be null");
        Objects.requireNonNull(reportType, "Report type cannot be null");
        Objects.requireNonNull(authority, "Authority cannot be null");
        Objects.requireNonNull(periodStart, "Period start date cannot be null");
        Objects.requireNonNull(periodEnd, "Period end date cannot be null");
        Objects.requireNonNull(submittedBy, "Submitted by cannot be null");
        Objects.requireNonNull(reportData, "Report data cannot be null");
        
        if (periodEnd.isBefore(periodStart)) {
            throw new IllegalArgumentException("Period end date must be after start date");
        }
    }
}