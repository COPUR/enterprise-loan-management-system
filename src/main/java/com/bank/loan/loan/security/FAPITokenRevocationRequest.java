package com.bank.loanmanagement.loan.security;

public record FAPITokenRevocationRequest(
    String token,
    String tokenTypeHint,
    String clientId,
    String clientSecret
) {}