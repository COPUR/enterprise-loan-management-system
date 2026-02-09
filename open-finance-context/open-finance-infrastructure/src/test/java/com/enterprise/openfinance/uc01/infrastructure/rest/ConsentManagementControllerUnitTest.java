package com.enterprise.openfinance.uc01.infrastructure.rest;

import com.enterprise.openfinance.uc01.domain.command.CreateConsentCommand;
import com.enterprise.openfinance.uc01.domain.model.Consent;
import com.enterprise.openfinance.uc01.domain.port.in.ConsentManagementUseCase;
import com.enterprise.openfinance.uc01.infrastructure.rest.dto.ConsentResponse;
import com.enterprise.openfinance.uc01.infrastructure.rest.dto.CreateConsentRequest;
import com.enterprise.openfinance.uc01.infrastructure.rest.dto.RevokeConsentRequest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class ConsentManagementControllerUnitTest {

    private static final Instant NOW = Instant.parse("2026-02-09T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private static final String AUTHORIZATION = "DPoP test-token";
    private static final String DPOP = "dpop-proof";
    private static final String INTERACTION_ID = "interaction-id";

    @Test
    void shouldCreateConsentAndReturnCreatedResponse() {
        ConsentManagementUseCase useCase = Mockito.mock(ConsentManagementUseCase.class);
        ConsentManagementController controller = new ConsentManagementController(useCase, CLOCK);
        CreateConsentRequest request = new CreateConsentRequest(
                "CUST-1",
                "TPP-1",
                Set.of(" read_accounts ", "READ-BALANCES", " "),
                "PFM",
                NOW.plusSeconds(3600)
        );
        Consent created = sampleConsent("CUST-1", "TPP-1", Set.of("ReadAccounts", "ReadBalances"), NOW.plusSeconds(3600));
        Mockito.when(useCase.createConsent(Mockito.any(CreateConsentCommand.class))).thenReturn(created);

        ResponseEntity<ConsentResponse> response = controller.createConsent(AUTHORIZATION, DPOP, INTERACTION_ID, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).hasToString("/open-finance/v1/consents/" + created.getConsentId());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().consentId()).isEqualTo(created.getConsentId());

        ArgumentCaptor<CreateConsentCommand> commandCaptor = ArgumentCaptor.forClass(CreateConsentCommand.class);
        Mockito.verify(useCase).createConsent(commandCaptor.capture());
        assertThat(commandCaptor.getValue().scopes()).containsExactlyInAnyOrder("READ_ACCOUNTS", "READ-BALANCES");
    }

    @Test
    void shouldReturnNotFoundWhenConsentDoesNotExist() {
        ConsentManagementUseCase useCase = Mockito.mock(ConsentManagementUseCase.class);
        ConsentManagementController controller = new ConsentManagementController(useCase, CLOCK);
        Mockito.when(useCase.getConsent("CONSENT-MISSING")).thenReturn(Optional.empty());

        ResponseEntity<ConsentResponse> response = controller.getConsent(AUTHORIZATION, DPOP, INTERACTION_ID, "CONSENT-MISSING");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturnConsentListForCustomer() {
        ConsentManagementUseCase useCase = Mockito.mock(ConsentManagementUseCase.class);
        ConsentManagementController controller = new ConsentManagementController(useCase, CLOCK);
        Consent consent = sampleConsent("CUST-2", "TPP-2", Set.of("ReadAccounts"), NOW.plusSeconds(3600));
        Mockito.when(useCase.listConsentsByCustomer("CUST-2")).thenReturn(List.of(consent));

        ResponseEntity<List<ConsentResponse>> response = controller.listConsentsByCustomer(AUTHORIZATION, DPOP, INTERACTION_ID, "CUST-2");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).customerId()).isEqualTo("CUST-2");
    }

    @Test
    void shouldAuthorizeConsentWhenFound() {
        ConsentManagementUseCase useCase = Mockito.mock(ConsentManagementUseCase.class);
        ConsentManagementController controller = new ConsentManagementController(useCase, CLOCK);
        Consent consent = sampleConsent("CUST-3", "TPP-3", Set.of("ReadAccounts"), NOW.plusSeconds(3600));
        consent.authorize(NOW.plusSeconds(10));
        Mockito.when(useCase.authorizeConsent(consent.getConsentId())).thenReturn(Optional.of(consent));

        ResponseEntity<ConsentResponse> response = controller.authorizeConsent(
                AUTHORIZATION,
                DPOP,
                INTERACTION_ID,
                consent.getConsentId()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status().name()).isEqualTo("AUTHORIZED");
    }

    @Test
    void shouldReturnNotFoundWhenAuthorizingMissingConsent() {
        ConsentManagementUseCase useCase = Mockito.mock(ConsentManagementUseCase.class);
        ConsentManagementController controller = new ConsentManagementController(useCase, CLOCK);
        Mockito.when(useCase.authorizeConsent("CONSENT-MISSING")).thenReturn(Optional.empty());

        ResponseEntity<ConsentResponse> response = controller.authorizeConsent(AUTHORIZATION, DPOP, INTERACTION_ID, "CONSENT-MISSING");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldRevokeConsentWhenFound() {
        ConsentManagementUseCase useCase = Mockito.mock(ConsentManagementUseCase.class);
        ConsentManagementController controller = new ConsentManagementController(useCase, CLOCK);
        Consent consent = sampleConsent("CUST-4", "TPP-4", Set.of("ReadAccounts"), NOW.plusSeconds(3600));
        consent.revoke("Customer request", NOW.plusSeconds(20));
        Mockito.when(useCase.revokeConsent(consent.getConsentId(), "Customer request")).thenReturn(Optional.of(consent));

        ResponseEntity<ConsentResponse> response = controller.revokeConsent(
                AUTHORIZATION,
                DPOP,
                INTERACTION_ID,
                consent.getConsentId(),
                new RevokeConsentRequest("Customer request")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status().name()).isEqualTo("REVOKED");
    }

    @Test
    void shouldReturnNotFoundWhenRevokingMissingConsent() {
        ConsentManagementUseCase useCase = Mockito.mock(ConsentManagementUseCase.class);
        ConsentManagementController controller = new ConsentManagementController(useCase, CLOCK);
        Mockito.when(useCase.revokeConsent("CONSENT-MISSING", "reason")).thenReturn(Optional.empty());

        ResponseEntity<ConsentResponse> response = controller.revokeConsent(
                AUTHORIZATION,
                DPOP,
                INTERACTION_ID,
                "CONSENT-MISSING",
                new RevokeConsentRequest("reason")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldRejectAuthorizationHeaderThatIsNotDpop() {
        ConsentManagementUseCase useCase = Mockito.mock(ConsentManagementUseCase.class);
        ConsentManagementController controller = new ConsentManagementController(useCase, CLOCK);
        CreateConsentRequest request = new CreateConsentRequest(
                "CUST-5",
                "TPP-5",
                Set.of("ReadAccounts"),
                "PFM",
                NOW.plusSeconds(3600)
        );

        assertThatThrownBy(() -> controller.createConsent("Bearer token", DPOP, INTERACTION_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DPoP token type");
    }

    @Test
    void shouldRejectBlankDpopHeader() {
        ConsentManagementUseCase useCase = Mockito.mock(ConsentManagementUseCase.class);
        ConsentManagementController controller = new ConsentManagementController(useCase, CLOCK);

        assertThatThrownBy(() -> controller.getConsent(AUTHORIZATION, " ", INTERACTION_ID, "CONSENT-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DPoP header is required");
    }

    @Test
    void shouldRejectBlankInteractionIdHeader() {
        ConsentManagementUseCase useCase = Mockito.mock(ConsentManagementUseCase.class);
        ConsentManagementController controller = new ConsentManagementController(useCase, CLOCK);

        assertThatThrownBy(() -> controller.listConsentsByCustomer(AUTHORIZATION, DPOP, "", "CUST-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("X-FAPI-Interaction-ID header is required");
    }

    private static Consent sampleConsent(String customerId, String participantId, Set<String> scopes, Instant expiresAt) {
        return Consent.create(
                new CreateConsentCommand(customerId, participantId, scopes, "PFM", expiresAt),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }
}
