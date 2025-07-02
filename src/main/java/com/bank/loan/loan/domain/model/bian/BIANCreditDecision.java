package com.bank.loanmanagement.loan.domain.model.bian;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BIANCreditDecision {
    private String decisionId;
    private String decisionStatus;
    private BigDecimal approvedAmount;
    private BigDecimal interestRate;
    private Integer termInMonths;
    private LocalDateTime decisionDate;
    private String decisionReason;
    private String deciderId;
}