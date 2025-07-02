package com.bank.loanmanagement.loan.domain.model.bian;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BIANMarketRiskAssessment {
    private String assessmentId;
    private BigDecimal interestRateRisk;
    private BigDecimal currencyRisk;
    private BigDecimal liquidityRisk;
    private String riskLevel;
    private LocalDateTime assessedAt;
    private String methodology;
}