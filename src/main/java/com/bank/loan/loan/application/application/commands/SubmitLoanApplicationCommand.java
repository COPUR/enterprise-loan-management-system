package com.bank.loanmanagement.loan.application.commands;

import java.math.BigDecimal;

public record SubmitLoanApplicationCommand(
    String customerId,
    BigDecimal amount,
    Integer termInMonths,
    String loanType,
    String purpose,
    String collateralDescription,
    BigDecimal monthlyIncome,
    String applicantId,
    String correlationId,
    String tenantId
) {}