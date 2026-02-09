package com.enterprise.openfinance.uc03.infrastructure.persistence;

import com.enterprise.openfinance.uc03.domain.model.AccountStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryPayeeDirectoryAdapterTest {

    private final InMemoryPayeeDirectoryAdapter adapter = new InMemoryPayeeDirectoryAdapter();

    @Test
    void shouldReturnSeededActiveEntry() {
        var entry = adapter.findBySchemeAndIdentification("IBAN", "GB82WEST12345698765432");

        assertThat(entry).isPresent();
        assertThat(entry.orElseThrow().legalName()).isEqualTo("Al Tareq Trading LLC");
        assertThat(entry.orElseThrow().accountStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void shouldReturnSeededClosedEntry() {
        var entry = adapter.findBySchemeAndIdentification("IBAN", "DE89370400440532013000");

        assertThat(entry).isPresent();
        assertThat(entry.orElseThrow().accountStatus()).isEqualTo(AccountStatus.CLOSED);
    }

    @Test
    void shouldReturnEmptyForUnknownEntry() {
        assertThat(adapter.findBySchemeAndIdentification("IBAN", "FR7630006000011234567890189")).isEmpty();
    }
}
