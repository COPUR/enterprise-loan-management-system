package com.bank.loanmanagement.loan.application.commands;

import java.math.BigDecimal;
import java.util.Map;

public record FraudDetectionCommand(
    String transactionId,
    String customerId,
    BigDecimal amount,
    String transactionType,
    String location,
    Map<String, Object> deviceData,
    Map<String, Object> behavioralData
) {}