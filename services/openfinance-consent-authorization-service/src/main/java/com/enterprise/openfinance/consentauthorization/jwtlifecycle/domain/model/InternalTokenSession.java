package com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model;

import java.time.Instant;

public final class InternalTokenSession {

    private final String jti;
    private final String subject;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private boolean active;
    private Instant revokedAt;

    private InternalTokenSession(
            String jti,
            String subject,
            Instant issuedAt,
            Instant expiresAt,
            boolean active,
            Instant revokedAt
    ) {
        if (isBlank(jti)) {
            throw new IllegalArgumentException("jti is required");
        }
        if (isBlank(subject)) {
            throw new IllegalArgumentException("subject is required");
        }
        if (issuedAt == null || expiresAt == null) {
            throw new IllegalArgumentException("issuedAt and expiresAt are required");
        }
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("expiresAt must be after issuedAt");
        }
        this.jti = jti;
        this.subject = subject;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.active = active;
        this.revokedAt = revokedAt;
    }

    public static InternalTokenSession active(String jti, String subject, Instant issuedAt, Instant expiresAt) {
        return new InternalTokenSession(jti, subject, issuedAt, expiresAt, true, null);
    }

    public void deactivate(Instant at) {
        this.active = false;
        this.revokedAt = at;
    }

    public boolean isActive(Instant now) {
        return active && now.isBefore(expiresAt);
    }

    public String getJti() {
        return jti;
    }

    public String getSubject() {
        return subject;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isActiveFlag() {
        return active;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

