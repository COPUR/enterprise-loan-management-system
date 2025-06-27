package com.banking.loan.infrastructure.messaging;

import java.time.LocalDateTime;
import java.util.Map;

public record BankingMessage(
    String messageId,
    String messageType,
    Map<String, Object> payload,
    LocalDateTime timestamp,
    String correlationId
) {
    public static BankingMessage create(String messageType, Map<String, Object> payload) {
        return new BankingMessage(
            java.util.UUID.randomUUID().toString(),
            messageType,
            payload,
            LocalDateTime.now(),
            java.util.UUID.randomUUID().toString()
        );
    }
}