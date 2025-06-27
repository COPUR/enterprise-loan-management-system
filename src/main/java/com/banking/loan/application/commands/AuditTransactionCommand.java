package com.banking.loan.application.commands;

import java.util.Map;

public record AuditTransactionCommand(
    String transactionId,
    String auditType,
    String auditReason,
    Map<String, Object> auditCriteria,
    String auditedBy
) {}