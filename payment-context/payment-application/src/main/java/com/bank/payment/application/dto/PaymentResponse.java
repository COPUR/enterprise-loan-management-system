package com.bank.payment.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * DTO representing payment data in responses
 */
public record PaymentResponse(
    String paymentId,
    String customerId,
    String fromAccountId,
    String toAccountId,
    BigDecimal amount,
    BigDecimal fee,
    BigDecimal totalAmount,
    String currency,
    String paymentType,
    String status,
    String description,
    String failureReason,
    LocalDateTime createdAt,
    LocalDateTime completedAt,
    Instant lastModifiedAt
) {
    
    public static PaymentResponse from(com.bank.payment.domain.Payment payment) {
        return new PaymentResponse(
            payment.getId().getValue(),
            payment.getCustomerId().getValue(),
            payment.getFromAccountId().getValue(),
            payment.getToAccountId().getValue(),
            payment.getAmount().getAmount(),
            payment.getFee().getAmount(),
            payment.getTotalAmount().getAmount(),
            payment.getAmount().getCurrency().getCurrencyCode(),
            payment.getPaymentType().name(),
            payment.getStatus().name(),
            payment.getDescription(),
            payment.getFailureReason(),
            payment.getCreatedAt(),
            payment.getCompletedAt(),
            payment.getUpdatedAt().atZone(java.time.ZoneOffset.UTC).toInstant()
        );
    }
}