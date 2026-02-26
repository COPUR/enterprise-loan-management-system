package com.enterprise.openfinance.payeeverification.infrastructure.rest;

import com.enterprise.openfinance.payeeverification.domain.model.AccountStatus;
import com.enterprise.openfinance.payeeverification.domain.model.ConfirmationRequest;
import com.enterprise.openfinance.payeeverification.domain.model.ConfirmationResult;
import com.enterprise.openfinance.payeeverification.domain.model.NameMatchDecision;
import com.enterprise.openfinance.payeeverification.domain.port.in.ConfirmationOfPayeeUseCase;
import com.enterprise.openfinance.payeeverification.infrastructure.rest.dto.ConfirmationOfPayeeRequest;
import com.enterprise.openfinance.payeeverification.infrastructure.rest.dto.ConfirmationOfPayeeResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class ConfirmationOfPayeeControllerUnitTest {

    @Test
    void shouldReturnOkWithNoStoreAndCacheHeader() {
        ConfirmationOfPayeeUseCase useCase = Mockito.mock(ConfirmationOfPayeeUseCase.class);
        ConfirmationOfPayeeController controller = new ConfirmationOfPayeeController(useCase);
        ConfirmationOfPayeeRequest request = request("GB82WEST12345698765432", "IBAN", "Al Tareq Trading LLC");
        Mockito.when(useCase.confirm(Mockito.any())).thenReturn(
                new ConfirmationResult(AccountStatus.ACTIVE, NameMatchDecision.MATCH, null, 100, false)
        );

        ResponseEntity<ConfirmationOfPayeeResponse> response = controller.confirm(
                "DPoP token",
                "proof",
                "ix-1",
                "TPP-100",
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("Cache-Control")).contains("no-store");
        assertThat(response.getHeaders().getFirst("X-OF-Cache")).isEqualTo("MISS");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().nameMatched()).isEqualTo("Match");

        ArgumentCaptor<ConfirmationRequest> captor = ArgumentCaptor.forClass(ConfirmationRequest.class);
        Mockito.verify(useCase).confirm(captor.capture());
        assertThat(captor.getValue().tppId()).isEqualTo("TPP-100");
    }

    @Test
    void shouldUseUnknownTppWhenFinancialIdMissing() {
        ConfirmationOfPayeeUseCase useCase = Mockito.mock(ConfirmationOfPayeeUseCase.class);
        ConfirmationOfPayeeController controller = new ConfirmationOfPayeeController(useCase);
        Mockito.when(useCase.confirm(Mockito.any())).thenReturn(
                new ConfirmationResult(AccountStatus.ACTIVE, NameMatchDecision.MATCH, null, 100, true)
        );

        controller.confirm("Bearer token", "proof", "ix-2", null, request("GB82WEST12345698765432", "IBAN", "Name"));

        ArgumentCaptor<ConfirmationRequest> captor = ArgumentCaptor.forClass(ConfirmationRequest.class);
        Mockito.verify(useCase).confirm(captor.capture());
        assertThat(captor.getValue().tppId()).isEqualTo("UNKNOWN_TPP");
    }

    @Test
    void shouldRejectUnsupportedAuthorizationType() {
        ConfirmationOfPayeeUseCase useCase = Mockito.mock(ConfirmationOfPayeeUseCase.class);
        ConfirmationOfPayeeController controller = new ConfirmationOfPayeeController(useCase);

        assertThatThrownBy(() -> controller.confirm(
                "Basic abc",
                "proof",
                "ix-3",
                "TPP-1",
                request("GB82WEST12345698765432", "IBAN", "Name")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bearer or DPoP");
    }

    @Test
    void shouldRejectBlankDpopHeader() {
        ConfirmationOfPayeeUseCase useCase = Mockito.mock(ConfirmationOfPayeeUseCase.class);
        ConfirmationOfPayeeController controller = new ConfirmationOfPayeeController(useCase);

        assertThatThrownBy(() -> controller.confirm(
                "DPoP token",
                " ",
                "ix-4",
                "TPP-1",
                request("GB82WEST12345698765432", "IBAN", "Name")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DPoP header is required");
    }

    @Test
    void shouldRejectBlankInteractionId() {
        ConfirmationOfPayeeUseCase useCase = Mockito.mock(ConfirmationOfPayeeUseCase.class);
        ConfirmationOfPayeeController controller = new ConfirmationOfPayeeController(useCase);

        assertThatThrownBy(() -> controller.confirm(
                "DPoP token",
                "proof",
                " ",
                "TPP-1",
                request("GB82WEST12345698765432", "IBAN", "Name")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("X-FAPI-Interaction-ID");
    }

    private static ConfirmationOfPayeeRequest request(String identification, String schemeName, String name) {
        return new ConfirmationOfPayeeRequest(new ConfirmationOfPayeeRequest.Data(identification, schemeName, name));
    }
}
