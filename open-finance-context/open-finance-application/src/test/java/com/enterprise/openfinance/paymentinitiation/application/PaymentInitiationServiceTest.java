package com.enterprise.openfinance.paymentinitiation.application;

import com.enterprise.openfinance.paymentinitiation.domain.command.SubmitPaymentCommand;
import com.enterprise.openfinance.paymentinitiation.domain.model.IdempotencyRecord;
import com.enterprise.openfinance.paymentinitiation.domain.model.PaymentConsent;
import com.enterprise.openfinance.paymentinitiation.domain.model.PaymentConsentStatus;
import com.enterprise.openfinance.paymentinitiation.domain.model.PaymentInitiation;
import com.enterprise.openfinance.paymentinitiation.domain.model.PaymentResult;
import com.enterprise.openfinance.paymentinitiation.domain.model.PaymentSettings;
import com.enterprise.openfinance.paymentinitiation.domain.model.PaymentStatus;
import com.enterprise.openfinance.paymentinitiation.domain.model.PaymentTransaction;
import com.enterprise.openfinance.paymentinitiation.domain.model.RiskAssessmentDecision;
import com.enterprise.openfinance.paymentinitiation.domain.port.out.FundsReservationPort;
import com.enterprise.openfinance.paymentinitiation.domain.port.out.JwsSignatureValidationPort;
import com.enterprise.openfinance.paymentinitiation.domain.port.out.PaymentConsentPort;
import com.enterprise.openfinance.paymentinitiation.domain.port.out.PaymentEventPort;
import com.enterprise.openfinance.paymentinitiation.domain.port.out.PaymentIdempotencyPort;
import com.enterprise.openfinance.paymentinitiation.domain.port.out.PaymentTransactionPort;
import com.enterprise.openfinance.paymentinitiation.domain.port.out.PayloadHashPort;
import com.enterprise.openfinance.paymentinitiation.domain.port.out.RiskAssessmentPort;
import com.enterprise.openfinance.paymentinitiation.domain.service.PaymentStatusPolicy;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class PaymentInitiationServiceTest {

    private static final Instant NOW = Instant.parse("2026-02-09T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void shouldSubmitImmediatePaymentWhenAllValid() {
        TestConsentPort consentPort = new TestConsentPort(authorizedConsent());
        TestIdempotencyPort idempotencyPort = new TestIdempotencyPort();
        TestTransactionPort transactionPort = new TestTransactionPort();
        TestFundsReservation funds = new TestFundsReservation(true);
        TestRisk risk = new TestRisk(RiskAssessmentDecision.PASS);
        TestEventPublisher events = new TestEventPublisher();

        PaymentInitiationService service = service(
                consentPort,
                idempotencyPort,
                transactionPort,
                funds,
                risk,
                new TestSignatureValidation(true),
                new TestPayloadHash(),
                events
        );

        PaymentResult result = service.submitPayment(validCommand("IDEMP-001", "{\"amount\":\"100.00\"}", null));

        assertThat(result.status()).isEqualTo(PaymentStatus.ACCEPTED_SETTLEMENT_IN_PROCESS);
        assertThat(result.idempotencyReplay()).isFalse();
        assertThat(result.paymentId()).isNotBlank();
        assertThat(transactionPort.saveCount.get()).isEqualTo(1);
        assertThat(funds.reserveCount.get()).isEqualTo(1);
        assertThat(events.publishCount.get()).isEqualTo(1);
    }

    @Test
    void shouldReturnCachedResultForIdempotentReplay() {
        TestConsentPort consentPort = new TestConsentPort(authorizedConsent());
        TestIdempotencyPort idempotencyPort = new TestIdempotencyPort();
        TestTransactionPort transactionPort = new TestTransactionPort();
        TestFundsReservation funds = new TestFundsReservation(true);

        PaymentInitiationService service = service(
                consentPort,
                idempotencyPort,
                transactionPort,
                funds,
                new TestRisk(RiskAssessmentDecision.PASS),
                new TestSignatureValidation(true),
                new TestPayloadHash(),
                new TestEventPublisher()
        );

        PaymentResult first = service.submitPayment(validCommand("IDEMP-001", "{\"amount\":\"100.00\"}", null));
        PaymentResult second = service.submitPayment(validCommand("IDEMP-001", "{\"amount\":\"100.00\"}", null));

        assertThat(first.idempotencyReplay()).isFalse();
        assertThat(second.idempotencyReplay()).isTrue();
        assertThat(second.paymentId()).isEqualTo(first.paymentId());
        assertThat(transactionPort.saveCount.get()).isEqualTo(1);
        assertThat(funds.reserveCount.get()).isEqualTo(1);
    }

    @Test
    void shouldRejectIdempotencyConflictWhenPayloadDiffers() {
        PaymentInitiationService service = service(
                new TestConsentPort(authorizedConsent()),
                new TestIdempotencyPort(),
                new TestTransactionPort(),
                new TestFundsReservation(true),
                new TestRisk(RiskAssessmentDecision.PASS),
                new TestSignatureValidation(true),
                new TestPayloadHash(),
                new TestEventPublisher()
        );

        service.submitPayment(validCommand("IDEMP-001", "{\"amount\":\"100.00\"}", null));

        assertThatThrownBy(() -> service.submitPayment(validCommand("IDEMP-001", "{\"amount\":\"101.00\"}", null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Idempotency conflict");
    }

    @Test
    void shouldRejectInvalidSignature() {
        PaymentInitiationService service = service(
                new TestConsentPort(authorizedConsent()),
                new TestIdempotencyPort(),
                new TestTransactionPort(),
                new TestFundsReservation(true),
                new TestRisk(RiskAssessmentDecision.PASS),
                new TestSignatureValidation(false),
                new TestPayloadHash(),
                new TestEventPublisher()
        );

        assertThatThrownBy(() -> service.submitPayment(validCommand("IDEMP-001", "{\"amount\":\"100.00\"}", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Signature Invalid");
    }

    @Test
    void shouldRejectWhenConsentBindingFails() {
        PaymentInitiationService service = service(
                new TestConsentPort(authorizedConsent()),
                new TestIdempotencyPort(),
                new TestTransactionPort(),
                new TestFundsReservation(true),
                new TestRisk(RiskAssessmentDecision.PASS),
                new TestSignatureValidation(true),
                new TestPayloadHash(),
                new TestEventPublisher()
        );

        SubmitPaymentCommand invalidAmount = new SubmitPaymentCommand(
                "TPP-001",
                "IDEMP-001",
                "CONS-001",
                new PaymentInitiation(
                        "INSTR-001",
                        "E2E-001",
                        "ACC-DEBTOR-001",
                        new BigDecimal("600.00"),
                        "AED",
                        "IBAN",
                        "AE120001000000123456789",
                        "Vendor LLC",
                        null
                ),
                "ix-001",
                "{\"amount\":\"600.00\"}",
                "detached-jws"
        );

        assertThatThrownBy(() -> service.submitPayment(invalidAmount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Consent binding validation failed");
    }

    @Test
    void shouldRejectWhenFundsAreInsufficient() {
        PaymentInitiationService service = service(
                new TestConsentPort(authorizedConsent()),
                new TestIdempotencyPort(),
                new TestTransactionPort(),
                new TestFundsReservation(false),
                new TestRisk(RiskAssessmentDecision.PASS),
                new TestSignatureValidation(true),
                new TestPayloadHash(),
                new TestEventPublisher()
        );

        assertThatThrownBy(() -> service.submitPayment(validCommand("IDEMP-001", "{\"amount\":\"100.00\"}", null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Insufficient funds");
    }

    @Test
    void shouldReturnPendingForFutureDatedPaymentWithoutFundsReservation() {
        TestFundsReservation funds = new TestFundsReservation(true);
        PaymentInitiationService service = service(
                new TestConsentPort(authorizedConsent()),
                new TestIdempotencyPort(),
                new TestTransactionPort(),
                funds,
                new TestRisk(RiskAssessmentDecision.PASS),
                new TestSignatureValidation(true),
                new TestPayloadHash(),
                new TestEventPublisher()
        );

        PaymentResult result = service.submitPayment(validCommand(
                "IDEMP-001",
                "{\"amount\":\"100.00\"}",
                LocalDate.parse("2026-02-10")
        ));

        assertThat(result.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(funds.reserveCount.get()).isZero();
    }

    @Test
    void shouldReturnRejectedWhenRiskEngineRejects() {
        TestFundsReservation funds = new TestFundsReservation(true);
        PaymentInitiationService service = service(
                new TestConsentPort(authorizedConsent()),
                new TestIdempotencyPort(),
                new TestTransactionPort(),
                funds,
                new TestRisk(RiskAssessmentDecision.REJECT),
                new TestSignatureValidation(true),
                new TestPayloadHash(),
                new TestEventPublisher()
        );

        PaymentResult result = service.submitPayment(validCommand("IDEMP-001", "{\"amount\":\"100.00\"}", null));

        assertThat(result.status()).isEqualTo(PaymentStatus.REJECTED);
        assertThat(funds.reserveCount.get()).isZero();
    }

    @Test
    void shouldGetStoredPaymentStatusById() {
        TestTransactionPort transactionPort = new TestTransactionPort();
        PaymentInitiationService service = service(
                new TestConsentPort(authorizedConsent()),
                new TestIdempotencyPort(),
                transactionPort,
                new TestFundsReservation(true),
                new TestRisk(RiskAssessmentDecision.PASS),
                new TestSignatureValidation(true),
                new TestPayloadHash(),
                new TestEventPublisher()
        );

        PaymentResult created = service.submitPayment(validCommand("IDEMP-001", "{\"amount\":\"100.00\"}", null));
        Optional<PaymentTransaction> loaded = service.getPayment(created.paymentId());

        assertThat(loaded).isPresent();
        assertThat(loaded.orElseThrow().paymentId()).isEqualTo(created.paymentId());
    }

    private static PaymentInitiationService service(
            PaymentConsentPort consentPort,
            PaymentIdempotencyPort idempotencyPort,
            PaymentTransactionPort transactionPort,
            FundsReservationPort fundsReservationPort,
            RiskAssessmentPort riskAssessmentPort,
            JwsSignatureValidationPort signatureValidationPort,
            PayloadHashPort payloadHashPort,
            PaymentEventPort paymentEventPort
    ) {
        return new PaymentInitiationService(
                consentPort,
                idempotencyPort,
                transactionPort,
                fundsReservationPort,
                riskAssessmentPort,
                signatureValidationPort,
                payloadHashPort,
                paymentEventPort,
                new PaymentStatusPolicy(),
                new PaymentSettings(Duration.ofHours(24)),
                CLOCK
        );
    }

    private static SubmitPaymentCommand validCommand(String idempotencyKey, String payload, LocalDate executionDate) {
        return new SubmitPaymentCommand(
                "TPP-001",
                idempotencyKey,
                "CONS-001",
                new PaymentInitiation(
                        "INSTR-001",
                        "E2E-001",
                        "ACC-DEBTOR-001",
                        new BigDecimal("100.00"),
                        "AED",
                        "IBAN",
                        "AE120001000000123456789",
                        "Vendor LLC",
                        executionDate
                ),
                "ix-001",
                payload,
                "detached-jws"
        );
    }

    private static PaymentConsent authorizedConsent() {
        return new PaymentConsent(
                "CONS-001",
                PaymentConsentStatus.AUTHORIZED,
                new BigDecimal("500.00"),
                "AED",
                PaymentConsent.hashPayee("AE120001000000123456789"),
                Instant.parse("2026-12-31T00:00:00Z")
        );
    }

    private static final class TestConsentPort implements PaymentConsentPort {
        private final PaymentConsent consent;

        private TestConsentPort(PaymentConsent consent) {
            this.consent = consent;
        }

        @Override
        public Optional<PaymentConsent> findById(String consentId) {
            return Optional.ofNullable(consent != null && consent.consentId().equals(consentId) ? consent : null);
        }
    }

    private static final class TestIdempotencyPort implements PaymentIdempotencyPort {
        private final Map<String, IdempotencyRecord> records = new HashMap<>();

        @Override
        public Optional<IdempotencyRecord> find(String idempotencyKey, String tppId, Instant now) {
            IdempotencyRecord record = records.get(idempotencyKey + ":" + tppId);
            if (record == null || !record.expiresAt().isAfter(now)) {
                records.remove(idempotencyKey + ":" + tppId);
                return Optional.empty();
            }
            return Optional.of(record);
        }

        @Override
        public void save(IdempotencyRecord record) {
            records.put(record.idempotencyKey() + ":" + record.tppId(), record);
        }
    }

    private static final class TestTransactionPort implements PaymentTransactionPort {
        private final Map<String, PaymentTransaction> data = new HashMap<>();
        private final AtomicInteger saveCount = new AtomicInteger();

        @Override
        public PaymentTransaction save(PaymentTransaction transaction) {
            saveCount.incrementAndGet();
            data.put(transaction.paymentId(), transaction);
            return transaction;
        }

        @Override
        public Optional<PaymentTransaction> findByPaymentId(String paymentId) {
            return Optional.ofNullable(data.get(paymentId));
        }
    }

    private static final class TestFundsReservation implements FundsReservationPort {
        private final boolean allowed;
        private final AtomicInteger reserveCount = new AtomicInteger();

        private TestFundsReservation(boolean allowed) {
            this.allowed = allowed;
        }

        @Override
        public boolean reserve(String debtorAccountId, BigDecimal amount, String currency, String reservationReference) {
            reserveCount.incrementAndGet();
            return allowed;
        }
    }

    private static final class TestRisk implements RiskAssessmentPort {
        private final RiskAssessmentDecision decision;

        private TestRisk(RiskAssessmentDecision decision) {
            this.decision = decision;
        }

        @Override
        public RiskAssessmentDecision assess(PaymentInitiation initiation, String tppId) {
            return decision;
        }
    }

    private static final class TestSignatureValidation implements JwsSignatureValidationPort {
        private final boolean valid;

        private TestSignatureValidation(boolean valid) {
            this.valid = valid;
        }

        @Override
        public boolean isValid(String detachedJwsSignature, String payload) {
            return valid;
        }
    }

    private static final class TestPayloadHash implements PayloadHashPort {
        @Override
        public String hash(String payload) {
            return Integer.toHexString(payload.hashCode());
        }
    }

    private static final class TestEventPublisher implements PaymentEventPort {
        private final AtomicInteger publishCount = new AtomicInteger();

        @Override
        public void publishSubmitted(PaymentTransaction transaction) {
            publishCount.incrementAndGet();
        }
    }
}
