package com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.port.out;

import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalTokenIssueResult;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalTokenPrincipal;

import java.time.Instant;

public interface InternalJwtPort {

    InternalTokenIssueResult issueToken(String subject, Instant issuedAt);

    InternalTokenPrincipal verify(String token, Instant now);
}

