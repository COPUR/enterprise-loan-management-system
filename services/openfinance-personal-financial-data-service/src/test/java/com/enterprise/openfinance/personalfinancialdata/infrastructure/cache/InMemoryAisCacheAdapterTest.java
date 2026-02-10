package com.enterprise.openfinance.personalfinancialdata.infrastructure.cache;

import com.enterprise.openfinance.personalfinancialdata.domain.model.AccountSnapshot;
import com.enterprise.openfinance.personalfinancialdata.domain.model.BalanceSnapshot;
import com.enterprise.openfinance.personalfinancialdata.infrastructure.config.AisCacheProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryAisCacheAdapterTest {

    @Test
    void shouldReturnAccountsBeforeExpiry() {
        InMemoryAisCacheAdapter adapter = new InMemoryAisCacheAdapter(properties(10));
        Instant now = Instant.parse("2026-02-09T10:00:00Z");

        adapter.putAccounts("k1", List.of(account("ACC-001")), now.plusSeconds(30));

        assertThat(adapter.getAccounts("k1", now.plusSeconds(10))).isPresent();
    }

    @Test
    void shouldEvictExpiredAccountsAndBalances() {
        InMemoryAisCacheAdapter adapter = new InMemoryAisCacheAdapter(properties(10));
        Instant now = Instant.parse("2026-02-09T10:00:00Z");

        adapter.putAccounts("k1", List.of(account("ACC-001")), now.plusSeconds(5));
        adapter.putBalances("k2", List.of(balance("ACC-001")), now.plusSeconds(5));

        assertThat(adapter.getAccounts("k1", now.plusSeconds(10))).isEmpty();
        assertThat(adapter.getBalances("k2", now.plusSeconds(10))).isEmpty();
    }

    @Test
    void shouldEvictWhenCapacityExceeded() {
        InMemoryAisCacheAdapter adapter = new InMemoryAisCacheAdapter(properties(1));
        Instant now = Instant.parse("2026-02-09T10:00:00Z");

        adapter.putAccounts("k1", List.of(account("ACC-001")), now.plusSeconds(30));
        adapter.putAccounts("k2", List.of(account("ACC-002")), now.plusSeconds(30));

        assertThat(adapter.getAccounts("k2", now.plusSeconds(1))).isPresent();
        assertThat(adapter.getAccounts("k1", now.plusSeconds(1))).isEmpty();
    }

    private static AisCacheProperties properties(int maxEntries) {
        AisCacheProperties properties = new AisCacheProperties();
        properties.setTtl(Duration.ofSeconds(30));
        properties.setMaxEntries(maxEntries);
        return properties;
    }

    private static AccountSnapshot account(String accountId) {
        return new AccountSnapshot(
                accountId,
                "PSU-001",
                "AE210001000000123456789",
                "AED",
                "Current",
                "Enabled",
                "IBAN",
                "Primary"
        );
    }

    private static BalanceSnapshot balance(String accountId) {
        return new BalanceSnapshot(accountId, "InterimAvailable", new BigDecimal("100.00"), "AED", Instant.parse("2026-02-09T09:00:00Z"));
    }
}
