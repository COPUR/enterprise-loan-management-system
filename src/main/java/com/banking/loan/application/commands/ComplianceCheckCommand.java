package com.banking.loan.application.commands;

import java.util.Map;

public record ComplianceCheckCommand(
    String entityId,
    String entityType,
    String complianceType,
    Map<String, Object> data,
    String jurisdiction
) {}