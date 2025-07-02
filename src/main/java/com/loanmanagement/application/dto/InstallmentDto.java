package com.loanmanagement.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InstallmentDto(
    Long id,
    BigDecimal amount,
    BigDecimal paidAmount,
    LocalDate dueDate,
    LocalDate paymentDate,
    Boolean isPaid
) {}
