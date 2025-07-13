package com.amanahfi.onboarding.domain.customer;

import java.time.LocalDateTime;

/**
 * Domain Event: Customer Suspended
 * Published when customer account is suspended
 */
public record CustomerSuspendedEvent(
    String customerId,
    String reason,
    LocalDateTime occurredAt
) {
    public CustomerSuspendedEvent(String customerId, String reason) {
        this(customerId, reason, LocalDateTime.now());
    }
}