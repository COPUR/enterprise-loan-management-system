package com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.security;

import com.enterprise.openfinance.consentauthorization.jwtlifecycle.application.LoginAttemptGuard;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.exception.InternalAuthenticationException;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.config.InternalSecurityProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryLoginAttemptGuard implements LoginAttemptGuard {

    private final InternalSecurityProperties properties;
    private final ConcurrentHashMap<String, AttemptState> attempts = new ConcurrentHashMap<>();

    public InMemoryLoginAttemptGuard(InternalSecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public void checkAllowed(String username, Instant now) {
        AttemptState state = attempts.get(username);
        if (state == null) {
            return;
        }
        if (state.lockedUntil() != null && now.isBefore(state.lockedUntil())) {
            throw new InternalAuthenticationException("Authentication temporarily locked", true);
        }
    }

    @Override
    public void recordFailure(String username, Instant now) {
        attempts.compute(username, (key, current) -> {
            if (current == null || current.lastFailureAt().plus(properties.getFailedAttemptWindow()).isBefore(now)) {
                return new AttemptState(1, now, null);
            }

            int count = current.count() + 1;
            Instant lockUntil = null;
            if (count >= properties.getMaxFailedAttempts()) {
                lockUntil = now.plus(properties.getLockDuration());
            }
            return new AttemptState(count, now, lockUntil);
        });
    }

    @Override
    public void reset(String username) {
        attempts.remove(username);
    }

    private record AttemptState(int count, Instant lastFailureAt, Instant lockedUntil) {
    }
}

