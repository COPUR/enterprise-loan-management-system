package com.bank.loanmanagement.loan.application.results;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record PaymentBreakdown(
    BigDecimal totalLateFees,
    BigDecimal totalInterest,
    BigDecimal totalPrincipal,
    BigDecimal totalEscrow,
    BigDecimal totalOtherFees,
    BigDecimal totalProcessingFees
) {
    public BigDecimal getTotalAllocated() {
        return totalLateFees
            .add(totalInterest)
            .add(totalPrincipal)
            .add(totalEscrow)
            .add(totalOtherFees)
            .add(totalProcessingFees);
    }
}