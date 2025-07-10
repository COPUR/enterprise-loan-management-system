package com.loanmanagement.customer.domain.event;

import java.time.LocalDateTime;

public record CustomerUpdatedEvent(
        Long customerId,
        String email,
        String phone,
        String monthlyIncome,
        String currency,
        String status,
        LocalDateTime occurredAt
) {
    public CustomerUpdatedEvent(Long customerId, String email, String phone,
                              String monthlyIncome, String currency, String status) {
        this(customerId, email, phone, monthlyIncome, currency, status, LocalDateTime.now());
    }
}