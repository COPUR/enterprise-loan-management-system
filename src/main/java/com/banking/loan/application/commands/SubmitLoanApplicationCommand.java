package com.banking.loan.application.commands;

import java.math.BigDecimal;

public record SubmitLoanApplicationCommand(
    String customerId,
    BigDecimal amount,
    Integer termInMonths,
    String loanType,
    String purpose,
    String collateralDescription,
    BigDecimal monthlyIncome
) {}