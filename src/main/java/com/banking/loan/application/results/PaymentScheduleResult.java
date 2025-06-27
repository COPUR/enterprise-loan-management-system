package com.banking.loan.application.results;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentScheduleResult(
    String scheduleId,
    String loanId,
    BigDecimal amount,
    LocalDateTime scheduledDate,
    String status,
    String message
) {}