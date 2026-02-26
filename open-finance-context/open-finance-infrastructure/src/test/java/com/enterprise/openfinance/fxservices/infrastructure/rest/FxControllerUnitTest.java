package com.enterprise.openfinance.fxservices.infrastructure.rest;

import com.enterprise.openfinance.fxservices.domain.command.CreateFxQuoteCommand;
import com.enterprise.openfinance.fxservices.domain.command.ExecuteFxDealCommand;
import com.enterprise.openfinance.fxservices.domain.model.FxDeal;
import com.enterprise.openfinance.fxservices.domain.model.FxDealResult;
import com.enterprise.openfinance.fxservices.domain.model.FxDealStatus;
import com.enterprise.openfinance.fxservices.domain.model.FxQuote;
import com.enterprise.openfinance.fxservices.domain.model.FxQuoteItemResult;
import com.enterprise.openfinance.fxservices.domain.model.FxQuoteResult;
import com.enterprise.openfinance.fxservices.domain.model.FxQuoteStatus;
import com.enterprise.openfinance.fxservices.domain.port.in.FxUseCase;
import com.enterprise.openfinance.fxservices.infrastructure.rest.dto.FxDealRequest;
import com.enterprise.openfinance.fxservices.infrastructure.rest.dto.FxDealResponse;
import com.enterprise.openfinance.fxservices.infrastructure.rest.dto.FxQuoteRequest;
import com.enterprise.openfinance.fxservices.infrastructure.rest.dto.FxQuoteResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class FxControllerUnitTest {

    @Test
    void shouldCreateExecuteAndGetQuote() {
        FxUseCase useCase = Mockito.mock(FxUseCase.class);
        FxController controller = new FxController(useCase);

        FxQuote quote = quote("Q-1", FxQuoteStatus.QUOTED);
        FxDeal deal = deal("DEAL-1", quote.quoteId());

        Mockito.when(useCase.createQuote(Mockito.any(CreateFxQuoteCommand.class))).thenReturn(new FxQuoteResult(quote));
        Mockito.when(useCase.executeDeal(Mockito.any(ExecuteFxDealCommand.class))).thenReturn(new FxDealResult(deal, false));
        Mockito.when(useCase.getQuote(Mockito.any())).thenReturn(Optional.of(new FxQuoteItemResult(quote, false)));

        ResponseEntity<FxQuoteResponse> quoteResponse = controller.createQuote(
                "DPoP token",
                "proof",
                "ix-1",
                "TPP-1",
                new FxQuoteRequest(new FxQuoteRequest.Data("AED", "USD", new BigDecimal("1000.00")))
        );

        ResponseEntity<FxDealResponse> dealResponse = controller.executeDeal(
                "DPoP token",
                "proof",
                "ix-1",
                "TPP-1",
                "IDEMP-1",
                new FxDealRequest(new FxDealRequest.Data("Q-1"))
        );

        ResponseEntity<FxQuoteResponse> getResponse = controller.getQuote(
                "DPoP token",
                "proof",
                "ix-1",
                "TPP-1",
                "Q-1",
                null
        );

        assertThat(quoteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(dealResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(dealResponse.getHeaders().getFirst("X-OF-Idempotency")).isEqualTo("MISS");
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldReturnNotModifiedWhenEtagMatches() {
        FxUseCase useCase = Mockito.mock(FxUseCase.class);
        FxController controller = new FxController(useCase);

        FxQuote quote = quote("Q-1", FxQuoteStatus.QUOTED);
        Mockito.when(useCase.getQuote(Mockito.any())).thenReturn(Optional.of(new FxQuoteItemResult(quote, false)));

        ResponseEntity<FxQuoteResponse> first = controller.getQuote(
                "DPoP token", "proof", "ix-1", "TPP-1", "Q-1", null
        );

        ResponseEntity<FxQuoteResponse> second = controller.getQuote(
                "DPoP token", "proof", "ix-1", "TPP-1", "Q-1", first.getHeaders().getETag()
        );

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
    }

    @Test
    void shouldRejectUnsupportedAuthorizationType() {
        FxUseCase useCase = Mockito.mock(FxUseCase.class);
        FxController controller = new FxController(useCase);

        assertThatThrownBy(() -> controller.createQuote(
                "Basic bad",
                "proof",
                "ix-1",
                "TPP-1",
                new FxQuoteRequest(new FxQuoteRequest.Data("AED", "USD", new BigDecimal("1000.00")))
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bearer or DPoP");
    }

    private static FxQuote quote(String id, FxQuoteStatus status) {
        return new FxQuote(
                id,
                "TPP-1",
                "AED",
                "USD",
                new BigDecimal("1000.00"),
                new BigDecimal("272.29"),
                new BigDecimal("0.272290"),
                status,
                Instant.parse("2026-02-09T10:30:00Z"),
                Instant.parse("2026-02-09T10:00:00Z"),
                Instant.parse("2026-02-09T10:00:00Z")
        );
    }

    private static FxDeal deal(String id, String quoteId) {
        return new FxDeal(
                id,
                quoteId,
                "TPP-1",
                "IDEMP-1",
                "AED",
                "USD",
                new BigDecimal("1000.00"),
                new BigDecimal("272.29"),
                new BigDecimal("0.272290"),
                FxDealStatus.BOOKED,
                Instant.parse("2026-02-09T10:00:00Z")
        );
    }
}
