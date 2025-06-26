package com.banking.loan.application.results;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record LoanApprovalResult(
    String loanId,
    String status,
    BigDecimal approvedAmount,
    LocalDateTime approvedAt,
    String approverId,
    List<String> conditions,
    String message
) {}