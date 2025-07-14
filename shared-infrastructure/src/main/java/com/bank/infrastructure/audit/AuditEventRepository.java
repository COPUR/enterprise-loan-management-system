package com.bank.infrastructure.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Audit Event Repository for Persistence
 * 
 * Provides data access layer for audit events with specialized queries
 * for compliance reporting and security monitoring
 */
@Repository
public interface AuditEventRepository extends JpaRepository<AuditEventEntity, String> {
    
    /**
     * Find audit events by criteria
     */
    @Query("SELECT a FROM AuditEventEntity a WHERE " +
           "(:userId IS NULL OR a.userId = :userId) AND " +
           "(:customerId IS NULL OR a.customerId = :customerId) AND " +
           "(:category IS NULL OR a.category = :category) AND " +
           "(:severity IS NULL OR a.severity = :severity) AND " +
           "a.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY a.timestamp DESC")
    List<AuditEventEntity> findByCriteria(
        @Param("userId") String userId,
        @Param("customerId") String customerId,
        @Param("category") AuditEvent.EventCategory category,
        @Param("severity") AuditEvent.EventSeverity severity,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
    
    /**
     * Find events by resource
     */
    List<AuditEventEntity> findByResourceOrderByTimestampDesc(String resource);
    
    /**
     * Find user activity in time range
     */
    List<AuditEventEntity> findByUserIdAndTimestampBetweenOrderByTimestampDesc(
        String userId, LocalDateTime start, LocalDateTime end
    );
    
    /**
     * Find events for compliance regulation
     */
    @Query("SELECT a FROM AuditEventEntity a WHERE " +
           "a.metadata LIKE %:regulation% AND " +
           "a.timestamp BETWEEN :start AND :end " +
           "ORDER BY a.timestamp DESC")
    List<AuditEventEntity> findByComplianceRegulation(
        @Param("regulation") String regulation,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
    
    /**
     * Count recent authentication failures
     */
    @Query("SELECT COUNT(a) FROM AuditEventEntity a WHERE " +
           "a.userId = :userId AND " +
           "a.category = 'AUTHENTICATION' AND " +
           "a.result = 'FAILURE' AND " +
           "a.timestamp > :since")
    long countRecentAuthenticationFailures(
        @Param("userId") String userId,
        @Param("since") LocalDateTime since
    );
    
    /**
     * Find high-risk security events
     */
    @Query("SELECT a FROM AuditEventEntity a WHERE " +
           "a.category = 'SECURITY' AND " +
           "a.severity IN ('ERROR', 'CRITICAL') AND " +
           "a.timestamp > :since " +
           "ORDER BY a.timestamp DESC")
    List<AuditEventEntity> findRecentHighRiskSecurityEvents(@Param("since") LocalDateTime since);
    
    /**
     * Find fraud detection events
     */
    @Query("SELECT a FROM AuditEventEntity a WHERE " +
           "a.category = 'FRAUD_DETECTION' AND " +
           "a.result = 'BLOCKED' AND " +
           "a.timestamp BETWEEN :start AND :end " +
           "ORDER BY a.timestamp DESC")
    List<AuditEventEntity> findFraudEventsInPeriod(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
    
    /**
     * Find events by correlation ID
     */
    List<AuditEventEntity> findByCorrelationIdOrderByTimestampAsc(String correlationId);
    
    /**
     * Count events by category in time period
     */
    @Query("SELECT a.category, COUNT(a) FROM AuditEventEntity a WHERE " +
           "a.timestamp BETWEEN :start AND :end " +
           "GROUP BY a.category")
    List<Object[]> countEventsByCategory(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
    
    /**
     * Find compliance violations
     */
    @Query("SELECT a FROM AuditEventEntity a WHERE " +
           "a.result = 'FAILURE' AND " +
           "a.severity IN ('ERROR', 'CRITICAL') AND " +
           "a.timestamp BETWEEN :start AND :end " +
           "ORDER BY a.severity DESC, a.timestamp DESC")
    List<AuditEventEntity> findComplianceViolations(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
    
    // Simple query methods for AuditService
    List<AuditEventEntity> findByTimestampBetween(LocalDateTime from, LocalDateTime to);
    List<AuditEventEntity> findByUserId(String userId);
    List<AuditEventEntity> findByCategory(AuditEvent.EventCategory category);
    long countBySeverityAndTimestampBetween(AuditEvent.EventSeverity severity, LocalDateTime from, LocalDateTime to);
}

/**
 * Default implementation for criteria-based queries
 */
interface AuditEventRepositoryCustom {
    List<AuditEvent> findByCriteria(AuditQueryCriteria criteria);
}

/**
 * Audit query criteria for flexible searching
 */
class AuditQueryCriteria {
    private String userId;
    private String customerId;
    private AuditEvent.EventCategory category;
    private AuditEvent.EventSeverity severity;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String eventType;
    private String resource;
    private String ipAddress;
    
    // Constructors, getters, and setters
    public AuditQueryCriteria() {}
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public AuditEvent.EventCategory getCategory() { return category; }
    public void setCategory(AuditEvent.EventCategory category) { this.category = category; }
    
    public AuditEvent.EventSeverity getSeverity() { return severity; }
    public void setSeverity(AuditEvent.EventSeverity severity) { this.severity = severity; }
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    
    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}