package com.bank.loanmanagement.loan.application.queries;

public record GetCustomerDetailsQuery(
    String customerId,
    Boolean includeKycDetails,
    Boolean includeCreditHistory
) {}