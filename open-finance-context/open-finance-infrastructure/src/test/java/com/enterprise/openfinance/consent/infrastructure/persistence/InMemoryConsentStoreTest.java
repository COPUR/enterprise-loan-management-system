package com.enterprise.openfinance.consent.infrastructure.persistence;

import com.enterprise.openfinance.consent.domain.command.CreateConsentCommand;
import com.enterprise.openfinance.consent.domain.model.Consent;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryConsentStoreTest {

    private static final Instant NOW = Instant.parse("2026-02-09T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void shouldSaveAndFindConsentById() {
        InMemoryConsentStore store = new InMemoryConsentStore();
        Consent consent = consent("CUST-1", "TPP-1");

        Consent saved = store.save(consent);
        Optional<Consent> found = store.findById(consent.getConsentId());

        assertThat(saved).isSameAs(consent);
        assertThat(found).containsSame(consent);
    }

    @Test
    void shouldReturnEmptyForUnknownConsentId() {
        InMemoryConsentStore store = new InMemoryConsentStore();

        assertThat(store.findById("CONSENT-UNKNOWN")).isEmpty();
    }

    @Test
    void shouldFindConsentsByCustomerId() {
        InMemoryConsentStore store = new InMemoryConsentStore();
        Consent first = consent("CUST-2", "TPP-1");
        Consent second = consent("CUST-2", "TPP-2");
        Consent third = consent("CUST-3", "TPP-3");
        store.save(first);
        store.save(second);
        store.save(third);

        List<Consent> customerConsents = store.findByCustomerId("CUST-2");

        assertThat(customerConsents).containsExactlyInAnyOrder(first, second);
        assertThat(customerConsents).doesNotContain(third);
    }

    private static Consent consent(String customerId, String participantId) {
        return Consent.create(
                new CreateConsentCommand(
                        customerId,
                        participantId,
                        Set.of("ReadAccounts"),
                        "PFM",
                        NOW.plusSeconds(3600)
                ),
                CLOCK
        );
    }
}
