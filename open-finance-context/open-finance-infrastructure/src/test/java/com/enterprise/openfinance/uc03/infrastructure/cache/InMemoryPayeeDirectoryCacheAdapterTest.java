package com.enterprise.openfinance.uc03.infrastructure.cache;

import com.enterprise.openfinance.uc03.domain.model.AccountStatus;
import com.enterprise.openfinance.uc03.domain.model.DirectoryEntry;
import com.enterprise.openfinance.uc03.infrastructure.config.CoPCacheProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryPayeeDirectoryCacheAdapterTest {

    @Test
    void shouldReturnCachedValueBeforeExpiry() {
        CoPCacheProperties properties = new CoPCacheProperties();
        properties.setMaxEntries(10);
        InMemoryPayeeDirectoryCacheAdapter cache = new InMemoryPayeeDirectoryCacheAdapter(properties);
        DirectoryEntry entry = new DirectoryEntry("IBAN", "GB82WEST12345698765432", "Al Tareq Trading LLC", AccountStatus.ACTIVE);
        Instant now = Instant.parse("2026-02-09T10:00:00Z");

        cache.put("key", entry, now.plusSeconds(30));

        assertThat(cache.get("key", now)).contains(entry);
    }

    @Test
    void shouldEvictExpiredEntries() {
        CoPCacheProperties properties = new CoPCacheProperties();
        properties.setMaxEntries(10);
        InMemoryPayeeDirectoryCacheAdapter cache = new InMemoryPayeeDirectoryCacheAdapter(properties);
        DirectoryEntry entry = new DirectoryEntry("IBAN", "GB82WEST12345698765432", "Al Tareq Trading LLC", AccountStatus.ACTIVE);
        Instant now = Instant.parse("2026-02-09T10:00:00Z");

        cache.put("key", entry, now.plusSeconds(1));

        assertThat(cache.get("key", now.plusSeconds(2))).isEmpty();
    }

    @Test
    void shouldEvictWhenCapacityExceeded() {
        CoPCacheProperties properties = new CoPCacheProperties();
        properties.setTtl(Duration.ofSeconds(30));
        properties.setMaxEntries(1);
        InMemoryPayeeDirectoryCacheAdapter cache = new InMemoryPayeeDirectoryCacheAdapter(properties);
        Instant now = Instant.parse("2026-02-09T10:00:00Z");
        DirectoryEntry first = new DirectoryEntry("IBAN", "GB82WEST12345698765432", "First", AccountStatus.ACTIVE);
        DirectoryEntry second = new DirectoryEntry("IBAN", "DE89370400440532013000", "Second", AccountStatus.CLOSED);

        cache.put("first", first, now.plusSeconds(30));
        cache.put("second", second, now.plusSeconds(30));

        assertThat(cache.get("second", now)).contains(second);
    }
}
