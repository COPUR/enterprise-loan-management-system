package com.amanahfi.onboarding.domain.customer;

import java.time.LocalDateTime;

/**
 * Domain Event: Customer Reactivated
 * Published when suspended customer is reactivated
 */
public record CustomerReactivatedEvent(
    String customerId,
    String reason,
    LocalDateTime occurredAt
) {
    public CustomerReactivatedEvent(String customerId, String reason) {
        this(customerId, reason, LocalDateTime.now());
    }
}