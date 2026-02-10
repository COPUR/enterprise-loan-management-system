package com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence;

import com.enterprise.openfinance.personalfinancialdata.domain.model.TransactionSnapshot;
import com.enterprise.openfinance.personalfinancialdata.domain.port.out.TransactionReadPort;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryTransactionReadAdapter implements TransactionReadPort {

    private final Map<String, List<TransactionSnapshot>> data = new ConcurrentHashMap<>();

    public InMemoryTransactionReadAdapter() {
        seed();
    }

    @Override
    public List<TransactionSnapshot> findByAccountId(String accountId) {
        return data.getOrDefault(accountId, List.of());
    }

    private void seed() {
        Instant start = Instant.parse("2026-01-01T10:00:00Z");

        List<TransactionSnapshot> acc001 = new ArrayList<>();
        for (int i = 1; i <= 150; i++) {
            Instant booking = start.plus(i - 1L, ChronoUnit.DAYS);
            acc001.add(new TransactionSnapshot(
                    "TXN-AED-" + i,
                    "ACC-001",
                    new BigDecimal("10.00").add(new BigDecimal(i)),
                    "AED",
                    booking,
                    booking,
                    i % 2 == 0 ? "Debit" : "Credit",
                    "Booked",
                    "Merchant " + i
            ));
        }

        List<TransactionSnapshot> acc002 = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            Instant booking = start.plus(i - 1L, ChronoUnit.DAYS);
            acc002.add(new TransactionSnapshot(
                    "TXN-USD-" + i,
                    "ACC-002",
                    new BigDecimal("25.00").add(new BigDecimal(i)),
                    "USD",
                    booking,
                    booking,
                    i % 2 == 0 ? "Debit" : "Credit",
                    "Booked",
                    "US Merchant " + i
            ));
        }

        data.put("ACC-001", List.copyOf(acc001));
        data.put("ACC-002", List.copyOf(acc002));
        data.put("ACC-003", List.of(
                new TransactionSnapshot(
                        "TXN-OTH-1",
                        "ACC-003",
                        new BigDecimal("50.00"),
                        "AED",
                        start,
                        start,
                        "Debit",
                        "Booked",
                        "Other Merchant"
                )
        ));
    }
}
