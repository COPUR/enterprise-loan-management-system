package com.enterprise.openfinance.bankingmetadata.infrastructure.cache;

import com.enterprise.openfinance.bankingmetadata.domain.model.AccountSchemeMetadata;
import com.enterprise.openfinance.bankingmetadata.domain.model.FxDetails;
import com.enterprise.openfinance.bankingmetadata.domain.model.GeoLocation;
import com.enterprise.openfinance.bankingmetadata.domain.model.MetadataItemResult;
import com.enterprise.openfinance.bankingmetadata.domain.model.MetadataListResult;
import com.enterprise.openfinance.bankingmetadata.domain.model.MetadataQueryResult;
import com.enterprise.openfinance.bankingmetadata.domain.model.PartyMetadata;
import com.enterprise.openfinance.bankingmetadata.domain.model.StandingOrderMetadata;
import com.enterprise.openfinance.bankingmetadata.domain.model.TransactionMetadata;
import com.enterprise.openfinance.bankingmetadata.infrastructure.config.BankingMetadataCacheProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryMetadataCacheAdapterTest {

    @Test
    void shouldCacheAndExpireAllValueTypes() {
        InMemoryMetadataCacheAdapter adapter = new InMemoryMetadataCacheAdapter(properties(10));
        Instant now = Instant.parse("2026-02-09T10:00:00Z");

        adapter.putTransactions("tx", new MetadataQueryResult<>(List.of(transaction()), 1, 100, 1, false), now.plusSeconds(5));
        adapter.putParties("party", new MetadataListResult<>(List.of(party()), false), now.plusSeconds(5));
        adapter.putAccount("account", new MetadataItemResult<>(new AccountSchemeMetadata("ACC-001", "IBAN", "MOB-1"), false), now.plusSeconds(5));
        adapter.putStandingOrders("so", new MetadataQueryResult<>(List.of(standingOrder()), 1, 100, 1, false), now.plusSeconds(5));

        assertThat(adapter.getTransactions("tx", now.plusSeconds(1))).isPresent();
        assertThat(adapter.getParties("party", now.plusSeconds(1))).isPresent();
        assertThat(adapter.getAccount("account", now.plusSeconds(1))).isPresent();
        assertThat(adapter.getStandingOrders("so", now.plusSeconds(1))).isPresent();

        assertThat(adapter.getTransactions("tx", now.plusSeconds(10))).isEmpty();
        assertThat(adapter.getParties("party", now.plusSeconds(10))).isEmpty();
        assertThat(adapter.getAccount("account", now.plusSeconds(10))).isEmpty();
        assertThat(adapter.getStandingOrders("so", now.plusSeconds(10))).isEmpty();
    }

    @Test
    void shouldEvictWhenCapacityExceeded() {
        InMemoryMetadataCacheAdapter adapter = new InMemoryMetadataCacheAdapter(properties(1));
        Instant now = Instant.parse("2026-02-09T10:00:00Z");

        adapter.putTransactions("k1", new MetadataQueryResult<>(List.of(transaction()), 1, 100, 1, false), now.plusSeconds(30));
        adapter.putTransactions("k2", new MetadataQueryResult<>(List.of(transaction()), 1, 100, 1, false), now.plusSeconds(30));

        assertThat(adapter.getTransactions("k2", now.plusSeconds(1))).isPresent();
        assertThat(adapter.getTransactions("k1", now.plusSeconds(1))).isEmpty();
    }

    private static BankingMetadataCacheProperties properties(int maxEntries) {
        BankingMetadataCacheProperties properties = new BankingMetadataCacheProperties();
        properties.setTtl(Duration.ofSeconds(30));
        properties.setMaxEntries(maxEntries);
        return properties;
    }

    private static TransactionMetadata transaction() {
        return new TransactionMetadata(
                "TXN-001",
                "ACC-001",
                Instant.parse("2026-01-10T00:00:00Z"),
                new BigDecimal("100.00"),
                new BigDecimal("2.00"),
                "AED",
                "Merchant",
                "5411",
                new GeoLocation(25.2048, 55.2708),
                new FxDetails(new BigDecimal("0.27290"), "AED", "USD")
        );
    }

    private static PartyMetadata party() {
        return new PartyMetadata("ACC-001", "Al Tareq Trading LLC", "VERIFIED", Instant.parse("2018-05-01T00:00:00Z"));
    }

    private static StandingOrderMetadata standingOrder() {
        return new StandingOrderMetadata(
                "SO-001",
                "ACC-001",
                "EvryMnth",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-12-01T00:00:00Z"),
                new BigDecimal("500.00"),
                "AED"
        );
    }
}
