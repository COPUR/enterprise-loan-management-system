package com.bank.loanmanagement.domain.model.bian;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BIANBreachManagement {
    private String breachId;
    private String breachType;
    private String severity;
    private LocalDateTime detectedAt;
    private String status;
    private List<String> mitigationActions;
    private String assignedTo;
}