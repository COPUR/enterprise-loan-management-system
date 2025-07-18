package com.bank.infrastructure.security;

import com.bank.infrastructure.context.BankingContextPropagation;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Banking Security Monitor for Real-time Threat Detection
 * 
 * Comprehensive security monitoring system that provides:
 * - Real-time threat detection and response
 * - Behavioral analysis and anomaly detection
 * - Security metrics and alerting
 * - Compliance monitoring and reporting
 * - Incident response coordination
 * - Security posture assessment
 * 
 * Integrates with banking operations to provide continuous
 * security monitoring and automated threat response.
 */
@Component
public class BankingSecurityMonitor {

    private final Executor securityExecutor;
    private final BankingContextPropagation contextPropagation;
    
    // Security metrics tracking
    private final AtomicLong totalSecurityEvents = new AtomicLong(0);
    private final AtomicLong criticalAlerts = new AtomicLong(0);
    private final AtomicLong blockedAttacks = new AtomicLong(0);
    private final AtomicInteger activeSessions = new AtomicInteger(0);
    
    // Security event storage
    private final ConcurrentLinkedQueue<SecurityEvent> securityEvents = new ConcurrentLinkedQueue<>();
    private final Map<String, UserBehaviorProfile> userProfiles = new ConcurrentHashMap<>();
    private final Map<String, ThreatPattern> threatPatterns = new ConcurrentHashMap<>();
    private final Set<String> blockedIPs = ConcurrentHashMap.newKeySet();
    private final Map<String, SecurityIncident> activeIncidents = new ConcurrentHashMap<>();

    public BankingSecurityMonitor(@Qualifier("fraudExecutor") Executor securityExecutor,
                                BankingContextPropagation contextPropagation) {
        this.securityExecutor = securityExecutor;
        this.contextPropagation = contextPropagation;
        initializeThreatPatterns();
    }

    /**
     * Security event types for banking operations
     */
    public enum SecurityEventType {
        LOGIN_ATTEMPT, FAILED_LOGIN, SUSPICIOUS_TRANSACTION, DATA_ACCESS,
        PRIVILEGE_ESCALATION, UNUSUAL_LOCATION, MULTIPLE_SESSIONS,
        API_ABUSE, BRUTE_FORCE, SQL_INJECTION, XSS_ATTEMPT, CSRF_ATTEMPT,
        UNAUTHORIZED_ACCESS, DATA_EXPORT, CONFIGURATION_CHANGE,
        QUANTUM_CRYPTO_EVENT, COMPLIANCE_VIOLATION, FRAUD_DETECTED
    }

    /**
     * Security threat levels
     */
    public enum ThreatLevel {
        LOW(1), MEDIUM(2), HIGH(3), CRITICAL(4), EMERGENCY(5);
        
        private final int level;
        ThreatLevel(int level) { this.level = level; }
        public int getLevel() { return level; }
    }

    /**
     * Security event record
     */
    public record SecurityEvent(
        String eventId,
        SecurityEventType eventType,
        ThreatLevel threatLevel,
        String userId,
        String sessionId,
        String sourceIP,
        String userAgent,
        String resourceAccessed,
        Map<String, Object> eventData,
        Instant timestamp,
        String description
    ) {}

    /**
     * User behavior profile for anomaly detection
     */
    public static class UserBehaviorProfile {
        private final String userId;
        private final Map<String, Long> actionFrequency = new ConcurrentHashMap<>();
        private final Set<String> usualLocations = ConcurrentHashMap.newKeySet();
        private final Set<String> usualDevices = ConcurrentHashMap.newKeySet();
        private final Map<String, Instant> lastActivity = new ConcurrentHashMap<>();
        private double riskScore = 0.0;
        private Instant lastUpdated = Instant.now();

        public UserBehaviorProfile(String userId) {
            this.userId = userId;
        }

        public void updateActivity(String activity, String location, String device) {
            actionFrequency.merge(activity, 1L, Long::sum);
            usualLocations.add(location);
            usualDevices.add(device);
            lastActivity.put(activity, Instant.now());
            lastUpdated = Instant.now();
        }

        public boolean isAnomalousActivity(String activity, String location, String device) {
            // Check for unusual location
            if (!usualLocations.contains(location) && usualLocations.size() > 0) {
                return true;
            }
            
            // Check for unusual device
            if (!usualDevices.contains(device) && usualDevices.size() > 0) {
                return true;
            }
            
            // Check for unusual frequency
            Long avgFrequency = actionFrequency.values().stream()
                .mapToLong(Long::longValue)
                .boxed()
                .collect(Collectors.averagingLong(Long::longValue))
                .longValue();
                
            Long currentFrequency = actionFrequency.getOrDefault(activity, 0L);
            return currentFrequency > avgFrequency * 3; // 3x normal frequency
        }

        // Getters
        public String getUserId() { return userId; }
        public double getRiskScore() { return riskScore; }
        public void setRiskScore(double riskScore) { this.riskScore = riskScore; }
        public Instant getLastUpdated() { return lastUpdated; }
        public Map<String, Long> getActionFrequency() { return new HashMap<>(actionFrequency); }
        public Set<String> getUsualLocations() { return new HashSet<>(usualLocations); }
        public Set<String> getUsualDevices() { return new HashSet<>(usualDevices); }
    }

    /**
     * Threat pattern for attack detection
     */
    public record ThreatPattern(
        String patternId,
        String patternName,
        String description,
        List<String> indicators,
        ThreatLevel severity,
        String responseAction,
        Duration timeWindow
    ) {}

    /**
     * Security incident tracking
     */
    public record SecurityIncident(
        String incidentId,
        String title,
        String description,
        ThreatLevel severity,
        String userId,
        String sourceIP,
        Instant startTime,
        Instant lastActivity,
        IncidentStatus status,
        List<String> affectedResources,
        Map<String, String> responseActions
    ) {
        public enum IncidentStatus {
            OPEN, INVESTIGATING, CONTAINED, RESOLVED, CLOSED
        }
    }

    /**
     * Record security event and analyze for threats
     */
    public CompletableFuture<SecurityAnalysisResult> recordSecurityEvent(
            SecurityEventType eventType,
            String userId,
            String sessionId,
            String sourceIP,
            String userAgent,
            String resourceAccessed,
            Map<String, Object> eventData) {
        
        return contextPropagation.executeWithContext((Supplier<SecurityAnalysisResult>) () -> {
            String eventId = UUID.randomUUID().toString();
            
            // Determine threat level
            ThreatLevel threatLevel = assessThreatLevel(eventType, userId, sourceIP, eventData);
            
            // Create security event
            SecurityEvent event = new SecurityEvent(
                eventId,
                eventType,
                threatLevel,
                userId,
                sessionId,
                sourceIP,
                userAgent,
                resourceAccessed,
                eventData,
                Instant.now(),
                generateEventDescription(eventType, userId, resourceAccessed)
            );
            
            // Store event
            securityEvents.offer(event);
            totalSecurityEvents.incrementAndGet();
            
            // Analyze for anomalies
            SecurityAnalysisResult analysis = analyzeSecurityEvent(event);
            
            // Take automated response if needed
            if (analysis.requiresImmediateAction()) {
                takeAutomatedSecurityAction(event, analysis);
            }
            
            // Update user behavior profile
            updateUserBehaviorProfile(event);
            
            return analysis;
            
        }, securityExecutor);
    }

    /**
     * Analyze security event for threats and anomalies
     */
    private SecurityAnalysisResult analyzeSecurityEvent(SecurityEvent event) {
        List<String> detectedThreats = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        boolean requiresAction = false;
        double riskScore = 0.0;

        // Check against known threat patterns
        for (ThreatPattern pattern : threatPatterns.values()) {
            if (matchesThreatPattern(event, pattern)) {
                detectedThreats.add(pattern.patternName());
                riskScore += pattern.severity().getLevel() * 20.0;
                requiresAction = pattern.severity().getLevel() >= 3;
            }
        }

        // Behavioral analysis
        UserBehaviorProfile profile = userProfiles.get(event.userId());
        if (profile != null) {
            String location = extractLocation(event.sourceIP());
            String device = extractDevice(event.userAgent());
            
            if (profile.isAnomalousActivity(event.eventType().name(), location, device)) {
                detectedThreats.add("Anomalous user behavior detected");
                riskScore += 30.0;
                requiresAction = true;
            }
        }

        // Check for blocked IPs
        if (blockedIPs.contains(event.sourceIP())) {
            detectedThreats.add("Request from blocked IP address");
            riskScore = 100.0;
            requiresAction = true;
        }

        // Generate recommendations
        if (riskScore > 80) {
            recommendations.add("Block IP address immediately");
            recommendations.add("Terminate user session");
            recommendations.add("Initiate security incident");
        } else if (riskScore > 60) {
            recommendations.add("Increase monitoring for this user");
            recommendations.add("Request additional authentication");
        } else if (riskScore > 40) {
            recommendations.add("Log for further analysis");
            recommendations.add("Monitor subsequent activities");
        }

        return new SecurityAnalysisResult(
            event.eventId(),
            detectedThreats,
            riskScore,
            requiresAction,
            recommendations,
            Instant.now()
        );
    }

    /**
     * Take automated security action based on analysis
     */
    private void takeAutomatedSecurityAction(SecurityEvent event, SecurityAnalysisResult analysis) {
        if (analysis.riskScore() >= 90) {
            // Critical threat - immediate blocking
            blockedIPs.add(event.sourceIP());
            criticalAlerts.incrementAndGet();
            
            // Create security incident
            createSecurityIncident(event, analysis);
            
            // Notify security team (in production, integrate with SIEM/SOAR)
            notifySecurityTeam(event, analysis);
            
        } else if (analysis.riskScore() >= 70) {
            // High threat - enhanced monitoring
            enhanceUserMonitoring(event.userId());
            
        } else if (analysis.riskScore() >= 50) {
            // Medium threat - additional authentication
            requestAdditionalAuthentication(event.userId(), event.sessionId());
        }
        
        blockedAttacks.incrementAndGet();
    }

    /**
     * Create security incident
     */
    private void createSecurityIncident(SecurityEvent event, SecurityAnalysisResult analysis) {
        String incidentId = "INC-" + System.currentTimeMillis();
        
        SecurityIncident incident = new SecurityIncident(
            incidentId,
            "Security Threat Detected: " + event.eventType(),
            "Automated detection of security threat with risk score: " + analysis.riskScore(),
            event.threatLevel(),
            event.userId(),
            event.sourceIP(),
            Instant.now(),
            Instant.now(),
            SecurityIncident.IncidentStatus.OPEN,
            List.of(event.resourceAccessed()),
            Map.of(
                "autoBlocked", "true",
                "riskScore", String.valueOf(analysis.riskScore()),
                "detectedThreats", String.join(", ", analysis.detectedThreats())
            )
        );
        
        activeIncidents.put(incidentId, incident);
    }

    /**
     * Initialize threat patterns
     */
    private void initializeThreatPatterns() {
        // Brute force attack pattern
        threatPatterns.put("BRUTE_FORCE", new ThreatPattern(
            "BRUTE_FORCE",
            "Brute Force Attack",
            "Multiple failed login attempts in short time",
            List.of("FAILED_LOGIN"),
            ThreatLevel.HIGH,
            "BLOCK_IP",
            Duration.ofMinutes(5)
        ));
        
        // SQL injection pattern
        threatPatterns.put("SQL_INJECTION", new ThreatPattern(
            "SQL_INJECTION",
            "SQL Injection Attack",
            "Malicious SQL code in input parameters",
            List.of("SQL_INJECTION"),
            ThreatLevel.CRITICAL,
            "BLOCK_IP_AND_SESSION",
            Duration.ofSeconds(1)
        ));
        
        // Unusual location pattern
        threatPatterns.put("UNUSUAL_LOCATION", new ThreatPattern(
            "UNUSUAL_LOCATION",
            "Access from Unusual Location",
            "Login from unexpected geographic location",
            List.of("UNUSUAL_LOCATION"),
            ThreatLevel.MEDIUM,
            "REQUIRE_2FA",
            Duration.ofHours(1)
        ));
        
        // Privilege escalation pattern
        threatPatterns.put("PRIVILEGE_ESCALATION", new ThreatPattern(
            "PRIVILEGE_ESCALATION",
            "Privilege Escalation Attempt",
            "Attempt to access resources beyond user privileges",
            List.of("PRIVILEGE_ESCALATION"),
            ThreatLevel.HIGH,
            "TERMINATE_SESSION",
            Duration.ofMinutes(1)
        ));
    }

    /**
     * Check if event matches threat pattern
     */
    private boolean matchesThreatPattern(SecurityEvent event, ThreatPattern pattern) {
        return pattern.indicators().contains(event.eventType().name());
    }

    /**
     * Update user behavior profile
     */
    private void updateUserBehaviorProfile(SecurityEvent event) {
        String userId = event.userId();
        if (userId != null && !userId.isEmpty()) {
            UserBehaviorProfile profile = userProfiles.computeIfAbsent(userId, UserBehaviorProfile::new);
            
            String location = extractLocation(event.sourceIP());
            String device = extractDevice(event.userAgent());
            
            profile.updateActivity(event.eventType().name(), location, device);
            
            // Calculate risk score based on recent activities
            double riskScore = calculateUserRiskScore(profile);
            profile.setRiskScore(riskScore);
        }
    }

    /**
     * Calculate user risk score
     */
    private double calculateUserRiskScore(UserBehaviorProfile profile) {
        double riskScore = 0.0;
        
        // Multiple locations in short time
        if (profile.getUsualLocations().size() > 5) {
            riskScore += 20.0;
        }
        
        // Multiple devices
        if (profile.getUsualDevices().size() > 3) {
            riskScore += 15.0;
        }
        
        // High activity frequency
        double avgFrequency = profile.getActionFrequency().values().stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
            
        if (avgFrequency > 100) { // More than 100 actions on average
            riskScore += 25.0;
        }
        
        return Math.min(riskScore, 100.0);
    }

    /**
     * Assess threat level based on event characteristics
     */
    private ThreatLevel assessThreatLevel(SecurityEventType eventType, String userId, 
                                        String sourceIP, Map<String, Object> eventData) {
        switch (eventType) {
            case SQL_INJECTION, XSS_ATTEMPT, UNAUTHORIZED_ACCESS -> {
                return ThreatLevel.CRITICAL;
            }
            case BRUTE_FORCE, PRIVILEGE_ESCALATION, FRAUD_DETECTED -> {
                return ThreatLevel.HIGH;
            }
            case SUSPICIOUS_TRANSACTION, UNUSUAL_LOCATION, MULTIPLE_SESSIONS -> {
                return ThreatLevel.MEDIUM;
            }
            case FAILED_LOGIN, API_ABUSE -> {
                return ThreatLevel.MEDIUM;
            }
            default -> {
                return ThreatLevel.LOW;
            }
        }
    }

    /**
     * Generate event description
     */
    private String generateEventDescription(SecurityEventType eventType, String userId, String resource) {
        return String.format("Security event %s for user %s accessing %s", 
            eventType.name(), userId != null ? userId : "unknown", 
            resource != null ? resource : "unknown resource");
    }

    /**
     * Extract location from IP address (simplified)
     */
    private String extractLocation(String ipAddress) {
        // In production, use GeoIP database
        if (ipAddress.startsWith("192.168.") || ipAddress.startsWith("10.") || ipAddress.startsWith("127.")) {
            return "Internal";
        }
        return "External-" + ipAddress.substring(0, ipAddress.lastIndexOf('.'));
    }

    /**
     * Extract device information from user agent
     */
    private String extractDevice(String userAgent) {
        if (userAgent == null) return "Unknown";
        
        if (userAgent.contains("Mobile")) return "Mobile";
        if (userAgent.contains("Chrome")) return "Chrome-Desktop";
        if (userAgent.contains("Firefox")) return "Firefox-Desktop";
        if (userAgent.contains("Safari")) return "Safari-Desktop";
        
        return "Unknown-Device";
    }

    /**
     * Placeholder methods for security actions
     */
    private void notifySecurityTeam(SecurityEvent event, SecurityAnalysisResult analysis) {
        // In production, integrate with SIEM, SOAR, or notification system
        System.out.println("SECURITY ALERT: Critical threat detected - " + event.eventId());
    }

    private void enhanceUserMonitoring(String userId) {
        // Increase monitoring frequency for user
        System.out.println("Enhanced monitoring activated for user: " + userId);
    }

    private void requestAdditionalAuthentication(String userId, String sessionId) {
        // Request 2FA or additional verification
        System.out.println("Additional authentication requested for user: " + userId);
    }

    /**
     * Get security dashboard metrics
     */
    public SecurityDashboard getSecurityDashboard() {
        long recentEvents = securityEvents.stream()
            .mapToLong(event -> event.timestamp().isAfter(Instant.now().minus(Duration.ofHours(24))) ? 1 : 0)
            .sum();
            
        Map<ThreatLevel, Long> threatDistribution = securityEvents.stream()
            .collect(Collectors.groupingBy(SecurityEvent::threatLevel, Collectors.counting()));
            
        List<String> topThreats = securityEvents.stream()
            .filter(event -> event.timestamp().isAfter(Instant.now().minus(Duration.ofHours(24))))
            .collect(Collectors.groupingBy(SecurityEvent::eventType, Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<SecurityEventType, Long>comparingByValue().reversed())
            .limit(5)
            .map(entry -> entry.getKey().name())
            .collect(Collectors.toList());

        return new SecurityDashboard(
            totalSecurityEvents.get(),
            recentEvents,
            criticalAlerts.get(),
            blockedAttacks.get(),
            blockedIPs.size(),
            activeIncidents.size(),
            userProfiles.size(),
            threatDistribution,
            topThreats
        );
    }

    /**
     * Scheduled security maintenance
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void performSecurityMaintenance() {
        // Clean old events (keep last 30 days)
        Instant cutoff = Instant.now().minus(Duration.ofDays(30));
        securityEvents.removeIf(event -> event.timestamp().isBefore(cutoff));
        
        // Clean old user profiles (inactive for 90 days)
        Instant profileCutoff = Instant.now().minus(Duration.ofDays(90));
        userProfiles.entrySet().removeIf(entry -> entry.getValue().getLastUpdated().isBefore(profileCutoff));
        
        // Update threat patterns (in production, sync with threat intelligence feeds)
        updateThreatIntelligence();
    }

    private void updateThreatIntelligence() {
        // In production, integrate with threat intelligence feeds
        System.out.println("Threat intelligence updated");
    }

    // Result classes
    public record SecurityAnalysisResult(
        String eventId,
        List<String> detectedThreats,
        double riskScore,
        boolean requiresImmediateAction,
        List<String> recommendations,
        Instant analyzedAt
    ) {
        public boolean requiresImmediateAction() {
            return requiresImmediateAction;
        }
    }

    public record SecurityDashboard(
        long totalSecurityEvents,
        long recentEvents,
        long criticalAlerts,
        long blockedAttacks,
        int blockedIPs,
        int activeIncidents,
        int monitoredUsers,
        Map<ThreatLevel, Long> threatDistribution,
        List<String> topThreats
    ) {}
}