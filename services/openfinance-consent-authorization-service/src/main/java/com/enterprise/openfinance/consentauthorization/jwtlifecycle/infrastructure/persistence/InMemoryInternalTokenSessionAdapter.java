package com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.persistence;

import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalTokenSession;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.port.out.InternalTokenSessionPort;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryInternalTokenSessionAdapter implements InternalTokenSessionPort {

    private final Map<String, InternalTokenSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void deactivateAllForSubject(String subject, Instant revokedAt) {
        sessions.values().stream()
                .filter(session -> session.getSubject().equals(subject))
                .forEach(session -> session.deactivate(revokedAt));
    }

    @Override
    public void save(InternalTokenSession session) {
        sessions.put(session.getJti(), session);
    }

    @Override
    public boolean isActive(String jti, Instant now) {
        InternalTokenSession session = sessions.get(jti);
        return session != null && session.isActive(now);
    }

    @Override
    public void deactivate(String jti, Instant revokedAt) {
        InternalTokenSession session = sessions.get(jti);
        if (session != null) {
            session.deactivate(revokedAt);
        }
    }
}

