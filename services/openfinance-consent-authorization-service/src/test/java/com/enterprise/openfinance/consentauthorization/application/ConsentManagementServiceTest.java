package com.enterprise.openfinance.consentauthorization.application;

import com.enterprise.openfinance.consentauthorization.domain.command.CreateConsentCommand;
import com.enterprise.openfinance.consentauthorization.domain.model.Consent;
import com.enterprise.openfinance.consentauthorization.domain.model.ConsentStatus;
import com.enterprise.openfinance.consentauthorization.domain.port.out.ConsentStore;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsentManagementServiceTest {

    private static final Instant NOW = Instant.parse("2026-02-09T10:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void shouldCreateAuthorizeAndRevokeConsent() {
        ConsentStore store = new InMemoryStore();
        ConsentManagementService service = new ConsentManagementService(store, FIXED_CLOCK);

        CreateConsentCommand command = new CreateConsentCommand(
                "CUST-1001",
                "TPP-01",
                java.util.Set.of("read_accounts", "read_balances"),
                "Personal finance management",
                NOW.plusSeconds(3600)
        );

        Consent created = service.createConsent(command);
        assertThat(created.getConsentId()).startsWith("CONSENT-");
        assertThat(created.getStatus()).isEqualTo(ConsentStatus.PENDING);

        Optional<Consent> authorized = service.authorizeConsent(created.getConsentId());
        assertThat(authorized).isPresent();
        assertThat(authorized.orElseThrow().getStatus()).isEqualTo(ConsentStatus.AUTHORIZED);

        Optional<Consent> revoked = service.revokeConsent(created.getConsentId(), "Customer request");
        assertThat(revoked).isPresent();
        assertThat(revoked.orElseThrow().getStatus()).isEqualTo(ConsentStatus.REVOKED);
        assertThat(revoked.orElseThrow().getRevocationReason()).isEqualTo("Customer request");
    }

    @Test
    void shouldListConsentsByCustomer() {
        ConsentStore store = new InMemoryStore();
        ConsentManagementService service = new ConsentManagementService(store, FIXED_CLOCK);

        service.createConsent(new CreateConsentCommand(
                "CUST-2001", "TPP-01", java.util.Set.of("read_accounts"), "PFM", NOW.plusSeconds(7200)));
        service.createConsent(new CreateConsentCommand(
                "CUST-2001", "TPP-02", java.util.Set.of("read_transactions"), "PFM", NOW.plusSeconds(7200)));
        service.createConsent(new CreateConsentCommand(
                "CUST-OTHER", "TPP-03", java.util.Set.of("read_accounts"), "PFM", NOW.plusSeconds(7200)));

        List<Consent> consents = service.listConsentsByCustomer("CUST-2001");
        assertThat(consents).hasSize(2);
    }

    @Test
    void shouldRejectConsentWithInvalidPermissions() {
        ConsentStore store = new InMemoryStore();
        ConsentManagementService service = new ConsentManagementService(store, FIXED_CLOCK);

        assertThatThrownBy(() -> service.createConsent(new CreateConsentCommand(
                "CUST-3001",
                "TPP-01",
                Set.of("ReadAdminData"),
                "PFM",
                NOW.plusSeconds(7200)
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid scope");
    }

    @Test
    void shouldPersistExpiredStateAndBlockAuthorization() {
        MutableClock clock = new MutableClock(NOW);
        InMemoryStore store = new InMemoryStore();
        ConsentManagementService service = new ConsentManagementService(store, clock);

        Consent created = service.createConsent(new CreateConsentCommand(
                "CUST-3002",
                "TPP-01",
                Set.of("ReadAccounts"),
                "PFM",
                NOW.plusSeconds(30)
        ));

        clock.setInstant(NOW.plusSeconds(31));

        assertThatThrownBy(() -> service.authorizeConsent(created.getConsentId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");

        Consent persisted = store.findById(created.getConsentId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(ConsentStatus.EXPIRED);
    }

    @Test
    void shouldPersistExpiredStateWhenReadingConsent() {
        MutableClock clock = new MutableClock(NOW);
        InMemoryStore store = new InMemoryStore();
        ConsentManagementService service = new ConsentManagementService(store, clock);

        Consent created = service.createConsent(new CreateConsentCommand(
                "CUST-3003",
                "TPP-01",
                Set.of("ReadAccounts"),
                "PFM",
                NOW.plusSeconds(10)
        ));

        clock.setInstant(NOW.plusSeconds(20));

        Optional<Consent> fetched = service.getConsent(created.getConsentId());
        assertThat(fetched).isPresent();
        assertThat(fetched.orElseThrow().getStatus()).isEqualTo(ConsentStatus.EXPIRED);
        assertThat(store.findById(created.getConsentId()).orElseThrow().getStatus())
                .isEqualTo(ConsentStatus.EXPIRED);
    }

    @Test
    void shouldValidateDependentScopeChecksForActiveConsent() {
        InMemoryStore store = new InMemoryStore();
        ConsentManagementService service = new ConsentManagementService(store, FIXED_CLOCK);

        Consent consent = service.createConsent(new CreateConsentCommand(
                "CUST-3004",
                "TPP-02",
                Set.of("ReadAccounts", "ReadBalances"),
                "PFM",
                NOW.plusSeconds(7200)
        ));

        service.authorizeConsent(consent.getConsentId());

        assertThat(service.hasActiveConsentForScopes(consent.getConsentId(), Set.of("read_accounts")))
                .isTrue();
        assertThat(service.hasActiveConsentForScopes(consent.getConsentId(), Set.of("read_transactions")))
                .isFalse();

        service.revokeConsent(consent.getConsentId(), "Customer requested revocation");
        assertThat(service.hasActiveConsentForScopes(consent.getConsentId(), Set.of("read_accounts")))
                .isFalse();
    }

    @Test
    void shouldReturnFalseForUnknownConsentInScopeValidation() {
        ConsentStore store = new InMemoryStore();
        ConsentManagementService service = new ConsentManagementService(store, FIXED_CLOCK);

        assertThatCode(() -> assertThat(service.hasActiveConsentForScopes(
                "CONSENT-UNKNOWN",
                Set.of("read_accounts")
        )).isFalse()).doesNotThrowAnyException();
    }

    @Test
    void shouldReturnFalseForBlankConsentIdInScopeValidation() {
        ConsentStore store = new InMemoryStore();
        ConsentManagementService service = new ConsentManagementService(store, FIXED_CLOCK);

        assertThat(service.hasActiveConsentForScopes(" ", Set.of("read_accounts"))).isFalse();
    }

    private static final class InMemoryStore implements ConsentStore {
        private final ConcurrentHashMap<String, Consent> data = new ConcurrentHashMap<>();

        @Override
        public Consent save(Consent consent) {
            data.put(consent.getConsentId(), consent);
            return consent;
        }

        @Override
        public Optional<Consent> findById(String consentId) {
            return Optional.ofNullable(data.get(consentId));
        }

        @Override
        public List<Consent> findByCustomerId(String customerId) {
            List<Consent> consents = new ArrayList<>();
            for (Consent consent : data.values()) {
                if (customerId.equals(consent.getCustomerId())) {
                    consents.add(consent);
                }
            }
            return consents;
        }
    }

    private static final class MutableClock extends Clock {
        private Instant current;

        private MutableClock(Instant initial) {
            this.current = initial;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }

        private void setInstant(Instant newCurrent) {
            this.current = newCurrent;
        }
    }
}
