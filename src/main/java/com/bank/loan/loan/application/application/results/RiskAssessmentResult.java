package com.bank.loanmanagement.loan.application.results;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record RiskAssessmentResult(
    String assessmentId,
    String loanId,
    Double riskScore,
    String riskGrade,
    BigDecimal recommendedAmount,
    BigDecimal suggestedInterestRate,
    Map<String, Object> riskFactors,
    LocalDateTime assessedAt,
    String recommendation
) {}