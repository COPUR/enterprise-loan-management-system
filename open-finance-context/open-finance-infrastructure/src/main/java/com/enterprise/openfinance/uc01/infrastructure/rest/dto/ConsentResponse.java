package com.enterprise.openfinance.uc01.infrastructure.rest.dto;

import com.enterprise.openfinance.uc01.domain.model.Consent;
import com.enterprise.openfinance.uc01.domain.model.ConsentStatus;

import java.time.Instant;
import java.util.Set;

public record ConsentResponse(
        String consentId,
        String customerId,
        String participantId,
        Set<String> scopes,
        String purpose,
        ConsentStatus status,
        Instant createdAt,
        Instant expiresAt,
        Instant authorizedAt,
        Instant revokedAt,
        String revocationReason,
        boolean active
) {
    public static ConsentResponse from(Consent consent, Instant now) {
        return new ConsentResponse(
                consent.getConsentId(),
                consent.getCustomerId(),
                consent.getParticipantId(),
                consent.getScopes(),
                consent.getPurpose(),
                consent.getStatus(),
                consent.getCreatedAt(),
                consent.getExpiresAt(),
                consent.getAuthorizedAt(),
                consent.getRevokedAt(),
                consent.getRevocationReason(),
                consent.isActive(now));
    }
}
