package com.enterprise.openfinance.uc11.application;

import com.enterprise.openfinance.uc11.domain.command.CreateFxQuoteCommand;
import com.enterprise.openfinance.uc11.domain.command.ExecuteFxDealCommand;
import com.enterprise.openfinance.uc11.domain.exception.BusinessRuleViolationException;
import com.enterprise.openfinance.uc11.domain.exception.ForbiddenException;
import com.enterprise.openfinance.uc11.domain.exception.IdempotencyConflictException;
import com.enterprise.openfinance.uc11.domain.exception.ResourceNotFoundException;
import com.enterprise.openfinance.uc11.domain.exception.ServiceUnavailableException;
import com.enterprise.openfinance.uc11.domain.model.FxDeal;
import com.enterprise.openfinance.uc11.domain.model.FxDealResult;
import com.enterprise.openfinance.uc11.domain.model.FxDealStatus;
import com.enterprise.openfinance.uc11.domain.model.FxIdempotencyRecord;
import com.enterprise.openfinance.uc11.domain.model.FxQuote;
import com.enterprise.openfinance.uc11.domain.model.FxQuoteItemResult;
import com.enterprise.openfinance.uc11.domain.model.FxQuoteResult;
import com.enterprise.openfinance.uc11.domain.model.FxQuoteStatus;
import com.enterprise.openfinance.uc11.domain.model.FxRateSnapshot;
import com.enterprise.openfinance.uc11.domain.model.FxSettings;
import com.enterprise.openfinance.uc11.domain.port.in.FxUseCase;
import com.enterprise.openfinance.uc11.domain.port.out.FxCachePort;
import com.enterprise.openfinance.uc11.domain.port.out.FxDealPort;
import com.enterprise.openfinance.uc11.domain.port.out.FxEventPort;
import com.enterprise.openfinance.uc11.domain.port.out.FxIdempotencyPort;
import com.enterprise.openfinance.uc11.domain.port.out.FxQuotePort;
import com.enterprise.openfinance.uc11.domain.port.out.FxRatePort;
import com.enterprise.openfinance.uc11.domain.query.GetFxQuoteQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class FxService implements FxUseCase {

    private final FxRatePort ratePort;
    private final FxQuotePort quotePort;
    private final FxDealPort dealPort;
    private final FxIdempotencyPort idempotencyPort;
    private final FxCachePort cachePort;
    private final FxEventPort eventPort;
    private final FxSettings settings;
    private final Clock clock;
    private final Supplier<String> quoteIdGenerator;
    private final Supplier<String> dealIdGenerator;

    @Autowired
    public FxService(FxRatePort ratePort,
                     FxQuotePort quotePort,
                     FxDealPort dealPort,
                     FxIdempotencyPort idempotencyPort,
                     FxCachePort cachePort,
                     FxEventPort eventPort,
                     FxSettings settings,
                     Clock clock) {
        this(
                ratePort,
                quotePort,
                dealPort,
                idempotencyPort,
                cachePort,
                eventPort,
                settings,
                clock,
                () -> "Q-" + UUID.randomUUID(),
                () -> "DEAL-" + UUID.randomUUID()
        );
    }

    FxService(FxRatePort ratePort,
              FxQuotePort quotePort,
              FxDealPort dealPort,
              FxIdempotencyPort idempotencyPort,
              FxCachePort cachePort,
              FxEventPort eventPort,
              FxSettings settings,
              Clock clock,
              Supplier<String> quoteIdGenerator,
              Supplier<String> dealIdGenerator) {
        this.ratePort = ratePort;
        this.quotePort = quotePort;
        this.dealPort = dealPort;
        this.idempotencyPort = idempotencyPort;
        this.cachePort = cachePort;
        this.eventPort = eventPort;
        this.settings = settings;
        this.clock = clock;
        this.quoteIdGenerator = quoteIdGenerator;
        this.dealIdGenerator = dealIdGenerator;
    }

    @Override
    @Transactional
    public FxQuoteResult createQuote(CreateFxQuoteCommand command) {
        Instant now = Instant.now(clock);
        FxRateSnapshot rate = ratePort.findRate(command.pair(), now)
                .orElseThrow(() -> new ServiceUnavailableException("Market closed for pair " + command.pair()));

        BigDecimal normalizedRate = rate.rate().setScale(settings.rateScale(), RoundingMode.HALF_UP);
        BigDecimal targetAmount = command.sourceAmount()
                .multiply(normalizedRate)
                .setScale(2, RoundingMode.HALF_UP);

        FxQuote quote = new FxQuote(
                quoteIdGenerator.get(),
                command.tppId(),
                command.sourceCurrency(),
                command.targetCurrency(),
                command.sourceAmount().setScale(2, RoundingMode.HALF_UP),
                targetAmount,
                normalizedRate,
                FxQuoteStatus.QUOTED,
                now.plus(settings.quoteTtl()),
                now,
                now
        );

        FxQuote saved = quotePort.save(quote);
        eventPort.publishQuoteCreated(saved);
        return new FxQuoteResult(saved);
    }

    @Override
    @Transactional
    public FxDealResult executeDeal(ExecuteFxDealCommand command) {
        Instant now = Instant.now(clock);
        String requestHash = hash(command.requestFingerprint() + '|' + command.interactionId());

        Optional<FxIdempotencyRecord> existing = idempotencyPort.find(command.idempotencyKey(), command.tppId(), now);
        if (existing.isPresent()) {
            FxIdempotencyRecord record = existing.orElseThrow();
            if (!record.requestHash().equals(requestHash)) {
                throw new IdempotencyConflictException("Idempotency conflict");
            }

            FxDeal replayDeal = dealPort.findById(record.dealId())
                    .orElseThrow(() -> new ResourceNotFoundException("Deal not found for idempotency record"));
            ensureOwnership(replayDeal, command.tppId());
            return new FxDealResult(replayDeal, true);
        }

        FxQuote quote = quotePort.findById(command.quoteId())
                .orElseThrow(() -> new ResourceNotFoundException("Quote not found"));
        ensureOwnership(quote, command.tppId());

        if (quote.status() == FxQuoteStatus.BOOKED) {
            throw new BusinessRuleViolationException("Quote already finalized");
        }

        if (quote.status() == FxQuoteStatus.EXPIRED || quote.isExpired(now)) {
            quotePort.save(quote.expire(now));
            throw new BusinessRuleViolationException("Quote Expired");
        }

        FxQuote bookedQuote = quotePort.save(quote.book(now));

        FxDeal deal = new FxDeal(
                dealIdGenerator.get(),
                bookedQuote.quoteId(),
                command.tppId(),
                command.idempotencyKey(),
                bookedQuote.sourceCurrency(),
                bookedQuote.targetCurrency(),
                bookedQuote.sourceAmount(),
                bookedQuote.targetAmount(),
                bookedQuote.exchangeRate(),
                FxDealStatus.BOOKED,
                now
        );

        FxDeal savedDeal = dealPort.save(deal);

        idempotencyPort.save(new FxIdempotencyRecord(
                command.idempotencyKey(),
                command.tppId(),
                requestHash,
                savedDeal.dealId(),
                now.plus(settings.idempotencyTtl())
        ));

        eventPort.publishDealBooked(savedDeal);
        return new FxDealResult(savedDeal, false);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FxQuoteItemResult> getQuote(GetFxQuoteQuery query) {
        Instant now = Instant.now(clock);
        String key = cacheKey(query.quoteId(), query.tppId());

        Optional<FxQuoteItemResult> cached = cachePort.getQuote(key, now);
        if (cached.isPresent()) {
            return Optional.of(cached.orElseThrow().withCacheHit(true));
        }

        Optional<FxQuote> quoteOptional = quotePort.findById(query.quoteId());
        if (quoteOptional.isEmpty()) {
            return Optional.empty();
        }

        FxQuote quote = quoteOptional.orElseThrow();
        ensureOwnership(quote, query.tppId());

        if (quote.status() == FxQuoteStatus.QUOTED && quote.isExpired(now)) {
            quote = quotePort.save(quote.expire(now));
        }

        FxQuoteItemResult result = new FxQuoteItemResult(quote, false);
        cachePort.putQuote(key, result, now.plus(settings.cacheTtl()));
        return Optional.of(result);
    }

    private static String cacheKey(String quoteId, String tppId) {
        return quoteId + ':' + tppId;
    }

    private static void ensureOwnership(FxQuote quote, String tppId) {
        if (!quote.belongsTo(tppId)) {
            throw new ForbiddenException("Quote not owned by participant");
        }
    }

    private static void ensureOwnership(FxDeal deal, String tppId) {
        if (!deal.belongsTo(tppId)) {
            throw new ForbiddenException("Deal not owned by participant");
        }
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to hash payload", exception);
        }
    }
}
