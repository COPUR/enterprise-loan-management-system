package com.bank.loanmanagement.loan.domain.model.bian;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@Builder
public class BIANDocumentationRequirement {
    private String requirementId;
    private String documentType;
    private String documentDescription;
    private Boolean mandatory;
    private String status;
    private LocalDateTime submissionDate;
    private String verificationStatus;
}