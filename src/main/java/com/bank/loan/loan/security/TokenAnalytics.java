package com.bank.loanmanagement.loan.security;

import java.time.Instant;
import java.util.Map;

public record TokenAnalytics(
    String clientId,
    long totalTokensIssued,
    long activeTokens,
    long revokedTokens,
    double averageTokenLifetime,
    Instant lastTokenIssued,
    Map<String, Long> scopeUsage
) {}