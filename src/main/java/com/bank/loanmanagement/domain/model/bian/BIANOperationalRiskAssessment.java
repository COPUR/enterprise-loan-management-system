package com.bank.loanmanagement.domain.model.bian;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BIANOperationalRiskAssessment {
    private String assessmentId;
    private List<String> riskFactors;
    private BigDecimal riskScore;
    private String riskLevel;
    private List<String> mitigationMeasures;
    private LocalDateTime assessedAt;
    private String assessorId;
}