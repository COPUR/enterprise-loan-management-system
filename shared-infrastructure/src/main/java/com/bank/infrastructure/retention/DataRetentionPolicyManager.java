package com.bank.infrastructure.retention;

import com.bank.infrastructure.security.BankingComplianceFramework.ComplianceStandard;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Data Retention Policy Manager for Banking Regulations
 * 
 * Comprehensive data lifecycle management system providing:
 * - Automated data retention policy enforcement
 * - Regulatory compliance for multiple jurisdictions
 * - Secure data archival and purging processes
 * - GDPR "right to be forgotten" implementation
 * - Banking-specific retention requirements
 * - Audit trail preservation for compliance
 * - Legal hold management for litigation
 * - Data anonymization and pseudonymization
 * 
 * Supports retention policies for:
 * - Banking transaction records (7-10 years)
 * - Customer identification documents (5-10 years)
 * - Loan documentation (7-30 years)
 * - Audit trails and compliance records (7+ years)
 * - Tax and regulatory filings (7+ years)
 * - Islamic banking records (Sharia compliance)
 */
@Component
public class DataRetentionPolicyManager {

    // Retention tracking
    private final Map<String, RetentionPolicy> retentionPolicies = new ConcurrentHashMap<>();
    private final Map<String, DataRecord> managedRecords = new ConcurrentHashMap<>();
    private final Map<String, LegalHold> legalHolds = new ConcurrentHashMap<>();
    private final Map<String, ArchivalRecord> archivedRecords = new ConcurrentHashMap<>();
    private final Set<String> scheduledForDeletion = ConcurrentHashMap.newKeySet();

    // Metrics
    private final AtomicLong recordsArchived = new AtomicLong(0);
    private final AtomicLong recordsPurged = new AtomicLong(0);
    private final AtomicLong retentionViolations = new AtomicLong(0);
    private final AtomicLong gdprRequests = new AtomicLong(0);

    // Standard retention periods (in years)
    private static final Map<DataCategory, Integer> STANDARD_RETENTION_YEARS = Map.of(
        DataCategory.TRANSACTION_RECORDS, 7,
        DataCategory.CUSTOMER_DOCUMENTS, 5,
        DataCategory.LOAN_DOCUMENTATION, 10,
        DataCategory.AUDIT_TRAILS, 7,
        DataCategory.TAX_RECORDS, 7,
        DataCategory.COMPLIANCE_RECORDS, 10,
        DataCategory.CREDIT_REPORTS, 7,
        DataCategory.ISLAMIC_CONTRACTS, 10,
        DataCategory.ANTI_MONEY_LAUNDERING, 5,
        DataCategory.KNOW_YOUR_CUSTOMER, 5
    );

    public DataRetentionPolicyManager() {
        initializeStandardPolicies();
    }

    /**
     * Data categories for retention management
     */
    public enum DataCategory {
        TRANSACTION_RECORDS, CUSTOMER_DOCUMENTS, LOAN_DOCUMENTATION,
        AUDIT_TRAILS, TAX_RECORDS, COMPLIANCE_RECORDS, CREDIT_REPORTS,
        ISLAMIC_CONTRACTS, ANTI_MONEY_LAUNDERING, KNOW_YOUR_CUSTOMER,
        PERSONAL_DATA, SENSITIVE_DATA, FINANCIAL_STATEMENTS, CORRESPONDENCE
    }

    /**
     * Data record lifecycle status
     */
    public enum DataLifecycleStatus {
        ACTIVE, ARCHIVED, SCHEDULED_FOR_DELETION, DELETED, 
        LEGAL_HOLD, ANONYMIZED, PSEUDONYMIZED
    }

    /**
     * Retention action types
     */
    public enum RetentionAction {
        ARCHIVE, DELETE, ANONYMIZE, PSEUDONYMIZE, EXTEND_RETENTION, LEGAL_HOLD
    }

    /**
     * Data retention policy record
     */
    public record RetentionPolicy(
        String policyId,
        String policyName,
        DataCategory dataCategory,
        Duration retentionPeriod,
        Duration archivalPeriod,
        RetentionAction retentionAction,
        List<ComplianceStandard> applicableStandards,
        Map<String, String> jurisdictionRequirements,
        boolean allowsGdprDeletion,
        String description,
        Instant createdAt,
        Instant lastUpdated
    ) {}

    /**
     * Data record under retention management
     */
    public record DataRecord(
        String recordId,
        String entityId,
        String entityType,
        DataCategory category,
        String dataLocation,
        Instant createdAt,
        Instant lastAccessedAt,
        DataLifecycleStatus status,
        String retentionPolicyId,
        Instant retentionExpiryDate,
        Instant archivalDate,
        boolean hasLegalHold,
        Map<String, Object> metadata
    ) {}

    /**
     * Legal hold for litigation or investigation
     */
    public record LegalHold(
        String holdId,
        String description,
        String requestedBy,
        Instant effectiveDate,
        Instant expiryDate,
        LegalHoldStatus status,
        Set<String> affectedRecords,
        String legalBasis,
        Map<String, String> contactInformation
    ) {}

    /**
     * Legal hold status
     */
    public enum LegalHoldStatus {
        ACTIVE, EXPIRED, RELEASED, PENDING
    }

    /**
     * Archival record
     */
    public record ArchivalRecord(
        String archiveId,
        String originalRecordId,
        String archiveLocation,
        String compressionType,
        String encryptionMethod,
        Instant archivedAt,
        String archivedBy,
        long originalSize,
        long compressedSize,
        String checksumHash
    ) {}

    /**
     * GDPR data subject request
     */
    public record GdprDataRequest(
        String requestId,
        String customerId,
        GdprRequestType requestType,
        String requestReason,
        Instant requestDate,
        Instant processedDate,
        GdprRequestStatus status,
        List<String> affectedRecords,
        String processingNotes
    ) {}

    /**
     * GDPR request types
     */
    public enum GdprRequestType {
        RIGHT_TO_ACCESS, RIGHT_TO_RECTIFICATION, RIGHT_TO_ERASURE,
        RIGHT_TO_RESTRICT_PROCESSING, RIGHT_TO_DATA_PORTABILITY, 
        RIGHT_TO_OBJECT, WITHDRAWAL_OF_CONSENT
    }

    /**
     * GDPR request status
     */
    public enum GdprRequestStatus {
        RECEIVED, PROCESSING, COMPLETED, REJECTED, PARTIALLY_COMPLETED
    }

    /**
     * Initialize standard retention policies
     */
    private void initializeStandardPolicies() {
        for (Map.Entry<DataCategory, Integer> entry : STANDARD_RETENTION_YEARS.entrySet()) {
            DataCategory category = entry.getKey();
            int years = entry.getValue();
            
            RetentionPolicy policy = new RetentionPolicy(
                "POLICY_" + category.name(),
                getStandardPolicyName(category),
                category,
                Duration.ofDays(years * 365L),
                Duration.ofDays(365L), // Archive after 1 year
                getStandardRetentionAction(category),
                getApplicableStandards(category),
                getJurisdictionRequirements(category),
                isGdprDeletionAllowed(category),
                getStandardPolicyDescription(category),
                Instant.now(),
                Instant.now()
            );
            
            retentionPolicies.put(policy.policyId(), policy);
        }
    }

    /**
     * Register data record for retention management
     */
    public void registerDataRecord(String recordId, String entityId, String entityType, 
                                   DataCategory category, String dataLocation) {
        RetentionPolicy policy = findApplicablePolicy(category);
        if (policy == null) {
            throw new IllegalArgumentException("No retention policy found for category: " + category);
        }
        
        Instant now = Instant.now();
        Instant retentionExpiry = now.plus(policy.retentionPeriod());
        
        DataRecord record = new DataRecord(
            recordId,
            entityId,
            entityType,
            category,
            dataLocation,
            now,
            now,
            DataLifecycleStatus.ACTIVE,
            policy.policyId(),
            retentionExpiry,
            null,
            false,
            Map.of("registered", "true", "category", category.toString())
        );
        
        managedRecords.put(recordId, record);
    }

    /**
     * Process GDPR data subject request
     */
    @Transactional
    public GdprDataRequest processGdprRequest(String customerId, GdprRequestType requestType, String reason) {
        gdprRequests.incrementAndGet();
        
        String requestId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        
        List<String> affectedRecords = findCustomerRecords(customerId);
        
        GdprDataRequest request = new GdprDataRequest(
            requestId,
            customerId,
            requestType,
            reason,
            now,
            null,
            GdprRequestStatus.RECEIVED,
            affectedRecords,
            "Request received and being processed"
        );
        
        // Process based on request type
        switch (requestType) {
            case RIGHT_TO_ERASURE -> processRightToErasure(request);
            case RIGHT_TO_ACCESS -> processRightToAccess(request);
            case RIGHT_TO_RECTIFICATION -> processRightToRectification(request);
            case RIGHT_TO_RESTRICT_PROCESSING -> processRightToRestrictProcessing(request);
            case RIGHT_TO_DATA_PORTABILITY -> processRightToDataPortability(request);
            case RIGHT_TO_OBJECT -> processRightToObject(request);
            case WITHDRAWAL_OF_CONSENT -> processWithdrawalOfConsent(request);
        }
        
        return request;
    }

    /**
     * Apply legal hold to records
     */
    public LegalHold applyLegalHold(String description, String requestedBy, Set<String> recordIds, 
                                   String legalBasis, Instant expiryDate) {
        String holdId = UUID.randomUUID().toString();
        
        LegalHold legalHold = new LegalHold(
            holdId,
            description,
            requestedBy,
            Instant.now(),
            expiryDate,
            LegalHoldStatus.ACTIVE,
            new HashSet<>(recordIds),
            legalBasis,
            Map.of("requestedBy", requestedBy, "contact", "legal@bank.com")
        );
        
        legalHolds.put(holdId, legalHold);
        
        // Update affected records
        for (String recordId : recordIds) {
            DataRecord record = managedRecords.get(recordId);
            if (record != null) {
                DataRecord updatedRecord = new DataRecord(
                    record.recordId(),
                    record.entityId(),
                    record.entityType(),
                    record.category(),
                    record.dataLocation(),
                    record.createdAt(),
                    record.lastAccessedAt(),
                    DataLifecycleStatus.LEGAL_HOLD,
                    record.retentionPolicyId(),
                    record.retentionExpiryDate(),
                    record.archivalDate(),
                    true,
                    record.metadata()
                );
                managedRecords.put(recordId, updatedRecord);
            }
        }
        
        return legalHold;
    }

    /**
     * Archive eligible records
     */
    @Transactional
    public List<ArchivalRecord> archiveEligibleRecords() {
        List<ArchivalRecord> archived = new ArrayList<>();
        Instant now = Instant.now();
        
        List<DataRecord> eligibleForArchival = managedRecords.values().stream()
            .filter(record -> record.status() == DataLifecycleStatus.ACTIVE)
            .filter(record -> !record.hasLegalHold())
            .filter(record -> shouldArchive(record, now))
            .collect(Collectors.toList());
        
        for (DataRecord record : eligibleForArchival) {
            try {
                ArchivalRecord archiveRecord = performArchival(record);
                archived.add(archiveRecord);
                
                // Update record status
                DataRecord updatedRecord = new DataRecord(
                    record.recordId(),
                    record.entityId(),
                    record.entityType(),
                    record.category(),
                    record.dataLocation(),
                    record.createdAt(),
                    record.lastAccessedAt(),
                    DataLifecycleStatus.ARCHIVED,
                    record.retentionPolicyId(),
                    record.retentionExpiryDate(),
                    now,
                    record.hasLegalHold(),
                    record.metadata()
                );
                managedRecords.put(record.recordId(), updatedRecord);
                recordsArchived.incrementAndGet();
                
            } catch (Exception e) {
                System.err.println("Failed to archive record " + record.recordId() + ": " + e.getMessage());
            }
        }
        
        return archived;
    }

    /**
     * Purge expired records
     */
    @Transactional
    public List<String> purgeExpiredRecords() {
        List<String> purged = new ArrayList<>();
        Instant now = Instant.now();
        
        List<DataRecord> eligibleForPurge = managedRecords.values().stream()
            .filter(record -> record.status() == DataLifecycleStatus.ARCHIVED)
            .filter(record -> !record.hasLegalHold())
            .filter(record -> record.retentionExpiryDate().isBefore(now))
            .collect(Collectors.toList());
        
        for (DataRecord record : eligibleForPurge) {
            try {
                performSecureDeletion(record);
                purged.add(record.recordId());
                
                // Update record status
                DataRecord updatedRecord = new DataRecord(
                    record.recordId(),
                    record.entityId(),
                    record.entityType(),
                    record.category(),
                    record.dataLocation(),
                    record.createdAt(),
                    record.lastAccessedAt(),
                    DataLifecycleStatus.DELETED,
                    record.retentionPolicyId(),
                    record.retentionExpiryDate(),
                    record.archivalDate(),
                    record.hasLegalHold(),
                    record.metadata()
                );
                managedRecords.put(record.recordId(), updatedRecord);
                recordsPurged.incrementAndGet();
                
            } catch (Exception e) {
                System.err.println("Failed to purge record " + record.recordId() + ": " + e.getMessage());
            }
        }
        
        return purged;
    }

    /**
     * Scheduled retention policy enforcement
     */
    @Scheduled(fixedRate = 86400000) // Daily
    public void enforceRetentionPolicies() {
        System.out.println("Starting scheduled retention policy enforcement...");
        
        // Archive eligible records
        List<ArchivalRecord> archived = archiveEligibleRecords();
        System.out.println("Archived " + archived.size() + " records");
        
        // Purge expired records
        List<String> purged = purgeExpiredRecords();
        System.out.println("Purged " + purged.size() + " records");
        
        // Release expired legal holds
        releaseExpiredLegalHolds();
        
        // Check for retention violations
        checkRetentionViolations();
        
        // Clean up orphaned archival records
        cleanupOrphanedArchives();
        
        System.out.println("Retention policy enforcement completed");
    }

    /**
     * Get retention dashboard metrics
     */
    public RetentionDashboard getRetentionDashboard() {
        Map<DataLifecycleStatus, Long> statusCounts = managedRecords.values().stream()
            .collect(Collectors.groupingBy(DataRecord::status, Collectors.counting()));
            
        Map<DataCategory, Long> categoryCounts = managedRecords.values().stream()
            .collect(Collectors.groupingBy(DataRecord::category, Collectors.counting()));
            
        long recordsNearingExpiry = managedRecords.values().stream()
            .filter(record -> record.retentionExpiryDate().isBefore(Instant.now().plus(Duration.ofDays(30))))
            .count();
            
        long recordsWithLegalHold = managedRecords.values().stream()
            .filter(DataRecord::hasLegalHold)
            .count();
        
        return new RetentionDashboard(
            managedRecords.size(),
            recordsArchived.get(),
            recordsPurged.get(),
            gdprRequests.get(),
            retentionViolations.get(),
            legalHolds.size(),
            recordsNearingExpiry,
            recordsWithLegalHold,
            statusCounts,
            categoryCounts
        );
    }

    // Helper methods
    private RetentionPolicy findApplicablePolicy(DataCategory category) {
        return retentionPolicies.values().stream()
            .filter(policy -> policy.dataCategory() == category)
            .findFirst()
            .orElse(null);
    }

    private boolean shouldArchive(DataRecord record, Instant now) {
        RetentionPolicy policy = retentionPolicies.get(record.retentionPolicyId());
        if (policy == null) return false;
        
        Instant archiveDate = record.createdAt().plus(policy.archivalPeriod());
        return now.isAfter(archiveDate);
    }

    private ArchivalRecord performArchival(DataRecord record) {
        String archiveId = UUID.randomUUID().toString();
        
        // In production, implement actual archival process
        // - Compress data
        // - Encrypt archived data
        // - Move to long-term storage
        // - Generate checksums
        
        return new ArchivalRecord(
            archiveId,
            record.recordId(),
            "/archive/banking/" + record.category() + "/" + archiveId,
            "gzip",
            "AES-256",
            Instant.now(),
            "system",
            1024L, // Original size
            512L,  // Compressed size
            "sha256:abcd1234..." // Checksum
        );
    }

    private void performSecureDeletion(DataRecord record) {
        // In production, implement secure deletion
        // - Overwrite data multiple times
        // - Verify deletion
        // - Update audit logs
        // - Remove from all indexes
        
        System.out.println("Securely deleting record: " + record.recordId());
    }

    private List<String> findCustomerRecords(String customerId) {
        return managedRecords.values().stream()
            .filter(record -> customerId.equals(record.entityId()) || 
                            record.metadata().containsValue(customerId))
            .map(DataRecord::recordId)
            .collect(Collectors.toList());
    }

    private void processRightToErasure(GdprDataRequest request) {
        // Implement GDPR right to erasure
        for (String recordId : request.affectedRecords()) {
            DataRecord record = managedRecords.get(recordId);
            if (record != null && !record.hasLegalHold()) {
                RetentionPolicy policy = retentionPolicies.get(record.retentionPolicyId());
                if (policy != null && policy.allowsGdprDeletion()) {
                    scheduleForDeletion(recordId);
                }
            }
        }
    }

    private void processRightToAccess(GdprDataRequest request) {
        // Implement GDPR right to access
        System.out.println("Processing right to access request for customer: " + request.customerId());
    }

    private void processRightToRectification(GdprDataRequest request) {
        // Implement GDPR right to rectification
        System.out.println("Processing right to rectification request for customer: " + request.customerId());
    }

    private void processRightToRestrictProcessing(GdprDataRequest request) {
        // Implement GDPR right to restrict processing
        System.out.println("Processing right to restrict processing request for customer: " + request.customerId());
    }

    private void processRightToDataPortability(GdprDataRequest request) {
        // Implement GDPR right to data portability
        System.out.println("Processing right to data portability request for customer: " + request.customerId());
    }

    private void processRightToObject(GdprDataRequest request) {
        // Implement GDPR right to object
        System.out.println("Processing right to object request for customer: " + request.customerId());
    }

    private void processWithdrawalOfConsent(GdprDataRequest request) {
        // Implement GDPR withdrawal of consent
        System.out.println("Processing withdrawal of consent request for customer: " + request.customerId());
    }

    private void scheduleForDeletion(String recordId) {
        scheduledForDeletion.add(recordId);
        
        DataRecord record = managedRecords.get(recordId);
        if (record != null) {
            DataRecord updatedRecord = new DataRecord(
                record.recordId(),
                record.entityId(),
                record.entityType(),
                record.category(),
                record.dataLocation(),
                record.createdAt(),
                record.lastAccessedAt(),
                DataLifecycleStatus.SCHEDULED_FOR_DELETION,
                record.retentionPolicyId(),
                record.retentionExpiryDate(),
                record.archivalDate(),
                record.hasLegalHold(),
                record.metadata()
            );
            managedRecords.put(recordId, updatedRecord);
        }
    }

    private void releaseExpiredLegalHolds() {
        Instant now = Instant.now();
        
        legalHolds.values().stream()
            .filter(hold -> hold.status() == LegalHoldStatus.ACTIVE)
            .filter(hold -> hold.expiryDate() != null && hold.expiryDate().isBefore(now))
            .forEach(hold -> {
                // Update legal hold status
                LegalHold updatedHold = new LegalHold(
                    hold.holdId(),
                    hold.description(),
                    hold.requestedBy(),
                    hold.effectiveDate(),
                    hold.expiryDate(),
                    LegalHoldStatus.EXPIRED,
                    hold.affectedRecords(),
                    hold.legalBasis(),
                    hold.contactInformation()
                );
                legalHolds.put(hold.holdId(), updatedHold);
                
                // Update affected records
                for (String recordId : hold.affectedRecords()) {
                    DataRecord record = managedRecords.get(recordId);
                    if (record != null) {
                        DataRecord updatedRecord = new DataRecord(
                            record.recordId(),
                            record.entityId(),
                            record.entityType(),
                            record.category(),
                            record.dataLocation(),
                            record.createdAt(),
                            record.lastAccessedAt(),
                            DataLifecycleStatus.ACTIVE,
                            record.retentionPolicyId(),
                            record.retentionExpiryDate(),
                            record.archivalDate(),
                            false,
                            record.metadata()
                        );
                        managedRecords.put(recordId, updatedRecord);
                    }
                }
            });
    }

    private void checkRetentionViolations() {
        // Check for retention policy violations
        Instant now = Instant.now();
        
        long violations = managedRecords.values().stream()
            .filter(record -> record.retentionExpiryDate().isBefore(now.minus(Duration.ofDays(30))))
            .filter(record -> record.status() == DataLifecycleStatus.ACTIVE)
            .count();
            
        retentionViolations.addAndGet(violations);
    }

    private void cleanupOrphanedArchives() {
        // Clean up orphaned archival records
        archivedRecords.entrySet().removeIf(entry -> {
            String originalRecordId = entry.getValue().originalRecordId();
            DataRecord record = managedRecords.get(originalRecordId);
            return record == null || record.status() == DataLifecycleStatus.DELETED;
        });
    }

    // Configuration helper methods
    private String getStandardPolicyName(DataCategory category) {
        return "Standard " + category.toString().replace("_", " ") + " Retention Policy";
    }

    private RetentionAction getStandardRetentionAction(DataCategory category) {
        return switch (category) {
            case PERSONAL_DATA -> RetentionAction.ANONYMIZE;
            case SENSITIVE_DATA -> RetentionAction.DELETE;
            default -> RetentionAction.ARCHIVE;
        };
    }

    private List<ComplianceStandard> getApplicableStandards(DataCategory category) {
        return switch (category) {
            case TRANSACTION_RECORDS -> List.of(ComplianceStandard.SOX, ComplianceStandard.AML);
            case PERSONAL_DATA -> List.of(ComplianceStandard.GDPR);
            case AUDIT_TRAILS -> List.of(ComplianceStandard.SOX);
            case ISLAMIC_CONTRACTS -> List.of(ComplianceStandard.SHARIA);
            default -> List.of(ComplianceStandard.SOX);
        };
    }

    private Map<String, String> getJurisdictionRequirements(DataCategory category) {
        return Map.of(
            "US", "SOX compliance required",
            "EU", "GDPR compliance required",
            "UK", "Data Protection Act compliance required"
        );
    }

    private boolean isGdprDeletionAllowed(DataCategory category) {
        return category != DataCategory.AUDIT_TRAILS && 
               category != DataCategory.TAX_RECORDS &&
               category != DataCategory.COMPLIANCE_RECORDS;
    }

    private String getStandardPolicyDescription(DataCategory category) {
        return "Standard retention policy for " + category.toString().toLowerCase().replace("_", " ") + 
               " as per regulatory requirements";
    }

    // Result classes
    public record RetentionDashboard(
        int totalManagedRecords,
        long recordsArchived,
        long recordsPurged,
        long gdprRequests,
        long retentionViolations,
        int activeLegalHolds,
        long recordsNearingExpiry,
        long recordsWithLegalHold,
        Map<DataLifecycleStatus, Long> statusDistribution,
        Map<DataCategory, Long> categoryDistribution
    ) {}
}