package com.banking.loan.application.commands;

import java.math.BigDecimal;
import java.util.Map;

public record UpdateCustomerCommand(
    String customerId,
    String email,
    String phoneNumber,
    BigDecimal monthlyIncome,
    String address,
    Map<String, Object> additionalData
) {}