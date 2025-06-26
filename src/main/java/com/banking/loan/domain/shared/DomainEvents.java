package com.banking.loan.domain.shared;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Domain Events for the Banking System
 * All events implement the DomainEvent interface for consistent handling
 */

// ============= LOAN DOMAIN EVENTS =============

@Value
@Builder
public class LoanApplicationSubmittedEvent implements DomainEvent {
    UUID eventId = UUID.randomUUID();
    String eventType = "LoanApplicationSubmitted";
    String aggregateId;
    Long aggregateVersion;
    Instant occurredOn = Instant.now();
    String triggeredBy;
    String correlationId;
    String tenantId;
    EventMetadata metadata;
    
    // Event-specific data
    String customerId;
    BigDecimal loanAmount;
    Integer termInMonths;
    String loanType;
    String applicationChannel;
}

@Value
@Builder
public class LoanApprovedEvent implements DomainEvent {
    UUID eventId = UUID.randomUUID();
    String eventType = "LoanApproved";
    String aggregateId;
    Long aggregateVersion;
    Instant occurredOn = Instant.now();
    String triggeredBy;
    String correlationId;
    String tenantId;
    EventMetadata metadata;
    
    // Event-specific data
    String customerId;
    BigDecimal approvedAmount;
    BigDecimal interestRate;
    LocalDate firstPaymentDate;
    String approvalReason;
}

@Value
@Builder
public class LoanRejectedEvent implements DomainEvent {
    UUID eventId = UUID.randomUUID();
    String eventType = "LoanRejected";
    String aggregateId;
    Long aggregateVersion;
    Instant occurredOn = Instant.now();
    String triggeredBy;
    String correlationId;
    String tenantId;
    EventMetadata metadata;
    
    // Event-specific data
    String customerId;
    List<String> rejectionReasons;
    String riskScore;
}

@Value
@Builder
public class LoanFullyPaidEvent implements DomainEvent {
    UUID eventId = UUID.randomUUID();
    String eventType = "LoanFullyPaid";
    String aggregateId;
    Long aggregateVersion;
    Instant occurredOn = Instant.now();
    String triggeredBy;
    String correlationId;
    String tenantId;
    EventMetadata metadata;
    
    // Event-specific data
    String customerId;
    Instant completionDate;
    BigDecimal totalAmountPaid;
}

@Value
@Builder
public class AIRiskAssessmentCompletedEvent implements DomainEvent {
    UUID eventId = UUID.randomUUID();
    String eventType = "AIRiskAssessmentCompleted";
    String aggregateId;
    Long aggregateVersion;
    Instant occurredOn = Instant.now();
    String triggeredBy;
    String correlationId;
    String tenantId;
    EventMetadata metadata;
    
    // Event-specific data
    String customerId;
    Double riskScore;
    Double confidenceLevel;
    String recommendation;
    Map<String, Object> aiAnalysis;
}

// ============= PAYMENT DOMAIN EVENTS =============

@Value
@Builder
public class PaymentProcessedEvent implements DomainEvent {
    UUID eventId = UUID.randomUUID();
    String eventType = "PaymentProcessed";
    String aggregateId;
    Long aggregateVersion;
    Instant occurredOn = Instant.now();
    String triggeredBy;
    String correlationId;
    String tenantId;
    EventMetadata metadata;
    
    // Event-specific data
    String customerId;
    String loanId;
    BigDecimal paymentAmount;
    Integer installmentNumber;
    String paymentMethod;
    String paymentStatus;
    String transactionReference;
}

@Value
@Builder
public class PaymentFailedEvent implements DomainEvent {
    UUID eventId = UUID.randomUUID();
    String eventType = "PaymentFailed";
    String aggregateId;
    Long aggregateVersion;
    Instant occurredOn = Instant.now();
    String triggeredBy;
    String correlationId;
    String tenantId;
    EventMetadata metadata;
    
    // Event-specific data
    String customerId;
    String loanId;
    BigDecimal attemptedAmount;
    String failureReason;
    String paymentMethod;
    String errorCode;
}

@Value
@Builder
public class PaymentScheduleUpdatedEvent implements DomainEvent {
    UUID eventId = UUID.randomUUID();
    String eventType = "PaymentScheduleUpdated";
    String aggregateId;
    Long aggregateVersion;
    Instant occurredOn = Instant.now();
    String triggeredBy;
    String correlationId;
    String tenantId;
    EventMetadata metadata;
    
    // Event-specific data
    String loanId;
    String customerId;
    LocalDate newNextPaymentDate;
    BigDecimal newInstallmentAmount;
    String updateReason;
}

// ============= CUSTOMER DOMAIN EVENTS =============

@Value
@Builder
public class CustomerCreatedEvent implements DomainEvent {
    UUID eventId = UUID.randomUUID();
    String eventType = "CustomerCreated";
    String aggregateId;
    Long aggregateVersion;
    Instant occurredOn = Instant.now();
    String triggeredBy;
    String correlationId;
    String tenantId;
    EventMetadata metadata;
    
    // Event-specific data
    String customerType;
    String nationalId;
    String email;
    String phoneNumber;
    String kycStatus;
    String onboardingChannel;
}

@Value
@Builder
public class CustomerUpdatedEvent implements DomainEvent {
    UUID eventId = UUID.randomUUID();
    String eventType = "CustomerUpdated";
    String aggregateId;
    Long aggregateVersion;
    Instant occurredOn = Instant.now();
    String triggeredBy;
    String correlationId;
    String tenantId;
    EventMetadata metadata;
    
    // Event-specific data
    List<String> updatedFields;
    Map<String, Object> previousValues;
    Map<String, Object> newValues;
    String updateReason;
}

@Value
@Builder
public class CustomerBlockedEvent implements DomainEvent {
    UUID eventId = UUID.randomUUID();
    String eventType = "CustomerBlocked";
    String aggregateId;
    Long aggregateVersion;
    Instant occurredOn = Instant.now();
    String triggeredBy;
    String correlationId;
    String tenantId;
    EventMetadata metadata;
    
    // Event-specific data
    String blockReason;
    String blockType;
    Instant blockExpiryDate;
    List<String> affectedServices;
}

// ============= FRAUD DOMAIN EVENTS =============

@Value
@Builder
public class FraudDetectedEvent implements DomainEvent {
    UUID eventId = UUID.randomUUID();
    String eventType = "FraudDetected";
    String aggregateId;
    Long aggregateVersion;
    Instant occurredOn = Instant.now();
    String triggeredBy;
    String correlationId;
    String tenantId;
    EventMetadata metadata;
    
    // Event-specific data
    String customerId;
    String transactionId;
    String riskLevel;
    List<String> fraudIndicators;
    Double fraudScore;
    String detectionMethod;
    Map<String, Object> evidenceData;
}

@Value
@Builder
public class FraudInvestigationCompletedEvent implements DomainEvent {
    UUID eventId = UUID.randomUUID();
    String eventType = "FraudInvestigationCompleted";
    String aggregateId;
    Long aggregateVersion;
    Instant occurredOn = Instant.now();
    String triggeredBy;
    String correlationId;
    String tenantId;
    EventMetadata metadata;
    
    // Event-specific data
    String customerId;
    String investigationResult;
    String investigatorId;
    List<String> actionsRequired;
    Map<String, Object> investigationDetails;
}

// ============= AI DOMAIN EVENTS =============

@Value
@Builder
public class AIRecommendationGeneratedEvent implements DomainEvent {
    UUID eventId = UUID.randomUUID();
    String eventType = "AIRecommendationGenerated";
    String aggregateId;
    Long aggregateVersion;
    Instant occurredOn = Instant.now();
    String triggeredBy;
    String correlationId;
    String tenantId;
    EventMetadata metadata;
    
    // Event-specific data
    String customerId;
    String recommendationType;
    List<Map<String, Object>> recommendations;
    Double confidenceScore;
    String modelUsed;
    Map<String, Object> modelParameters;
}

@Value
@Builder
public class AIModelUpdatedEvent implements DomainEvent {
    UUID eventId = UUID.randomUUID();
    String eventType = "AIModelUpdated";
    String aggregateId;
    Long aggregateVersion;
    Instant occurredOn = Instant.now();
    String triggeredBy;
    String correlationId;
    String tenantId;
    EventMetadata metadata;
    
    // Event-specific data
    String modelName;
    String previousVersion;
    String newVersion;
    Map<String, Object> performanceMetrics;
    List<String> improvements;
}

// ============= COMPLIANCE DOMAIN EVENTS =============

@Value
@Builder
public class ComplianceCheckCompletedEvent implements DomainEvent {
    UUID eventId = UUID.randomUUID();
    String eventType = "ComplianceCheckCompleted";
    String aggregateId;
    Long aggregateVersion;
    Instant occurredOn = Instant.now();
    String triggeredBy;
    String correlationId;
    String tenantId;
    EventMetadata metadata;
    
    // Event-specific data
    String entityId;
    String entityType;
    String complianceType;
    String checkResult;
    List<String> violations;
    Map<String, Object> checkDetails;
}

@Value
@Builder
public class RegulatoryReportGeneratedEvent implements DomainEvent {
    UUID eventId = UUID.randomUUID();
    String eventType = "RegulatoryReportGenerated";
    String aggregateId;
    Long aggregateVersion;
    Instant occurredOn = Instant.now();
    String triggeredBy;
    String correlationId;
    String tenantId;
    EventMetadata metadata;
    
    // Event-specific data
    String reportType;
    String reportPeriod;
    String regulatoryBody;
    String reportLocation;
    Map<String, Object> reportSummary;
}

// ============= SAGA COORDINATION EVENTS =============

@Value
@Builder
public class SagaStartedEvent implements DomainEvent {
    UUID eventId = UUID.randomUUID();
    String eventType = "SagaStarted";
    String aggregateId;
    Long aggregateVersion;
    Instant occurredOn = Instant.now();
    String triggeredBy;
    String correlationId;
    String tenantId;
    EventMetadata metadata;
    
    // Event-specific data
    String sagaType;
    Map<String, Object> sagaData;
    List<String> participatingServices;
    Instant timeoutAt;
}

@Value
@Builder
public class SagaCompletedEvent implements DomainEvent {
    UUID eventId = UUID.randomUUID();
    String eventType = "SagaCompleted";
    String aggregateId;
    Long aggregateVersion;
    Instant occurredOn = Instant.now();
    String triggeredBy;
    String correlationId;
    String tenantId;
    EventMetadata metadata;
    
    // Event-specific data
    String sagaType;
    String completionStatus;
    Map<String, Object> finalState;
    Long durationMs;
}

@Value
@Builder
public class SagaFailedEvent implements DomainEvent {
    UUID eventId = UUID.randomUUID();
    String eventType = "SagaFailed";
    String aggregateId;
    Long aggregateVersion;
    Instant occurredOn = Instant.now();
    String triggeredBy;
    String correlationId;
    String tenantId;
    EventMetadata metadata;
    
    // Event-specific data
    String sagaType;
    String failureReason;
    String failedStep;
    Map<String, Object> compensationActions;
    boolean compensationCompleted;
}