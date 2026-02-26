package com.enterprise.openfinance.payeeverification.application;

import com.enterprise.openfinance.payeeverification.domain.model.AccountStatus;
import com.enterprise.openfinance.payeeverification.domain.model.ConfirmationRequest;
import com.enterprise.openfinance.payeeverification.domain.model.ConfirmationResult;
import com.enterprise.openfinance.payeeverification.domain.model.ConfirmationSettings;
import com.enterprise.openfinance.payeeverification.domain.model.DirectoryEntry;
import com.enterprise.openfinance.payeeverification.domain.model.NameMatchDecision;
import com.enterprise.openfinance.payeeverification.domain.port.out.NameSimilarityPort;
import com.enterprise.openfinance.payeeverification.domain.port.out.PayeeAuditLogPort;
import com.enterprise.openfinance.payeeverification.domain.port.out.PayeeDirectoryCachePort;
import com.enterprise.openfinance.payeeverification.domain.port.out.PayeeDirectoryPort;
import com.enterprise.openfinance.payeeverification.domain.service.ConfirmationDecisionPolicy;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class ConfirmationOfPayeeServiceTest {

    private static final Instant NOW = Instant.parse("2026-02-09T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void shouldReturnMatchForExactName() {
        TestDirectory directory = new TestDirectory();
        directory.add("IBAN", "GB82WEST12345698765432", "Al Tareq Trading LLC", AccountStatus.ACTIVE);
        TestSimilarity similarity = new TestSimilarity(100);
        TestAudit audit = new TestAudit();
        TestCache cache = new TestCache();
        ConfirmationOfPayeeService service = service(directory, cache, audit, similarity);

        ConfirmationResult result = service.confirm(request("Al Tareq Trading LLC"));

        assertThat(result.accountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(result.nameMatched()).isEqualTo(NameMatchDecision.MATCH);
        assertThat(result.matchedName()).isNull();
        assertThat(result.matchScore()).isEqualTo(100);
        assertThat(result.fromCache()).isFalse();
        assertThat(audit.count).isEqualTo(1);
    }

    @Test
    void shouldReturnCloseMatchAndMatchedNameWhenScoreAboveThreshold() {
        TestDirectory directory = new TestDirectory();
        directory.add("IBAN", "GB82WEST12345698765432", "Al Tareq Trading LLC", AccountStatus.ACTIVE);
        TestSimilarity similarity = new TestSimilarity(90);
        ConfirmationOfPayeeService service = service(directory, new TestCache(), new TestAudit(), similarity);

        ConfirmationResult result = service.confirm(request("Al Tariq Trading LLC"));

        assertThat(result.nameMatched()).isEqualTo(NameMatchDecision.CLOSE_MATCH);
        assertThat(result.matchedName()).isEqualTo("Al Tareq Trading LLC");
        assertThat(result.matchScore()).isEqualTo(90);
    }

    @Test
    void shouldReturnNoMatchWhenScoreBelowThreshold() {
        TestDirectory directory = new TestDirectory();
        directory.add("IBAN", "GB82WEST12345698765432", "Al Tareq Trading LLC", AccountStatus.ACTIVE);
        TestSimilarity similarity = new TestSimilarity(60);
        ConfirmationOfPayeeService service = service(directory, new TestCache(), new TestAudit(), similarity);

        ConfirmationResult result = service.confirm(request("Random Corp"));

        assertThat(result.nameMatched()).isEqualTo(NameMatchDecision.NO_MATCH);
        assertThat(result.matchedName()).isNull();
        assertThat(result.matchScore()).isEqualTo(60);
    }

    @Test
    void shouldReturnUnableToCheckForClosedAccount() {
        TestDirectory directory = new TestDirectory();
        directory.add("IBAN", "GB82WEST12345698765432", "Closed Account", AccountStatus.CLOSED);
        ConfirmationOfPayeeService service = service(directory, new TestCache(), new TestAudit(), new TestSimilarity(100));

        ConfirmationResult result = service.confirm(request("Closed Account"));

        assertThat(result.accountStatus()).isEqualTo(AccountStatus.CLOSED);
        assertThat(result.nameMatched()).isEqualTo(NameMatchDecision.UNABLE_TO_CHECK);
        assertThat(result.matchScore()).isEqualTo(0);
    }

    @Test
    void shouldRejectInvalidIbanWhenSchemeIsIban() {
        ConfirmationOfPayeeService service = service(new TestDirectory(), new TestCache(), new TestAudit(), new TestSimilarity(100));

        assertThatThrownBy(() -> service.confirm(new ConfirmationRequest(
                "INVALID-IBAN",
                "IBAN",
                "Any Name",
                "TPP-ABC",
                "ix-1"
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid IBAN");
    }

    @Test
    void shouldUseCachedDirectoryEntryOnSubsequentCalls() {
        TestDirectory directory = new TestDirectory();
        directory.add("IBAN", "GB82WEST12345698765432", "Al Tareq Trading LLC", AccountStatus.ACTIVE);
        TestCache cache = new TestCache();
        ConfirmationOfPayeeService service = service(directory, cache, new TestAudit(), new TestSimilarity(100));

        ConfirmationResult first = service.confirm(request("Al Tareq Trading LLC"));
        ConfirmationResult second = service.confirm(request("Al Tareq Trading LLC"));

        assertThat(first.fromCache()).isFalse();
        assertThat(second.fromCache()).isTrue();
        assertThat(directory.findCount.get()).isEqualTo(1);
    }

    @Test
    void shouldReturnUnableToCheckWhenDirectoryEntryNotFound() {
        ConfirmationOfPayeeService service = service(new TestDirectory(), new TestCache(), new TestAudit(), new TestSimilarity(100));

        ConfirmationResult result = service.confirm(request("Unknown"));

        assertThat(result.accountStatus()).isEqualTo(AccountStatus.UNKNOWN);
        assertThat(result.nameMatched()).isEqualTo(NameMatchDecision.UNABLE_TO_CHECK);
        assertThat(result.matchScore()).isEqualTo(0);
    }

    private static ConfirmationRequest request(String name) {
        return new ConfirmationRequest(
                "GB82WEST12345698765432",
                "IBAN",
                name,
                "TPP-ABC",
                "interaction-001"
        );
    }

    private static ConfirmationOfPayeeService service(
            PayeeDirectoryPort directory,
            PayeeDirectoryCachePort cache,
            PayeeAuditLogPort audit,
            NameSimilarityPort similarity) {
        return new ConfirmationOfPayeeService(
                directory,
                cache,
                audit,
                similarity,
                new ConfirmationDecisionPolicy(85),
                new ConfirmationSettings(85, Duration.ofSeconds(30)),
                CLOCK
        );
    }

    private static final class TestDirectory implements PayeeDirectoryPort {
        private final Map<String, DirectoryEntry> data = new HashMap<>();
        private final AtomicInteger findCount = new AtomicInteger();

        private void add(String schemeName, String identification, String legalName, AccountStatus status) {
            data.put(key(schemeName, identification), new DirectoryEntry(schemeName, identification, legalName, status));
        }

        @Override
        public Optional<DirectoryEntry> findBySchemeAndIdentification(String schemeName, String identification) {
            findCount.incrementAndGet();
            return Optional.ofNullable(data.get(key(schemeName, identification)));
        }

        private static String key(String schemeName, String identification) {
            return schemeName + ":" + identification;
        }
    }

    private static final class TestCache implements PayeeDirectoryCachePort {
        private final Map<String, CacheItem> cache = new HashMap<>();

        @Override
        public Optional<DirectoryEntry> get(String key, Instant now) {
            CacheItem item = cache.get(key);
            if (item == null || !item.expiresAt.isAfter(now)) {
                cache.remove(key);
                return Optional.empty();
            }
            return Optional.of(item.entry);
        }

        @Override
        public void put(String key, DirectoryEntry entry, Instant expiresAt) {
            cache.put(key, new CacheItem(entry, expiresAt));
        }

        private record CacheItem(DirectoryEntry entry, Instant expiresAt) {
        }
    }

    private static final class TestAudit implements PayeeAuditLogPort {
        private int count;

        @Override
        public void log(com.enterprise.openfinance.payeeverification.domain.model.ConfirmationAuditRecord record) {
            count++;
        }
    }

    private static final class TestSimilarity implements NameSimilarityPort {
        private final int score;

        private TestSimilarity(int score) {
            this.score = score;
        }

        @Override
        public int similarityScore(String left, String right) {
            return score;
        }
    }
}
