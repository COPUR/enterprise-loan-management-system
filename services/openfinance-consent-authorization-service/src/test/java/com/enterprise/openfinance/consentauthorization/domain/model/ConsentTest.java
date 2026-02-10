package com.enterprise.openfinance.consentauthorization.domain.model;

import com.enterprise.openfinance.consentauthorization.domain.command.CreateConsentCommand;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsentTest {

    private static final Instant NOW = Instant.parse("2026-02-09T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void shouldCreatePendingConsentWithNormalizedScopes() {
        Consent consent = Consent.create(command(Set.of("ReadAccounts", " read_balances ", "READ-TRANSACTIONS"), NOW.plusSeconds(1200)), CLOCK);

        assertThat(consent.getConsentId()).startsWith("CONSENT-");
        assertThat(consent.getStatus()).isEqualTo(ConsentStatus.PENDING);
        assertThat(consent.getScopes()).containsExactlyInAnyOrder("READACCOUNTS", "READBALANCES", "READTRANSACTIONS");
        assertThat(consent.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldRejectCreateWhenExpiryIsNotInFuture() {
        assertThatThrownBy(() -> Consent.create(command(Set.of("ReadAccounts"), NOW), CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("expiresAt must be in the future");
    }

    @Test
    void shouldRejectUnsupportedScopeOnCreate() {
        assertThatThrownBy(() -> Consent.create(command(Set.of("ReadAdminData"), NOW.plusSeconds(3600)), CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid scope");
    }

    @Test
    void shouldRejectBlankScopeOnCreate() {
        assertThatThrownBy(() -> Consent.create(command(Set.of(" "), NOW.plusSeconds(3600)), CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scope cannot be blank");
    }

    @Test
    void shouldAuthorizeConsentAndRemainIdempotent() {
        Consent consent = Consent.create(command(Set.of("ReadAccounts"), NOW.plusSeconds(3600)), CLOCK);

        Instant firstAuth = NOW.plusSeconds(30);
        consent.authorize(firstAuth);
        assertThat(consent.getStatus()).isEqualTo(ConsentStatus.AUTHORIZED);
        assertThat(consent.getAuthorizedAt()).isEqualTo(firstAuth);

        consent.authorize(NOW.plusSeconds(60));
        assertThat(consent.getAuthorizedAt()).isEqualTo(firstAuth);
    }

    @Test
    void shouldNotAuthorizeRevokedConsent() {
        Consent consent = Consent.create(command(Set.of("ReadAccounts"), NOW.plusSeconds(3600)), CLOCK);
        consent.revoke("Customer request", NOW.plusSeconds(10));

        assertThatThrownBy(() -> consent.authorize(NOW.plusSeconds(20)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("revoked");
        assertThat(consent.getStatus()).isEqualTo(ConsentStatus.REVOKED);
    }

    @Test
    void shouldMarkConsentExpiredWhenAuthorizationAfterExpiry() {
        Consent consent = Consent.create(command(Set.of("ReadAccounts"), NOW.plusSeconds(10)), CLOCK);

        assertThatThrownBy(() -> consent.authorize(NOW.plusSeconds(11)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");
        assertThat(consent.getStatus()).isEqualTo(ConsentStatus.EXPIRED);
    }

    @Test
    void shouldRequireRevocationReason() {
        Consent consent = Consent.create(command(Set.of("ReadAccounts"), NOW.plusSeconds(3600)), CLOCK);

        assertThatThrownBy(() -> consent.revoke(" ", NOW.plusSeconds(30)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("revocation reason is required");
    }

    @Test
    void shouldKeepInitialRevocationDataWhenRevokedTwice() {
        Consent consent = Consent.create(command(Set.of("ReadAccounts"), NOW.plusSeconds(3600)), CLOCK);
        Instant firstRevokeAt = NOW.plusSeconds(30);

        consent.revoke("Initial reason", firstRevokeAt);
        consent.revoke("Another reason", NOW.plusSeconds(60));

        assertThat(consent.getStatus()).isEqualTo(ConsentStatus.REVOKED);
        assertThat(consent.getRevokedAt()).isEqualTo(firstRevokeAt);
        assertThat(consent.getRevocationReason()).isEqualTo("Initial reason");
    }

    @Test
    void shouldReturnActiveForAuthorizedAndNotExpiredConsent() {
        Consent consent = Consent.create(command(Set.of("ReadAccounts"), NOW.plusSeconds(3600)), CLOCK);
        consent.authorize(NOW.plusSeconds(10));

        assertThat(consent.isActive(NOW.plusSeconds(20))).isTrue();
    }

    @Test
    void shouldTransitionPendingConsentToExpiredWhenCheckingActive() {
        Consent consent = Consent.create(command(Set.of("ReadAccounts"), NOW.plusSeconds(10)), CLOCK);

        assertThat(consent.isActive(NOW.plusSeconds(11))).isFalse();
        assertThat(consent.getStatus()).isEqualTo(ConsentStatus.EXPIRED);
    }

    @Test
    void shouldNotTransitionRevokedConsentToExpiredWhenCheckingActive() {
        Consent consent = Consent.create(command(Set.of("ReadAccounts"), NOW.plusSeconds(10)), CLOCK);
        consent.revoke("Customer request", NOW.plusSeconds(5));

        assertThat(consent.isActive(NOW.plusSeconds(11))).isFalse();
        assertThat(consent.getStatus()).isEqualTo(ConsentStatus.REVOKED);
    }

    @Test
    void shouldEvaluateScopeCoverageWithNormalization() {
        Consent consent = Consent.create(command(Set.of("ReadAccounts", "ReadBalances"), NOW.plusSeconds(3600)), CLOCK);

        assertThat(consent.coversScopes(null)).isTrue();
        assertThat(consent.coversScopes(Set.of())).isTrue();
        assertThat(consent.coversScopes(Set.of("read_accounts"))).isTrue();
        assertThat(consent.coversScopes(Set.of("read-accounts", "READ_BALANCES"))).isTrue();
        assertThat(consent.coversScopes(Set.of("read_transactions"))).isFalse();
    }

    private static CreateConsentCommand command(Set<String> scopes, Instant expiresAt) {
        return new CreateConsentCommand("CUST-9001", "TPP-9001", scopes, "PFM", expiresAt);
    }
}
