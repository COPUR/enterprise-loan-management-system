package com.bank.loan.loan.security.migration;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Migration Management Controller
 * Provides endpoints for managing FAPI 2.0 + DPoP migration phases
 */
@RestController
@RequestMapping("/api/v1/migration")
@PreAuthorize("hasRole('ADMIN') or hasRole('MIGRATION_MANAGER')")
public class MigrationController {

    private final PhasedMigrationStrategy.MigrationOrchestrator migrationOrchestrator;
    private final PhasedMigrationStrategy.MigrationMetrics migrationMetrics;
    private final PhasedMigrationStrategy.MigrationSafetyControls safetyControls;
    private final PhasedMigrationStrategy migrationStrategy;

    public MigrationController(PhasedMigrationStrategy.MigrationOrchestrator migrationOrchestrator,
                             PhasedMigrationStrategy.MigrationMetrics migrationMetrics,
                             PhasedMigrationStrategy.MigrationSafetyControls safetyControls,
                             PhasedMigrationStrategy migrationStrategy) {
        this.migrationOrchestrator = migrationOrchestrator;
        this.migrationMetrics = migrationMetrics;
        this.safetyControls = safetyControls;
        this.migrationStrategy = migrationStrategy;
    }

    /**
     * Get current migration status
     */
    @GetMapping("/status")
    public ResponseEntity<MigrationStatusResponse> getMigrationStatus() {
        PhasedMigrationStrategy.MigrationReport report = migrationMetrics.generateMigrationReport();
        
        MigrationStatusResponse response = new MigrationStatusResponse(
            report.getCurrentPhase(),
            migrationStrategy.isEnabled(),
            migrationStrategy.getPhaseStartTime(),
            migrationStrategy.getPhaseEndTime(),
            migrationStrategy.getRolloutPercentage(),
            report.getClientStats().getMigrationPercentage(),
            report.getPerformanceMetrics().getErrorRate(),
            report.getSecurityMetrics().getSecurityIncidents(),
            safetyControls.shouldTriggerRollback()
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get detailed migration report
     */
    @GetMapping("/report")
    public ResponseEntity<PhasedMigrationStrategy.MigrationReport> getMigrationReport() {
        PhasedMigrationStrategy.MigrationReport report = migrationMetrics.generateMigrationReport();
        return ResponseEntity.ok(report);
    }

    /**
     * Check if specific client should use FAPI 2.0
     */
    @GetMapping("/client/{clientId}/fapi2-status")
    public ResponseEntity<ClientMigrationResponse> getClientMigrationStatus(
            @PathVariable String clientId,
            @RequestParam(required = false) String userAgent,
            @RequestParam(required = false) String ipAddress) {
        
        boolean shouldUseFAPI2 = migrationOrchestrator.shouldUseFAPI2ForClient(clientId, userAgent, ipAddress);
        
        ClientMigrationResponse response = new ClientMigrationResponse(
            clientId,
            shouldUseFAPI2,
            migrationOrchestrator.getCurrentPhase(),
            shouldUseFAPI2 ? "FAPI 2.0 + DPoP" : "FAPI 1.0",
            LocalDateTime.now()
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Advance to next migration phase
     */
    @PostMapping("/advance-phase")
    @PreAuthorize("hasRole('MIGRATION_MANAGER')")
    public ResponseEntity<Map<String, Object>> advancePhase(@RequestBody AdvancePhaseRequest request) {
        PhasedMigrationStrategy.MigrationPhase currentPhase = migrationStrategy.getCurrentPhase();
        PhasedMigrationStrategy.MigrationPhase nextPhase = getNextPhase(currentPhase);
        
        if (nextPhase == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "No next phase available",
                "currentPhase", currentPhase.name()
            ));
        }
        
        // Validate phase advancement criteria
        if (!validatePhaseAdvancementCriteria(currentPhase)) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Phase advancement criteria not met",
                "currentPhase", currentPhase.name(),
                "required_criteria", getPhaseAdvancementCriteria(currentPhase)
            ));
        }
        
        // Advance phase
        migrationStrategy.setCurrentPhase(nextPhase);
        migrationStrategy.setPhaseStartTime(LocalDateTime.now());
        migrationStrategy.setPhaseEndTime(calculatePhaseEndTime(nextPhase));
        
        return ResponseEntity.ok(Map.of(
            "message", "Successfully advanced to next phase",
            "previousPhase", currentPhase.name(),
            "currentPhase", nextPhase.name(),
            "phaseStartTime", migrationStrategy.getPhaseStartTime(),
            "phaseEndTime", migrationStrategy.getPhaseEndTime()
        ));
    }

    /**
     * Trigger manual rollback
     */
    @PostMapping("/rollback")
    @PreAuthorize("hasRole('MIGRATION_MANAGER')")
    public ResponseEntity<Map<String, Object>> triggerRollback(@RequestBody RollbackRequest request) {
        if (!migrationStrategy.isAllowRollback()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Rollback is not allowed in current configuration"
            ));
        }
        
        // Perform rollback
        PhasedMigrationStrategy.MigrationPhase currentPhase = migrationStrategy.getCurrentPhase();
        migrationStrategy.setCurrentPhase(PhasedMigrationStrategy.MigrationPhase.PHASE_1_INTERNAL_TESTING);
        migrationStrategy.setRolloutPercentage(0);
        
        return ResponseEntity.ok(Map.of(
            "message", "Rollback initiated successfully",
            "rollbackReason", request.getReason(),
            "previousPhase", currentPhase.name(),
            "currentPhase", migrationStrategy.getCurrentPhase().name(),
            "rollbackTime", LocalDateTime.now()
        ));
    }

    /**
     * Update rollout percentage
     */
    @PostMapping("/rollout-percentage")
    @PreAuthorize("hasRole('MIGRATION_MANAGER')")
    public ResponseEntity<Map<String, Object>> updateRolloutPercentage(@RequestBody UpdateRolloutRequest request) {
        int newPercentage = request.getPercentage();
        
        if (newPercentage < 0 || newPercentage > 100) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Rollout percentage must be between 0 and 100"
            ));
        }
        
        int previousPercentage = migrationStrategy.getRolloutPercentage();
        migrationStrategy.setRolloutPercentage(newPercentage);
        
        return ResponseEntity.ok(Map.of(
            "message", "Rollout percentage updated successfully",
            "previousPercentage", previousPercentage,
            "newPercentage", newPercentage,
            "updatedAt", LocalDateTime.now()
        ));
    }

    /**
     * Get migration phase timeline
     */
    @GetMapping("/timeline")
    public ResponseEntity<MigrationTimelineResponse> getMigrationTimeline() {
        MigrationTimelineResponse timeline = new MigrationTimelineResponse();
        
        // Add all phases with their planned durations
        for (PhasedMigrationStrategy.MigrationPhase phase : PhasedMigrationStrategy.MigrationPhase.values()) {
            PhaseInfo phaseInfo = new PhaseInfo(
                phase,
                phase.getDisplayName(),
                phase.getDescription(),
                getPlannedPhaseDuration(phase),
                getPhaseCriteria(phase),
                phase == migrationStrategy.getCurrentPhase()
            );
            timeline.addPhase(phaseInfo);
        }
        
        return ResponseEntity.ok(timeline);
    }

    // Helper methods
    private PhasedMigrationStrategy.MigrationPhase getNextPhase(PhasedMigrationStrategy.MigrationPhase currentPhase) {
        PhasedMigrationStrategy.MigrationPhase[] phases = PhasedMigrationStrategy.MigrationPhase.values();
        for (int i = 0; i < phases.length - 1; i++) {
            if (phases[i] == currentPhase) {
                return phases[i + 1];
            }
        }
        return null; // Already at last phase
    }

    private boolean validatePhaseAdvancementCriteria(PhasedMigrationStrategy.MigrationPhase currentPhase) {
        // Implementation would check specific criteria for each phase
        PhasedMigrationStrategy.MigrationReport report = migrationMetrics.generateMigrationReport();
        
        switch (currentPhase) {
            case PHASE_0_PREPARATION:
                return true; // Can always advance from preparation
                
            case PHASE_1_INTERNAL_TESTING:
                return report.getPerformanceMetrics().getErrorRate() < 5.0;
                
            case PHASE_2_PILOT_CLIENTS:
                return report.getPerformanceMetrics().getErrorRate() < 2.0 &&
                       report.getSecurityMetrics().getSecurityIncidents() == 0;
                
            case PHASE_3_GRADUAL_ROLLOUT:
                return report.getClientStats().getMigrationPercentage() >= 90.0;
                
            case PHASE_4_FULL_MIGRATION:
                return report.getClientStats().getMigrationPercentage() >= 95.0;
                
            default:
                return false;
        }
    }

    private String[] getPhaseAdvancementCriteria(PhasedMigrationStrategy.MigrationPhase phase) {
        switch (phase) {
            case PHASE_1_INTERNAL_TESTING:
                return new String[]{"Error rate < 5%", "No critical security incidents"};
            case PHASE_2_PILOT_CLIENTS:
                return new String[]{"Error rate < 2%", "Zero security incidents", "Pilot client feedback positive"};
            case PHASE_3_GRADUAL_ROLLOUT:
                return new String[]{"90% client migration complete", "Performance within SLA"};
            case PHASE_4_FULL_MIGRATION:
                return new String[]{"95% client migration complete", "All legacy flows disabled"};
            default:
                return new String[]{"Manual approval required"};
        }
    }

    private LocalDateTime calculatePhaseEndTime(PhasedMigrationStrategy.MigrationPhase phase) {
        LocalDateTime now = LocalDateTime.now();
        return switch (phase) {
            case PHASE_0_PREPARATION -> now.plusDays(30);
            case PHASE_1_INTERNAL_TESTING -> now.plusDays(14);
            case PHASE_2_PILOT_CLIENTS -> now.plusDays(21);
            case PHASE_3_GRADUAL_ROLLOUT -> now.plusDays(60);
            case PHASE_4_FULL_MIGRATION -> now.plusDays(30);
            case PHASE_5_LEGACY_CLEANUP -> now.plusDays(14);
        };
    }

    private int getPlannedPhaseDuration(PhasedMigrationStrategy.MigrationPhase phase) {
        return switch (phase) {
            case PHASE_0_PREPARATION -> 30;
            case PHASE_1_INTERNAL_TESTING -> 14;
            case PHASE_2_PILOT_CLIENTS -> 21;
            case PHASE_3_GRADUAL_ROLLOUT -> 60;
            case PHASE_4_FULL_MIGRATION -> 30;
            case PHASE_5_LEGACY_CLEANUP -> 14;
        };
    }

    private String[] getPhaseCriteria(PhasedMigrationStrategy.MigrationPhase phase) {
        return getPhaseAdvancementCriteria(phase);
    }

    // Request/Response DTOs
    public static class MigrationStatusResponse {
        private final PhasedMigrationStrategy.MigrationPhase currentPhase;
        private final boolean migrationEnabled;
        private final LocalDateTime phaseStartTime;
        private final LocalDateTime phaseEndTime;
        private final int rolloutPercentage;
        private final double migrationPercentage;
        private final double errorRate;
        private final int securityIncidents;
        private final boolean shouldRollback;

        public MigrationStatusResponse(PhasedMigrationStrategy.MigrationPhase currentPhase,
                                     boolean migrationEnabled,
                                     LocalDateTime phaseStartTime,
                                     LocalDateTime phaseEndTime,
                                     int rolloutPercentage,
                                     double migrationPercentage,
                                     double errorRate,
                                     int securityIncidents,
                                     boolean shouldRollback) {
            this.currentPhase = currentPhase;
            this.migrationEnabled = migrationEnabled;
            this.phaseStartTime = phaseStartTime;
            this.phaseEndTime = phaseEndTime;
            this.rolloutPercentage = rolloutPercentage;
            this.migrationPercentage = migrationPercentage;
            this.errorRate = errorRate;
            this.securityIncidents = securityIncidents;
            this.shouldRollback = shouldRollback;
        }

        // Getters
        public PhasedMigrationStrategy.MigrationPhase getCurrentPhase() { return currentPhase; }
        public boolean isMigrationEnabled() { return migrationEnabled; }
        public LocalDateTime getPhaseStartTime() { return phaseStartTime; }
        public LocalDateTime getPhaseEndTime() { return phaseEndTime; }
        public int getRolloutPercentage() { return rolloutPercentage; }
        public double getMigrationPercentage() { return migrationPercentage; }
        public double getErrorRate() { return errorRate; }
        public int getSecurityIncidents() { return securityIncidents; }
        public boolean shouldRollback() { return shouldRollback; }
    }

    public static class ClientMigrationResponse {
        private final String clientId;
        private final boolean shouldUseFAPI2;
        private final PhasedMigrationStrategy.MigrationPhase migrationPhase;
        private final String securityProfile;
        private final LocalDateTime checkTime;

        public ClientMigrationResponse(String clientId, boolean shouldUseFAPI2,
                                     PhasedMigrationStrategy.MigrationPhase migrationPhase,
                                     String securityProfile, LocalDateTime checkTime) {
            this.clientId = clientId;
            this.shouldUseFAPI2 = shouldUseFAPI2;
            this.migrationPhase = migrationPhase;
            this.securityProfile = securityProfile;
            this.checkTime = checkTime;
        }

        // Getters
        public String getClientId() { return clientId; }
        public boolean shouldUseFAPI2() { return shouldUseFAPI2; }
        public PhasedMigrationStrategy.MigrationPhase getMigrationPhase() { return migrationPhase; }
        public String getSecurityProfile() { return securityProfile; }
        public LocalDateTime getCheckTime() { return checkTime; }
    }

    public static class AdvancePhaseRequest {
        private String reason;
        private boolean skipValidation;

        // Getters and setters
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public boolean isSkipValidation() { return skipValidation; }
        public void setSkipValidation(boolean skipValidation) { this.skipValidation = skipValidation; }
    }

    public static class RollbackRequest {
        private String reason;
        private boolean emergency;

        // Getters and setters
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public boolean isEmergency() { return emergency; }
        public void setEmergency(boolean emergency) { this.emergency = emergency; }
    }

    public static class UpdateRolloutRequest {
        private int percentage;
        private String reason;

        // Getters and setters
        public int getPercentage() { return percentage; }
        public void setPercentage(int percentage) { this.percentage = percentage; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class MigrationTimelineResponse {
        private java.util.List<PhaseInfo> phases = new java.util.ArrayList<>();

        public void addPhase(PhaseInfo phase) {
            this.phases.add(phase);
        }

        public java.util.List<PhaseInfo> getPhases() { return phases; }
    }

    public static class PhaseInfo {
        private final PhasedMigrationStrategy.MigrationPhase phase;
        private final String displayName;
        private final String description;
        private final int plannedDurationDays;
        private final String[] criteria;
        private final boolean current;

        public PhaseInfo(PhasedMigrationStrategy.MigrationPhase phase, String displayName,
                        String description, int plannedDurationDays, String[] criteria, boolean current) {
            this.phase = phase;
            this.displayName = displayName;
            this.description = description;
            this.plannedDurationDays = plannedDurationDays;
            this.criteria = criteria;
            this.current = current;
        }

        // Getters
        public PhasedMigrationStrategy.MigrationPhase getPhase() { return phase; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public int getPlannedDurationDays() { return plannedDurationDays; }
        public String[] getCriteria() { return criteria; }
        public boolean isCurrent() { return current; }
    }
}