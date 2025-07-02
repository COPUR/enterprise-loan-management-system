package com.bank.loanmanagement.loan.domain.model.bian;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BIANUnderwritingAssessment {
    private String assessmentId;
    private String riskGrade;
    private BigDecimal creditScore;
    private String assessmentStatus;
    private LocalDateTime assessmentDate;
    private BigDecimal debtToIncomeRatio;
    private String collateralValuation;
    private String assessorId;
}