package com.banking.loan.application.queries;

public record GetLoanDetailsQuery(
    String loanId,
    String customerId,
    String requestedBy,
    boolean includeAIAnalysis,
    boolean includeComplianceData
) {}