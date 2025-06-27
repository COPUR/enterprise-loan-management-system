package com.bank.loanmanagement.security;

public record FAPITokenRevocationRequest(
    String token,
    String tokenTypeHint,
    String clientId,
    String clientSecret
) {}