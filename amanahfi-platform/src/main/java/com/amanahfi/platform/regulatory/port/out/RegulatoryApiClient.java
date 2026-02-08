package com.amanahfi.platform.regulatory.port.out;

import com.amanahfi.platform.regulatory.domain.*;

import java.util.Map;

/**
 * Output port for regulatory authority API integration
 * Supports CBUAE, VARA, HSA and other regulatory authorities
 */
public interface RegulatoryApiClient {
    
    /**
     * Submit compliance assessment to regulatory authority
     */
    void submitAssessment(
        RegulatoryAuthority authority,
        ComplianceSummary compliance,
        ComplianceAssessment assessment
    );
    
    /**
     * Submit regulatory report
     * @return Acknowledgment number from authority
     */
    String submitReport(
        RegulatoryAuthority authority,
        ComplianceReport report,
        Map<String, Object> reportData
    );
    
    /**
     * Notify authority of compliance violation
     * @return Notification reference number
     */
    String notifyViolation(
        RegulatoryAuthority authority,
        ComplianceSummary compliance,
        ComplianceViolation violation
    );
    
    /**
     * Notify authority of violation remediation
     * @return Notification reference number
     */
    String notifyRemediation(
        Jurisdiction jurisdiction,
        String violationId,
        RemediationDetails remediation
    );
    
    /**
     * Check regulatory API health status
     */
    boolean isHealthy(RegulatoryAuthority authority);
}