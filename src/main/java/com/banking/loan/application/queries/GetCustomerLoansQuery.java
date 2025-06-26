package com.banking.loan.application.queries;

public record GetCustomerLoansQuery(
    String customerId,
    Integer page,
    Integer size,
    String status
) {}