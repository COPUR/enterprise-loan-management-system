package com.bank.loanmanagement.loan.security;

public record FAPITokenValidationRequest(
    String accessToken,
    String dpopProof,
    String httpMethod,
    String httpUri,
    String clientId
) {}