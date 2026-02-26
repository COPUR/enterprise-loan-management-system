package com.enterprise.openfinance.bankingmetadata.infrastructure.persistence;

import com.enterprise.openfinance.bankingmetadata.domain.model.FxDetails;
import com.enterprise.openfinance.bankingmetadata.domain.model.GeoLocation;
import com.enterprise.openfinance.bankingmetadata.domain.model.TransactionMetadata;
import com.enterprise.openfinance.bankingmetadata.domain.port.out.TransactionMetadataReadPort;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryTransactionMetadataReadAdapter implements TransactionMetadataReadPort {

    private final Map<String, List<TransactionMetadata>> transactionsByAccount = new ConcurrentHashMap<>();

    public InMemoryTransactionMetadataReadAdapter() {
        seed();
    }

    @Override
    public List<TransactionMetadata> findByAccountId(String accountId) {
        List<TransactionMetadata> transactions = transactionsByAccount.getOrDefault(accountId, List.of());
        return transactions.stream()
                .sorted(Comparator.comparing(TransactionMetadata::bookingDateTime).reversed())
                .toList();
    }

    private void seed() {
        List<TransactionMetadata> acc1Transactions = new ArrayList<>();
        acc1Transactions.add(new TransactionMetadata(
                "TXM-1001",
                "ACC-001",
                Instant.parse("2026-01-15T10:00:00Z"),
                new BigDecimal("120.00"),
                new BigDecimal("2.50"),
                "AED",
                "Spinneys",
                "5411",
                new GeoLocation(25.1972, 55.2744),
                null
        ));
        acc1Transactions.add(new TransactionMetadata(
                "TXM-1002",
                "ACC-001",
                Instant.parse("2026-01-10T08:30:00Z"),
                new BigDecimal("450.00"),
                new BigDecimal("0.00"),
                "AED",
                "Global Trade",
                "5732",
                new GeoLocation(25.2048, 55.2708),
                new FxDetails(new BigDecimal("0.272900"), "AED", "USD")
        ));
        acc1Transactions.add(new TransactionMetadata(
                "TXM-1003",
                "ACC-001",
                Instant.parse("2025-12-28T12:00:00Z"),
                new BigDecimal("75.25"),
                new BigDecimal("1.00"),
                "AED",
                "Fuel Station",
                "5541",
                new GeoLocation(25.2231, 55.3210),
                null
        ));
        transactionsByAccount.put("ACC-001", List.copyOf(acc1Transactions));

        transactionsByAccount.put("ACC-002", List.of(
                new TransactionMetadata(
                        "TXM-2001",
                        "ACC-002",
                        Instant.parse("2026-01-20T09:00:00Z"),
                        new BigDecimal("890.00"),
                        new BigDecimal("0.00"),
                        "USD",
                        "Office Supplier",
                        "5111",
                        new GeoLocation(25.2100, 55.2800),
                        null
                )
        ));
    }
}
