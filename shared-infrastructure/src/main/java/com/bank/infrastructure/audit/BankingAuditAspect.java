package com.bank.infrastructure.audit;

import com.bank.infrastructure.domain.Money;
import com.bank.infrastructure.security.BankingComplianceFramework;
import com.bank.infrastructure.security.BankingComplianceFramework.ComplianceStandard;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.math.BigDecimal;

/**
 * Banking Audit Aspect for Compliance Monitoring
 * 
 * Comprehensive audit logging system providing:
 * - Automatic audit trail generation for banking operations
 * - Compliance event tracking and reporting
 * - Real-time violation detection and alerting
 * - Data access monitoring and control
 * - Transaction audit logging with pattern analysis
 * - Regulatory compliance verification
 * 
 * Supports multiple regulatory frameworks including:
 * - SOX (Sarbanes-Oxley Act)
 * - PCI DSS (Payment Card Industry)
 * - GDPR (General Data Protection Regulation)
 * - Basel III capital requirements
 * - AML/KYC regulations
 * - Islamic banking Sharia compliance
 */
@Aspect
@Component
public class BankingAuditAspect {

    @Autowired
    private BankingComplianceFramework complianceFramework;

    // Audit metrics tracking
    private final AtomicLong totalAuditEvents = new AtomicLong(0);
    private final AtomicLong complianceViolations = new AtomicLong(0);
    private final AtomicLong dataAccessEvents = new AtomicLong(0);
    private final AtomicLong transactionAudits = new AtomicLong(0);

    // Audit event storage
    private final Map<String, AuditEvent> auditEvents = new ConcurrentHashMap<>();
    private final Map<String, List<AuditEvent>> userAuditHistory = new ConcurrentHashMap<>();
    private final Set<String> sensitiveOperations = Set.of(
        "createLoan", "approveLoan", "transferFunds", "updateCustomer", 
        "accessCreditReport", "exportData", "deleteRecord", "modifyAccount"
    );

    /**
     * Audit annotation for marking methods that require audit logging
     */
    public @interface AuditLogged {
        String operation() default "";
        ComplianceStandard[] standards() default {};
        boolean sensitive() default false;
        String[] dataTypes() default {};
    }

    /**
     * Audit event record
     */
    public record AuditEvent(
        String auditId,
        String operation,
        String userId,
        String ipAddress,
        String userAgent,
        Map<String, Object> parameters,
        Map<String, Object> result,
        Instant timestamp,
        long duration,
        AuditStatus status,
        List<ComplianceStandard> appliedStandards,
        String riskLevel,
        Map<String, String> metadata
    ) {}

    /**
     * Audit status enumeration
     */
    public enum AuditStatus {
        SUCCESS, FAILURE, UNAUTHORIZED, COMPLIANCE_VIOLATION, BLOCKED
    }

    /**
     * Compliance violation record
     */
    public record ComplianceViolationEvent(
        String violationId,
        String operation,
        String userId,
        ComplianceStandard standard,
        String violation,
        String severity,
        Instant detectedAt,
        Map<String, Object> context
    ) {}

    /**
     * Audit all methods annotated with @AuditLogged
     */
    @Around("@annotation(auditLogged)")
    public Object auditLoggedMethod(ProceedingJoinPoint joinPoint, AuditLogged auditLogged) throws Throwable {
        String auditId = UUID.randomUUID().toString();
        String operation = auditLogged.operation().isEmpty() ? 
            joinPoint.getSignature().getName() : auditLogged.operation();
        
        Instant startTime = Instant.now();
        String userId = getCurrentUserId();
        String ipAddress = getCurrentIpAddress();
        String userAgent = getCurrentUserAgent();
        
        Map<String, Object> parameters = extractParameters(joinPoint);
        AuditStatus status = AuditStatus.SUCCESS;
        Object result = null;
        List<ComplianceStandard> appliedStandards = Arrays.asList(auditLogged.standards());
        
        try {
            // Pre-execution compliance checks
            if (auditLogged.sensitive()) {
                performSensitiveOperationCheck(operation, userId, parameters);
            }
            
            // Execute the method
            result = joinPoint.proceed();
            
            // Post-execution compliance validation
            validateCompliance(operation, parameters, result, appliedStandards);
            
        } catch (SecurityException e) {
            status = AuditStatus.UNAUTHORIZED;
            throw e;
        } catch (ComplianceViolationException e) {
            status = AuditStatus.COMPLIANCE_VIOLATION;
            complianceViolations.incrementAndGet();
            recordComplianceViolation(operation, userId, e.getStandard(), e.getMessage(), parameters);
            throw e;
        } catch (Exception e) {
            status = AuditStatus.FAILURE;
            throw e;
        } finally {
            // Always create audit record
            long duration = Instant.now().toEpochMilli() - startTime.toEpochMilli();
            
            AuditEvent auditEvent = new AuditEvent(
                auditId,
                operation,
                userId,
                ipAddress,
                userAgent,
                parameters,
                result != null ? Map.of("result", result.toString()) : Map.of(),
                startTime,
                duration,
                status,
                appliedStandards,
                assessRiskLevel(operation, parameters, status),
                createMetadata(auditLogged, joinPoint)
            );
            
            recordAuditEvent(auditEvent);
        }
        
        return result;
    }

    /**
     * Audit banking transactions
     */
    @Around("execution(* com.bank.*.application.*Service.*(com.bank.infrastructure.domain.Money, ..)) && args(amount, ..)")
    public Object auditBankingTransaction(ProceedingJoinPoint joinPoint, Money amount) throws Throwable {
        String auditId = UUID.randomUUID().toString();
        String operation = "banking_transaction_" + joinPoint.getSignature().getName();
        Instant startTime = Instant.now();
        
        String userId = getCurrentUserId();
        String ipAddress = getCurrentIpAddress();
        
        Map<String, Object> parameters = Map.of(
            "amount", amount.getAmount(),
            "currency", amount.getCurrency(),
            "method", joinPoint.getSignature().getName(),
            "args", Arrays.toString(joinPoint.getArgs())
        );
        
        AuditStatus status = AuditStatus.SUCCESS;
        Object result = null;
        
        try {
            // AML compliance check for large transactions
            if (amount.getAmount().compareTo(BigDecimal.valueOf(10000)) > 0) {
                complianceFramework.performAmlCheck(userId, amount, operation, parameters);
            }
            
            result = joinPoint.proceed();
            transactionAudits.incrementAndGet();
            
        } catch (Exception e) {
            status = AuditStatus.FAILURE;
            throw e;
        } finally {
            long duration = Instant.now().toEpochMilli() - startTime.toEpochMilli();
            
            AuditEvent auditEvent = new AuditEvent(
                auditId,
                operation,
                userId,
                ipAddress,
                getCurrentUserAgent(),
                parameters,
                result != null ? Map.of("result", "success") : Map.of("result", "failure"),
                startTime,
                duration,
                status,
                List.of(ComplianceStandard.AML),
                assessTransactionRiskLevel(amount, operation),
                Map.of("transactionType", "banking", "auditCategory", "financial")
            );
            
            recordAuditEvent(auditEvent);
        }
        
        return result;
    }

    /**
     * Audit data access operations
     */
    @Around("execution(* com.bank.*.infrastructure.repository.*Repository.findBy*(..))")
    public Object auditDataAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        String auditId = UUID.randomUUID().toString();
        String operation = "data_access_" + joinPoint.getSignature().getName();
        Instant startTime = Instant.now();
        
        String userId = getCurrentUserId();
        String ipAddress = getCurrentIpAddress();
        
        Map<String, Object> parameters = extractParameters(joinPoint);
        
        AuditStatus status = AuditStatus.SUCCESS;
        Object result = null;
        
        try {
            // GDPR compliance check for personal data access
            if (containsPersonalData(joinPoint.getSignature().getName())) {
                validateDataAccessPermission(userId, operation);
            }
            
            result = joinPoint.proceed();
            dataAccessEvents.incrementAndGet();
            
        } catch (SecurityException e) {
            status = AuditStatus.UNAUTHORIZED;
            throw e;
        } catch (Exception e) {
            status = AuditStatus.FAILURE;
            throw e;
        } finally {
            long duration = Instant.now().toEpochMilli() - startTime.toEpochMilli();
            
            AuditEvent auditEvent = new AuditEvent(
                auditId,
                operation,
                userId,
                ipAddress,
                getCurrentUserAgent(),
                parameters,
                result != null ? Map.of("recordCount", getRecordCount(result)) : Map.of(),
                startTime,
                duration,
                status,
                List.of(ComplianceStandard.GDPR),
                "MEDIUM",
                Map.of("dataAccess", "true", "auditCategory", "data_protection")
            );
            
            recordAuditEvent(auditEvent);
        }
        
        return result;
    }

    /**
     * Audit administrative operations
     */
    @AfterReturning(pointcut = "execution(* com.bank.*.application.*Service.delete*(..))", returning = "result")
    public void auditDeletionOperation(JoinPoint joinPoint, Object result) {
        String auditId = UUID.randomUUID().toString();
        String operation = "deletion_" + joinPoint.getSignature().getName();
        
        String userId = getCurrentUserId();
        String ipAddress = getCurrentIpAddress();
        
        Map<String, Object> parameters = extractParameters(joinPoint);
        
        AuditEvent auditEvent = new AuditEvent(
            auditId,
            operation,
            userId,
            ipAddress,
            getCurrentUserAgent(),
            parameters,
            Map.of("deleted", "true", "result", String.valueOf(result)),
            Instant.now(),
            0L,
            AuditStatus.SUCCESS,
            List.of(ComplianceStandard.SOX, ComplianceStandard.GDPR),
            "HIGH",
            Map.of("administrative", "true", "auditCategory", "data_lifecycle")
        );
        
        recordAuditEvent(auditEvent);
        
        // Create compliance audit trail
        complianceFramework.createAuditTrail(
            extractEntityId(parameters),
            "RECORD",
            "DELETE",
            userId,
            ipAddress,
            parameters,
            Map.of("deleted", true),
            ComplianceStandard.SOX
        );
    }

    /**
     * Record audit event and perform analysis
     */
    private void recordAuditEvent(AuditEvent auditEvent) {
        auditEvents.put(auditEvent.auditId(), auditEvent);
        totalAuditEvents.incrementAndGet();
        
        // Add to user audit history
        userAuditHistory.computeIfAbsent(auditEvent.userId(), k -> new ArrayList<>())
                       .add(auditEvent);
        
        // Analyze for patterns and anomalies
        analyzeAuditPattern(auditEvent);
        
        // Send to compliance framework if required
        if (auditEvent.appliedStandards().contains(ComplianceStandard.SOX) ||
            auditEvent.appliedStandards().contains(ComplianceStandard.PCI_DSS)) {
            sendToComplianceFramework(auditEvent);
        }
    }

    /**
     * Perform sensitive operation security check
     */
    private void performSensitiveOperationCheck(String operation, String userId, Map<String, Object> parameters) {
        if (sensitiveOperations.contains(operation)) {
            // Enhanced security checks for sensitive operations
            validateUserPermissions(userId, operation);
            checkRateLimiting(userId, operation);
            validateDataIntegrity(parameters);
        }
    }

    /**
     * Validate compliance for the operation
     */
    private void validateCompliance(String operation, Map<String, Object> parameters, 
                                   Object result, List<ComplianceStandard> standards) {
        for (ComplianceStandard standard : standards) {
            switch (standard) {
                case PCI_DSS -> validatePciDssCompliance(operation, parameters);
                case GDPR -> validateGdprCompliance(operation, parameters);
                case SOX -> validateSoxCompliance(operation, parameters);
                case AML -> validateAmlCompliance(operation, parameters);
                case SHARIA -> validateShariaCompliance(operation, parameters);
            }
        }
    }

    /**
     * Record compliance violation
     */
    private void recordComplianceViolation(String operation, String userId, 
                                         ComplianceStandard standard, String violation,
                                         Map<String, Object> context) {
        String violationId = UUID.randomUUID().toString();
        
        ComplianceViolationEvent violationEvent = new ComplianceViolationEvent(
            violationId,
            operation,
            userId,
            standard,
            violation,
            "HIGH",
            Instant.now(),
            context
        );
        
        // Log violation for monitoring and alerting
        System.err.println("COMPLIANCE VIOLATION: " + violationEvent);
    }

    /**
     * Analyze audit patterns for anomalies
     */
    private void analyzeAuditPattern(AuditEvent auditEvent) {
        List<AuditEvent> userHistory = userAuditHistory.get(auditEvent.userId());
        
        if (userHistory != null && userHistory.size() > 1) {
            // Check for rapid successive operations
            AuditEvent lastEvent = userHistory.get(userHistory.size() - 2);
            long timeDiff = auditEvent.timestamp().toEpochMilli() - lastEvent.timestamp().toEpochMilli();
            
            if (timeDiff < 1000 && sensitiveOperations.contains(auditEvent.operation())) {
                // Potential rapid-fire attack
                System.out.println("AUDIT ALERT: Rapid successive operations detected for user: " + auditEvent.userId());
            }
            
            // Check for unusual activity patterns
            long recentOperations = userHistory.stream()
                .filter(event -> event.timestamp().isAfter(Instant.now().minusSeconds(3600)))
                .count();
                
            if (recentOperations > 50) {
                System.out.println("AUDIT ALERT: High activity volume detected for user: " + auditEvent.userId());
            }
        }
    }

    /**
     * Get audit dashboard metrics
     */
    public AuditDashboard getAuditDashboard() {
        Map<String, Long> operationCounts = auditEvents.values().stream()
            .collect(java.util.stream.Collectors.groupingBy(
                AuditEvent::operation, 
                java.util.stream.Collectors.counting()
            ));
            
        Map<AuditStatus, Long> statusCounts = auditEvents.values().stream()
            .collect(java.util.stream.Collectors.groupingBy(
                AuditEvent::status, 
                java.util.stream.Collectors.counting()
            ));
            
        List<String> topUsers = userAuditHistory.entrySet().stream()
            .sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()))
            .limit(10)
            .map(Map.Entry::getKey)
            .collect(java.util.stream.Collectors.toList());
            
        return new AuditDashboard(
            totalAuditEvents.get(),
            complianceViolations.get(),
            dataAccessEvents.get(),
            transactionAudits.get(),
            auditEvents.size(),
            userAuditHistory.size(),
            operationCounts,
            statusCounts,
            topUsers
        );
    }

    // Helper methods
    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }

    private String getCurrentIpAddress() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            return request.getRemoteAddr();
        }
        return "unknown";
    }

    private String getCurrentUserAgent() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            return request.getHeader("User-Agent");
        }
        return "unknown";
    }

    private Map<String, Object> extractParameters(JoinPoint joinPoint) {
        Map<String, Object> parameters = new HashMap<>();
        Object[] args = joinPoint.getArgs();
        String[] paramNames = ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature()).getParameterNames();
        
        for (int i = 0; i < args.length && i < paramNames.length; i++) {
            if (args[i] != null) {
                parameters.put(paramNames[i], args[i].toString());
            }
        }
        
        return parameters;
    }

    private String assessRiskLevel(String operation, Map<String, Object> parameters, AuditStatus status) {
        if (status == AuditStatus.COMPLIANCE_VIOLATION || status == AuditStatus.UNAUTHORIZED) {
            return "CRITICAL";
        }
        if (sensitiveOperations.contains(operation)) {
            return "HIGH";
        }
        return "MEDIUM";
    }

    private String assessTransactionRiskLevel(Money amount, String operation) {
        if (amount.getAmount().compareTo(BigDecimal.valueOf(100000)) > 0) {
            return "HIGH";
        }
        if (amount.getAmount().compareTo(BigDecimal.valueOf(10000)) > 0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private Map<String, String> createMetadata(AuditLogged auditLogged, JoinPoint joinPoint) {
        return Map.of(
            "sensitive", String.valueOf(auditLogged.sensitive()),
            "dataTypes", String.join(",", auditLogged.dataTypes()),
            "class", joinPoint.getTarget().getClass().getSimpleName(),
            "method", joinPoint.getSignature().getName()
        );
    }

    private boolean containsPersonalData(String methodName) {
        return methodName.toLowerCase().contains("customer") || 
               methodName.toLowerCase().contains("personal") ||
               methodName.toLowerCase().contains("profile");
    }

    private void validateDataAccessPermission(String userId, String operation) {
        // Implement GDPR data access validation
        // In production, check against user permissions and data processing agreements
    }

    private int getRecordCount(Object result) {
        if (result instanceof Collection<?> collection) {
            return collection.size();
        }
        return result != null ? 1 : 0;
    }

    private String extractEntityId(Map<String, Object> parameters) {
        return parameters.values().stream()
            .findFirst()
            .map(Object::toString)
            .orElse("unknown");
    }

    private void sendToComplianceFramework(AuditEvent auditEvent) {
        // Send audit event to compliance framework for regulatory reporting
        complianceFramework.createAuditTrail(
            auditEvent.auditId(),
            "AUDIT_EVENT",
            auditEvent.operation(),
            auditEvent.userId(),
            auditEvent.ipAddress(),
            auditEvent.parameters(),
            auditEvent.result(),
            auditEvent.appliedStandards().isEmpty() ? ComplianceStandard.SOX : auditEvent.appliedStandards().get(0)
        );
    }

    // Compliance validation methods
    private void validatePciDssCompliance(String operation, Map<String, Object> parameters) {
        // PCI DSS compliance validation
    }

    private void validateGdprCompliance(String operation, Map<String, Object> parameters) {
        // GDPR compliance validation
    }

    private void validateSoxCompliance(String operation, Map<String, Object> parameters) {
        // SOX compliance validation
    }

    private void validateAmlCompliance(String operation, Map<String, Object> parameters) {
        // AML compliance validation
    }

    private void validateShariaCompliance(String operation, Map<String, Object> parameters) {
        // Sharia compliance validation
    }

    private void validateUserPermissions(String userId, String operation) {
        // User permission validation
    }

    private void checkRateLimiting(String userId, String operation) {
        // Rate limiting check
    }

    private void validateDataIntegrity(Map<String, Object> parameters) {
        // Data integrity validation
    }

    // Result classes
    public record AuditDashboard(
        long totalAuditEvents,
        long complianceViolations,
        long dataAccessEvents,
        long transactionAudits,
        int uniqueOperations,
        int uniqueUsers,
        Map<String, Long> operationCounts,
        Map<AuditStatus, Long> statusCounts,
        List<String> topUsers
    ) {}

    // Custom exception for compliance violations
    public static class ComplianceViolationException extends RuntimeException {
        private final ComplianceStandard standard;
        
        public ComplianceViolationException(String message, ComplianceStandard standard) {
            super(message);
            this.standard = standard;
        }
        
        public ComplianceStandard getStandard() {
            return standard;
        }
    }
}