package com.amanahfi.compliance.infrastructure.adapter;

import com.amanahfi.compliance.domain.check.AmlScreeningResult;
import com.amanahfi.compliance.domain.check.CheckType;
import com.amanahfi.compliance.domain.check.RiskScore;
import com.amanahfi.compliance.port.out.ExternalAmlProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Adapter for external AML screening providers
 * Integrates with CBUAE-approved AML screening services
 */
@Component
@Slf4j
public class ExternalAmlProviderAdapter implements ExternalAmlProvider {

    private final RestTemplate restTemplate;
    
    @Value("${compliance.aml.provider.url:https://api.aml-provider.com}")
    private String amlProviderUrl;
    
    @Value("${compliance.aml.provider.api-key:demo-key}")
    private String apiKey;
    
    @Value("${compliance.aml.provider.enabled:true}")
    private boolean enabled;

    public ExternalAmlProviderAdapter(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public AmlScreeningResult performScreening(String entityId, CheckType checkType) {
        log.info("Performing AML screening for entity: {} with check type: {}", entityId, checkType);
        
        if (!enabled) {
            return createMockScreeningResult(entityId, false);
        }
        
        try {
            // Create screening request
            Map<String, Object> request = Map.of(
                "entityId", entityId,
                "checkType", checkType.toString(),
                "apiKey", apiKey,
                "timestamp", LocalDateTime.now()
            );
            
            // Call external AML provider (mock implementation)
            Map<String, Object> response = performExternalCall(request);
            
            return mapToAmlScreeningResult(response);
            
        } catch (Exception e) {
            log.error("Error performing AML screening for entity: {}: {}", entityId, e.getMessage());
            return createMockScreeningResult(entityId, true);
        }
    }

    @Override
    public AmlScreeningResult performEnhancedDueDiligence(String entityId, String additionalContext) {
        log.info("Performing enhanced due diligence for entity: {}", entityId);
        
        if (!enabled) {
            return createMockScreeningResult(entityId, false);
        }
        
        try {
            // Enhanced screening with additional context
            Map<String, Object> request = Map.of(
                "entityId", entityId,
                "checkType", "ENHANCED_DUE_DILIGENCE",
                "additionalContext", additionalContext,
                "apiKey", apiKey,
                "timestamp", LocalDateTime.now()
            );
            
            Map<String, Object> response = performExternalCall(request);
            
            return mapToAmlScreeningResult(response);
            
        } catch (Exception e) {
            log.error("Error performing enhanced due diligence for entity: {}: {}", entityId, e.getMessage());
            return createMockScreeningResult(entityId, true);
        }
    }

    @Override
    public boolean isAvailable() {
        if (!enabled) {
            return false;
        }
        
        try {
            // Health check call to external provider
            String healthUrl = amlProviderUrl + "/health";
            restTemplate.getForObject(healthUrl, String.class);
            return true;
        } catch (Exception e) {
            log.warn("AML provider health check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "CBUAE-Approved-AML-Provider";
    }

    // Private helper methods

    private Map<String, Object> performExternalCall(Map<String, Object> request) {
        // In real implementation, this would make REST call to external AML provider
        // For now, simulate the response based on entity characteristics
        
        String entityId = (String) request.get("entityId");
        String checkType = (String) request.get("checkType");
        
        // Simulate risk assessment logic
        int riskScore = calculateMockRiskScore(entityId);
        boolean isHighRisk = riskScore > 75;
        
        return Map.of(
            "entityId", entityId,
            "riskScore", riskScore,
            "riskLevel", isHighRisk ? "HIGH" : "LOW",
            "matches", isHighRisk ? List.of("Watchlist match found") : List.of(),
            "recommendations", isHighRisk ? List.of("Manual review required") : List.of("Approved"),
            "timestamp", LocalDateTime.now(),
            "providerId", "AML-PROVIDER-001"
        );
    }

    private AmlScreeningResult mapToAmlScreeningResult(Map<String, Object> response) {
        Integer riskScore = (Integer) response.get("riskScore");
        String riskLevel = (String) response.get("riskLevel");
        List<String> matches = (List<String>) response.getOrDefault("matches", List.of());
        List<String> recommendations = (List<String>) response.getOrDefault("recommendations", List.of());
        
        return AmlScreeningResult.builder()
            .entityId((String) response.get("entityId"))
            .riskScore(RiskScore.fromValue(riskScore))
            .highRisk("HIGH".equals(riskLevel))
            .watchlistMatches(matches)
            .recommendations(recommendations)
            .screeningDate(LocalDateTime.now())
            .providerId((String) response.get("providerId"))
            .build();
    }

    private AmlScreeningResult createMockScreeningResult(String entityId, boolean hasError) {
        if (hasError) {
            return AmlScreeningResult.builder()
                .entityId(entityId)
                .riskScore(RiskScore.MEDIUM)
                .highRisk(false)
                .watchlistMatches(List.of())
                .recommendations(List.of("Screening failed - manual review required"))
                .screeningDate(LocalDateTime.now())
                .providerId("MOCK-PROVIDER")
                .build();
        }
        
        // Create low-risk mock result
        return AmlScreeningResult.builder()
            .entityId(entityId)
            .riskScore(RiskScore.LOW)
            .highRisk(false)
            .watchlistMatches(List.of())
            .recommendations(List.of("Approved for processing"))
            .screeningDate(LocalDateTime.now())
            .providerId("MOCK-PROVIDER")
            .build();
    }

    private int calculateMockRiskScore(String entityId) {
        // Simple mock risk calculation based on entity ID characteristics
        int score = 30; // Base score
        
        if (entityId.contains("HIGH_RISK")) {
            score += 50;
        }
        
        if (entityId.contains("WATCHLIST")) {
            score += 40;
        }
        
        if (entityId.length() > 20) {
            score += 10; // Longer IDs might indicate complex entities
        }
        
        return Math.min(100, score);
    }
}