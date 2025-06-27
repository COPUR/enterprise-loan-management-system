package com.banking.loan.infrastructure.adapters.in.dto;

import java.math.BigDecimal;
import java.util.List;

public record ApproveLoanRequest(
    String approverId,
    String approvalNotes,
    BigDecimal approvedAmount,
    BigDecimal approvedInterestRate,
    List<String> conditions
) {}