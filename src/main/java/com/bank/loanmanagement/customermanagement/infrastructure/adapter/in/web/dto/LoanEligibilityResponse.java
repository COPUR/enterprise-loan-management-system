package com.bank.loanmanagement.customermanagement.infrastructure.adapter.in.web.dto;

/**
 * DTO for loan eligibility check response.
 */
public record LoanEligibilityResponse(
    boolean eligible,
    String requestedAmount
) {}