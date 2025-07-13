package com.bank.infrastructure.audit;

import org.springframework.stereotype.Component;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Audit Event Publisher for Real-time Monitoring
 * 
 * Publishes audit events to various channels for real-time monitoring
 */
@Component
public class AuditEventPublisher {
    
    private final ApplicationEventPublisher applicationEventPublisher;
    
    @Autowired
    public AuditEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
    
    /**
     * Publish audit event for real-time monitoring
     */
    public void publish(AuditEvent event) {
        // Publish to Spring event system
        applicationEventPublisher.publishEvent(event);
        
        // In production, would also publish to:
        // - Kafka topics for real-time analytics
        // - SIEM systems for security monitoring
        // - Compliance dashboards
        // - Alert systems
        
        logEventPublication(event);
    }
    
    private void logEventPublication(AuditEvent event) {
        System.out.println("AUDIT_EVENT_PUBLISHED: " + event.toStructuredLog());
    }
}