package com.loanmanagement.payment.domain.event;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentProcessedEvent(
        Long paymentId,
        Long loanId,
        String amount,
        String currency,
        LocalDate paymentDate,
        String status,
        String penaltyAmount,
        String discountAmount,
        LocalDateTime occurredAt
) {
    public PaymentProcessedEvent(Long paymentId, Long loanId, String amount,
                               String currency, LocalDate paymentDate, String status,
                               String penaltyAmount, String discountAmount) {
        this(paymentId, loanId, amount, currency, paymentDate, status,
             penaltyAmount, discountAmount, LocalDateTime.now());
    }
}