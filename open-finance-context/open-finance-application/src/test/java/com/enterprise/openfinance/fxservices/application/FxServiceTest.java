package com.enterprise.openfinance.fxservices.application;

import com.enterprise.openfinance.fxservices.domain.command.CreateFxQuoteCommand;
import com.enterprise.openfinance.fxservices.domain.command.ExecuteFxDealCommand;
import com.enterprise.openfinance.fxservices.domain.exception.BusinessRuleViolationException;
import com.enterprise.openfinance.fxservices.domain.exception.ForbiddenException;
import com.enterprise.openfinance.fxservices.domain.exception.IdempotencyConflictException;
import com.enterprise.openfinance.fxservices.domain.exception.ResourceNotFoundException;
import com.enterprise.openfinance.fxservices.domain.exception.ServiceUnavailableException;
import com.enterprise.openfinance.fxservices.domain.model.FxDeal;
import com.enterprise.openfinance.fxservices.domain.model.FxDealResult;
import com.enterprise.openfinance.fxservices.domain.model.FxDealStatus;
import com.enterprise.openfinance.fxservices.domain.model.FxIdempotencyRecord;
import com.enterprise.openfinance.fxservices.domain.model.FxQuote;
import com.enterprise.openfinance.fxservices.domain.model.FxQuoteItemResult;
import com.enterprise.openfinance.fxservices.domain.model.FxQuoteResult;
import com.enterprise.openfinance.fxservices.domain.model.FxQuoteStatus;
import com.enterprise.openfinance.fxservices.domain.model.FxRateSnapshot;
import com.enterprise.openfinance.fxservices.domain.model.FxSettings;
import com.enterprise.openfinance.fxservices.domain.port.out.FxCachePort;
import com.enterprise.openfinance.fxservices.domain.port.out.FxDealPort;
import com.enterprise.openfinance.fxservices.domain.port.out.FxEventPort;
import com.enterprise.openfinance.fxservices.domain.port.out.FxIdempotencyPort;
import com.enterprise.openfinance.fxservices.domain.port.out.FxQuotePort;
import com.enterprise.openfinance.fxservices.domain.port.out.FxRatePort;
import com.enterprise.openfinance.fxservices.domain.query.GetFxQuoteQuery;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class FxServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-02-09T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void shouldCreateQuoteAndCacheGet() {
        TestQuotePort quotePort = new TestQuotePort();
        TestCachePort cachePort = new TestCachePort();
        FxService service = service(quotePort, new TestDealPort(), new TestIdempotencyPort(), cachePort, new TestRatePort(true));

        FxQuoteResult created = service.createQuote(new CreateFxQuoteCommand("TPP-001", "ix-1", "AED", "USD", new BigDecimal("1000.00")));

        Optional<FxQuoteItemResult> first = service.getQuote(new GetFxQuoteQuery(created.quote().quoteId(), "TPP-001", "ix-1"));
        Optional<FxQuoteItemResult> second = service.getQuote(new GetFxQuoteQuery(created.quote().quoteId(), "TPP-001", "ix-1"));

        assertThat(created.quote().status()).isEqualTo(FxQuoteStatus.QUOTED);
        assertThat(first).isPresent();
        assertThat(first.orElseThrow().cacheHit()).isFalse();
        assertThat(second.orElseThrow().cacheHit()).isTrue();
    }

    @Test
    void shouldExecuteDealWithIdempotentReplayAndConflict() {
        TestQuotePort quotePort = new TestQuotePort();
        TestIdempotencyPort idempotencyPort = new TestIdempotencyPort();
        FxService service = service(quotePort, new TestDealPort(), idempotencyPort, new TestCachePort(), new TestRatePort(true));

        FxQuoteResult created = service.createQuote(new CreateFxQuoteCommand("TPP-001", "ix-1", "AED", "USD", new BigDecimal("1000.00")));

        ExecuteFxDealCommand first = new ExecuteFxDealCommand("TPP-001", created.quote().quoteId(), "IDEMP-1", "ix-1");
        ExecuteFxDealCommand replay = new ExecuteFxDealCommand("TPP-001", created.quote().quoteId(), "IDEMP-1", "ix-1");
        ExecuteFxDealCommand conflict = new ExecuteFxDealCommand("TPP-001", created.quote().quoteId(), "IDEMP-1", "ix-2");

        FxDealResult booked = service.executeDeal(first);
        FxDealResult replayed = service.executeDeal(replay);

        assertThat(booked.idempotencyReplay()).isFalse();
        assertThat(booked.deal().status()).isEqualTo(FxDealStatus.BOOKED);
        assertThat(replayed.idempotencyReplay()).isTrue();
        assertThat(replayed.deal().dealId()).isEqualTo(booked.deal().dealId());

        assertThatThrownBy(() -> service.executeDeal(conflict)).isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void shouldRejectExpiredAndUnauthorizedAndMissingQuotes() {
        TestQuotePort quotePort = new TestQuotePort();
        FxService service = service(quotePort, new TestDealPort(), new TestIdempotencyPort(), new TestCachePort(), new TestRatePort(true));

        FxQuote expired = new FxQuote(
                "Q-EXPIRED",
                "TPP-001",
                "AED",
                "USD",
                new BigDecimal("1000.00"),
                new BigDecimal("272.29"),
                new BigDecimal("0.272290"),
                FxQuoteStatus.QUOTED,
                Instant.parse("2026-02-09T09:00:00Z"),
                Instant.parse("2026-02-09T08:00:00Z"),
                Instant.parse("2026-02-09T08:00:00Z")
        );
        quotePort.save(expired);

        assertThatThrownBy(() -> service.executeDeal(new ExecuteFxDealCommand("TPP-001", "Q-EXPIRED", "IDEMP-2", "ix-1")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Quote Expired");

        FxQuoteResult created = service.createQuote(new CreateFxQuoteCommand("TPP-001", "ix-1", "AED", "USD", new BigDecimal("1000.00")));

        assertThatThrownBy(() -> service.getQuote(new GetFxQuoteQuery(created.quote().quoteId(), "TPP-OTHER", "ix-1")))
                .isInstanceOf(ForbiddenException.class);

        assertThatThrownBy(() -> service.executeDeal(new ExecuteFxDealCommand("TPP-001", "Q-404", "IDEMP-3", "ix-1")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldFailWhenMarketClosed() {
        FxService service = service(new TestQuotePort(), new TestDealPort(), new TestIdempotencyPort(), new TestCachePort(), new TestRatePort(false));

        assertThatThrownBy(() -> service.createQuote(new CreateFxQuoteCommand("TPP-001", "ix-1", "AED", "USD", new BigDecimal("1000.00"))) )
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("Market closed");
    }

    private static FxService service(TestQuotePort quotePort,
                                     TestDealPort dealPort,
                                     TestIdempotencyPort idempotencyPort,
                                     TestCachePort cachePort,
                                     TestRatePort ratePort) {
        return new FxService(
                ratePort,
                quotePort,
                dealPort,
                idempotencyPort,
                cachePort,
                new NoOpEventPort(),
                new FxSettings(Duration.ofSeconds(30), Duration.ofHours(24), Duration.ofSeconds(30), 6),
                CLOCK,
                () -> "Q-" + UUID.randomUUID(),
                () -> "DEAL-" + UUID.randomUUID()
        );
    }

    private static final class TestRatePort implements FxRatePort {
        private final boolean marketOpen;

        private TestRatePort(boolean marketOpen) {
            this.marketOpen = marketOpen;
        }

        @Override
        public Optional<FxRateSnapshot> findRate(String pair, Instant now) {
            if (!marketOpen) {
                return Optional.empty();
            }
            return Optional.of(new FxRateSnapshot(pair, new BigDecimal("0.272290"), now, "STREAM"));
        }
    }

    private static final class TestQuotePort implements FxQuotePort {
        private final Map<String, FxQuote> data = new ConcurrentHashMap<>();

        @Override
        public FxQuote save(FxQuote quote) {
            data.put(quote.quoteId(), quote);
            return quote;
        }

        @Override
        public Optional<FxQuote> findById(String quoteId) {
            return Optional.ofNullable(data.get(quoteId));
        }
    }

    private static final class TestDealPort implements FxDealPort {
        private final Map<String, FxDeal> data = new ConcurrentHashMap<>();

        @Override
        public FxDeal save(FxDeal deal) {
            data.put(deal.dealId(), deal);
            return deal;
        }

        @Override
        public Optional<FxDeal> findById(String dealId) {
            return Optional.ofNullable(data.get(dealId));
        }
    }

    private static final class TestIdempotencyPort implements FxIdempotencyPort {
        private final Map<String, FxIdempotencyRecord> data = new ConcurrentHashMap<>();

        @Override
        public Optional<FxIdempotencyRecord> find(String key, String tppId, Instant now) {
            FxIdempotencyRecord record = data.get(key + ':' + tppId);
            if (record == null || !record.isActiveAt(now)) {
                data.remove(key + ':' + tppId);
                return Optional.empty();
            }
            return Optional.of(record);
        }

        @Override
        public void save(FxIdempotencyRecord record) {
            data.put(record.idempotencyKey() + ':' + record.tppId(), record);
        }
    }

    private static final class TestCachePort implements FxCachePort {
        private final Map<String, Entry<FxQuoteItemResult>> data = new ConcurrentHashMap<>();

        @Override
        public Optional<FxQuoteItemResult> getQuote(String key, Instant now) {
            Entry<FxQuoteItemResult> entry = data.get(key);
            if (entry == null || !entry.expiresAt().isAfter(now)) {
                data.remove(key);
                return Optional.empty();
            }
            return Optional.of(entry.value().withCacheHit(true));
        }

        @Override
        public void putQuote(String key, FxQuoteItemResult result, Instant expiresAt) {
            data.put(key, new Entry<>(result.withCacheHit(false), expiresAt));
        }
    }

    private static final class NoOpEventPort implements FxEventPort {
        @Override
        public void publishQuoteCreated(FxQuote quote) {
        }

        @Override
        public void publishDealBooked(FxDeal deal) {
        }
    }

    private record Entry<T>(T value, Instant expiresAt) {
    }
}
