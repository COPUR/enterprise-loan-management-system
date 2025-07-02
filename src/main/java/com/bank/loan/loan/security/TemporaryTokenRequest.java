package com.bank.loanmanagement.loan.security;

import java.time.Duration;

public record TemporaryTokenRequest(
    String operation,
    Duration expiryDuration,
    String scope,
    String clientId,
    String userId
) {}