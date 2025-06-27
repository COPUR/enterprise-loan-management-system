package com.banking.loan.domain.loan;

import java.time.LocalDateTime;
import java.util.List;

public record ComplianceCheck(
    String checkId,
    String status,
    List<String> regulations,
    List<String> violations,
    LocalDateTime checkedAt,
    String checkedBy
) {
    public static ComplianceCheck pending() {
        return new ComplianceCheck(
            "PENDING",
            "PENDING",
            List.of(),
            List.of(),
            LocalDateTime.now(),
            "SYSTEM"
        );
    }
}