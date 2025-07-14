package com.bank.infrastructure.audit;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Simplified Audit Service for Banking Platform
 * 
 * Provides basic audit logging capabilities for:
 * - Regulatory compliance tracking
 * - Security monitoring
 * - Business activity logging
 */
@Service
public class AuditService {
    
    private final AuditEventRepository auditEventRepository;
    
    @Autowired
    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }
    
    /**
     * Log audit event synchronously
     */
    @Transactional
    public AuditEventEntity logAuditEvent(AuditEventEntity event) {
        // Basic validation
        if (event == null) {
            throw new IllegalArgumentException("Audit event cannot be null");
        }
        
        // Set timestamp if not provided
        if (event.getTimestamp() == null) {
            event.setTimestamp(LocalDateTime.now());
        }
        
        // Save event
        return auditEventRepository.save(event);
    }
    
    /**
     * Log audit event asynchronously
     */
    @Async
    public CompletableFuture<AuditEventEntity> logAuditEventAsync(AuditEventEntity event) {
        try {
            AuditEventEntity savedEvent = logAuditEvent(event);
            return CompletableFuture.completedFuture(savedEvent);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Find events by time range
     */
    public List<AuditEventEntity> findEventsByTimeRange(LocalDateTime from, LocalDateTime to) {
        return auditEventRepository.findByTimestampBetween(from, to);
    }
    
    /**
     * Find events by user
     */
    public List<AuditEventEntity> findEventsByUser(String userId) {
        return auditEventRepository.findByUserId(userId);
    }
    
    /**
     * Find events by category
     */
    public List<AuditEventEntity> findEventsByCategory(AuditEvent.EventCategory category) {
        return auditEventRepository.findByCategory(category);
    }
    
    /**
     * Count events by severity in time range
     */
    public long countEventsBySeverity(AuditEvent.EventSeverity severity, LocalDateTime from, LocalDateTime to) {
        return auditEventRepository.countBySeverityAndTimestampBetween(severity, from, to);
    }
}