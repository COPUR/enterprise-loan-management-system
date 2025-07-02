package com.loanmanagement.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanDto(
    Long id,
    Long customerId,
    BigDecimal loanAmount,
    Integer numberOfInstallments,
    LocalDate createDate,
    Boolean isPaid
) {}
