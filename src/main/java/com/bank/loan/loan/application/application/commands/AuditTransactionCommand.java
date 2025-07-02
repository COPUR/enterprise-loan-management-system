package com.bank.loan.loan.application.application.commands;

import java.util.Map;

public record AuditTransactionCommand(
    String transactionId,
    String auditType,
    String auditReason,
    Map<String, Object> auditCriteria,
    String auditedBy
) {}