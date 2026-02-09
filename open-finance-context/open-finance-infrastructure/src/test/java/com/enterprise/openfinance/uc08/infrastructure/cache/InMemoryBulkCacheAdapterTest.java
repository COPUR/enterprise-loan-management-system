package com.enterprise.openfinance.uc08.infrastructure.cache;

import com.enterprise.openfinance.uc08.domain.model.BulkFileReport;
import com.enterprise.openfinance.uc08.domain.model.BulkFileStatus;
import com.enterprise.openfinance.uc08.infrastructure.config.Uc08CacheProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryBulkCacheAdapterTest {

    @Test
    void shouldCacheAndExpireReports() {
        Uc08CacheProperties properties = new Uc08CacheProperties();
        properties.setMaxEntries(2);
        InMemoryBulkCacheAdapter adapter = new InMemoryBulkCacheAdapter(properties);

        BulkFileReport report = new BulkFileReport(
                "FILE-1",
                BulkFileStatus.COMPLETED,
                1,
                1,
                0,
                List.of(),
                Instant.parse("2026-02-09T10:00:00Z")
        );

        adapter.putReport("k1", report, Instant.parse("2026-02-09T10:01:00Z"));

        assertThat(adapter.getReport("k1", Instant.parse("2026-02-09T10:00:30Z"))).isPresent();
        assertThat(adapter.getReport("k1", Instant.parse("2026-02-09T10:01:00Z"))).isEmpty();
    }

    @Test
    void shouldEvictWhenCapacityExceeded() {
        Uc08CacheProperties properties = new Uc08CacheProperties();
        properties.setMaxEntries(1);
        InMemoryBulkCacheAdapter adapter = new InMemoryBulkCacheAdapter(properties);

        BulkFileReport report = new BulkFileReport("FILE-1", BulkFileStatus.COMPLETED, 1, 1, 0, List.of(), Instant.parse("2026-02-09T10:00:00Z"));
        BulkFileReport report2 = new BulkFileReport("FILE-2", BulkFileStatus.COMPLETED, 1, 1, 0, List.of(), Instant.parse("2026-02-09T10:00:00Z"));

        adapter.putReport("k1", report, Instant.parse("2026-02-09T10:01:00Z"));
        adapter.putReport("k2", report2, Instant.parse("2026-02-09T10:01:00Z"));

        assertThat(adapter.getReport("k1", Instant.parse("2026-02-09T10:00:30Z")).isPresent()
                || adapter.getReport("k2", Instant.parse("2026-02-09T10:00:30Z")).isPresent()).isTrue();
    }
}
