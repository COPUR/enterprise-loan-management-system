package com.bank.loanmanagement.loan.security;

import com.bank.loanmanagement.loan.security.model.TokenBinding;
import java.time.Instant;
import java.util.Set;

public record StoredTokenData(
    String tokenId,
    String clientId,
    Set<String> scopes,
    Instant issuedAt,
    Instant expiresAt,
    TokenBinding tokenBinding,
    String dpopJwkThumbprint,
    boolean revoked
) {}