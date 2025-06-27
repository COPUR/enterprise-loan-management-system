package com.banking.loan.application.results;

import java.time.LocalDateTime;

public record CustomerCreationResult(
    String customerId,
    String status,
    LocalDateTime createdAt,
    String message
) {}