package com.banking.loan.domain.loan;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResult(
    String paymentId,
    String transactionReference,
    BigDecimal amount,
    String status,
    LocalDateTime processedAt,
    String message
) {
    public static PaymentResult successful(String paymentId, String transactionRef, BigDecimal amount) {
        return new PaymentResult(
            paymentId,
            transactionRef,
            amount,
            "SUCCESS",
            LocalDateTime.now(),
            "Payment processed successfully"
        );
    }
    
    public static PaymentResult failed(String paymentId, String reason) {
        return new PaymentResult(
            paymentId,
            null,
            BigDecimal.ZERO,
            "FAILED",
            LocalDateTime.now(),
            reason
        );
    }
}