package com.amanahfi.platform.regulatory.infrastructure.adapter;

import com.amanahfi.platform.regulatory.domain.*;
import com.amanahfi.platform.regulatory.port.out.RegulatoryApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Unified regulatory API client that delegates to specific authority adapters
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UnifiedRegulatoryApiClient implements RegulatoryApiClient {
    
    private final CbuaeApiAdapter cbuaeAdapter;
    private final VaraApiAdapter varaAdapter;
    private final HsaApiAdapter hsaAdapter;
    
    @Override
    public void submitAssessment(
            RegulatoryAuthority authority,
            ComplianceSummary compliance,
            ComplianceAssessment assessment) {
        
        log.info("Submitting assessment to authority: {}", authority);
        
        switch (authority) {
            case CBUAE -> cbuaeAdapter.submitOpenFinanceCompliance(compliance, assessment);
            case VARA -> varaAdapter.submitVirtualAssetCompliance(compliance, assessment);
            case HSA -> hsaAdapter.submitShariaComplianceAssessment(compliance, assessment);
            default -> log.warn("No API adapter configured for authority: {}", authority);
        }
    }
    
    @Override
    public String submitReport(
            RegulatoryAuthority authority,
            ComplianceReport report,
            Map<String, Object> reportData) {
        
        log.info("Submitting report to authority: {}", authority);
        
        return switch (authority) {
            case CBUAE -> {
                if (report.getReportType() == ComplianceReport.ReportType.SUSPICIOUS_ACTIVITY) {
                    // For SAR, we need violation details
                    yield cbuaeAdapter.submitSuspiciousActivityReport(
                        createDummyViolation(), reportData);
                } else {
                    yield cbuaeAdapter.submitAmlReport(report, reportData);
                }
            }
            case VARA -> varaAdapter.submitCbdcTransactionReport(report, reportData);
            case HSA -> hsaAdapter.submitIslamicFinanceReport(report, reportData);
            default -> {
                log.warn("No API adapter configured for authority: {}", authority);
                yield "FALLBACK-" + report.getReportId();
            }
        };
    }
    
    @Override
    public String notifyViolation(
            RegulatoryAuthority authority,
            ComplianceSummary compliance,
            ComplianceViolation violation) {
        
        log.info("Notifying authority {} of violation: {}", authority, violation.getViolationId());
        
        Map<String, Object> violationDetails = Map.of(
            "complianceId", compliance.getComplianceId().getValue(),
            "entityId", compliance.getEntityId(),
            "violationCode", violation.getViolationCode(),
            "severity", violation.getSeverity()
        );
        
        return switch (authority) {
            case CBUAE -> cbuaeAdapter.submitSuspiciousActivityReport(violation, violationDetails);
            case VARA -> varaAdapter.notifyCryptoViolation(violation, violationDetails);
            case HSA -> hsaAdapter.notifyShariaViolation(violation, violationDetails);
            default -> {
                log.warn("No API adapter configured for authority: {}", authority);
                yield "FALLBACK-VIOL-" + violation.getViolationId();
            }
        };
    }
    
    @Override
    public String notifyRemediation(
            Jurisdiction jurisdiction,
            String violationId,
            RemediationDetails remediation) {
        
        log.info("Notifying remediation for violation: {} in jurisdiction: {}", 
            violationId, jurisdiction);
        
        // For now, return a reference number
        // In production, this would notify the appropriate authority
        return "REM-" + jurisdiction.getIsoCode() + "-" + violationId;
    }
    
    @Override
    public boolean isHealthy(RegulatoryAuthority authority) {
        return switch (authority) {
            case CBUAE -> cbuaeAdapter.checkApiHealth();
            case VARA -> varaAdapter.checkApiHealth();
            case HSA -> hsaAdapter.checkApiHealth();
            default -> {
                log.warn("No API adapter configured for authority: {}", authority);
                yield false;
            }
        };
    }
    
    private ComplianceViolation createDummyViolation() {
        // Create a dummy violation for SAR submission
        // In production, this would be properly mapped
        return ComplianceViolation.builder()
            .violationId("DUMMY-VIOL")
            .authority(RegulatoryAuthority.CBUAE)
            .severity(ViolationSeverity.MEDIUM)
            .violationCode("SAR-001")
            .description("Suspicious activity detected")
            .status(ViolationStatus.DETECTED)
            .build();
    }
}