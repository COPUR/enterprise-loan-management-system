package com.bank.customer.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for customer response in REST API.
 */
public record CustomerResponse(
    String id,
    String firstName,
    String lastName,
    String fullName,
    String email,
    String phoneNumber,
    BigDecimal creditLimitAmount,
    String creditLimitCurrency,
    BigDecimal usedCreditAmount,
    String usedCreditCurrency,
    BigDecimal availableCreditAmount,
    String availableCreditCurrency,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}