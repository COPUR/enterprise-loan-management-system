package com.banking.loan.application.commands;

import java.util.Map;

public record PerformKYCCommand(
    String customerId,
    String kycType,
    Map<String, Object> documents,
    String initiatedBy
) {}