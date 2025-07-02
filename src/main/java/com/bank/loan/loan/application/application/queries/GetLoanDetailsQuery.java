package com.bank.loanmanagement.loan.application.queries;

public record GetLoanDetailsQuery(
    String loanId,
    String customerId,
    String requestedBy,
    boolean includeAIAnalysis,
    boolean includeComplianceData
) {}