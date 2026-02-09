package com.enterprise.openfinance.uc10.infrastructure.rest;

import com.enterprise.openfinance.uc10.domain.command.AcceptMotorQuoteCommand;
import com.enterprise.openfinance.uc10.domain.command.CreateMotorQuoteCommand;
import com.enterprise.openfinance.uc10.domain.model.MotorInsuranceQuote;
import com.enterprise.openfinance.uc10.domain.model.MotorQuoteItemResult;
import com.enterprise.openfinance.uc10.domain.model.MotorQuoteResult;
import com.enterprise.openfinance.uc10.domain.model.QuoteStatus;
import com.enterprise.openfinance.uc10.domain.port.in.InsuranceQuoteUseCase;
import com.enterprise.openfinance.uc10.infrastructure.rest.dto.MotorQuoteActionRequest;
import com.enterprise.openfinance.uc10.infrastructure.rest.dto.MotorQuoteRequest;
import com.enterprise.openfinance.uc10.infrastructure.rest.dto.MotorQuoteResponse;
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
class InsuranceQuoteControllerUnitTest {

    @Test
    void shouldCreateAcceptAndGetQuote() {
        InsuranceQuoteUseCase useCase = Mockito.mock(InsuranceQuoteUseCase.class);
        InsuranceQuoteController controller = new InsuranceQuoteController(useCase);

        MotorInsuranceQuote quoted = quote("Q-1", QuoteStatus.QUOTED, null);
        MotorInsuranceQuote accepted = quote("Q-1", QuoteStatus.ACCEPTED, "POL-1");

        Mockito.when(useCase.createQuote(Mockito.any(CreateMotorQuoteCommand.class)))
                .thenReturn(new MotorQuoteResult(quoted, false));
        Mockito.when(useCase.acceptQuote(Mockito.any(AcceptMotorQuoteCommand.class)))
                .thenReturn(new MotorQuoteResult(accepted, false));
        Mockito.when(useCase.getQuote(Mockito.any()))
                .thenReturn(Optional.of(new MotorQuoteItemResult(accepted, false)));

        ResponseEntity<MotorQuoteResponse> createResponse = controller.createQuote(
                "DPoP token",
                "proof",
                "ix-1",
                "TPP-001",
                new MotorQuoteRequest(new MotorQuoteRequest.Data(
                        new MotorQuoteRequest.VehicleDetails("Toyota", "Camry", 2023),
                        new MotorQuoteRequest.DriverDetails(35, 10)
                ))
        );

        ResponseEntity<MotorQuoteResponse> acceptResponse = controller.acceptQuote(
                "DPoP token",
                "proof",
                "ix-1",
                "TPP-001",
                "IDEMP-1",
                "Q-1",
                new MotorQuoteActionRequest(new MotorQuoteActionRequest.Data(
                        "ACCEPT",
                        "PAY-1",
                        new MotorQuoteActionRequest.VehicleDetails("Toyota", "Camry", 2023),
                        new MotorQuoteActionRequest.DriverDetails(35, 10)
                ))
        );

        ResponseEntity<MotorQuoteResponse> getResponse = controller.getQuote(
                "DPoP token",
                "proof",
                "ix-1",
                "TPP-001",
                "Q-1",
                null
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getHeaders().getFirst("X-OF-Idempotency")).isEqualTo("MISS");
        assertThat(acceptResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getHeaders().getFirst("X-OF-Cache")).isEqualTo("MISS");
    }

    @Test
    void shouldReturnNotModifiedWhenEtagMatches() {
        InsuranceQuoteUseCase useCase = Mockito.mock(InsuranceQuoteUseCase.class);
        InsuranceQuoteController controller = new InsuranceQuoteController(useCase);

        MotorInsuranceQuote quote = quote("Q-1", QuoteStatus.QUOTED, null);
        Mockito.when(useCase.getQuote(Mockito.any())).thenReturn(Optional.of(new MotorQuoteItemResult(quote, false)));

        ResponseEntity<MotorQuoteResponse> first = controller.getQuote(
                "DPoP token", "proof", "ix-1", "TPP-001", "Q-1", null
        );

        ResponseEntity<MotorQuoteResponse> second = controller.getQuote(
                "DPoP token", "proof", "ix-1", "TPP-001", "Q-1", first.getHeaders().getETag()
        );

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
    }

    @Test
    void shouldRejectUnsupportedAuthorizationType() {
        InsuranceQuoteUseCase useCase = Mockito.mock(InsuranceQuoteUseCase.class);
        InsuranceQuoteController controller = new InsuranceQuoteController(useCase);

        assertThatThrownBy(() -> controller.getQuote(
                "Basic token", "proof", "ix-1", "TPP-001", "Q-1", null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bearer or DPoP");
    }

    private static MotorInsuranceQuote quote(String quoteId, QuoteStatus status, String policyId) {
        return new MotorInsuranceQuote(
                quoteId,
                "TPP-001",
                "TOYOTA",
                "CAMRY",
                2023,
                35,
                10,
                new BigDecimal("1000.00"),
                "AED",
                status,
                Instant.parse("2026-02-09T10:30:00Z"),
                "risk-hash",
                policyId,
                policyId == null ? null : "POLNO-1",
                policyId == null ? null : "CERT-1",
                policyId == null ? null : "PAY-1",
                Instant.parse("2026-02-09T10:00:00Z"),
                Instant.parse("2026-02-09T10:00:00Z")
        );
    }
}
