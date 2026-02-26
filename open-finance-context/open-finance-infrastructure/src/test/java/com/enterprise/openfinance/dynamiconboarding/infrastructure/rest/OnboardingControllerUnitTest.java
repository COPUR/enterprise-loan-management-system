package com.enterprise.openfinance.dynamiconboarding.infrastructure.rest;

import com.enterprise.openfinance.dynamiconboarding.domain.command.CreateOnboardingAccountCommand;
import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingAccount;
import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingAccountItemResult;
import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingAccountResult;
import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingAccountStatus;
import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingApplicantProfile;
import com.enterprise.openfinance.dynamiconboarding.domain.port.in.OnboardingUseCase;
import com.enterprise.openfinance.dynamiconboarding.infrastructure.rest.dto.OnboardingAccountCreateRequest;
import com.enterprise.openfinance.dynamiconboarding.infrastructure.rest.dto.OnboardingAccountResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class OnboardingControllerUnitTest {

    @Test
    void shouldCreateAndGetOnboardingAccount() {
        OnboardingUseCase useCase = Mockito.mock(OnboardingUseCase.class);
        OnboardingController controller = new OnboardingController(useCase);

        OnboardingAccount account = account("ACC-001");
        Mockito.when(useCase.createAccount(Mockito.any(CreateOnboardingAccountCommand.class)))
                .thenReturn(new OnboardingAccountResult(account, false));
        Mockito.when(useCase.getAccount(Mockito.any()))
                .thenReturn(Optional.of(new OnboardingAccountItemResult(account, false)));

        ResponseEntity<OnboardingAccountResponse> createResponse = controller.createAccount(
                "DPoP token",
                "proof",
                "ix-dynamic-onboarding-1",
                "IDEMP-001",
                "TPP-001",
                new OnboardingAccountCreateRequest(
                        new OnboardingAccountCreateRequest.Data("jwe:Alice Ahmed|7841987001|AE", "USD")
                )
        );

        ResponseEntity<OnboardingAccountResponse> getResponse = controller.getAccount(
                "DPoP token",
                "proof",
                "ix-dynamic-onboarding-1",
                "TPP-001",
                "ACC-001",
                null
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getHeaders().getFirst("X-OF-Idempotency")).isEqualTo("MISS");
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldReturnNotModifiedWhenEtagMatches() {
        OnboardingUseCase useCase = Mockito.mock(OnboardingUseCase.class);
        OnboardingController controller = new OnboardingController(useCase);

        OnboardingAccount account = account("ACC-001");
        Mockito.when(useCase.getAccount(Mockito.any()))
                .thenReturn(Optional.of(new OnboardingAccountItemResult(account, false)));

        ResponseEntity<OnboardingAccountResponse> first = controller.getAccount(
                "DPoP token", "proof", "ix-dynamic-onboarding-1", "TPP-001", "ACC-001", null
        );

        ResponseEntity<OnboardingAccountResponse> second = controller.getAccount(
                "DPoP token", "proof", "ix-dynamic-onboarding-1", "TPP-001", "ACC-001", first.getHeaders().getETag()
        );

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
    }

    @Test
    void shouldRejectUnsupportedAuthorizationType() {
        OnboardingUseCase useCase = Mockito.mock(OnboardingUseCase.class);
        OnboardingController controller = new OnboardingController(useCase);

        assertThatThrownBy(() -> controller.createAccount(
                "Basic bad",
                "proof",
                "ix-dynamic-onboarding-1",
                "IDEMP-001",
                "TPP-001",
                new OnboardingAccountCreateRequest(
                        new OnboardingAccountCreateRequest.Data("jwe:Alice Ahmed|7841987001|AE", "USD")
                )
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bearer or DPoP");
    }

    private static OnboardingAccount account(String accountId) {
        return new OnboardingAccount(
                accountId,
                "TPP-001",
                "CIF-001",
                new OnboardingApplicantProfile("Alice Ahmed", "7841987001", "AE"),
                "USD",
                OnboardingAccountStatus.OPENED,
                null,
                Instant.parse("2026-02-09T10:00:00Z"),
                Instant.parse("2026-02-09T10:00:00Z")
        );
    }
}
