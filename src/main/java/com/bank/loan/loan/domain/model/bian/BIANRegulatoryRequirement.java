package com.bank.loanmanagement.loan.domain.model.bian;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@Builder
public class BIANRegulatoryRequirement {
    private String requirementId;
    private String regulationType;
    private String description;
    private String status;
    private LocalDateTime effectiveDate;
    private String jurisdiction;
    private Boolean mandatory;
}