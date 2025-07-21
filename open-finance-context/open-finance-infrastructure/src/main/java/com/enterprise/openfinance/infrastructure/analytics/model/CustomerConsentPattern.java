package com.enterprise.openfinance.infrastructure.analytics.model;

import com.enterprise.openfinance.domain.model.consent.ConsentScope;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Customer consent behavioral patterns for analytics.
 * Helps understand customer preferences while maintaining privacy.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "customer_patterns")
public class CustomerConsentPattern {
    
    @Id
    private String id;
    
    @Indexed
    private String customerId; // Masked for privacy
    
    private Long totalConsents;
    
    private Map<String, Long> participantConsents; // participant -> count
    
    private List<ConsentScope> recentScopes; // Last 10 scopes
    
    @Indexed
    private Instant firstConsentDate;
    
    @Indexed
    private Instant lastConsentDate;
    
    private Double averageConsentDuration; // in days
    
    private Long totalRevocations;
    
    private String preferredParticipant;
    
    private List<String> frequentScopes;
    
    // Behavioral insights
    private String consentBehaviorPattern; // CONSERVATIVE, MODERATE, LIBERAL
    private Double trustScore; // 0.0 to 1.0
    private String riskProfile; // LOW, MEDIUM, HIGH
    
    public static CustomerConsentPattern empty(String customerId) {
        return CustomerConsentPattern.builder()
            .customerId(customerId)
            .totalConsents(0L)
            .participantConsents(Map.of())
            .recentScopes(List.of())
            .totalRevocations(0L)
            .averageConsentDuration(0.0)
            .trustScore(0.5)
            .riskProfile("UNKNOWN")
            .consentBehaviorPattern("UNKNOWN")
            .build();
    }
}