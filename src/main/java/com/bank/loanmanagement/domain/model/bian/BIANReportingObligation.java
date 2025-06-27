package com.bank.loanmanagement.domain.model.bian;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@Builder
public class BIANReportingObligation {
    private String obligationId;
    private String reportType;
    private String frequency;
    private LocalDateTime nextDueDate;
    private String regulatoryAuthority;
    private String status;
}