package com.enterprise.openfinance.uc05.infrastructure.cache;

import com.enterprise.openfinance.uc05.domain.model.CorporateAccountListResult;
import com.enterprise.openfinance.uc05.domain.model.CorporateBalanceListResult;
import com.enterprise.openfinance.uc05.domain.model.CorporatePagedResult;
import com.enterprise.openfinance.uc05.domain.model.CorporateTransactionSnapshot;
import com.enterprise.openfinance.uc05.infrastructure.config.Uc05CacheProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryCorporateCacheAdapterTest {

    @Test
    void shouldCacheAndExpireAllValueTypes() {
        InMemoryCorporateCacheAdapter adapter = new InMemoryCorporateCacheAdapter(properties(10));
        Instant now = Instant.parse("2026-02-09T10:00:00Z");

        adapter.putAccounts("accounts", new CorporateAccountListResult(List.of(), false), now.plusSeconds(5));
        adapter.putBalances("balances", new CorporateBalanceListResult(List.of(), false, false), now.plusSeconds(5));
        adapter.putTransactions("transactions", new CorporatePagedResult<>(List.of(transaction()), 1, 100, 1, false), now.plusSeconds(5));

        assertThat(adapter.getAccounts("accounts", now.plusSeconds(1))).isPresent();
        assertThat(adapter.getBalances("balances", now.plusSeconds(1))).isPresent();
        assertThat(adapter.getTransactions("transactions", now.plusSeconds(1))).isPresent();

        assertThat(adapter.getAccounts("accounts", now.plusSeconds(10))).isEmpty();
        assertThat(adapter.getBalances("balances", now.plusSeconds(10))).isEmpty();
        assertThat(adapter.getTransactions("transactions", now.plusSeconds(10))).isEmpty();
    }

    @Test
    void shouldEvictWhenCapacityExceeded() {
        InMemoryCorporateCacheAdapter adapter = new InMemoryCorporateCacheAdapter(properties(1));
        Instant now = Instant.parse("2026-02-09T10:00:00Z");

        adapter.putAccounts("k1", new CorporateAccountListResult(List.of(), false), now.plusSeconds(30));
        adapter.putAccounts("k2", new CorporateAccountListResult(List.of(), false), now.plusSeconds(30));

        assertThat(adapter.getAccounts("k2", now.plusSeconds(1))).isPresent();
        assertThat(adapter.getAccounts("k1", now.plusSeconds(1))).isEmpty();
    }

    private static Uc05CacheProperties properties(int maxEntries) {
        Uc05CacheProperties properties = new Uc05CacheProperties();
        properties.setTtl(Duration.ofSeconds(30));
        properties.setMaxEntries(maxEntries);
        return properties;
    }

    private static CorporateTransactionSnapshot transaction() {
        return new CorporateTransactionSnapshot(
                "TX-001",
                "ACC-M-001",
                new BigDecimal("100.00"),
                "AED",
                Instant.parse("2026-02-05T00:00:00Z"),
                "SWEEP",
                "ZBA",
                "desc"
        );
    }
}
