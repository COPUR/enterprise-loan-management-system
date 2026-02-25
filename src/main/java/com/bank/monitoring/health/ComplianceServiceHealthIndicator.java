package com.bank.monitoring.health;

import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Banking Health Indicator for Compliance Service
 * Monitors regulatory compliance checks, FAPI compliance, and audit trail health
 */
@Component("complianceService")
public class ComplianceServiceHealthIndicator implements HealthIndicator {

    private static final int COMPLIANCE_CHECKS_THRESHOLD = 1;
    private static final String HEALTH_CHECK_VERSION = "1.0.0";
    
    private final JdbcTemplate jdbcTemplate;
    
    public ComplianceServiceHealthIndicator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Override
    public Health health() {
        long startTime = System.currentTimeMillis();
        
        try {
            // Get recent compliance checks
            Integer complianceChecks = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM compliance_events WHERE created_at > NOW() - INTERVAL '1 HOUR'", 
                Integer.class
            );
            
            // Check FAPI compliance status
            String fapiCompliance = checkFapiCompliance();
            
            // Check audit trail health
            String auditTrailStatus = checkAuditTrailHealth();
            
            // Get last compliance check time
            String lastCheckTime = getLastComplianceCheckTime();
            
            // Check regulatory framework status
            String regulatoryStatus = checkRegulatoryFrameworkStatus();
            
            long checkDuration = System.currentTimeMillis() - startTime;
            
            Health.Builder healthBuilder = Health.up()
                .withDetail("complianceChecks", complianceChecks)
                .withDetail("fapiCompliance", fapiCompliance)
                .withDetail("auditTrail", auditTrailStatus)
                .withDetail("lastCheckTime", lastCheckTime)
                .withDetail("regulatoryFramework", regulatoryStatus)
                .withDetail("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .withDetail("checkDurationMs", checkDuration)
                .withDetail("healthCheckVersion", HEALTH_CHECK_VERSION);
            
            // Check if compliance service is active
            if (complianceChecks < COMPLIANCE_CHECKS_THRESHOLD) {
                return healthBuilder
                    .down()
                    .withDetail("status", "INACTIVE")
                    .withDetail("reason", "No recent compliance activity")
                    .withDetail("threshold", COMPLIANCE_CHECKS_THRESHOLD)
                    .build();
            }
            
            // Check FAPI compliance
            if (!"ACTIVE".equals(fapiCompliance)) {
                return healthBuilder
                    .down()
                    .withDetail("status", "FAPI_COMPLIANCE_ISSUE")
                    .build();
            }
            
            return healthBuilder
                .withDetail("status", "COMPLIANT")
                .build();
                
        } catch (Exception e) {
            long checkDuration = System.currentTimeMillis() - startTime;
            
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("status", "ERROR")
                .withDetail("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .withDetail("checkDurationMs", checkDuration)
                .withDetail("healthCheckVersion", HEALTH_CHECK_VERSION)
                .build();
        }
    }
    
    private String checkFapiCompliance() {
        try {
            // Check recent FAPI compliance validations
            Integer fapiChecks = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM compliance_events WHERE event_type = 'FAPI_VALIDATION' " +
                "AND created_at > NOW() - INTERVAL '30 MINUTES'", 
                Integer.class
            );
            
            if (fapiChecks != null && fapiChecks > 0) {
                // Check for any FAPI violations
                Integer violations = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM compliance_events WHERE event_type = 'FAPI_VIOLATION' " +
                    "AND created_at > NOW() - INTERVAL '1 HOUR'", 
                    Integer.class
                );
                
                return (violations == null || violations == 0) ? "ACTIVE" : "VIOLATIONS_DETECTED";
            } else {
                return "NO_RECENT_ACTIVITY";
            }
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
    
    private String checkAuditTrailHealth() {
        try {
            // Check if audit events are being recorded
            Integer auditEvents = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE created_at > NOW() - INTERVAL '15 MINUTES'", 
                Integer.class
            );
            
            if (auditEvents != null && auditEvents > 0) {
                return "ACTIVE";
            } else {
                return "NO_RECENT_EVENTS";
            }
        } catch (Exception e) {
            return "ERROR";
        }
    }
    
    private String getLastComplianceCheckTime() {
        try {
            String lastCheck = jdbcTemplate.queryForObject(
                "SELECT MAX(created_at)::text FROM compliance_events", 
                String.class
            );
            
            return lastCheck != null ? lastCheck : "NONE";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
    
    private String checkRegulatoryFrameworkStatus() {
        try {
            // Check various regulatory compliance metrics
            Integer pciCompliance = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM compliance_events WHERE framework = 'PCI_DSS' " +
                "AND status = 'COMPLIANT' AND created_at > NOW() - INTERVAL '24 HOURS'", 
                Integer.class
            );
            
            Integer gdprCompliance = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM compliance_events WHERE framework = 'GDPR' " +
                "AND status = 'COMPLIANT' AND created_at > NOW() - INTERVAL '24 HOURS'", 
                Integer.class
            );
            
            Integer soxCompliance = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM compliance_events WHERE framework = 'SOX' " +
                "AND status = 'COMPLIANT' AND created_at > NOW() - INTERVAL '24 HOURS'", 
                Integer.class
            );
            
            boolean allCompliant = (pciCompliance != null && pciCompliance > 0) &&
                                 (gdprCompliance != null && gdprCompliance > 0) &&
                                 (soxCompliance != null && soxCompliance > 0);
            
            if (allCompliant) {
                return "ALL_FRAMEWORKS_COMPLIANT";
            } else {
                return String.format("PARTIAL_COMPLIANCE (PCI:%d, GDPR:%d, SOX:%d)", 
                    pciCompliance, gdprCompliance, soxCompliance);
            }
        } catch (Exception e) {
            return "FRAMEWORK_CHECK_ERROR";
        }
    }
}