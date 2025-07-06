package com.bank.loan.loan.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Idempotency Service for Financial Operations
 * 
 * Provides idempotency guarantees for critical financial operations to prevent:
 * - Duplicate transactions
 * - Double payments
 * - Multiple loan applications
 * - Repeated financial operations
 * 
 * Implements banking-grade idempotency with:
 * - Time-based expiration
 * - Operation-specific scoping
 * - Audit trail integration
 * - Memory-efficient storage
 */
@Service
public class IdempotencyService {

    private final Map<String, IdempotencyRecord> idempotencyStore = new ConcurrentHashMap<>();
    private final AuditService auditService;

    // Default expiration times by operation type (in hours)
    private static final Map<String, Integer> OPERATION_EXPIRY_HOURS = Map.of(
        "LOAN_APPLICATION", 24,          // 24 hours for loan applications
        "PAYMENT_PROCESSING", 1,         // 1 hour for payments
        "AI_ANALYSIS", 4,                // 4 hours for AI operations
        "LOAN_APPROVAL", 8,              // 8 hours for approvals
        "DEFAULT", 2                     // 2 hours default
    );

    public IdempotencyService(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Check if an idempotent operation has already been processed
     * 
     * @param idempotencyKey Unique key for the operation
     * @param operationType Type of operation (LOAN_APPLICATION, PAYMENT_PROCESSING, etc.)
     * @param userId User performing the operation
     * @return true if operation was already processed
     */
    public boolean isOperationProcessed(String idempotencyKey, String operationType, String userId) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return false;
        }

        String fullKey = buildFullKey(idempotencyKey, operationType, userId);
        IdempotencyRecord record = idempotencyStore.get(fullKey);

        if (record == null) {
            return false;
        }

        // Check if record has expired
        if (isRecordExpired(record, operationType)) {
            // Remove expired record
            idempotencyStore.remove(fullKey);
            auditService.logComplianceEvent("IDEMPOTENCY_RECORD_EXPIRED", fullKey, 
                "Expired idempotency record removed for operation: " + operationType, 
                userId, "SYSTEM", null);
            return false;
        }

        // Log idempotency hit
        auditService.logComplianceEvent("IDEMPOTENCY_HIT", fullKey, 
            "Duplicate operation detected and prevented for: " + operationType, 
            userId, "SYSTEM", null);

        return true;
    }

    /**
     * Store the result of an idempotent operation
     * 
     * @param idempotencyKey Unique key for the operation
     * @param operationType Type of operation
     * @param userId User performing the operation
     * @param result Result object to store
     * @param operationDetails Additional operation details for audit
     */
    public void storeOperationResult(String idempotencyKey, String operationType, String userId, 
                                   Object result, String operationDetails) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return;
        }

        String fullKey = buildFullKey(idempotencyKey, operationType, userId);
        
        IdempotencyRecord record = new IdempotencyRecord(
            idempotencyKey,
            operationType,
            userId,
            result,
            LocalDateTime.now(),
            operationDetails
        );

        idempotencyStore.put(fullKey, record);

        // Log idempotency storage
        auditService.logComplianceEvent("IDEMPOTENCY_STORED", fullKey, 
            "Idempotency record stored for operation: " + operationType + " - " + operationDetails, 
            userId, "SYSTEM", null);
    }

    /**
     * Retrieve the stored result of an idempotent operation
     * 
     * @param idempotencyKey Unique key for the operation
     * @param operationType Type of operation
     * @param userId User performing the operation
     * @return Stored result object, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getOperationResult(String idempotencyKey, String operationType, String userId, Class<T> resultType) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return null;
        }

        String fullKey = buildFullKey(idempotencyKey, operationType, userId);
        IdempotencyRecord record = idempotencyStore.get(fullKey);

        if (record == null) {
            return null;
        }

        // Check if record has expired
        if (isRecordExpired(record, operationType)) {
            idempotencyStore.remove(fullKey);
            return null;
        }

        try {
            return resultType.cast(record.result);
        } catch (ClassCastException e) {
            // Log type mismatch but don't fail
            auditService.logComplianceEvent("IDEMPOTENCY_TYPE_MISMATCH", fullKey, 
                "Type mismatch in idempotency result retrieval: " + e.getMessage(), 
                userId, "SYSTEM", null);
            return null;
        }
    }

    /**
     * Remove an idempotency record (for testing or manual cleanup)
     * 
     * @param idempotencyKey Unique key for the operation
     * @param operationType Type of operation
     * @param userId User performing the operation
     */
    public void removeOperationRecord(String idempotencyKey, String operationType, String userId) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return;
        }

        String fullKey = buildFullKey(idempotencyKey, operationType, userId);
        IdempotencyRecord removed = idempotencyStore.remove(fullKey);

        if (removed != null) {
            auditService.logComplianceEvent("IDEMPOTENCY_REMOVED", fullKey, 
                "Idempotency record manually removed for operation: " + operationType, 
                userId, "SYSTEM", null);
        }
    }

    /**
     * Cleanup expired idempotency records
     * Should be called periodically by a scheduled task
     */
    public void cleanupExpiredRecords() {
        int removedCount = 0;
        
        for (Map.Entry<String, IdempotencyRecord> entry : idempotencyStore.entrySet()) {
            IdempotencyRecord record = entry.getValue();
            
            if (isRecordExpired(record, record.operationType)) {
                idempotencyStore.remove(entry.getKey());
                removedCount++;
            }
        }

        if (removedCount > 0) {
            auditService.logComplianceEvent("IDEMPOTENCY_CLEANUP", "SYSTEM", 
                "Cleaned up " + removedCount + " expired idempotency records", 
                "SYSTEM", "SYSTEM", null);
        }
    }

    /**
     * Get statistics about idempotency usage
     */
    public IdempotencyStats getIdempotencyStats() {
        int totalRecords = idempotencyStore.size();
        
        Map<String, Integer> recordsByType = new ConcurrentHashMap<>();
        int expiredRecords = 0;
        
        for (IdempotencyRecord record : idempotencyStore.values()) {
            recordsByType.merge(record.operationType, 1, Integer::sum);
            
            if (isRecordExpired(record, record.operationType)) {
                expiredRecords++;
            }
        }

        return new IdempotencyStats(totalRecords, recordsByType, expiredRecords);
    }

    // ========================================================================
    // Private Helper Methods
    // ========================================================================

    private String buildFullKey(String idempotencyKey, String operationType, String userId) {
        // Include user ID to scope idempotency per user
        return operationType + ":" + userId + ":" + idempotencyKey;
    }

    private boolean isRecordExpired(IdempotencyRecord record, String operationType) {
        int expiryHours = OPERATION_EXPIRY_HOURS.getOrDefault(operationType, 
                                                             OPERATION_EXPIRY_HOURS.get("DEFAULT"));
        
        LocalDateTime expiryTime = record.timestamp.plus(expiryHours, ChronoUnit.HOURS);
        return LocalDateTime.now().isAfter(expiryTime);
    }

    // ========================================================================
    // Inner Classes
    // ========================================================================

    /**
     * Idempotency record to store operation results
     */
    private static class IdempotencyRecord {
        final String idempotencyKey;
        final String operationType;
        final String userId;
        final Object result;
        final LocalDateTime timestamp;
        final String operationDetails;

        IdempotencyRecord(String idempotencyKey, String operationType, String userId, 
                         Object result, LocalDateTime timestamp, String operationDetails) {
            this.idempotencyKey = idempotencyKey;
            this.operationType = operationType;
            this.userId = userId;
            this.result = result;
            this.timestamp = timestamp;
            this.operationDetails = operationDetails;
        }
    }

    /**
     * Idempotency statistics
     */
    public static class IdempotencyStats {
        private final int totalRecords;
        private final Map<String, Integer> recordsByType;
        private final int expiredRecords;

        public IdempotencyStats(int totalRecords, Map<String, Integer> recordsByType, int expiredRecords) {
            this.totalRecords = totalRecords;
            this.recordsByType = recordsByType;
            this.expiredRecords = expiredRecords;
        }

        public int getTotalRecords() { return totalRecords; }
        public Map<String, Integer> getRecordsByType() { return recordsByType; }
        public int getExpiredRecords() { return expiredRecords; }
        public int getActiveRecords() { return totalRecords - expiredRecords; }

        @Override
        public String toString() {
            return "IdempotencyStats{" +
                    "totalRecords=" + totalRecords +
                    ", activeRecords=" + getActiveRecords() +
                    ", expiredRecords=" + expiredRecords +
                    ", recordsByType=" + recordsByType +
                    '}';
        }
    }
}