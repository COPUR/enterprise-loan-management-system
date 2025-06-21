# Logging Best Practices for Banking Systems

## Overview

This document establishes comprehensive logging best practices for the Enterprise Loan Management System, focusing on PCI-DSS v4 compliance, data sovereignty, and banking industry standards.

## Logging Principles

### 1. Security-First Approach
- **Never log sensitive data**: PCI data, PII, credentials
- **Data classification awareness**: Tag all logs with classification levels
- **Encryption at rest and in transit**: Protect log data throughout its lifecycle
- **Access controls**: Implement role-based access to log data

### 2. Compliance-Driven Logging
- **Regulatory requirements**: Meet PCI-DSS, GDPR, SOX mandates
- **Audit trail integrity**: Ensure logs cannot be tampered with
- **Data retention policies**: Comply with regional data protection laws
- **Cross-border transfer controls**: Respect data sovereignty requirements

### 3. Operational Excellence
- **Structured logging**: Use consistent JSON format
- **Contextual information**: Include correlation IDs and business context
- **Performance consideration**: Async logging to avoid blocking operations
- **Centralized collection**: Single source of truth for all log data

## Structured Logging Format

### Standard Log Schema
```json
{
  "@timestamp": "2024-01-15T10:30:45.123Z",
  "level": "INFO",
  "service": "customer-management-service",
  "version": "1.0.0",
  "environment": "production",
  "region": "US",
  "host": "customer-service-01",
  "thread": "http-nio-8080-exec-1",
  "logger": "com.bank.loanmanagement.customermanagement.service.CustomerService",
  "message": "Customer account created successfully",
  "event_type": "CUSTOMER_CREATED",
  "correlation_id": "550e8400-e29b-41d4-a716-446655440000",
  "transaction_id": "txn_789012345",
  "user_id": "usr_123456",
  "customer_id": "cust_987654321",
  "business_context": {
    "operation": "create_customer",
    "customer_type": "individual",
    "onboarding_channel": "web",
    "kyc_status": "pending"
  },
  "data_classification": "restricted",
  "compliance": {
    "framework": ["PCI-DSS-v4", "GDPR"],
    "audit_required": true,
    "retention_period": "P2555D"
  },
  "performance": {
    "duration_ms": 145,
    "memory_usage_mb": 256
  },
  "security": {
    "ip_address": "10.0.1.100",
    "user_agent": "Mozilla/5.0...",
    "authentication_method": "JWT"
  }
}
```

### Banking-Specific Fields
```java
public class BankingLogFields {
    // Business Context
    public static final String CUSTOMER_ID = "customer_id";
    public static final String ACCOUNT_NUMBER = "account_number"; // Always masked
    public static final String TRANSACTION_ID = "transaction_id";
    public static final String LOAN_ID = "loan_id";
    public static final String PAYMENT_ID = "payment_id";
    
    // Compliance
    public static final String DATA_CLASSIFICATION = "data_classification";
    public static final String COMPLIANCE_FRAMEWORK = "compliance_framework";
    public static final String AUDIT_REQUIRED = "audit_required";
    public static final String PCI_SCOPE = "pci_scope";
    public static final String GDPR_APPLICABLE = "gdpr_applicable";
    
    // Banking Operations
    public static final String OPERATION_TYPE = "operation_type";
    public static final String BUSINESS_PROCESS = "business_process";
    public static final String FINANCIAL_AMOUNT = "financial_amount"; // Always in cents
    public static final String CURRENCY = "currency";
    public static final String RISK_LEVEL = "risk_level";
    
    // Regulatory
    public static final String REGULATORY_REQUIREMENT = "regulatory_requirement";
    public static final String DATA_RESIDENCY = "data_residency";
    public static final String JURISDICTION = "jurisdiction";
}
```

## Data Classification and Masking

### Classification Levels
```java
public enum DataClassification {
    PUBLIC("public", "P90D", false, false),
    INTERNAL("internal", "P365D", false, false),
    CONFIDENTIAL("confidential", "P1095D", true, false),
    RESTRICTED("restricted", "P2555D", true, true),
    PCI_DSS("pci_dss", "P1095D", true, true);
    
    private final String level;
    private final String retentionPeriod;
    private final boolean encryptionRequired;
    private final boolean auditRequired;
}
```

### Automatic Data Masking
```java
@Component
public class LogDataMasker {
    
    private static final Pattern CREDIT_CARD_PATTERN = 
        Pattern.compile("\\b\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}\\b");
    private static final Pattern SSN_PATTERN = 
        Pattern.compile("\\b\\d{3}-?\\d{2}-?\\d{4}\\b");
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    
    public String maskSensitiveData(String message) {
        if (message == null) return null;
        
        String masked = message;
        
        // Mask credit card numbers (PCI-DSS requirement)
        masked = CREDIT_CARD_PATTERN.matcher(masked)
            .replaceAll(match -> {
                String card = match.group();
                if (card.length() >= 10) {
                    return card.substring(0, 6) + "****" + card.substring(card.length() - 4);
                }
                return "****-****-****-****";
            });
            
        // Mask SSN
        masked = SSN_PATTERN.matcher(masked).replaceAll("***-**-****");
        
        // Mask email addresses
        masked = EMAIL_PATTERN.matcher(masked)
            .replaceAll(match -> {
                String email = match.group();
                int atIndex = email.indexOf('@');
                if (atIndex > 2) {
                    return email.substring(0, 2) + "***@" + email.substring(atIndex + 1);
                }
                return "***@domain.com";
            });
            
        return masked;
    }
}
```

## Banking Use Case Logging Patterns

### Customer Management Logging
```java
@Service
public class CustomerManagementService {
    
    private final Logger logger = LoggerFactory.getLogger(CustomerManagementService.class);
    private final LoggingPort loggingPort;
    
    @Transactional
    public Customer createCustomer(CreateCustomerCommand command) {
        String correlationId = generateCorrelationId();
        String transactionId = generateTransactionId();
        
        // Start operation logging
        loggingPort.logBusinessEvent(
            "CUSTOMER_CREATION_STARTED",
            transactionId,
            Map.of(
                "operation", "create_customer",
                "customer_type", command.getCustomerType().name(),
                "channel", command.getChannel(),
                "correlation_id", correlationId
            ),
            DataClassification.RESTRICTED
        );
        
        try {
            // Business logic with detailed logging
            Customer customer = performCustomerCreation(command, correlationId);
            
            // Success logging
            loggingPort.logAuditEvent(
                "CREATE",
                "customer",
                getCurrentUserId(),
                Map.of(
                    "customer_id", customer.getId().value(),
                    "customer_type", customer.getType().name(),
                    "correlation_id", correlationId,
                    "compliance_check", "passed",
                    "kyc_status", customer.getKycStatus().name()
                )
            );
            
            return customer;
            
        } catch (CustomerAlreadyExistsException e) {
            // Business exception logging
            loggingPort.logBusinessEvent(
                "CUSTOMER_CREATION_FAILED",
                transactionId,
                Map.of(
                    "error_type", "business_rule_violation",
                    "error_code", "CUSTOMER_EXISTS",
                    "correlation_id", correlationId
                ),
                DataClassification.INTERNAL
            );
            throw e;
            
        } catch (Exception e) {
            // Technical exception logging
            loggingPort.logError(
                "Customer creation failed due to technical error",
                e,
                Map.of(
                    "operation", "create_customer",
                    "correlation_id", correlationId,
                    "error_category", "technical"
                )
            );
            throw e;
        }
    }
    
    private Customer performCustomerCreation(CreateCustomerCommand command, String correlationId) {
        // KYC validation with logging
        logger.info("Starting KYC validation for customer creation",
            StructuredArguments.kv("operation", "kyc_validation"),
            StructuredArguments.kv("correlation_id", correlationId),
            StructuredArguments.kv("data_classification", DataClassification.CONFIDENTIAL.name()),
            StructuredArguments.kv("regulatory_requirement", "BSA_AML")
        );
        
        KycResult kycResult = kycService.validateCustomer(command.getPersonalDetails());
        
        logger.info("KYC validation completed",
            StructuredArguments.kv("kyc_status", kycResult.getStatus().name()),
            StructuredArguments.kv("kyc_score", kycResult.getScore()),
            StructuredArguments.kv("correlation_id", correlationId),
            StructuredArguments.kv("compliance_framework", "BSA_AML")
        );
        
        // Continue with customer creation...
        return createCustomerRecord(command, kycResult);
    }
}
```

### Loan Processing Logging
```java
@Service
public class LoanProcessingService {
    
    public LoanDecision processLoanApplication(LoanApplicationCommand command) {
        String correlationId = generateCorrelationId();
        String loanApplicationId = command.getApplicationId();
        
        // Business process start
        loggingPort.logBusinessEvent(
            "LOAN_APPLICATION_PROCESSING_STARTED",
            loanApplicationId,
            Map.of(
                "loan_type", command.getLoanType().name(),
                "requested_amount", command.getAmount().getValue(),
                "currency", command.getAmount().getCurrency(),
                "customer_id", command.getCustomerId().value(),
                "correlation_id", correlationId,
                "business_process", "loan_origination"
            ),
            DataClassification.CONFIDENTIAL
        );
        
        try {
            // Credit assessment with detailed logging
            CreditAssessment assessment = performCreditAssessment(command, correlationId);
            
            // Risk evaluation
            RiskEvaluation risk = evaluateRisk(command, assessment, correlationId);
            
            // Decision making
            LoanDecision decision = makeDecision(command, assessment, risk, correlationId);
            
            // Final outcome logging
            loggingPort.logAuditEvent(
                "LOAN_DECISION",
                "loan_application",
                getCurrentUserId(),
                Map.of(
                    "application_id", loanApplicationId,
                    "decision", decision.getDecision().name(),
                    "decision_reason", decision.getReason(),
                    "approved_amount", decision.getApprovedAmount(),
                    "interest_rate", decision.getInterestRate(),
                    "correlation_id", correlationId,
                    "risk_level", risk.getLevel().name(),
                    "credit_score", assessment.getCreditScore()
                )
            );
            
            return decision;
            
        } catch (Exception e) {
            loggingPort.logError(
                "Loan application processing failed",
                e,
                Map.of(
                    "application_id", loanApplicationId,
                    "customer_id", command.getCustomerId().value(),
                    "correlation_id", correlationId,
                    "business_process", "loan_origination"
                )
            );
            throw e;
        }
    }
    
    private CreditAssessment performCreditAssessment(LoanApplicationCommand command, String correlationId) {
        logger.info("Starting credit assessment",
            StructuredArguments.kv("operation", "credit_assessment"),
            StructuredArguments.kv("customer_id", command.getCustomerId().value()),
            StructuredArguments.kv("correlation_id", correlationId),
            StructuredArguments.kv("data_classification", DataClassification.CONFIDENTIAL.name()),
            StructuredArguments.kv("regulatory_requirement", "Basel_III")
        );
        
        // External credit bureau call
        try {
            CreditReport report = creditBureauService.getCreditReport(command.getCustomerId());
            
            logger.info("Credit report retrieved successfully",
                StructuredArguments.kv("credit_score", report.getCreditScore()),
                StructuredArguments.kv("credit_history_length", report.getHistoryLength()),
                StructuredArguments.kv("correlation_id", correlationId),
                StructuredArguments.kv("external_service", "credit_bureau"),
                StructuredArguments.kv("response_time_ms", report.getResponseTime())
            );
            
            return CreditAssessment.from(report);
            
        } catch (CreditBureauException e) {
            logger.error("Credit bureau service failed",
                StructuredArguments.kv("error_type", "external_service_failure"),
                StructuredArguments.kv("service", "credit_bureau"),
                StructuredArguments.kv("correlation_id", correlationId),
                StructuredArguments.kv("fallback_strategy", "manual_review"),
                e
            );
            
            // Fallback to manual review
            return CreditAssessment.requiresManualReview();
        }
    }
}
```

### Payment Processing Logging
```java
@Service
public class PaymentProcessingService {
    
    public PaymentResult processPayment(PaymentCommand command) {
        String correlationId = generateCorrelationId();
        String paymentId = command.getPaymentId();
        
        // PCI-DSS compliant payment logging
        loggingPort.logBusinessEvent(
            "PAYMENT_PROCESSING_STARTED",
            paymentId,
            Map.of(
                "payment_method", command.getPaymentMethod().name(),
                "amount", command.getAmount().getValue(),
                "currency", command.getAmount().getCurrency(),
                // Never log actual card details
                "card_type", getCardType(command.getCardDetails()),
                "correlation_id", correlationId,
                "merchant_id", getMerchantId()
            ),
            DataClassification.PCI_DSS
        );
        
        try {
            // PCI validation
            validatePciCompliance(command);
            
            logger.info("PCI compliance validation passed",
                StructuredArguments.kv("operation", "pci_validation"),
                StructuredArguments.kv("payment_id", paymentId),
                StructuredArguments.kv("correlation_id", correlationId),
                StructuredArguments.kv("compliance_framework", "PCI-DSS-v4"),
                StructuredArguments.kv("validation_result", "passed")
            );
            
            // Tokenization
            String token = tokenizePaymentData(command.getCardDetails(), correlationId);
            
            // Fraud detection
            FraudCheckResult fraudResult = performFraudCheck(command, token, correlationId);
            
            if (fraudResult.isSuspicious()) {
                loggingPort.logSecurityEvent(
                    "FRAUD_DETECTED",
                    getCurrentUserId(),
                    getClientIpAddress(),
                    getUserAgent(),
                    Map.of(
                        "payment_id", paymentId,
                        "fraud_score", fraudResult.getScore(),
                        "fraud_indicators", fraudResult.getIndicators(),
                        "action_taken", "payment_blocked",
                        "correlation_id", correlationId
                    )
                );
                
                throw new FraudDetectedException("Suspicious payment activity detected");
            }
            
            // Process payment
            PaymentGatewayResult gatewayResult = paymentGateway.processPayment(token, command.getAmount());
            
            // Success logging
            loggingPort.logAuditEvent(
                "PAYMENT_PROCESSED",
                "payment",
                getCurrentUserId(),
                Map.of(
                    "payment_id", paymentId,
                    "transaction_id", gatewayResult.getTransactionId(),
                    "gateway_response", gatewayResult.getResponseCode(),
                    "amount", command.getAmount().getValue(),
                    "currency", command.getAmount().getCurrency(),
                    "correlation_id", correlationId,
                    "processing_time_ms", gatewayResult.getProcessingTime()
                )
            );
            
            return PaymentResult.success(gatewayResult);
            
        } catch (FraudDetectedException e) {
            // Security event - fraud detected
            loggingPort.logSecurityEvent(
                "PAYMENT_FRAUD_BLOCKED",
                getCurrentUserId(),
                getClientIpAddress(),
                getUserAgent(),
                Map.of(
                    "payment_id", paymentId,
                    "correlation_id", correlationId,
                    "security_action", "payment_blocked"
                )
            );
            throw e;
            
        } catch (Exception e) {
            loggingPort.logError(
                "Payment processing failed",
                e,
                Map.of(
                    "payment_id", paymentId,
                    "correlation_id", correlationId,
                    "business_process", "payment_processing"
                )
            );
            throw e;
        }
    }
    
    private String tokenizePaymentData(CardDetails cardDetails, String correlationId) {
        logger.info("Starting payment data tokenization",
            StructuredArguments.kv("operation", "tokenization"),
            StructuredArguments.kv("correlation_id", correlationId),
            StructuredArguments.kv("data_classification", DataClassification.PCI_DSS.name()),
            StructuredArguments.kv("compliance_requirement", "PCI-DSS-3.4"),
            // Never log actual card data
            StructuredArguments.kv("card_type", cardDetails.getType().name()),
            StructuredArguments.kv("card_last_four", cardDetails.getLastFourDigits())
        );
        
        String token = tokenizationService.tokenize(cardDetails);
        
        logger.info("Payment data tokenization completed",
            StructuredArguments.kv("tokenization_result", "success"),
            StructuredArguments.kv("token_id", token),
            StructuredArguments.kv("correlation_id", correlationId)
        );
        
        return token;
    }
}
```

## Error Logging Patterns

### Structured Error Logging
```java
@Component
public class ErrorLoggingHandler {
    
    @EventListener
    public void handleBusinessException(BusinessException exception) {
        loggingPort.logBusinessEvent(
            "BUSINESS_RULE_VIOLATION",
            exception.getTransactionId(),
            Map.of(
                "error_type", "business_exception",
                "error_code", exception.getErrorCode(),
                "error_category", exception.getCategory(),
                "business_rule", exception.getViolatedRule(),
                "recovery_action", exception.getRecoveryAction()
            ),
            DataClassification.INTERNAL
        );
    }
    
    @EventListener
    public void handleSecurityException(SecurityException exception) {
        loggingPort.logSecurityEvent(
            "SECURITY_VIOLATION",
            exception.getUserId(),
            exception.getIpAddress(),
            exception.getUserAgent(),
            Map.of(
                "violation_type", exception.getViolationType(),
                "attempted_action", exception.getAttemptedAction(),
                "resource", exception.getResource(),
                "severity", exception.getSeverity().name(),
                "response_action", exception.getResponseAction()
            )
        );
    }
    
    @EventListener
    public void handleIntegrationException(IntegrationException exception) {
        loggingPort.logError(
            "External service integration failed",
            exception,
            Map.of(
                "service_name", exception.getServiceName(),
                "endpoint", exception.getEndpoint(),
                "http_status", exception.getHttpStatus(),
                "retry_count", exception.getRetryCount(),
                "circuit_breaker_state", exception.getCircuitBreakerState(),
                "fallback_used", exception.isFallbackUsed()
            )
        );
    }
}
```

## Performance Logging

### Performance Metrics Logging
```java
@Aspect
@Component
public class PerformanceLoggingAspect {
    
    @Around("@annotation(PerformanceLogged)")
    public Object logPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String operationName = joinPoint.getSignature().getName();
        String correlationId = getCorrelationId();
        
        long startTime = System.currentTimeMillis();
        long startMemory = getUsedMemory();
        
        try {
            Object result = joinPoint.proceed();
            
            long duration = System.currentTimeMillis() - startTime;
            long memoryUsed = getUsedMemory() - startMemory;
            
            // Performance success logging
            loggingPort.logPerformanceMetric(
                operationName,
                duration,
                Map.of(
                    "memory_used_bytes", memoryUsed,
                    "correlation_id", correlationId,
                    "outcome", "success",
                    "method_name", joinPoint.getSignature().toShortString()
                )
            );
            
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            
            // Performance failure logging
            loggingPort.logPerformanceMetric(
                operationName,
                duration,
                Map.of(
                    "correlation_id", correlationId,
                    "outcome", "failure",
                    "error_type", e.getClass().getSimpleName(),
                    "method_name", joinPoint.getSignature().toShortString()
                )
            );
            
            throw e;
        }
    }
}
```

## Compliance Logging

### GDPR Compliance Logging
```java
@Service
public class GdprComplianceLogger {
    
    public void logDataProcessing(String dataSubjectId, String processingPurpose, String legalBasis) {
        loggingPort.logAuditEvent(
            "DATA_PROCESSING",
            "personal_data",
            getCurrentProcessorId(),
            Map.of(
                "data_subject_id", hashForPrivacy(dataSubjectId),
                "processing_purpose", processingPurpose,
                "legal_basis", legalBasis,
                "compliance_framework", "GDPR",
                "article", "Article_6",
                "data_controller", getDataController(),
                "retention_period", getRetentionPeriod(processingPurpose)
            )
        );
    }
    
    public void logDataSubjectRequest(String requestType, String dataSubjectId, String requestId) {
        loggingPort.logAuditEvent(
            "DATA_SUBJECT_REQUEST",
            "gdpr_request",
            getCurrentUserId(),
            Map.of(
                "request_type", requestType, // ACCESS, RECTIFICATION, ERASURE, etc.
                "request_id", requestId,
                "data_subject_id", hashForPrivacy(dataSubjectId),
                "request_date", Instant.now().toString(),
                "response_deadline", calculateResponseDeadline(requestType),
                "compliance_framework", "GDPR",
                "article", getGdprArticle(requestType)
            )
        );
    }
    
    public void logDataBreach(String breachId, String breachType, int affectedRecords) {
        loggingPort.logSecurityEvent(
            "DATA_BREACH",
            getCurrentUserId(),
            getClientIpAddress(),
            getUserAgent(),
            Map.of(
                "breach_id", breachId,
                "breach_type", breachType,
                "affected_records", affectedRecords,
                "discovery_date", Instant.now().toString(),
                "notification_required", affectedRecords > 0,
                "notification_deadline", "72_hours",
                "compliance_framework", "GDPR",
                "article", "Article_33_34"
            )
        );
    }
}
```

## Regional Logging Configuration

### Data Residency Logging
```java
@Configuration
@ConditionalOnProperty(name = "logging.data-residency", havingValue = "EU")
public class EuLoggingConfiguration {
    
    @Bean
    public LoggingConfigurer euLoggingConfigurer() {
        return LoggingConfigurer.builder()
            .dataResidency("EU")
            .complianceFrameworks(List.of("GDPR", "PCI-DSS-v4"))
            .retentionPolicy(Map.of(
                "personal_data", "P1095D",
                "audit_logs", "P2555D",
                "transaction_logs", "P1095D"
            ))
            .crossBorderTransfer(false)
            .encryptionRequired(true)
            .build();
    }
}

@Configuration
@ConditionalOnProperty(name = "logging.data-residency", havingValue = "US")
public class UsLoggingConfiguration {
    
    @Bean
    public LoggingConfigurer usLoggingConfigurer() {
        return LoggingConfigurer.builder()
            .dataResidency("US")
            .complianceFrameworks(List.of("CCPA", "SOX", "PCI-DSS-v4"))
            .retentionPolicy(Map.of(
                "financial_data", "P2555D", // 7 years for SOX
                "audit_logs", "P2555D",
                "transaction_logs", "P1095D"
            ))
            .crossBorderTransfer(true)
            .adequacyDecisions(List.of("CA", "UK", "JP"))
            .build();
    }
}
```

## Log Analysis and Alerting

### Critical Event Detection
```java
@Component
public class CriticalEventDetector {
    
    @EventListener
    public void detectCriticalEvents(LogEvent event) {
        // Detect PCI compliance violations
        if (isPciViolation(event)) {
            alertManager.sendCriticalAlert(
                "PCI-DSS Compliance Violation",
                Map.of(
                    "event_id", event.getEventId(),
                    "violation_type", event.getViolationType(),
                    "immediate_action_required", true,
                    "notification_deadline", "72_hours"
                )
            );
        }
        
        // Detect fraud patterns
        if (isFraudPattern(event)) {
            alertManager.sendSecurityAlert(
                "Fraud Pattern Detected",
                Map.of(
                    "pattern_type", event.getFraudPatternType(),
                    "confidence_score", event.getFraudConfidence(),
                    "recommended_action", "block_account"
                )
            );
        }
        
        // Detect regulatory violations
        if (isRegulatoryViolation(event)) {
            alertManager.sendComplianceAlert(
                "Regulatory Violation",
                Map.of(
                    "regulation", event.getRegulation(),
                    "violation_severity", event.getSeverity(),
                    "remediation_required", true
                )
            );
        }
    }
}
```

## Testing Logging

### Unit Test Logging Verification
```java
@ExtendWith(MockitoExtension.class)
class CustomerServiceLoggingTest {
    
    @Mock
    private LoggingPort loggingPort;
    
    @InjectMocks
    private CustomerService customerService;
    
    @Test
    void shouldLogCustomerCreationEvent() {
        // Given
        CreateCustomerCommand command = CreateCustomerCommand.builder()
            .personalDetails(PersonalDetails.builder().build())
            .build();
            
        // When
        customerService.createCustomer(command);
        
        // Then
        verify(loggingPort).logBusinessEvent(
            eq("CUSTOMER_CREATED"),
            any(String.class),
            argThat(data -> 
                data.get("operation").equals("create_customer") &&
                data.get("customer_type") != null
            ),
            eq(DataClassification.RESTRICTED)
        );
    }
    
    @Test
    void shouldLogAuditEventForCustomerCreation() {
        // Given
        CreateCustomerCommand command = CreateCustomerCommand.builder().build();
        
        // When
        customerService.createCustomer(command);
        
        // Then
        verify(loggingPort).logAuditEvent(
            eq("CREATE"),
            eq("customer"),
            any(String.class),
            argThat(details -> 
                details.containsKey("customer_id") &&
                details.containsKey("compliance_check")
            )
        );
    }
}
```

## Log Monitoring and Maintenance

### Automated Log Health Checks
```java
@Component
public class LogHealthChecker {
    
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void checkLogHealth() {
        // Check log processing latency
        if (getLogProcessingLatency() > Duration.ofSeconds(30)) {
            alertManager.sendAlert("High log processing latency detected");
        }
        
        // Check log storage capacity
        if (getLogStorageUsage() > 0.85) {
            alertManager.sendAlert("Log storage capacity above 85%");
        }
        
        // Check compliance log completeness
        if (!areComplianceLogsComplete()) {
            alertManager.sendCriticalAlert("Missing compliance logs detected");
        }
    }
}
```

---

These logging best practices ensure comprehensive, compliant, and secure logging for banking operations while maintaining high performance and operational excellence.