package com.enterprise.openfinance.consentauthorization.jwtlifecycle.application;

import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.command.InternalAuthenticateCommand;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.exception.InternalAuthenticationException;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.exception.InternalTokenUnauthorizedException;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalTokenIssueResult;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalTokenPrincipal;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.port.out.InternalCredentialPort;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.port.out.InternalJwtPort;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.port.out.InternalTokenSessionPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalJwtLifecycleServiceTest {

    @Mock
    private InternalCredentialPort credentialPort;
    @Mock
    private InternalJwtPort jwtPort;
    @Mock
    private InternalTokenSessionPort tokenSessionPort;
    @Mock
    private LoginAttemptGuard loginAttemptGuard;

    private InternalJwtLifecycleService service;
    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        service = new InternalJwtLifecycleService(
                credentialPort,
                jwtPort,
                tokenSessionPort,
                loginAttemptGuard,
                clock
        );
    }

    @Test
    void authenticateShouldIssueAndPersistNewTokenSession() {
        Instant now = Instant.now(clock);
        InternalAuthenticateCommand command = new InternalAuthenticateCommand("svc-user", "svc-pass");
        InternalTokenIssueResult issued = new InternalTokenIssueResult(
                "jwt-token",
                "Bearer",
                600,
                "jti-1",
                now,
                now.plusSeconds(600)
        );

        when(credentialPort.matches("svc-user", "svc-pass")).thenReturn(true);
        when(jwtPort.issueToken("svc-user", now)).thenReturn(issued);

        InternalTokenIssueResult result = service.authenticate(command);

        assertThat(result.accessToken()).isEqualTo("jwt-token");
        verify(loginAttemptGuard).checkAllowed("svc-user", now);
        verify(loginAttemptGuard).reset("svc-user");
        verify(tokenSessionPort).deactivateAllForSubject("svc-user", now);
        verify(tokenSessionPort).save(any());

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        verify(jwtPort).issueToken(subjectCaptor.capture(), eq(now));
        assertThat(subjectCaptor.getValue()).isEqualTo("svc-user");
    }

    @Test
    void authenticateShouldRecordFailureAndRejectWhenCredentialsInvalid() {
        Instant now = Instant.now(clock);
        InternalAuthenticateCommand command = new InternalAuthenticateCommand("svc-user", "bad-pass");
        when(credentialPort.matches("svc-user", "bad-pass")).thenReturn(false);

        assertThatThrownBy(() -> service.authenticate(command))
                .isInstanceOf(InternalAuthenticationException.class)
                .hasMessageContaining("Invalid credentials");

        verify(loginAttemptGuard).checkAllowed("svc-user", now);
        verify(loginAttemptGuard).recordFailure("svc-user", now);
        verify(loginAttemptGuard, never()).reset("svc-user");
        verify(tokenSessionPort, never()).save(any());
    }

    @Test
    void validateBearerTokenShouldRejectInactiveToken() {
        Instant now = Instant.now(clock);
        when(jwtPort.verify("raw-token", now))
                .thenReturn(new InternalTokenPrincipal("svc-user", "jti-99", now.minusSeconds(5), now.plusSeconds(300)));
        when(tokenSessionPort.isActive("jti-99", now)).thenReturn(false);

        assertThatThrownBy(() -> service.validateBearerToken("Bearer raw-token"))
                .isInstanceOf(InternalTokenUnauthorizedException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    void logoutShouldDeactivateCurrentToken() {
        Instant now = Instant.now(clock);
        when(jwtPort.verify("raw-token", now))
                .thenReturn(new InternalTokenPrincipal("svc-user", "jti-22", now.minusSeconds(5), now.plusSeconds(300)));
        when(tokenSessionPort.isActive("jti-22", now)).thenReturn(true);

        service.logout("Bearer raw-token");

        verify(tokenSessionPort).deactivate("jti-22", now);
    }

    @Test
    void validateBearerTokenShouldRejectMissingBearerPrefix() {
        assertThatThrownBy(() -> service.validateBearerToken("DPoP token"))
                .isInstanceOf(InternalTokenUnauthorizedException.class)
                .hasMessageContaining("Bearer");
    }
}

