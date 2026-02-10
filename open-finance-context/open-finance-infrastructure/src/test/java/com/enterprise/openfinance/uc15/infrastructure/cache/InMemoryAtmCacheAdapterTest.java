package com.enterprise.openfinance.uc15.infrastructure.cache;

import com.enterprise.openfinance.uc15.domain.model.AtmListResult;
import com.enterprise.openfinance.uc15.domain.model.AtmLocation;
import com.enterprise.openfinance.uc15.domain.model.AtmStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryAtmCacheAdapterTest {

    @Test
    void shouldEvictExpiredEntries() {
        InMemoryAtmCacheAdapter adapter = new InMemoryAtmCacheAdapter();
        AtmListResult result = new AtmListResult(List.of(sample()), false);

        adapter.putAtms("key", result, Instant.parse("2026-02-10T10:05:00Z"));

        assertThat(adapter.getAtms("key", Instant.parse("2026-02-10T10:00:00Z"))).isPresent();
        assertThat(adapter.getAtms("key", Instant.parse("2026-02-10T10:06:00Z"))).isEmpty();
    }

    private static AtmLocation sample() {
        return new AtmLocation(
                "ATM-001",
                "ATM",
                AtmStatus.IN_SERVICE,
                25.2048,
                55.2708,
                "Sheikh Zayed Road",
                "Dubai",
                "AE",
                "Full",
                List.of("CashWithdrawal"),
                Instant.parse("2026-02-10T00:00:00Z")
        );
    }
}
