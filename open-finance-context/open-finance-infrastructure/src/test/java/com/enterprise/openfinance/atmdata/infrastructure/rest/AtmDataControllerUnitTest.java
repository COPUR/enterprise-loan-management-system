package com.enterprise.openfinance.atmdata.infrastructure.rest;

import com.enterprise.openfinance.atmdata.domain.model.AtmListResult;
import com.enterprise.openfinance.atmdata.domain.model.AtmLocation;
import com.enterprise.openfinance.atmdata.domain.model.AtmStatus;
import com.enterprise.openfinance.atmdata.domain.port.in.AtmDataUseCase;
import com.enterprise.openfinance.atmdata.infrastructure.rest.dto.AtmResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class AtmDataControllerUnitTest {

    @Test
    void shouldListAtmsWithCacheHeadersAndEtag() {
        AtmDataUseCase useCase = Mockito.mock(AtmDataUseCase.class);
        AtmDataController controller = new AtmDataController(useCase);

        Mockito.when(useCase.listAtms(Mockito.any()))
                .thenReturn(new AtmListResult(List.of(sample()), false));

        ResponseEntity<AtmResponse> response = controller.listAtms(
                "ix-atm-data-1",
                null,
                null,
                null,
                null,
                null
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("X-OF-Cache")).isEqualTo("MISS");
        assertThat(response.getHeaders().getETag()).isNotBlank();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().atm()).hasSize(1);
    }

    @Test
    void shouldReturnNotModifiedWhenEtagMatches() {
        AtmDataUseCase useCase = Mockito.mock(AtmDataUseCase.class);
        AtmDataController controller = new AtmDataController(useCase);

        Mockito.when(useCase.listAtms(Mockito.any()))
                .thenReturn(new AtmListResult(List.of(sample()), false));

        ResponseEntity<AtmResponse> first = controller.listAtms("ix-atm-data-2", null, null, null, null, null);
        ResponseEntity<AtmResponse> second = controller.listAtms(
                "ix-atm-data-2",
                null,
                null,
                null,
                null,
                first.getHeaders().getETag()
        );

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
    }

    @Test
    void shouldIncludeLocationFiltersInSelfLink() {
        AtmDataUseCase useCase = Mockito.mock(AtmDataUseCase.class);
        AtmDataController controller = new AtmDataController(useCase);

        Mockito.when(useCase.listAtms(Mockito.any()))
                .thenReturn(new AtmListResult(List.of(sample()), false));

        ResponseEntity<AtmResponse> response = controller.listAtms(
                "ix-atm-data-2b",
                "Bearer token",
                25.2048,
                55.2708,
                2.0,
                null
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().links().self()).contains("lat=25.2048");
        assertThat(response.getBody().links().self()).contains("long=55.2708");
        assertThat(response.getBody().links().self()).contains("radius=2.0");
    }

    @Test
    void shouldRejectUnsupportedAuthorizationType() {
        AtmDataUseCase useCase = Mockito.mock(AtmDataUseCase.class);
        AtmDataController controller = new AtmDataController(useCase);

        assertThatThrownBy(() -> controller.listAtms("ix-atm-data-3", "Basic invalid", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bearer or DPoP");
    }

    @Test
    void shouldRejectInvalidCoordinates() {
        AtmDataUseCase useCase = Mockito.mock(AtmDataUseCase.class);
        AtmDataController controller = new AtmDataController(useCase);

        assertThatThrownBy(() -> controller.listAtms("ix-atm-data-4", null, 95.0, 55.0, 5.0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("latitude");
    }

    private static AtmLocation sample() {
        return new AtmLocation(
                "ATM-001",
                "Downtown ATM",
                AtmStatus.IN_SERVICE,
                25.2048,
                55.2708,
                "Sheikh Zayed Road",
                "Dubai",
                "AE",
                "Full",
                List.of("CashWithdrawal"),
                Instant.parse("2026-02-10T00:00:00Z")
        );
    }
}
