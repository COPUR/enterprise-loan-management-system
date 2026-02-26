package com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.rest;

import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalTokenIssueResult;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalTokenPrincipal;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.port.in.InternalJwtLifecycleUseCase;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.rest.dto.InternalAuthenticateRequest;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.rest.dto.InternalBusinessResponse;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.rest.dto.InternalTokenResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

class InternalJwtLifecycleControllerTest {

    @Test
    void authenticateShouldReturnIssuedToken() {
        InternalJwtLifecycleUseCase useCase = Mockito.mock(InternalJwtLifecycleUseCase.class);
        InternalJwtLifecycleController controller = new InternalJwtLifecycleController(useCase);
        InternalTokenIssueResult token = new InternalTokenIssueResult(
                "jwt-token",
                "Bearer",
                600,
                "jti-1",
                Instant.parse("2026-02-25T00:00:00Z"),
                Instant.parse("2026-02-25T00:10:00Z")
        );
        Mockito.when(useCase.authenticate(any())).thenReturn(token);

        ResponseEntity<InternalTokenResponse> response = controller.authenticate(
                new InternalAuthenticateRequest("svc", "pass")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isEqualTo("jwt-token");
    }

    @Test
    void logoutShouldReturnOk() {
        InternalJwtLifecycleUseCase useCase = Mockito.mock(InternalJwtLifecycleUseCase.class);
        InternalJwtLifecycleController controller = new InternalJwtLifecycleController(useCase);

        ResponseEntity<Void> response = controller.logout("Bearer token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Mockito.verify(useCase).logout("Bearer token");
    }

    @Test
    void businessShouldMapAuthenticatedPrincipal() {
        InternalJwtLifecycleUseCase useCase = Mockito.mock(InternalJwtLifecycleUseCase.class);
        InternalJwtLifecycleController controller = new InternalJwtLifecycleController(useCase);
        InternalTokenPrincipal principal = new InternalTokenPrincipal(
                "svc-user",
                "jti-1",
                Instant.parse("2026-02-25T00:00:00Z"),
                Instant.parse("2026-02-25T00:10:00Z")
        );
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of()
        );

        ResponseEntity<InternalBusinessResponse> response = controller.business(authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().subject()).isEqualTo("svc-user");
        assertThat(response.getBody().status()).isEqualTo("AUTHORIZED");
    }
}
