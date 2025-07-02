package com.bank.loanmanagement.loan.application.queries;

import java.time.LocalDateTime;

public record GetPaymentHistoryQuery(
    String loanId,
    String customerId,
    LocalDateTime fromDate,
    LocalDateTime toDate,
    Integer page,
    Integer size,
    String requestedBy
) {}