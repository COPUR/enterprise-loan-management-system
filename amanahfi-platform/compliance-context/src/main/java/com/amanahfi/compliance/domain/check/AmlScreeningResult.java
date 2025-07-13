package com.amanahfi.compliance.domain.check;

import lombok.Builder;
import lombok.Value;
import java.util.List;

/**
 * AML Screening Result Value Object
 */
@Value
@Builder
public class AmlScreeningResult {
    String screeningId;
    String entityId;
    RiskScore riskScore;
    String summary;
    List<String> findings;
    List<String> watchlistMatches;
    boolean highRisk;
    
    public boolean hasFindings() {
        return findings != null && !findings.isEmpty();
    }
    
    public boolean isHighRisk() {
        return riskScore == RiskScore.HIGH || riskScore == RiskScore.CRITICAL;
    }
}