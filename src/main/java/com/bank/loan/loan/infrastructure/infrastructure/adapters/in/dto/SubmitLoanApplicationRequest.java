package com.bank.loanmanagement.loan.infrastructure.adapters.in.dto;

import java.math.BigDecimal;

public record SubmitLoanApplicationRequest(
    String customerId,
    BigDecimal amount,
    Integer termInMonths,
    String loanType,
    String purpose,
    String collateralDescription,
    BigDecimal monthlyIncome
) {}