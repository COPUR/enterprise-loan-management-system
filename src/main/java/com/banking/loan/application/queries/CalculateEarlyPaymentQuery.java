package com.banking.loan.application.queries;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CalculateEarlyPaymentQuery(
    String loanId,
    BigDecimal paymentAmount,
    LocalDateTime paymentDate,
    String calculationMethod,
    LocalDate calculationDate,
    String requestedBy
) {}