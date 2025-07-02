package com.bank.loanmanagement.loan.domain.model.bian;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@Builder
public class BIANApprovalCondition {
    private String conditionId;
    private String conditionType;
    private String conditionDescription;
    private Boolean mandatory;
    private String status;
    private LocalDateTime dueDate;
    private String fulfillmentStatus;
}