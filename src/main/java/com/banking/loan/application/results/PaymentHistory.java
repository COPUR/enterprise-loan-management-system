package com.banking.loan.application.results;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PaymentHistory(
    String loanId,
    List<PaymentRecord> payments,
    BigDecimal totalPaid,
    BigDecimal remainingBalance,
    Integer totalPayments
) {
    public record PaymentRecord(
        String paymentId,
        BigDecimal amount,
        LocalDateTime paymentDate,
        String status,
        String method,
        String reference
    ) {}
}