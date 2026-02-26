package com.enterprise.openfinance.paymentinitiation.infrastructure.rest;

import com.enterprise.openfinance.paymentinitiation.domain.command.SubmitPaymentCommand;
import com.enterprise.openfinance.paymentinitiation.domain.model.PaymentResult;
import com.enterprise.openfinance.paymentinitiation.domain.model.PaymentStatus;
import com.enterprise.openfinance.paymentinitiation.domain.model.PaymentTransaction;
import com.enterprise.openfinance.paymentinitiation.domain.port.in.PaymentInitiationUseCase;
import com.enterprise.openfinance.paymentinitiation.infrastructure.rest.dto.PaymentRequest;
import com.enterprise.openfinance.paymentinitiation.infrastructure.rest.dto.PaymentResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class PaymentInitiationControllerUnitTest {

    @Test
    void shouldReturnCreatedWithHeaders() {
        PaymentInitiationUseCase useCase = Mockito.mock(PaymentInitiationUseCase.class);
        PaymentInitiationController controller = new PaymentInitiationController(useCase);
        Mockito.when(useCase.submitPayment(Mockito.any())).thenReturn(
                new PaymentResult(
                        "PAY-001",
                        "CONS-001",
                        PaymentStatus.ACCEPTED_SETTLEMENT_IN_PROCESS,
                        "ix-001",
                        Instant.parse("2026-02-09T10:00:00Z"),
                        false
                )
        );

        ResponseEntity<PaymentResponse> response = controller.submitPayment(
                "DPoP token",
                "proof",
                "ix-001",
                "IDEMP-001",
                "detached-jws",
                "TPP-001",
                request("100.00", "ACC-DEBTOR-001", "Vendor LLC", null)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getFirst("X-OF-Idempotency")).isEqualTo("MISS");
        assertThat(response.getHeaders().getFirst("X-Idempotency-Key")).isEqualTo("IDEMP-001");
        assertThat(response.getHeaders().getLocation()).hasToString("/open-finance/v1/payments/PAY-001");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().status()).isEqualTo("AcceptedSettlementInProcess");

        ArgumentCaptor<SubmitPaymentCommand> captor = ArgumentCaptor.forClass(SubmitPaymentCommand.class);
        Mockito.verify(useCase).submitPayment(captor.capture());
        assertThat(captor.getValue().tppId()).isEqualTo("TPP-001");
        assertThat(captor.getValue().idempotencyKey()).isEqualTo("IDEMP-001");
    }

    @Test
    void shouldReturnIdempotencyHitHeaderWhenReplay() {
        PaymentInitiationUseCase useCase = Mockito.mock(PaymentInitiationUseCase.class);
        PaymentInitiationController controller = new PaymentInitiationController(useCase);
        Mockito.when(useCase.submitPayment(Mockito.any())).thenReturn(
                new PaymentResult(
                        "PAY-001",
                        "CONS-001",
                        PaymentStatus.ACCEPTED_SETTLEMENT_IN_PROCESS,
                        "ix-001",
                        Instant.parse("2026-02-09T10:00:00Z"),
                        true
                )
        );

        ResponseEntity<PaymentResponse> response = controller.submitPayment(
                "DPoP token",
                "proof",
                "ix-001",
                "IDEMP-001",
                "detached-jws",
                "TPP-001",
                request("100.00", "ACC-DEBTOR-001", "Vendor LLC", null)
        );

        assertThat(response.getHeaders().getFirst("X-OF-Idempotency")).isEqualTo("HIT");
    }

    @Test
    void shouldRejectUnsupportedAuthorizationType() {
        PaymentInitiationUseCase useCase = Mockito.mock(PaymentInitiationUseCase.class);
        PaymentInitiationController controller = new PaymentInitiationController(useCase);

        assertThatThrownBy(() -> controller.submitPayment(
                "Basic abc",
                "proof",
                "ix-001",
                "IDEMP-001",
                "detached-jws",
                "TPP-001",
                request("100.00", "ACC-DEBTOR-001", "Vendor LLC", null)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bearer or DPoP");
    }

    @Test
    void shouldReturnPaymentStatusWhenFound() {
        PaymentInitiationUseCase useCase = Mockito.mock(PaymentInitiationUseCase.class);
        PaymentInitiationController controller = new PaymentInitiationController(useCase);
        Mockito.when(useCase.getPayment("PAY-001")).thenReturn(Optional.of(new PaymentTransaction(
                "PAY-001",
                "CONS-001",
                "TPP-001",
                "IDEMP-001",
                PaymentStatus.ACCEPTED_SETTLEMENT_IN_PROCESS,
                "ACC-DEBTOR-001",
                "AE120001000000123456789",
                new BigDecimal("100.00"),
                "AED",
                null,
                Instant.parse("2026-02-09T10:00:00Z"),
                Instant.parse("2026-02-09T10:00:00Z")
        )));

        ResponseEntity<PaymentResponse> response = controller.getPaymentStatus(
                "DPoP token",
                "proof",
                "ix-001",
                "PAY-001"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().paymentId()).isEqualTo("PAY-001");
    }

    @Test
    void shouldReturnNotFoundWhenPaymentMissing() {
        PaymentInitiationUseCase useCase = Mockito.mock(PaymentInitiationUseCase.class);
        PaymentInitiationController controller = new PaymentInitiationController(useCase);
        Mockito.when(useCase.getPayment("PAY-404")).thenReturn(Optional.empty());

        ResponseEntity<PaymentResponse> response = controller.getPaymentStatus(
                "DPoP token",
                "proof",
                "ix-001",
                "PAY-404"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private static PaymentRequest request(String amount, String debtorAccountId, String creditorName, String executionDate) {
        return new PaymentRequest(
                new PaymentRequest.Data(
                        "CONS-001",
                        new PaymentRequest.Initiation(
                                "INSTR-001",
                                "E2E-001",
                                new PaymentRequest.Amount(amount, "AED"),
                                new PaymentRequest.Account("IBAN", "AE120001000000123456789", creditorName),
                                debtorAccountId,
                                creditorName,
                                executionDate == null ? null : java.time.LocalDate.parse(executionDate)
                        )
                )
        );
    }
}
