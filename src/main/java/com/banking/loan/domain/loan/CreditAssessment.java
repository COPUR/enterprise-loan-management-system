package com.banking.loan.domain.loan;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreditAssessment(
    String score,
    String grade,
    BigDecimal debtToIncomeRatio,
    LocalDateTime assessedAt,
    String assessorId
) {
    public static CreditAssessment pending() {
        return new CreditAssessment("PENDING", "PENDING", BigDecimal.ZERO, LocalDateTime.now(), "SYSTEM");
    }
}