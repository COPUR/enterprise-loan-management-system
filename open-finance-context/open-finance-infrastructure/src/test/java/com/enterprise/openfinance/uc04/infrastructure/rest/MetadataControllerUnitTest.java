package com.enterprise.openfinance.uc04.infrastructure.rest;

import com.enterprise.openfinance.uc04.domain.model.AccountSchemeMetadata;
import com.enterprise.openfinance.uc04.domain.model.FxDetails;
import com.enterprise.openfinance.uc04.domain.model.GeoLocation;
import com.enterprise.openfinance.uc04.domain.model.MetadataItemResult;
import com.enterprise.openfinance.uc04.domain.model.MetadataListResult;
import com.enterprise.openfinance.uc04.domain.model.MetadataQueryResult;
import com.enterprise.openfinance.uc04.domain.model.PartyMetadata;
import com.enterprise.openfinance.uc04.domain.model.StandingOrderMetadata;
import com.enterprise.openfinance.uc04.domain.model.TransactionMetadata;
import com.enterprise.openfinance.uc04.domain.port.in.MetadataUseCase;
import com.enterprise.openfinance.uc04.domain.query.GetTransactionMetadataQuery;
import com.enterprise.openfinance.uc04.infrastructure.rest.dto.AccountMetadataResponse;
import com.enterprise.openfinance.uc04.infrastructure.rest.dto.PartiesMetadataResponse;
import com.enterprise.openfinance.uc04.infrastructure.rest.dto.StandingOrdersMetadataResponse;
import com.enterprise.openfinance.uc04.infrastructure.rest.dto.TransactionMetadataResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class MetadataControllerUnitTest {

    @Test
    void shouldReturnTransactionsWithCacheHeaderAndEtag() {
        MetadataUseCase useCase = Mockito.mock(MetadataUseCase.class);
        MetadataController controller = new MetadataController(useCase);
        Mockito.when(useCase.getTransactions(Mockito.any())).thenReturn(new MetadataQueryResult<>(
                List.of(transaction("TXN-001")),
                1,
                100,
                1,
                true
        ));

        ResponseEntity<TransactionMetadataResponse> response = controller.getTransactionMetadata(
                "DPoP token",
                "proof",
                "ix-uc04-1",
                "CONS-META-001",
                "TPP-001",
                "ACC-001",
                null,
                null,
                1,
                100,
                null
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("X-OF-Cache")).isEqualTo("HIT");
        assertThat(response.getHeaders().getETag()).isNotBlank();
        assertThat(response.getBody()).isNotNull();

        ArgumentCaptor<GetTransactionMetadataQuery> queryCaptor = ArgumentCaptor.forClass(GetTransactionMetadataQuery.class);
        Mockito.verify(useCase).getTransactions(queryCaptor.capture());
        assertThat(queryCaptor.getValue().accountId()).isEqualTo("ACC-001");
    }

    @Test
    void shouldReturnNotModifiedWhenEtagMatches() {
        MetadataUseCase useCase = Mockito.mock(MetadataUseCase.class);
        MetadataController controller = new MetadataController(useCase);
        Mockito.when(useCase.getTransactions(Mockito.any())).thenReturn(new MetadataQueryResult<>(
                List.of(transaction("TXN-001")),
                1,
                100,
                1,
                false
        ));

        ResponseEntity<TransactionMetadataResponse> first = controller.getTransactionMetadata(
                "DPoP token",
                "proof",
                "ix-uc04-2",
                "CONS-META-001",
                "TPP-001",
                "ACC-001",
                null,
                null,
                1,
                100,
                null
        );

        String etag = first.getHeaders().getETag();

        ResponseEntity<TransactionMetadataResponse> second = controller.getTransactionMetadata(
                "DPoP token",
                "proof",
                "ix-uc04-2",
                "CONS-META-001",
                "TPP-001",
                "ACC-001",
                null,
                null,
                1,
                100,
                etag
        );

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
    }

    @Test
    void shouldReturnPartiesAccountAndStandingOrders() {
        MetadataUseCase useCase = Mockito.mock(MetadataUseCase.class);
        MetadataController controller = new MetadataController(useCase);

        Mockito.when(useCase.getParties(Mockito.any())).thenReturn(new MetadataListResult<>(List.of(party()), false));
        Mockito.when(useCase.getAccountMetadata(Mockito.any())).thenReturn(new MetadataItemResult<>(
                new AccountSchemeMetadata("ACC-001", "IBAN", "MOB-123"),
                true
        ));
        Mockito.when(useCase.getStandingOrders(Mockito.any())).thenReturn(new MetadataQueryResult<>(
                List.of(standingOrder("SO-001")),
                1,
                100,
                1,
                false
        ));

        ResponseEntity<PartiesMetadataResponse> partiesResponse = controller.getPartiesMetadata(
                "DPoP token",
                "proof",
                "ix-uc04-3",
                "CONS-META-001",
                "TPP-001",
                "ACC-001"
        );
        ResponseEntity<AccountMetadataResponse> accountResponse = controller.getAccountMetadata(
                "DPoP token",
                "proof",
                "ix-uc04-4",
                "CONS-META-001",
                "TPP-001",
                "ACC-001"
        );
        ResponseEntity<StandingOrdersMetadataResponse> standingOrdersResponse = controller.getStandingOrdersMetadata(
                "DPoP token",
                "proof",
                "ix-uc04-5",
                "CONS-META-001",
                "TPP-001",
                "ACC-001",
                1,
                100
        );

        assertThat(partiesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(accountResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(accountResponse.getHeaders().getFirst("X-OF-Cache")).isEqualTo("HIT");
        assertThat(standingOrdersResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldRejectUnsupportedAuthorizationType() {
        MetadataUseCase useCase = Mockito.mock(MetadataUseCase.class);
        MetadataController controller = new MetadataController(useCase);

        assertThatThrownBy(() -> controller.getPartiesMetadata(
                "Basic token",
                "proof",
                "ix-uc04-6",
                "CONS-META-001",
                "TPP-001",
                "ACC-001"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bearer or DPoP");
    }

    private static TransactionMetadata transaction(String id) {
        return new TransactionMetadata(
                id,
                "ACC-001",
                Instant.parse("2026-01-10T00:00:00Z"),
                new BigDecimal("100.00"),
                new BigDecimal("2.00"),
                "AED",
                "Merchant",
                "5411",
                new GeoLocation(25.2048, 55.2708),
                new FxDetails(new BigDecimal("0.27290"), "AED", "USD")
        );
    }

    private static PartyMetadata party() {
        return new PartyMetadata("ACC-001", "Al Tareq Trading LLC", "VERIFIED", Instant.parse("2018-05-01T00:00:00Z"));
    }

    private static StandingOrderMetadata standingOrder(String id) {
        return new StandingOrderMetadata(
                id,
                "ACC-001",
                "EvryMnth",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-12-01T00:00:00Z"),
                new BigDecimal("500.00"),
                "AED"
        );
    }
}
