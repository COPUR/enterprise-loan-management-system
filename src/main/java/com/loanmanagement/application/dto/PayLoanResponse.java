package com.loanmanagement.application.dto;

import java.math.BigDecimal;

public record PayLoanResponse(
    Integer installmentsPaid,
    BigDecimal totalAmountSpent,
    Boolean isLoanPaidCompletely
) {}