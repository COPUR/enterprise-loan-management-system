package com.bank.loanmanagement.loan.security;

import java.time.Instant;

public record TemporaryTokenResponse(
    String temporaryToken,
    String tokenId,
    Long expiresIn,
    String operation,
    String scope
) {}