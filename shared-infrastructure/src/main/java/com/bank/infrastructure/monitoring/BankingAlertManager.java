package com.bank.infrastructure.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Gauge;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Banking Alert Manager for Real-time Monitoring
 * 
 * Provides comprehensive alerting for banking operations:
 * - Performance degradation alerts
 * - Business rule violation alerts
 * - Compliance and security alerts
 * - System health alerts
 * - Islamic banking compliance alerts
 * - Escalation and notification management
 */
@Component
public class BankingAlertManager {
    
    private final MeterRegistry meterRegistry;
    
    // Alert counters
    private final Counter alertsGenerated;
    private final Counter alertsResolved;
    private final Counter criticalAlerts;
    private final Counter warningAlerts;
    private final Counter escalations;
    
    // Alert processing metrics
    private final Timer alertProcessingTime;
    private final Gauge activeAlerts;
    private final Gauge unresolvedCriticalAlerts;
    
    // Alert storage
    private final Map<String, Alert> activeAlertMap = new ConcurrentHashMap<>();
    private final AtomicInteger alertIdCounter = new AtomicInteger(1);
    
    // Alert thresholds
    private static final double HIGH_CPU_THRESHOLD = 80.0;
    private static final double HIGH_MEMORY_THRESHOLD = 85.0;
    private static final double HIGH_DB_RESPONSE_TIME_THRESHOLD = 1000.0; // ms
    private static final double LOW_CACHE_HIT_RATE_THRESHOLD = 70.0; // %
    private static final double HIGH_ERROR_RATE_THRESHOLD = 5.0; // %
    private static final double HIGH_LOAN_DEFAULT_RATE_THRESHOLD = 10.0; // %
    
    public BankingAlertManager(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize alert metrics
        this.alertsGenerated = Counter.builder("banking.alerts.generated")
            .description("Total alerts generated")
            .register(meterRegistry);
        
        this.alertsResolved = Counter.builder("banking.alerts.resolved")
            .description("Total alerts resolved")
            .register(meterRegistry);
        
        this.criticalAlerts = Counter.builder("banking.alerts.critical")
            .description("Critical alerts generated")
            .register(meterRegistry);
        
        this.warningAlerts = Counter.builder("banking.alerts.warning")
            .description("Warning alerts generated")
            .register(meterRegistry);
        
        this.escalations = Counter.builder("banking.alerts.escalations")
            .description("Alert escalations")
            .register(meterRegistry);
        
        this.alertProcessingTime = Timer.builder("banking.alerts.processing.time")
            .description("Alert processing time")
            .register(meterRegistry);
        
        this.activeAlerts = Gauge.builder("banking.alerts.active")
            .description("Number of active alerts")
            .register(meterRegistry, this, manager -> manager.getActiveAlertCount());
        
        this.unresolvedCriticalAlerts = Gauge.builder("banking.alerts.unresolved.critical")
            .description("Number of unresolved critical alerts")
            .register(meterRegistry, this, manager -> manager.getUnresolvedCriticalAlertCount());
    }
    
    /**
     * Generate system performance alert
     */
    public void checkSystemPerformance(double cpuUsage, double memoryUsage, double dbResponseTime) {
        if (cpuUsage > HIGH_CPU_THRESHOLD) {
            generateAlert(
                AlertType.PERFORMANCE,
                AlertSeverity.WARNING,
                "High CPU Usage",
                String.format("CPU usage is %.1f%%, exceeding threshold of %.1f%%", cpuUsage, HIGH_CPU_THRESHOLD),
                "system.cpu.high"
            );
        }
        
        if (memoryUsage > HIGH_MEMORY_THRESHOLD) {
            generateAlert(
                AlertType.PERFORMANCE,
                AlertSeverity.CRITICAL,
                "High Memory Usage",
                String.format("Memory usage is %.1f%%, exceeding threshold of %.1f%%", memoryUsage, HIGH_MEMORY_THRESHOLD),
                "system.memory.high"
            );
        }
        
        if (dbResponseTime > HIGH_DB_RESPONSE_TIME_THRESHOLD) {
            generateAlert(
                AlertType.PERFORMANCE,
                AlertSeverity.WARNING,
                "High Database Response Time",
                String.format("Database response time is %.1fms, exceeding threshold of %.1fms", dbResponseTime, HIGH_DB_RESPONSE_TIME_THRESHOLD),
                "database.response.slow"
            );
        }
    }
    
    /**
     * Generate business rule violation alert
     */
    public void checkBusinessRules(double loanDefaultRate, double paymentFailureRate) {
        if (loanDefaultRate > HIGH_LOAN_DEFAULT_RATE_THRESHOLD) {
            generateAlert(
                AlertType.BUSINESS,
                AlertSeverity.CRITICAL,
                "High Loan Default Rate",
                String.format("Loan default rate is %.1f%%, exceeding threshold of %.1f%%", loanDefaultRate, HIGH_LOAN_DEFAULT_RATE_THRESHOLD),
                "business.loan.default.high"
            );
        }
        
        if (paymentFailureRate > HIGH_ERROR_RATE_THRESHOLD) {
            generateAlert(
                AlertType.BUSINESS,
                AlertSeverity.WARNING,
                "High Payment Failure Rate",
                String.format("Payment failure rate is %.1f%%, exceeding threshold of %.1f%%", paymentFailureRate, HIGH_ERROR_RATE_THRESHOLD),
                "business.payment.failure.high"
            );
        }
    }
    
    /**
     * Generate cache performance alert
     */
    public void checkCachePerformance(double hitRate, String cacheType) {
        if (hitRate < LOW_CACHE_HIT_RATE_THRESHOLD) {
            generateAlert(
                AlertType.PERFORMANCE,
                AlertSeverity.WARNING,
                "Low Cache Hit Rate",
                String.format("%s cache hit rate is %.1f%%, below threshold of %.1f%%", cacheType, hitRate, LOW_CACHE_HIT_RATE_THRESHOLD),
                "cache.hit.rate.low"
            );
        }
    }
    
    /**
     * Generate security alert
     */
    public void generateSecurityAlert(String eventType, String description, String source) {
        generateAlert(
            AlertType.SECURITY,
            AlertSeverity.CRITICAL,
            "Security Event: " + eventType,
            description + " from source: " + source,
            "security." + eventType.toLowerCase().replace(" ", ".")
        );
    }
    
    /**
     * Generate compliance alert
     */
    public void generateComplianceAlert(String violationType, String description, String regulation) {
        generateAlert(
            AlertType.COMPLIANCE,
            AlertSeverity.CRITICAL,
            "Compliance Violation: " + violationType,
            description + " (Regulation: " + regulation + ")",
            "compliance." + violationType.toLowerCase().replace(" ", ".")
        );
    }
    
    /**
     * Generate Islamic banking compliance alert
     */
    public void generateShariahComplianceAlert(String violationType, String contractId, String description) {
        generateAlert(
            AlertType.ISLAMIC_COMPLIANCE,
            AlertSeverity.CRITICAL,
            "Shariah Compliance Violation: " + violationType,
            description + " (Contract ID: " + contractId + ")",
            "shariah." + violationType.toLowerCase().replace(" ", ".")
        );
    }
    
    /**
     * Generate database alert
     */
    public void generateDatabaseAlert(String alertType, String description, String severity) {
        AlertSeverity alertSeverity = AlertSeverity.valueOf(severity.toUpperCase());
        generateAlert(
            AlertType.DATABASE,
            alertSeverity,
            "Database Alert: " + alertType,
            description,
            "database." + alertType.toLowerCase().replace(" ", ".")
        );
    }
    
    /**
     * Generate event processing alert
     */
    public void generateEventProcessingAlert(String eventType, String error, int retryCount) {
        AlertSeverity severity = retryCount > 3 ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
        generateAlert(
            AlertType.EVENT_PROCESSING,
            severity,
            "Event Processing Failure",
            String.format("Failed to process %s event: %s (Retry count: %d)", eventType, error, retryCount),
            "event.processing.failure"
        );
    }
    
    /**
     * Core alert generation method
     */
    private void generateAlert(AlertType type, AlertSeverity severity, String title, String description, String alertKey) {
        // Check if similar alert already exists
        if (activeAlertMap.containsKey(alertKey)) {
            // Update existing alert
            Alert existingAlert = activeAlertMap.get(alertKey);
            existingAlert.incrementOccurrenceCount();
            existingAlert.setLastOccurrence(LocalDateTime.now());
            return;
        }
        
        // Create new alert
        Alert alert = new Alert(
            String.valueOf(alertIdCounter.getAndIncrement()),
            type,
            severity,
            title,
            description,
            alertKey,
            LocalDateTime.now()
        );
        
        // Store alert
        activeAlertMap.put(alertKey, alert);
        
        // Record metrics
        alertsGenerated.increment("type", type.name(), "severity", severity.name());
        
        if (severity == AlertSeverity.CRITICAL) {
            criticalAlerts.increment("type", type.name());
        } else {
            warningAlerts.increment("type", type.name());
        }
        
        // Process alert
        processAlert(alert);
    }
    
    /**
     * Process and route alert
     */
    private void processAlert(Alert alert) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            // Log alert
            logAlert(alert);
            
            // Send notifications based on severity
            if (alert.getSeverity() == AlertSeverity.CRITICAL) {
                sendCriticalAlertNotification(alert);
            } else {
                sendWarningAlertNotification(alert);
            }
            
            // Check for escalation
            checkEscalation(alert);
            
        } finally {
            sample.stop(alertProcessingTime);
        }
    }
    
    /**
     * Resolve alert
     */
    public void resolveAlert(String alertKey, String resolution) {
        Alert alert = activeAlertMap.remove(alertKey);
        if (alert != null) {
            alert.resolve(resolution);
            alertsResolved.increment("type", alert.getType().name(), "severity", alert.getSeverity().name());
            
            // Log resolution
            logAlertResolution(alert, resolution);
        }
    }
    
    /**
     * Auto-resolve alerts based on conditions
     */
    @Scheduled(fixedRate = 60000) // Run every minute
    public void autoResolveAlerts() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);
        
        activeAlertMap.entrySet().removeIf(entry -> {
            Alert alert = entry.getValue();
            
            // Auto-resolve old performance alerts
            if (alert.getType() == AlertType.PERFORMANCE && alert.getCreatedAt().isBefore(cutoff)) {
                resolveAlert(entry.getKey(), "Auto-resolved: Performance metrics normalized");
                return true;
            }
            
            return false;
        });
    }
    
    /**
     * Check for alert escalation
     */
    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    public void checkEscalations() {
        LocalDateTime escalationThreshold = LocalDateTime.now().minusMinutes(15);
        
        activeAlertMap.values().stream()
            .filter(alert -> alert.getSeverity() == AlertSeverity.CRITICAL)
            .filter(alert -> alert.getCreatedAt().isBefore(escalationThreshold))
            .filter(alert -> !alert.isEscalated())
            .forEach(this::escalateAlert);
    }
    
    /**
     * Escalate critical alert
     */
    private void escalateAlert(Alert alert) {
        alert.setEscalated(true);
        escalations.increment("type", alert.getType().name());
        
        // Send escalation notification
        sendEscalationNotification(alert);
        
        // Log escalation
        logAlertEscalation(alert);
    }
    
    /**
     * Get current alert statistics
     */
    public AlertStatistics getAlertStatistics() {
        long totalActive = activeAlertMap.size();
        long criticalActive = activeAlertMap.values().stream()
            .filter(alert -> alert.getSeverity() == AlertSeverity.CRITICAL)
            .count();
        long warningActive = totalActive - criticalActive;
        
        return new AlertStatistics(
            totalActive,
            criticalActive,
            warningActive,
            alertsGenerated.count(),
            alertsResolved.count(),
            escalations.count()
        );
    }
    
    /**
     * Get active alerts
     */
    public List<Alert> getActiveAlerts() {
        return new ArrayList<>(activeAlertMap.values());
    }
    
    /**
     * Get active alerts by type
     */
    public List<Alert> getActiveAlertsByType(AlertType type) {
        return activeAlertMap.values().stream()
            .filter(alert -> alert.getType() == type)
            .toList();
    }
    
    /**
     * Get active alerts by severity
     */
    public List<Alert> getActiveAlertsBySeverity(AlertSeverity severity) {
        return activeAlertMap.values().stream()
            .filter(alert -> alert.getSeverity() == severity)
            .toList();
    }
    
    // Helper methods for metrics
    
    private double getActiveAlertCount() {
        return activeAlertMap.size();
    }
    
    private double getUnresolvedCriticalAlertCount() {
        return activeAlertMap.values().stream()
            .filter(alert -> alert.getSeverity() == AlertSeverity.CRITICAL)
            .count();
    }
    
    // Notification methods (placeholders for actual implementation)
    
    private void logAlert(Alert alert) {
        System.out.println("ALERT: " + alert.getTitle() + " - " + alert.getDescription());
    }
    
    private void sendCriticalAlertNotification(Alert alert) {
        // Implementation would send to monitoring system, email, SMS, etc.
        System.out.println("CRITICAL ALERT: " + alert.getTitle());
    }
    
    private void sendWarningAlertNotification(Alert alert) {
        // Implementation would send to monitoring system
        System.out.println("WARNING ALERT: " + alert.getTitle());
    }
    
    private void sendEscalationNotification(Alert alert) {
        // Implementation would escalate to management
        System.out.println("ESCALATED ALERT: " + alert.getTitle());
    }
    
    private void logAlertResolution(Alert alert, String resolution) {
        System.out.println("RESOLVED: " + alert.getTitle() + " - " + resolution);
    }
    
    private void logAlertEscalation(Alert alert) {
        System.out.println("ESCALATED: " + alert.getTitle());
    }
    
    private void checkEscalation(Alert alert) {
        // Additional escalation logic if needed
    }
    
    // Inner classes
    
    public enum AlertType {
        PERFORMANCE, BUSINESS, SECURITY, COMPLIANCE, ISLAMIC_COMPLIANCE, DATABASE, EVENT_PROCESSING
    }
    
    public enum AlertSeverity {
        INFO, WARNING, CRITICAL
    }
    
    public static class Alert {
        private final String id;
        private final AlertType type;
        private final AlertSeverity severity;
        private final String title;
        private final String description;
        private final String alertKey;
        private final LocalDateTime createdAt;
        private LocalDateTime lastOccurrence;
        private int occurrenceCount;
        private boolean escalated;
        private boolean resolved;
        private String resolution;
        
        public Alert(String id, AlertType type, AlertSeverity severity, String title, 
                    String description, String alertKey, LocalDateTime createdAt) {
            this.id = id;
            this.type = type;
            this.severity = severity;
            this.title = title;
            this.description = description;
            this.alertKey = alertKey;
            this.createdAt = createdAt;
            this.lastOccurrence = createdAt;
            this.occurrenceCount = 1;
            this.escalated = false;
            this.resolved = false;
        }
        
        public void incrementOccurrenceCount() {
            this.occurrenceCount++;
        }
        
        public void resolve(String resolution) {
            this.resolved = true;
            this.resolution = resolution;
        }
        
        // Getters and setters
        public String getId() { return id; }
        public AlertType getType() { return type; }
        public AlertSeverity getSeverity() { return severity; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getAlertKey() { return alertKey; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getLastOccurrence() { return lastOccurrence; }
        public void setLastOccurrence(LocalDateTime lastOccurrence) { this.lastOccurrence = lastOccurrence; }
        public int getOccurrenceCount() { return occurrenceCount; }
        public boolean isEscalated() { return escalated; }
        public void setEscalated(boolean escalated) { this.escalated = escalated; }
        public boolean isResolved() { return resolved; }
        public String getResolution() { return resolution; }
    }
    
    public static class AlertStatistics {
        private final long totalActive;
        private final long criticalActive;
        private final long warningActive;
        private final double totalGenerated;
        private final double totalResolved;
        private final double totalEscalated;
        
        public AlertStatistics(long totalActive, long criticalActive, long warningActive,
                             double totalGenerated, double totalResolved, double totalEscalated) {
            this.totalActive = totalActive;
            this.criticalActive = criticalActive;
            this.warningActive = warningActive;
            this.totalGenerated = totalGenerated;
            this.totalResolved = totalResolved;
            this.totalEscalated = totalEscalated;
        }
        
        // Getters
        public long getTotalActive() { return totalActive; }
        public long getCriticalActive() { return criticalActive; }
        public long getWarningActive() { return warningActive; }
        public double getTotalGenerated() { return totalGenerated; }
        public double getTotalResolved() { return totalResolved; }
        public double getTotalEscalated() { return totalEscalated; }
        
        public double getResolutionRate() {
            return totalGenerated > 0 ? (totalResolved / totalGenerated) * 100 : 0;
        }
        
        public double getEscalationRate() {
            return totalGenerated > 0 ? (totalEscalated / totalGenerated) * 100 : 0;
        }
    }
}