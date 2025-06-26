package com.banking.loan.infrastructure.messaging;

import com.banking.loan.domain.shared.DomainEvent;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Industry-standard banking message structure compliant with ISO 20022
 * Provides comprehensive metadata for regulatory compliance and audit trails
 */
@Data
@Builder
public class BankingMessage {
    private BankingMessageHeader messageHeader;
    private BankingEventMetadata eventMetadata;
    private DomainEvent eventPayload;
    private BankingComplianceData complianceData;
    private BankingAuditTrail auditTrail;
}

/**
 * ISO 20022 compliant message header
 */
@Data
@Builder
public class BankingMessageHeader {
    private String messageId;
    private Instant timestamp;
    private String messageType;
    private String schemaVersion;
    private String source;
    private String correlationId;
    private String businessProcessReference;
    private String messageDefinitionIdentifier;
    private String creationDateTime;
    private Integer numberOfTransactions;
    private String controlSum;
    private String initiatingParty;
    private String forwardingAgent;
}

/**
 * Banking-specific event metadata for compliance and processing
 */
@Data
@Builder
public class BankingEventMetadata {
    private String tenantId;
    private String regionCode;
    private String regulatoryJurisdiction;
    private LocalDate businessDate;
    private String processingCenter;
    private String branchCode;
    private String currencyCode;
    private String timeZone;
    private String channelType;
    private String deviceType;
    private String ipAddress;
    private String userAgent;
    private Map<String, Object> extendedAttributes;
}

/**
 * Comprehensive compliance data for regulatory requirements
 */
@Data
@Builder
public class BankingComplianceData {
    // Anti-Money Laundering (AML)
    private String amlScreeningResult;
    private String amlRiskScore;
    private String amlLastScreeningDate;
    
    // Sanctions Screening
    private String sanctionsCheckResult;
    private String sanctionsListsChecked;
    private String sanctionsHitDetails;
    
    // Know Your Customer (KYC)
    private String kycStatus;
    private String kycLastUpdateDate;
    private String kycRiskRating;
    
    // Tax Compliance
    private boolean fatcaReportable;
    private boolean crsReportable;
    private String taxResidency;
    private String tinNumber;
    
    // PSD2 Compliance (European regulation)
    private String psd2Category;
    private String sca_status; // Strong Customer Authentication
    private String consentId;
    private String tppId; // Third Party Provider ID
    
    // Basel III / Capital Adequacy
    private String riskWeightCategory;
    private Double riskWeightPercentage;
    private String creditRating;
    private String exposureClass;
    
    // GDPR Data Protection
    private String dataProcessingLawfulBasis;
    private boolean consentGiven;
    private String dataRetentionPeriod;
    private boolean rightToBeRorgotten;
    
    // SOX Compliance (Sarbanes-Oxley)
    private String soxControlNumber;
    private String soxTestingResult;
    private String soxComplianceOfficer;
    
    // Islamic Banking Compliance
    private boolean shariahCompliant;
    private String shariahApprovalDate;
    private String shariahBoardMember;
    private String islamicContractType;
    
    // Overall Compliance
    private Double complianceScore;
    private String complianceStatus;
    private String lastComplianceCheck;
    private String nextComplianceReview;
}

/**
 * Comprehensive audit trail for regulatory and internal auditing
 */
@Data
@Builder
public class BankingAuditTrail {
    private String initiatedBy;
    private String processedBy;
    private String approvedBy;
    private String auditLevel;
    private String auditTrailId;
    private Instant creationTimestamp;
    private Instant lastModifiedTimestamp;
    private String businessJustification;
    private String supervisoryApproval;
    private String riskAssessment;
    private String segregationOfDuties;
    private String dualControl;
    private Map<String, String> systemIds;
    private Map<String, Object> beforeValues;
    private Map<String, Object> afterValues;
    private String changeReason;
    private String ipAddress;
    private String sessionId;
    private String userRole;
    private String department;
    private String applicationVersion;
    private String databaseVersion;
}

/**
 * Dead Letter Queue Event for failed message processing
 */
@Data
@Builder
public class DeadLetterEvent {
    private String originalEventType;
    private String originalEventId;
    private String originalAggregateId;
    private String originalTopic;
    private Integer originalPartition;
    private Long originalOffset;
    private String errorMessage;
    private String errorStackTrace;
    private Instant errorTimestamp;
    private Integer retryCount;
    private Integer maxRetries;
    private String errorCategory;
    private String errorSeverity;
    private String processingNode;
    private Map<String, Object> originalHeaders;
    private String originalPayload;
    private String recoveryAction;
    private boolean manualInterventionRequired;
    private String assignedTo;
    private String resolutionNotes;
    private Instant resolvedTimestamp;
}