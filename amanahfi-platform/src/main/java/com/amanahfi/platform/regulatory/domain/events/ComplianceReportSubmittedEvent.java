package com.amanahfi.platform.regulatory.domain.events;

import com.amanahfi.platform.regulatory.domain.ComplianceReport;
import com.amanahfi.platform.regulatory.domain.RegulatoryAuthority;
import com.amanahfi.platform.shared.domain.DomainEvent;
import com.amanahfi.platform.shared.domain.EventMetadata;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * Event raised when a compliance report is submitted
 */
@Value
@Builder
public class ComplianceReportSubmittedEvent implements DomainEvent {
    UUID eventId = UUID.randomUUID();
    String complianceId;
    String reportId;
    ComplianceReport.ReportType reportType;
    RegulatoryAuthority authority;
    Instant submittedAt;
    EventMetadata metadata = EventMetadata.create();
    
    @Override
    public String getAggregateId() {
        return complianceId;
    }
    
    @Override
    public String getAggregateType() {
        return "RegulatoryCompliance";
    }
    
    @Override
    public Instant getOccurredOn() {
        return submittedAt;
    }
    
    @Override
    public boolean requiresRegulatoryReporting() {
        return true;
    }
}