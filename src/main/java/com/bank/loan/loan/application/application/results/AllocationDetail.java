package com.bank.loanmanagement.loan.application.results;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AllocationDetail(
    String allocationId,
    Integer installmentNumber,
    String allocationType,
    BigDecimal amount,
    LocalDateTime appliedAt,
    String description,
    BigDecimal installmentBalanceBefore,
    BigDecimal installmentBalanceAfter
) {}