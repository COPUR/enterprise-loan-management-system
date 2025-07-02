package com.loanmanagement.application.service;

/**
 * Stub implementations for missing service dependencies.
 * These would be properly implemented in a complete system.
 */
public class MissingServiceStubs {

    /**
     * Email service interface for notification functionality.
     */
    public interface EmailService {
        void sendEmail(Long customerId, String subject, String message);
    }

    /**
     * SMS service interface for notification functionality.
     */
    public interface SmsService {
        void sendSms(Long customerId, String message);
    }

    /**
     * Audit log repository interface for persistence.
     */
    public interface AuditLogRepository {
        void save(AuditLog auditLog);
    }

    /**
     * Audit log entity for audit trail functionality.
     */
    public static class AuditLog {
        private final String eventType;
        private final String entityId;
        private final String details;
        private final java.time.LocalDateTime timestamp;

        public AuditLog(String eventType, String entityId, String details) {
            this.eventType = eventType;
            this.entityId = entityId;
            this.details = details;
            this.timestamp = java.time.LocalDateTime.now();
        }

        // Getters for completeness
        public String getEventType() { return eventType; }
        public String getEntityId() { return entityId; }
        public String getDetails() { return details; }
        public java.time.LocalDateTime getTimestamp() { return timestamp; }
    }
}
