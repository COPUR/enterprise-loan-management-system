package com.banking.loan.application.queries;

public record GetCustomerDetailsQuery(
    String customerId,
    Boolean includeKycDetails,
    Boolean includeCreditHistory
) {}