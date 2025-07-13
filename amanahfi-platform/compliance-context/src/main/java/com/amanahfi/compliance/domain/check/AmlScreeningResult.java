package com.amanahfi.compliance.domain.check;

import lombok.Value;
import java.util.List;

/**
 * AML Screening Result Value Object
 */
@Value
public class AmlScreeningResult {
    String screeningId;
    RiskScore riskScore;
    String summary;
    List<String> findings;
    
    public boolean hasFindings() {
        return findings != null && !findings.isEmpty();
    }
    
    public boolean isHighRisk() {
        return riskScore == RiskScore.HIGH || riskScore == RiskScore.CRITICAL;
    }
}