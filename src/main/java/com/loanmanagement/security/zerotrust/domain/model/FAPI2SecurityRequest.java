package com.loanmanagement.security.zerotrust.domain.model;

import java.time.LocalDateTime;

/**
 * Request for FAPI2 security compliance validation
 */
public record FAPI2SecurityRequest(
    String clientId,
    String scope,
    LocalDateTime timestamp
) {}