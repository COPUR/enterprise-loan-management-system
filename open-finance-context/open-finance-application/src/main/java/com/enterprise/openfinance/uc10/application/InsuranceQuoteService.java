package com.enterprise.openfinance.uc10.application;

import com.enterprise.openfinance.uc10.domain.command.AcceptMotorQuoteCommand;
import com.enterprise.openfinance.uc10.domain.command.CreateMotorQuoteCommand;
import com.enterprise.openfinance.uc10.domain.exception.BusinessRuleViolationException;
import com.enterprise.openfinance.uc10.domain.exception.ForbiddenException;
import com.enterprise.openfinance.uc10.domain.exception.IdempotencyConflictException;
import com.enterprise.openfinance.uc10.domain.exception.ResourceNotFoundException;
import com.enterprise.openfinance.uc10.domain.model.IssuedPolicy;
import com.enterprise.openfinance.uc10.domain.model.MotorInsuranceQuote;
import com.enterprise.openfinance.uc10.domain.model.MotorQuoteIdempotencyRecord;
import com.enterprise.openfinance.uc10.domain.model.MotorQuoteItemResult;
import com.enterprise.openfinance.uc10.domain.model.MotorQuoteResult;
import com.enterprise.openfinance.uc10.domain.model.QuoteSettings;
import com.enterprise.openfinance.uc10.domain.model.QuoteStatus;
import com.enterprise.openfinance.uc10.domain.port.in.InsuranceQuoteUseCase;
import com.enterprise.openfinance.uc10.domain.port.out.MotorQuoteCachePort;
import com.enterprise.openfinance.uc10.domain.port.out.MotorQuoteEventPort;
import com.enterprise.openfinance.uc10.domain.port.out.MotorQuoteIdempotencyPort;
import com.enterprise.openfinance.uc10.domain.port.out.MotorQuotePort;
import com.enterprise.openfinance.uc10.domain.port.out.PolicyIssuancePort;
import com.enterprise.openfinance.uc10.domain.port.out.QuotePricingPort;
import com.enterprise.openfinance.uc10.domain.query.GetMotorQuoteQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class InsuranceQuoteService implements InsuranceQuoteUseCase {

    private final MotorQuotePort quotePort;
    private final MotorQuoteIdempotencyPort idempotencyPort;
    private final MotorQuoteCachePort cachePort;
    private final QuotePricingPort pricingPort;
    private final PolicyIssuancePort policyIssuancePort;
    private final MotorQuoteEventPort eventPort;
    private final QuoteSettings settings;
    private final Clock clock;

    public InsuranceQuoteService(MotorQuotePort quotePort,
                                 MotorQuoteIdempotencyPort idempotencyPort,
                                 MotorQuoteCachePort cachePort,
                                 QuotePricingPort pricingPort,
                                 PolicyIssuancePort policyIssuancePort,
                                 MotorQuoteEventPort eventPort,
                                 QuoteSettings settings,
                                 Clock clock) {
        this.quotePort = quotePort;
        this.idempotencyPort = idempotencyPort;
        this.cachePort = cachePort;
        this.pricingPort = pricingPort;
        this.policyIssuancePort = policyIssuancePort;
        this.eventPort = eventPort;
        this.settings = settings;
        this.clock = clock;
    }

    @Override
    @Transactional
    public MotorQuoteResult createQuote(CreateMotorQuoteCommand command) {
        Instant now = Instant.now(clock);
        String quoteId = "Q-" + UUID.randomUUID();

        MotorInsuranceQuote quote = new MotorInsuranceQuote(
                quoteId,
                command.tppId(),
                command.vehicleMake(),
                command.vehicleModel(),
                command.vehicleYear(),
                command.driverAge(),
                command.licenseDurationYears(),
                pricingPort.calculatePremium(command),
                settings.currency(),
                QuoteStatus.QUOTED,
                now.plus(settings.quoteTtl()),
                hash(command.riskFingerprint()),
                null,
                null,
                null,
                null,
                now,
                now
        );

        MotorInsuranceQuote saved = quotePort.save(quote);
        eventPort.publishQuoteCreated(saved);
        return new MotorQuoteResult(saved, false);
    }

    @Override
    @Transactional
    public MotorQuoteResult acceptQuote(AcceptMotorQuoteCommand command) {
        Instant now = Instant.now(clock);
        String requestHash = hash(command.idempotencyFingerprint());

        Optional<MotorQuoteIdempotencyRecord> existingRecord = idempotencyPort.find(command.idempotencyKey(), command.tppId(), now);
        if (existingRecord.isPresent()) {
            MotorQuoteIdempotencyRecord record = existingRecord.orElseThrow();
            if (!record.requestHash().equals(requestHash)) {
                throw new IdempotencyConflictException("Idempotency conflict");
            }

            MotorInsuranceQuote replayQuote = quotePort.findById(record.quoteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Quote not found for idempotency record"));
            ensureOwnership(replayQuote, command.tppId());
            return new MotorQuoteResult(replayQuote, true);
        }

        MotorInsuranceQuote quote = quotePort.findById(command.quoteId())
                .orElseThrow(() -> new ResourceNotFoundException("Quote not found"));
        ensureOwnership(quote, command.tppId());

        if (quote.status() == QuoteStatus.ACCEPTED) {
            throw new BusinessRuleViolationException("Quote already finalized");
        }

        if (quote.status() == QuoteStatus.EXPIRED || quote.isExpired(now)) {
            quotePort.save(quote.expire(now));
            throw new BusinessRuleViolationException("Quote expired");
        }

        if (command.hasRiskSnapshot()) {
            String submittedHash = hash(command.riskFingerprint());
            if (!submittedHash.equals(quote.riskHash())) {
                throw new BusinessRuleViolationException("Quote bound to original inputs");
            }
        }

        IssuedPolicy issuedPolicy = policyIssuancePort.issuePolicy(quote, command.paymentReference(), command.interactionId(), now);
        MotorInsuranceQuote accepted = quote.accept(
                issuedPolicy.policyId(),
                issuedPolicy.policyNumber(),
                issuedPolicy.certificateId(),
                command.paymentReference(),
                now
        );
        MotorInsuranceQuote saved = quotePort.save(accepted);

        idempotencyPort.save(new MotorQuoteIdempotencyRecord(
                command.idempotencyKey(),
                command.tppId(),
                requestHash,
                saved.quoteId(),
                saved.policyId(),
                now.plus(settings.idempotencyTtl())
        ));

        eventPort.publishQuoteAccepted(saved);
        eventPort.publishPolicyIssued(saved);

        return new MotorQuoteResult(saved, false);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MotorQuoteItemResult> getQuote(GetMotorQuoteQuery query) {
        Instant now = Instant.now(clock);
        String key = cacheKey(query.quoteId(), query.tppId());

        Optional<MotorQuoteItemResult> cached = cachePort.getQuote(key, now);
        if (cached.isPresent()) {
            return Optional.of(cached.orElseThrow().withCacheHit(true));
        }

        Optional<MotorInsuranceQuote> quoteOptional = quotePort.findById(query.quoteId());
        if (quoteOptional.isEmpty()) {
            return Optional.empty();
        }

        MotorInsuranceQuote quote = quoteOptional.orElseThrow();
        ensureOwnership(quote, query.tppId());

        if (quote.status() == QuoteStatus.QUOTED && quote.isExpired(now)) {
            quote = quotePort.save(quote.expire(now));
        }

        MotorQuoteItemResult result = new MotorQuoteItemResult(quote, false);
        cachePort.putQuote(key, result, now.plus(settings.cacheTtl()));
        return Optional.of(result);
    }

    private static String cacheKey(String quoteId, String tppId) {
        return quoteId + ':' + tppId;
    }

    private static void ensureOwnership(MotorInsuranceQuote quote, String tppId) {
        if (!quote.belongsTo(tppId)) {
            throw new ForbiddenException("Quote not owned by participant");
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
