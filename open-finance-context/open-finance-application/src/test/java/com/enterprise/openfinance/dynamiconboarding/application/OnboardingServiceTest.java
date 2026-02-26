package com.enterprise.openfinance.dynamiconboarding.application;

import com.enterprise.openfinance.dynamiconboarding.domain.command.CreateOnboardingAccountCommand;
import com.enterprise.openfinance.dynamiconboarding.domain.exception.ComplianceViolationException;
import com.enterprise.openfinance.dynamiconboarding.domain.exception.DecryptionFailedException;
import com.enterprise.openfinance.dynamiconboarding.domain.exception.ForbiddenException;
import com.enterprise.openfinance.dynamiconboarding.domain.exception.IdempotencyConflictException;
import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingAccount;
import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingAccountItemResult;
import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingAccountResult;
import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingAccountStatus;
import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingApplicantProfile;
import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingIdempotencyRecord;
import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingSettings;
import com.enterprise.openfinance.dynamiconboarding.domain.port.out.KycDecryptionPort;
import com.enterprise.openfinance.dynamiconboarding.domain.port.out.OnboardingAccountPort;
import com.enterprise.openfinance.dynamiconboarding.domain.port.out.OnboardingCachePort;
import com.enterprise.openfinance.dynamiconboarding.domain.port.out.OnboardingEventPort;
import com.enterprise.openfinance.dynamiconboarding.domain.port.out.OnboardingIdempotencyPort;
import com.enterprise.openfinance.dynamiconboarding.domain.port.out.SanctionsScreeningPort;
import com.enterprise.openfinance.dynamiconboarding.domain.query.GetOnboardingAccountQuery;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

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
class OnboardingServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-02-09T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void shouldCreateOnboardingAccountAndCacheRead() {
        TestOnboardingAccountPort accountPort = new TestOnboardingAccountPort();
        TestOnboardingCachePort cachePort = new TestOnboardingCachePort();

        OnboardingService service = service(
                new SuccessfulKycPort(),
                new AlwaysAllowSanctionsPort(),
                accountPort,
                new TestOnboardingIdempotencyPort(),
                cachePort
        );

        OnboardingAccountResult created = service.createAccount(new CreateOnboardingAccountCommand(
                "TPP-001",
                "ix-dynamic-onboarding-1",
                "IDEMP-001",
                "jwe:Alice Ahmed|7841987001|AE",
                "USD"
        ));

        Optional<OnboardingAccountItemResult> first = service.getAccount(new GetOnboardingAccountQuery(
                created.account().accountId(),
                "TPP-001",
                "ix-dynamic-onboarding-1"
        ));

        Optional<OnboardingAccountItemResult> second = service.getAccount(new GetOnboardingAccountQuery(
                created.account().accountId(),
                "TPP-001",
                "ix-dynamic-onboarding-1"
        ));

        assertThat(created.idempotencyReplay()).isFalse();
        assertThat(created.account().status()).isEqualTo(OnboardingAccountStatus.OPENED);
        assertThat(first).isPresent();
        assertThat(first.orElseThrow().cacheHit()).isFalse();
        assertThat(second.orElseThrow().cacheHit()).isTrue();
    }

    @Test
    void shouldSupportIdempotentReplayAndRejectConflict() {
        TestOnboardingAccountPort accountPort = new TestOnboardingAccountPort();
        TestOnboardingIdempotencyPort idempotencyPort = new TestOnboardingIdempotencyPort();
        OnboardingService service = service(
                new SuccessfulKycPort(),
                new AlwaysAllowSanctionsPort(),
                accountPort,
                idempotencyPort,
                new TestOnboardingCachePort()
        );

        CreateOnboardingAccountCommand first = new CreateOnboardingAccountCommand(
                "TPP-001",
                "ix-dynamic-onboarding-1",
                "IDEMP-001",
                "jwe:Alice Ahmed|7841987001|AE",
                "USD"
        );
        CreateOnboardingAccountCommand replay = new CreateOnboardingAccountCommand(
                "TPP-001",
                "ix-dynamic-onboarding-1",
                "IDEMP-001",
                "jwe:Alice Ahmed|7841987001|AE",
                "USD"
        );
        CreateOnboardingAccountCommand conflict = new CreateOnboardingAccountCommand(
                "TPP-001",
                "ix-dynamic-onboarding-2",
                "IDEMP-001",
                "jwe:Alice Ahmed|7841987001|AE",
                "EUR"
        );

        OnboardingAccountResult created = service.createAccount(first);
        OnboardingAccountResult replayed = service.createAccount(replay);

        assertThat(created.idempotencyReplay()).isFalse();
        assertThat(replayed.idempotencyReplay()).isTrue();
        assertThat(replayed.account().accountId()).isEqualTo(created.account().accountId());

        assertThatThrownBy(() -> service.createAccount(conflict))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void shouldRejectDecryptionSanctionsUnauthorizedAndMissingResources() {
        TestOnboardingAccountPort accountPort = new TestOnboardingAccountPort();
        OnboardingService service = service(
                new SuccessfulKycPort(),
                new AlwaysAllowSanctionsPort(),
                accountPort,
                new TestOnboardingIdempotencyPort(),
                new TestOnboardingCachePort()
        );

        assertThatThrownBy(() -> service.createAccount(new CreateOnboardingAccountCommand(
                "TPP-001",
                "ix-dynamic-onboarding-1",
                "IDEMP-BAD",
                "not-jwe",
                "USD"
        ))).isInstanceOf(DecryptionFailedException.class)
                .hasMessageContaining("Decryption Failed");

        OnboardingService sanctionsService = service(
                new SuccessfulKycPort(),
                new BlockNameSanctionsPort(),
                new TestOnboardingAccountPort(),
                new TestOnboardingIdempotencyPort(),
                new TestOnboardingCachePort()
        );

        assertThatThrownBy(() -> sanctionsService.createAccount(new CreateOnboardingAccountCommand(
                "TPP-001",
                "ix-dynamic-onboarding-1",
                "IDEMP-SANCTION",
                "jwe:TEST_BLOCKED|7841987001|AE",
                "USD"
        ))).isInstanceOf(ComplianceViolationException.class)
                .hasMessageContaining("Onboarding Rejected");

        OnboardingAccountResult created = service.createAccount(new CreateOnboardingAccountCommand(
                "TPP-001",
                "ix-dynamic-onboarding-1",
                "IDEMP-OK-1",
                "jwe:Alice Ahmed|7841987001|AE",
                "USD"
        ));

        assertThatThrownBy(() -> service.getAccount(new GetOnboardingAccountQuery(
                created.account().accountId(),
                "TPP-OTHER",
                "ix-dynamic-onboarding-1"
        ))).isInstanceOf(ForbiddenException.class);

        assertThat(service.getAccount(new GetOnboardingAccountQuery("ACC-404", "TPP-001", "ix-dynamic-onboarding-1"))).isEmpty();

    }

    private static OnboardingService service(KycDecryptionPort kycDecryptionPort,
                                             SanctionsScreeningPort sanctionsScreeningPort,
                                             OnboardingAccountPort accountPort,
                                             OnboardingIdempotencyPort idempotencyPort,
                                             OnboardingCachePort cachePort) {
        return new OnboardingService(
                kycDecryptionPort,
                sanctionsScreeningPort,
                accountPort,
                idempotencyPort,
                cachePort,
                new NoOpOnboardingEventPort(),
                new OnboardingSettings(Duration.ofHours(24), Duration.ofSeconds(30), "ACC"),
                CLOCK,
                () -> "ACC-" + UUID.randomUUID(),
                () -> "CIF-" + UUID.randomUUID()
        );
    }

    private static final class SuccessfulKycPort implements KycDecryptionPort {
        @Override
        public OnboardingApplicantProfile decrypt(String encryptedPayload, String interactionId) {
            if (!encryptedPayload.startsWith("jwe:")) {
                throw new IllegalArgumentException("Decryption Failed");
            }
            String[] tokens = encryptedPayload.substring(4).split("\\|");
            return new OnboardingApplicantProfile(tokens[0], tokens[1], tokens[2]);
        }
    }

    private static final class AlwaysAllowSanctionsPort implements SanctionsScreeningPort {
        @Override
        public boolean isBlocked(OnboardingApplicantProfile profile, String interactionId) {
            return false;
        }
    }

    private static final class BlockNameSanctionsPort implements SanctionsScreeningPort {
        @Override
        public boolean isBlocked(OnboardingApplicantProfile profile, String interactionId) {
            return profile.fullName().contains("TEST_BLOCKED");
        }
    }

    private static final class TestOnboardingAccountPort implements OnboardingAccountPort {
        private final Map<String, OnboardingAccount> data = new ConcurrentHashMap<>();

        @Override
        public OnboardingAccount save(OnboardingAccount account) {
            data.put(account.accountId(), account);
            return account;
        }

        @Override
        public Optional<OnboardingAccount> findById(String accountId) {
            return Optional.ofNullable(data.get(accountId));
        }
    }

    private static final class TestOnboardingIdempotencyPort implements OnboardingIdempotencyPort {
        private final Map<String, OnboardingIdempotencyRecord> data = new ConcurrentHashMap<>();

        @Override
        public Optional<OnboardingIdempotencyRecord> find(String key, String tppId, Instant now) {
            String composite = key + ':' + tppId;
            OnboardingIdempotencyRecord record = data.get(composite);
            if (record == null || !record.isActiveAt(now)) {
                data.remove(composite);
                return Optional.empty();
            }
            return Optional.of(record);
        }

        @Override
        public void save(OnboardingIdempotencyRecord record) {
            data.put(record.idempotencyKey() + ':' + record.tppId(), record);
        }
    }

    private static final class TestOnboardingCachePort implements OnboardingCachePort {
        private final Map<String, Entry<OnboardingAccountItemResult>> data = new ConcurrentHashMap<>();

        @Override
        public Optional<OnboardingAccountItemResult> getAccount(String key, Instant now) {
            Entry<OnboardingAccountItemResult> entry = data.get(key);
            if (entry == null || !entry.expiresAt().isAfter(now)) {
                data.remove(key);
                return Optional.empty();
            }
            return Optional.of(entry.value().withCacheHit(true));
        }

        @Override
        public void putAccount(String key, OnboardingAccountItemResult result, Instant expiresAt) {
            data.put(key, new Entry<>(result.withCacheHit(false), expiresAt));
        }
    }

    private static final class NoOpOnboardingEventPort implements OnboardingEventPort {
        @Override
        public void publishAccountOpened(OnboardingAccount account) {
        }

        @Override
        public void publishOnboardingRejected(OnboardingApplicantProfile profile, String tppId, String reason) {
        }
    }

    private record Entry<T>(T value, Instant expiresAt) {
    }
}
