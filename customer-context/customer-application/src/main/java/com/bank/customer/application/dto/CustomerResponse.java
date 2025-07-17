package com.bank.customer.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO representing customer data in responses
 */
public record CustomerResponse(
    String customerId,
    String firstName,
    String lastName,
    String email,
    String phoneNumber,
    BigDecimal creditLimit,
    BigDecimal usedCredit,
    BigDecimal availableCredit,
    String status,
    Integer creditScore,
    BigDecimal monthlyIncome,
    Instant createdAt,
    Instant lastModifiedAt
) {
    
    public static CustomerResponse from(com.bank.customer.domain.Customer customer) {
        return new CustomerResponse(
            customer.getId().getValue(),
            customer.getFirstName(),
            customer.getLastName(),
            customer.getEmail(),
            customer.getPhoneNumber(),
            customer.getCreditProfile().getCreditLimit().getAmount(),
            customer.getCreditProfile().getUsedCredit().getAmount(),
            customer.getCreditProfile().getAvailableCredit().getAmount(),
            "ACTIVE", // Default status
            customer.getCreditScore(),
            customer.getMonthlyIncome() != null ? customer.getMonthlyIncome().getAmount() : null,
            customer.getCreatedAt().atZone(java.time.ZoneOffset.UTC).toInstant(),
            customer.getUpdatedAt().atZone(java.time.ZoneOffset.UTC).toInstant()
        );
    }
}