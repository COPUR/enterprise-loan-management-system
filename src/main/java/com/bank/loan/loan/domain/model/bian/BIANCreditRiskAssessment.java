package com.bank.loanmanagement.loan.domain.model.bian;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BIANCreditRiskAssessment {
    private String assessmentId;
    private String creditScore;
    private String riskGrade;
    private BigDecimal probabilityOfDefault;
    private BigDecimal lossGivenDefault;
    private BigDecimal exposureAtDefault;
    private LocalDateTime assessedAt;
    private String assessorId;
}