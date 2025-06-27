package com.bank.loanmanagement.security.model;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

/**
 * Temporary Token Data for FAPI security context
 * Follows DDD value object principles
 */
@Data
@Builder
public class TemporaryTokenData {
    private final String tokenId;
    private final String clientId;
    private final String scope;
    private final Instant expiresAt;
    private final String purpose;
    private final String userId;
    private final Instant createdAt;
}