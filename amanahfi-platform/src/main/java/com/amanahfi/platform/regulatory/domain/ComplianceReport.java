package com.amanahfi.platform.regulatory.domain;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Regulatory compliance report
 */
@Value
@Builder
public class ComplianceReport {
    String reportId;
    ReportType reportType;
    RegulatoryAuthority authority;
    LocalDate reportingPeriodStart;
    LocalDate reportingPeriodEnd;
    String submissionReference;
    Instant submittedAt;
    String submittedBy;
    ReportStatus status;
    String acknowledgmentNumber;
    
    public enum ReportType {
        // Periodic reports
        QUARTERLY_COMPLIANCE,
        MONTHLY_TRANSACTION,
        ANNUAL_AUDIT,
        
        // Event-driven reports
        SUSPICIOUS_ACTIVITY,
        LARGE_TRANSACTION,
        COMPLIANCE_BREACH,
        
        // Islamic finance specific
        SHARIA_COMPLIANCE,
        ZAKAT_CALCULATION,
        PROFIT_DISTRIBUTION,
        
        // Digital asset specific
        CBDC_TRANSACTION,
        VIRTUAL_ASSET_CUSTODY,
        CRYPTO_COMPLIANCE
    }
    
    public enum ReportStatus {
        DRAFT,
        SUBMITTED,
        ACKNOWLEDGED,
        UNDER_REVIEW,
        ACCEPTED,
        REJECTED,
        REQUIRES_RESUBMISSION
    }
}