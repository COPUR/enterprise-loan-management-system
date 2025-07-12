package com.amanahfi.platform.regulatory.port.in;

import com.amanahfi.platform.regulatory.domain.ComplianceId;
import com.amanahfi.platform.regulatory.domain.ComplianceReport;
import com.amanahfi.platform.regulatory.domain.ComplianceSummary;
import com.amanahfi.platform.regulatory.domain.RegulatoryAuthority;

import java.util.List;

/**
 * Input port for regulatory compliance use cases
 */
public interface RegulatoryComplianceUseCase {
    
    // Command operations
    ComplianceId createCompliance(CreateComplianceCommand command);
    void performAssessment(PerformAssessmentCommand command);
    void submitReport(SubmitReportCommand command);
    void recordViolation(RecordViolationCommand command);
    void remediateViolation(RemediateViolationCommand command);
    
    // Query operations
    ComplianceSummary getComplianceSummary(ComplianceId complianceId);
    List<ComplianceSummary> getComplianceByEntity(String entityId);
    List<ComplianceReport> getReportsByAuthority(ComplianceId complianceId, RegulatoryAuthority authority);
}