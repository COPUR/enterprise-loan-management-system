package com.bank.loanmanagement.loan.application.queries;

public record GetCustomerLoansQuery(
    String customerId,
    Integer page,
    Integer size,
    String status,
    String requestedBy
) {}