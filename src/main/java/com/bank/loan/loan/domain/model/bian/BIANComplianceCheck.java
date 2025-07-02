package com.bank.loanmanagement.loan.domain.model.bian;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BIANComplianceCheck {
    private String checkId;
    private String checkType;
    private String status;
    private LocalDateTime executedAt;
    private List<String> violations;
    private String result;
    private String checkedBy;
}