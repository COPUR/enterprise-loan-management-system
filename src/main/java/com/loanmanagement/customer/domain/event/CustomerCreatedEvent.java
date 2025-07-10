package com.loanmanagement.customer.domain.event;

import java.time.LocalDateTime;

public record CustomerCreatedEvent(
        Long customerId,
        String email,
        String firstName,
        String lastName,
        String monthlyIncome,
        String currency,
        LocalDateTime occurredAt
) {
    public CustomerCreatedEvent(Long customerId, String email, String firstName, 
                              String lastName, String monthlyIncome, String currency) {
        this(customerId, email, firstName, lastName, monthlyIncome, currency, LocalDateTime.now());
    }
}