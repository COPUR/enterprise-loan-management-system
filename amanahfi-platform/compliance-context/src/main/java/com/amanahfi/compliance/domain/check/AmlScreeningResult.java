package com.amanahfi.compliance.domain.check;

import lombok.Builder;
import lombok.Value;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AML Screening Result Value Object
 */
@Value
@Builder
public class AmlScreeningResult {
    String screeningId;
    String entityId;
    String providerId;
    RiskScore riskScore;
    String summary;
    List<String> findings;
    List<String> watchlistMatches;
    List<String> recommendations;
    LocalDateTime screeningDate;
    boolean highRisk;
    
    public boolean hasFindings() {
        return findings != null && !findings.isEmpty();
    }
    
    public boolean isHighRisk() {
        return riskScore == RiskScore.HIGH || riskScore == RiskScore.CRITICAL;
    }
}