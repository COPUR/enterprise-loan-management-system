package com.bank.loanmanagement.loan.application.commands;

import java.math.BigDecimal;
import java.util.Map;

public record RiskAssessmentCommand(
    String loanId,
    String customerId,
    BigDecimal loanAmount,
    Integer termInMonths,
    Map<String, Object> financialData,
    Map<String, Object> collateralData
) {}