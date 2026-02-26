package com.enterprise.openfinance.consentauthorization.jwtlifecycle.application;

import java.time.Instant;

public interface LoginAttemptGuard {

    void checkAllowed(String username, Instant now);

    void recordFailure(String username, Instant now);

    void reset(String username);
}

