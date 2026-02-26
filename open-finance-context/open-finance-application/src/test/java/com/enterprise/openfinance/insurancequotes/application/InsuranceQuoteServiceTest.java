package com.enterprise.openfinance.insurancequotes.application;

import com.enterprise.openfinance.insurancequotes.domain.command.AcceptMotorQuoteCommand;
import com.enterprise.openfinance.insurancequotes.domain.command.CreateMotorQuoteCommand;
import com.enterprise.openfinance.insurancequotes.domain.exception.BusinessRuleViolationException;
import com.enterprise.openfinance.insurancequotes.domain.exception.ForbiddenException;
import com.enterprise.openfinance.insurancequotes.domain.exception.IdempotencyConflictException;
import com.enterprise.openfinance.insurancequotes.domain.exception.ResourceNotFoundException;
import com.enterprise.openfinance.insurancequotes.domain.model.IssuedPolicy;
import com.enterprise.openfinance.insurancequotes.domain.model.MotorInsuranceQuote;
import com.enterprise.openfinance.insurancequotes.domain.model.MotorQuoteIdempotencyRecord;
import com.enterprise.openfinance.insurancequotes.domain.model.MotorQuoteItemResult;
import com.enterprise.openfinance.insurancequotes.domain.model.MotorQuoteResult;
import com.enterprise.openfinance.insurancequotes.domain.model.QuoteSettings;
import com.enterprise.openfinance.insurancequotes.domain.model.QuoteStatus;
import com.enterprise.openfinance.insurancequotes.domain.port.out.MotorQuoteCachePort;
import com.enterprise.openfinance.insurancequotes.domain.port.out.MotorQuoteIdempotencyPort;
import com.enterprise.openfinance.insurancequotes.domain.port.out.MotorQuotePort;
import com.enterprise.openfinance.insurancequotes.domain.port.out.PolicyIssuancePort;
import com.enterprise.openfinance.insurancequotes.domain.port.out.QuotePricingPort;
import com.enterprise.openfinance.insurancequotes.domain.query.GetMotorQuoteQuery;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class InsuranceQuoteServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-02-09T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void shouldCreateQuoteAndReadWithCache() {
        TestQuotePort quotePort = new TestQuotePort();
        TestCachePort cachePort = new TestCachePort();
        InsuranceQuoteService service = service(quotePort, new TestIdempotencyPort(), cachePort);

        MotorQuoteResult created = service.createQuote(new CreateMotorQuoteCommand(
                "TPP-001", "ix-1", "Toyota", "Camry", 2023, 35, 10
        ));

        Optional<MotorQuoteItemResult> first = service.getQuote(new GetMotorQuoteQuery(created.quote().quoteId(), "TPP-001", "ix-1"));
        Optional<MotorQuoteItemResult> second = service.getQuote(new GetMotorQuoteQuery(created.quote().quoteId(), "TPP-001", "ix-1"));

        assertThat(created.idempotencyReplay()).isFalse();
        assertThat(created.quote().status()).isEqualTo(QuoteStatus.QUOTED);
        assertThat(first).isPresent();
        assertThat(first.orElseThrow().cacheHit()).isFalse();
        assertThat(second.orElseThrow().cacheHit()).isTrue();
    }

    @Test
    void shouldAcceptQuoteWithIdempotencyReplayAndConflict() {
        TestQuotePort quotePort = new TestQuotePort();
        TestIdempotencyPort idempotencyPort = new TestIdempotencyPort();
        InsuranceQuoteService service = service(quotePort, idempotencyPort, new TestCachePort());

        MotorQuoteResult created = service.createQuote(new CreateMotorQuoteCommand(
                "TPP-001", "ix-1", "Toyota", "Camry", 2023, 35, 10
        ));

        AcceptMotorQuoteCommand first = new AcceptMotorQuoteCommand(
                "TPP-001", created.quote().quoteId(), "IDEMP-1", "ix-1", "ACCEPT", "PAY-1",
                "Toyota", "Camry", 2023, 35, 10
        );
        AcceptMotorQuoteCommand replay = new AcceptMotorQuoteCommand(
                "TPP-001", created.quote().quoteId(), "IDEMP-1", "ix-1", "ACCEPT", "PAY-1",
                "Toyota", "Camry", 2023, 35, 10
        );
        AcceptMotorQuoteCommand conflict = new AcceptMotorQuoteCommand(
                "TPP-001", created.quote().quoteId(), "IDEMP-1", "ix-1", "ACCEPT", "PAY-2",
                "Toyota", "Camry", 2023, 35, 10
        );

        MotorQuoteResult accepted = service.acceptQuote(first);
        MotorQuoteResult replayed = service.acceptQuote(replay);

        assertThat(accepted.idempotencyReplay()).isFalse();
        assertThat(accepted.quote().status()).isEqualTo(QuoteStatus.ACCEPTED);
        assertThat(accepted.quote().policyId()).isNotBlank();
        assertThat(replayed.idempotencyReplay()).isTrue();
        assertThat(replayed.quote().policyId()).isEqualTo(accepted.quote().policyId());

        assertThatThrownBy(() -> service.acceptQuote(conflict))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void shouldRejectManipulationExpiryAndOwnershipViolations() {
        TestQuotePort quotePort = new TestQuotePort();
        InsuranceQuoteService service = service(quotePort, new TestIdempotencyPort(), new TestCachePort());

        MotorQuoteResult created = service.createQuote(new CreateMotorQuoteCommand(
                "TPP-001", "ix-1", "Toyota", "Camry", 2023, 35, 10
        ));

        assertThatThrownBy(() -> service.acceptQuote(new AcceptMotorQuoteCommand(
                "TPP-001", created.quote().quoteId(), "IDEMP-2", "ix-1", "ACCEPT", "PAY-1",
                "Nissan", "Sunny", 2022, 40, 15
        ))).isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("bound to original inputs");

        assertThatThrownBy(() -> service.getQuote(new GetMotorQuoteQuery(created.quote().quoteId(), "TPP-OTHER", "ix-1")))
                .isInstanceOf(ForbiddenException.class);

        assertThatThrownBy(() -> service.acceptQuote(new AcceptMotorQuoteCommand(
                "TPP-001", "Q-UNKNOWN", "IDEMP-3", "ix-1", "ACCEPT", "PAY-1",
                null, null, null, null, null
        ))).isInstanceOf(ResourceNotFoundException.class);

        InsuranceQuoteService expiredService = new InsuranceQuoteService(
                quotePort,
                new TestIdempotencyPort(),
                new TestCachePort(),
                command -> new BigDecimal("1000.00"),
                (quote, paymentReference, interactionId, now) -> new IssuedPolicy("POL-1", "POLNO-1", "CERT-1"),
                new NoOpEventPort(),
                new QuoteSettings(Duration.ofMinutes(1), Duration.ofHours(24), Duration.ofSeconds(30), "AED", new BigDecimal("750.00")),
                Clock.fixed(Instant.parse("2026-02-09T11:30:00Z"), ZoneOffset.UTC)
        );

        assertThatThrownBy(() -> expiredService.acceptQuote(new AcceptMotorQuoteCommand(
                "TPP-001", created.quote().quoteId(), "IDEMP-4", "ix-1", "ACCEPT", "PAY-1",
                null, null, null, null, null
        ))).isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("expired");
    }

    private static InsuranceQuoteService service(TestQuotePort quotePort,
                                                 TestIdempotencyPort idempotencyPort,
                                                 TestCachePort cachePort) {
        return new InsuranceQuoteService(
                quotePort,
                idempotencyPort,
                cachePort,
                command -> new BigDecimal("1000.00"),
                (quote, paymentReference, interactionId, now) -> new IssuedPolicy("POL-1", "POLNO-1", "CERT-1"),
                new NoOpEventPort(),
                new QuoteSettings(Duration.ofMinutes(30), Duration.ofHours(24), Duration.ofSeconds(30), "AED", new BigDecimal("750.00")),
                CLOCK
        );
    }

    private static final class TestQuotePort implements MotorQuotePort {
        private final Map<String, MotorInsuranceQuote> data = new ConcurrentHashMap<>();

        @Override
        public MotorInsuranceQuote save(MotorInsuranceQuote quote) {
            data.put(quote.quoteId(), quote);
            return quote;
        }

        @Override
        public Optional<MotorInsuranceQuote> findById(String quoteId) {
            return Optional.ofNullable(data.get(quoteId));
        }
    }

    private static final class TestIdempotencyPort implements MotorQuoteIdempotencyPort {
        private final Map<String, MotorQuoteIdempotencyRecord> data = new ConcurrentHashMap<>();

        @Override
        public Optional<MotorQuoteIdempotencyRecord> find(String key, String tppId, Instant now) {
            MotorQuoteIdempotencyRecord record = data.get(key + ':' + tppId);
            if (record == null || !record.isActiveAt(now)) {
                data.remove(key + ':' + tppId);
                return Optional.empty();
            }
            return Optional.of(record);
        }

        @Override
        public void save(MotorQuoteIdempotencyRecord record) {
            data.put(record.idempotencyKey() + ':' + record.tppId(), record);
        }
    }

    private static final class TestCachePort implements MotorQuoteCachePort {
        private final Map<String, CacheEntry<MotorQuoteItemResult>> data = new ConcurrentHashMap<>();

        @Override
        public Optional<MotorQuoteItemResult> getQuote(String key, Instant now) {
            CacheEntry<MotorQuoteItemResult> entry = data.get(key);
            if (entry == null || !entry.expiresAt().isAfter(now)) {
                data.remove(key);
                return Optional.empty();
            }
            return Optional.of(entry.value().withCacheHit(true));
        }

        @Override
        public void putQuote(String key, MotorQuoteItemResult result, Instant expiresAt) {
            data.put(key, new CacheEntry<>(result.withCacheHit(false), expiresAt));
        }
    }

    private static final class NoOpEventPort implements com.enterprise.openfinance.insurancequotes.domain.port.out.MotorQuoteEventPort {
        @Override
        public void publishQuoteCreated(MotorInsuranceQuote quote) {
        }

        @Override
        public void publishQuoteAccepted(MotorInsuranceQuote quote) {
        }

        @Override
        public void publishPolicyIssued(MotorInsuranceQuote quote) {
        }
    }

    private record CacheEntry<T>(T value, Instant expiresAt) {
    }
}
