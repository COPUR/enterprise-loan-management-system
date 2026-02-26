package com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.rest;

import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalSystemSecretView;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.port.in.InternalSystemSecretUseCase;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.rest.dto.InternalSystemSecretResponse;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.rest.dto.InternalSystemSecretUpsertRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

class InternalSystemSecretControllerTest {

    @Test
    void upsertShouldReturnMaskedMetadata() {
        InternalSystemSecretUseCase useCase = Mockito.mock(InternalSystemSecretUseCase.class);
        InternalSystemSecretController controller = new InternalSystemSecretController(useCase);
        InternalSystemSecretView view = new InternalSystemSecretView(
                "INTERNAL.JWT_HMAC_SECRET",
                "ab****yz",
                "INTERNAL",
                1,
                Instant.parse("2026-02-25T10:00:00Z"),
                Instant.parse("2026-02-25T10:00:00Z")
        );
        Mockito.when(useCase.upsert(any())).thenReturn(view);

        ResponseEntity<InternalSystemSecretResponse> response = controller.upsert(
                new InternalSystemSecretUpsertRequest("internal.jwt_hmac_secret", "super-secret", "internal")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().secretKey()).isEqualTo("INTERNAL.JWT_HMAC_SECRET");
        assertThat(response.getBody().maskedValue()).isEqualTo("ab****yz");
    }

    @Test
    void findShouldReturnNotFoundWhenKeyIsMissing() {
        InternalSystemSecretUseCase useCase = Mockito.mock(InternalSystemSecretUseCase.class);
        InternalSystemSecretController controller = new InternalSystemSecretController(useCase);
        Mockito.when(useCase.getMetadata("MISSING")).thenReturn(Optional.empty());

        ResponseEntity<InternalSystemSecretResponse> response = controller.find("MISSING");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void findShouldReturnMetadataWhenKeyExists() {
        InternalSystemSecretUseCase useCase = Mockito.mock(InternalSystemSecretUseCase.class);
        InternalSystemSecretController controller = new InternalSystemSecretController(useCase);
        InternalSystemSecretView view = new InternalSystemSecretView(
                "PAYMENT.API.KEY",
                "pa****ey",
                "PAYMENT",
                2,
                Instant.parse("2026-02-01T10:00:00Z"),
                Instant.parse("2026-02-25T10:00:00Z")
        );
        Mockito.when(useCase.getMetadata("PAYMENT.API.KEY")).thenReturn(Optional.of(view));

        ResponseEntity<InternalSystemSecretResponse> response = controller.find("PAYMENT.API.KEY");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().classification()).isEqualTo("PAYMENT");
    }
}
