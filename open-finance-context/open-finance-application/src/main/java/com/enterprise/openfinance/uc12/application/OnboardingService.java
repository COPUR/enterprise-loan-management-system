package com.enterprise.openfinance.uc12.application;

import com.enterprise.openfinance.uc12.domain.command.CreateOnboardingAccountCommand;
import com.enterprise.openfinance.uc12.domain.exception.ComplianceViolationException;
import com.enterprise.openfinance.uc12.domain.exception.DecryptionFailedException;
import com.enterprise.openfinance.uc12.domain.exception.ForbiddenException;
import com.enterprise.openfinance.uc12.domain.exception.IdempotencyConflictException;
import com.enterprise.openfinance.uc12.domain.exception.ResourceNotFoundException;
import com.enterprise.openfinance.uc12.domain.model.OnboardingAccount;
import com.enterprise.openfinance.uc12.domain.model.OnboardingAccountItemResult;
import com.enterprise.openfinance.uc12.domain.model.OnboardingAccountResult;
import com.enterprise.openfinance.uc12.domain.model.OnboardingAccountStatus;
import com.enterprise.openfinance.uc12.domain.model.OnboardingApplicantProfile;
import com.enterprise.openfinance.uc12.domain.model.OnboardingIdempotencyRecord;
import com.enterprise.openfinance.uc12.domain.model.OnboardingSettings;
import com.enterprise.openfinance.uc12.domain.port.in.OnboardingUseCase;
import com.enterprise.openfinance.uc12.domain.port.out.KycDecryptionPort;
import com.enterprise.openfinance.uc12.domain.port.out.OnboardingAccountPort;
import com.enterprise.openfinance.uc12.domain.port.out.OnboardingCachePort;
import com.enterprise.openfinance.uc12.domain.port.out.OnboardingEventPort;
import com.enterprise.openfinance.uc12.domain.port.out.OnboardingIdempotencyPort;
import com.enterprise.openfinance.uc12.domain.port.out.SanctionsScreeningPort;
import com.enterprise.openfinance.uc12.domain.query.GetOnboardingAccountQuery;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.function.Supplier;

@Service
public class OnboardingService implements OnboardingUseCase {

    private final KycDecryptionPort kycDecryptionPort;
    private final SanctionsScreeningPort sanctionsScreeningPort;
    private final OnboardingAccountPort accountPort;
    private final OnboardingIdempotencyPort idempotencyPort;
    private final OnboardingCachePort cachePort;
    private final OnboardingEventPort eventPort;
    private final OnboardingSettings settings;
    private final Clock clock;
    private final Supplier<String> accountIdGenerator;
    private final Supplier<String> customerIdGenerator;

    @Autowired
    public OnboardingService(KycDecryptionPort kycDecryptionPort,
                             SanctionsScreeningPort sanctionsScreeningPort,
                             OnboardingAccountPort accountPort,
                             OnboardingIdempotencyPort idempotencyPort,
                             OnboardingCachePort cachePort,
                             OnboardingEventPort eventPort,
                             OnboardingSettings settings,
                             Clock clock) {
        this(
                kycDecryptionPort,
                sanctionsScreeningPort,
                accountPort,
                idempotencyPort,
                cachePort,
                eventPort,
                settings,
                clock,
                () -> settings.accountPrefix() + '-' + UUID.randomUUID(),
                () -> "CIF-" + UUID.randomUUID()
        );
    }

    OnboardingService(KycDecryptionPort kycDecryptionPort,
                      SanctionsScreeningPort sanctionsScreeningPort,
                      OnboardingAccountPort accountPort,
                      OnboardingIdempotencyPort idempotencyPort,
                      OnboardingCachePort cachePort,
                      OnboardingEventPort eventPort,
                      OnboardingSettings settings,
                      Clock clock,
                      Supplier<String> accountIdGenerator,
                      Supplier<String> customerIdGenerator) {
        this.kycDecryptionPort = kycDecryptionPort;
        this.sanctionsScreeningPort = sanctionsScreeningPort;
        this.accountPort = accountPort;
        this.idempotencyPort = idempotencyPort;
        this.cachePort = cachePort;
        this.eventPort = eventPort;
        this.settings = settings;
        this.clock = clock;
        this.accountIdGenerator = accountIdGenerator;
        this.customerIdGenerator = customerIdGenerator;
    }

    @Override
    @Transactional
    public OnboardingAccountResult createAccount(CreateOnboardingAccountCommand command) {
        Instant now = Instant.now(clock);
        String requestHash = hash(command.requestFingerprint() + '|' + command.interactionId());

        Optional<OnboardingIdempotencyRecord> existing = idempotencyPort.find(command.idempotencyKey(), command.tppId(), now);
        if (existing.isPresent()) {
            OnboardingIdempotencyRecord record = existing.orElseThrow();
            if (!record.requestHash().equals(requestHash)) {
                throw new IdempotencyConflictException("Idempotency conflict");
            }

            OnboardingAccount replay = accountPort.findById(record.accountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found for idempotency record"));
            ensureOwnership(replay, command.tppId());
            return new OnboardingAccountResult(replay, true);
        }

        OnboardingApplicantProfile profile;
        try {
            profile = kycDecryptionPort.decrypt(command.encryptedKycPayload(), command.interactionId());
        } catch (IllegalArgumentException exception) {
            throw new DecryptionFailedException("Decryption Failed");
        }

        if (sanctionsScreeningPort.isBlocked(profile, command.interactionId())) {
            eventPort.publishOnboardingRejected(profile, command.tppId(), "SANCTIONS_HIT");
            throw new ComplianceViolationException("Onboarding Rejected due to sanctions screening");
        }

        OnboardingAccount account = new OnboardingAccount(
                accountIdGenerator.get(),
                command.tppId(),
                customerIdGenerator.get(),
                profile,
                command.preferredCurrency(),
                OnboardingAccountStatus.OPENED,
                null,
                now,
                now
        );

        OnboardingAccount saved = accountPort.save(account);
        idempotencyPort.save(new OnboardingIdempotencyRecord(
                command.idempotencyKey(),
                command.tppId(),
                requestHash,
                saved.accountId(),
                now.plus(settings.idempotencyTtl())
        ));
        eventPort.publishAccountOpened(saved);

        return new OnboardingAccountResult(saved, false);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OnboardingAccountItemResult> getAccount(GetOnboardingAccountQuery query) {
        Instant now = Instant.now(clock);
        String key = cacheKey(query.accountId(), query.tppId());

        Optional<OnboardingAccountItemResult> cached = cachePort.getAccount(key, now);
        if (cached.isPresent()) {
            return Optional.of(cached.orElseThrow().withCacheHit(true));
        }

        Optional<OnboardingAccount> accountOptional = accountPort.findById(query.accountId());
        if (accountOptional.isEmpty()) {
            return Optional.empty();
        }

        OnboardingAccount account = accountOptional.orElseThrow();
        ensureOwnership(account, query.tppId());

        OnboardingAccountItemResult result = new OnboardingAccountItemResult(account, false);
        cachePort.putAccount(key, result, now.plus(settings.cacheTtl()));
        return Optional.of(result);
    }

    private static String cacheKey(String accountId, String tppId) {
        return accountId + ':' + tppId;
    }

    private static void ensureOwnership(OnboardingAccount account, String tppId) {
        if (!account.belongsTo(tppId)) {
            throw new ForbiddenException("Account not owned by participant");
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
