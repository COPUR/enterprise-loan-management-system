package com.bank.loanmanagement.security;

import java.time.Instant;
import java.util.Set;

public record FAPITokenValidationResult(
    boolean valid,
    String clientId,
    Set<String> scopes,
    Instant expiresAt,
    String subject,
    String errorDescription
) {}