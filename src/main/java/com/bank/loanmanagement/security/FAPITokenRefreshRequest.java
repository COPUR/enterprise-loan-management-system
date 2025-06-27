package com.bank.loanmanagement.security;

public record FAPITokenRefreshRequest(
    String refreshToken,
    String clientId,
    String dpopProof,
    String scope
) {}