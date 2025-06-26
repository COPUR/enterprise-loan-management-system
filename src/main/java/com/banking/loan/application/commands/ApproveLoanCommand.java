package com.banking.loan.application.commands;

import java.math.BigDecimal;
import java.util.List;

public record ApproveLoanCommand(
    String loanId,
    String approverId,
    String approvalNotes,
    BigDecimal approvedAmount,
    List<String> conditions
) {}