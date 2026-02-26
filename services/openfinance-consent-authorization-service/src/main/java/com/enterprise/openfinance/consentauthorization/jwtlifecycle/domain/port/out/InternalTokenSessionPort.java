package com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.port.out;

import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalTokenSession;

import java.time.Instant;

public interface InternalTokenSessionPort {

    void deactivateAllForSubject(String subject, Instant revokedAt);

    void save(InternalTokenSession session);

    boolean isActive(String jti, Instant now);

    void deactivate(String jti, Instant revokedAt);
}

