package com.bank.loanmanagement.security;

import java.time.Duration;

public record TemporaryTokenRequest(
    String operation,
    Duration expiryDuration,
    String scope,
    String clientId,
    String userId
) {}