package com.bank.loanmanagement.loan.application.commands;

import java.time.LocalDateTime;

public record BlockCustomerCommand(
    String customerId,
    String blockReason,
    LocalDateTime blockUntil,
    String blockedBy
) {}