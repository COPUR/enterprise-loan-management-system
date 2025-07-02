package com.bank.loanmanagement.loan.application.results;

import java.time.LocalDateTime;
import java.util.List;

public record CustomerUpdateResult(
    String customerId,
    List<String> updatedFields,
    LocalDateTime updatedAt,
    String message
) {}