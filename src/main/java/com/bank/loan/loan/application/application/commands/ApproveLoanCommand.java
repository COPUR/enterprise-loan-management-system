package com.bank.loan.loan.application.application.commands;

import java.math.BigDecimal;
import java.util.List;

public record ApproveLoanCommand(
    String loanId,
    String approverId,
    String approvalNotes,
    BigDecimal approvedAmount,
    BigDecimal approvedInterestRate,
    String approvedBy,
    String correlationId,
    List<String> conditions
) {}