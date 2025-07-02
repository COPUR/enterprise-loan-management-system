package com.bank.loanmanagement.loan.security;

public record FAPITokenRefreshRequest(
    String refreshToken,
    String clientId,
    String dpopProof,
    String scope
) {}