package com.banking.loan.application.commands;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SchedulePaymentCommand(
    String loanId,
    BigDecimal amount,
    LocalDateTime scheduledDate,
    String paymentMethod,
    Boolean recurring,
    String customerId
) {}