package com.loanmanagement.customer.domain.event;

import com.loanmanagement.shared.domain.model.Money;

import java.time.LocalDateTime;

/**
 * Domain event published when credit is reserved for a customer
 * This event enables other bounded contexts to react to credit reservations
 */
public record CreditReservedEvent(
        Long customerId,
        Money amount,
        LocalDateTime occurredAt
) {
    public CreditReservedEvent(Long customerId, Money amount) {
        this(customerId, amount, LocalDateTime.now());
    }
}