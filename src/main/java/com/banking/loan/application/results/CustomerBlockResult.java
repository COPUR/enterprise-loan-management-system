package com.banking.loan.application.results;

import java.time.LocalDateTime;

public record CustomerBlockResult(
    String customerId,
    String blockStatus,
    LocalDateTime blockedAt,
    LocalDateTime blockUntil,
    String reason,
    String message
) {}