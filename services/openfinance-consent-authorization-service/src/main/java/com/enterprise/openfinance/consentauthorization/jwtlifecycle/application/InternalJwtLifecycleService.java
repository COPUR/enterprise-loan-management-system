package com.enterprise.openfinance.consentauthorization.jwtlifecycle.application;

import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.command.InternalAuthenticateCommand;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.exception.InternalAuthenticationException;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.exception.InternalTokenUnauthorizedException;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalTokenIssueResult;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalTokenPrincipal;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalTokenSession;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.port.in.InternalJwtLifecycleUseCase;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.port.out.InternalCredentialPort;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.port.out.InternalJwtPort;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.port.out.InternalTokenSessionPort;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class InternalJwtLifecycleService implements InternalJwtLifecycleUseCase {

    private final InternalCredentialPort credentialPort;
    private final InternalJwtPort jwtPort;
    private final InternalTokenSessionPort tokenSessionPort;
    private final LoginAttemptGuard loginAttemptGuard;
    private final Clock clock;

    public InternalJwtLifecycleService(
            InternalCredentialPort credentialPort,
            InternalJwtPort jwtPort,
            InternalTokenSessionPort tokenSessionPort,
            LoginAttemptGuard loginAttemptGuard,
            Clock clock
    ) {
        this.credentialPort = credentialPort;
        this.jwtPort = jwtPort;
        this.tokenSessionPort = tokenSessionPort;
        this.loginAttemptGuard = loginAttemptGuard;
        this.clock = clock;
    }

    @Override
    public InternalTokenIssueResult authenticate(InternalAuthenticateCommand command) {
        Instant now = Instant.now(clock);
        loginAttemptGuard.checkAllowed(command.username(), now);

        if (!credentialPort.matches(command.username(), command.password())) {
            loginAttemptGuard.recordFailure(command.username(), now);
            throw new InternalAuthenticationException("Invalid credentials");
        }

        loginAttemptGuard.reset(command.username());
        tokenSessionPort.deactivateAllForSubject(command.username(), now);

        InternalTokenIssueResult token = jwtPort.issueToken(command.username(), now);
        tokenSessionPort.save(InternalTokenSession.active(
                token.jti(),
                command.username(),
                token.issuedAt(),
                token.expiresAt()
        ));
        return token;
    }

    @Override
    public InternalTokenPrincipal validateBearerToken(String authorizationHeader) {
        Instant now = Instant.now(clock);
        String token = extractBearerToken(authorizationHeader);
        InternalTokenPrincipal principal = jwtPort.verify(token, now);
        if (!tokenSessionPort.isActive(principal.jti(), now)) {
            throw new InternalTokenUnauthorizedException("Token is inactive or revoked");
        }
        return principal;
    }

    @Override
    public void logout(String authorizationHeader) {
        InternalTokenPrincipal principal = validateBearerToken(authorizationHeader);
        tokenSessionPort.deactivate(principal.jti(), Instant.now(clock));
    }

    private static String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new InternalTokenUnauthorizedException("Missing Authorization header");
        }

        if (!authorizationHeader.startsWith("Bearer ")) {
            throw new InternalTokenUnauthorizedException("Authorization header must use Bearer token");
        }

        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (token.isBlank()) {
            throw new InternalTokenUnauthorizedException("Bearer token is empty");
        }
        return token;
    }
}

