package com.enterprise.openfinance.consentauthorization.infrastructure.rest;

import com.enterprise.openfinance.consentauthorization.domain.command.AuthorizeWithPkceCommand;
import com.enterprise.openfinance.consentauthorization.domain.command.ExchangeAuthorizationCodeCommand;
import com.enterprise.openfinance.consentauthorization.domain.model.AuthorizationRedirect;
import com.enterprise.openfinance.consentauthorization.domain.model.TokenResult;
import com.enterprise.openfinance.consentauthorization.domain.port.in.PkceAuthorizationUseCase;
import com.enterprise.openfinance.consentauthorization.infrastructure.rest.dto.OAuthTokenResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class PkceAuthorizationControllerUnitTest {

    @Test
    void shouldReturnRedirectForAuthorize() {
        PkceAuthorizationUseCase useCase = Mockito.mock(PkceAuthorizationUseCase.class);
        PkceAuthorizationController controller = new PkceAuthorizationController(useCase);
        Mockito.when(useCase.authorize(Mockito.any(AuthorizeWithPkceCommand.class)))
                .thenReturn(new AuthorizationRedirect(
                        "AUTH_CODE_1",
                        "state-1",
                        "https://tpp.example/callback?code=AUTH_CODE_1&state=state-1",
                        Instant.parse("2026-02-11T10:05:00Z")
                ));

        ResponseEntity<Void> response = controller.authorize(
                "code",
                "client-app",
                "https://tpp.example/callback",
                "ReadAccounts",
                "state-1",
                "CONSENT-1",
                "challenge",
                "S256"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation()).hasToString("https://tpp.example/callback?code=AUTH_CODE_1&state=state-1");
    }

    @Test
    void shouldReturnTokenResponse() {
        PkceAuthorizationUseCase useCase = Mockito.mock(PkceAuthorizationUseCase.class);
        PkceAuthorizationController controller = new PkceAuthorizationController(useCase);
        Mockito.when(useCase.exchange(Mockito.any(ExchangeAuthorizationCodeCommand.class)))
                .thenReturn(new TokenResult(
                        "access-token",
                        "Bearer",
                        900,
                        "refresh-token",
                        "ReadAccounts"
                ));

        ResponseEntity<OAuthTokenResponse> response = controller.token(
                "authorization_code",
                "AUTH_CODE_1",
                "verifier",
                "client-app",
                "https://tpp.example/callback"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isEqualTo("access-token");
    }
}

