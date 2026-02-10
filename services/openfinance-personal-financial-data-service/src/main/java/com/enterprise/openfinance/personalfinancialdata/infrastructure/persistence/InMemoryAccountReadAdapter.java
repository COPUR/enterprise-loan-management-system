package com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence;

import com.enterprise.openfinance.personalfinancialdata.domain.model.AccountSnapshot;
import com.enterprise.openfinance.personalfinancialdata.domain.port.out.AccountReadPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(prefix = "openfinance.personalfinancialdata.persistence", name = "mode", havingValue = "inmemory")
public class InMemoryAccountReadAdapter implements AccountReadPort {

    private final Map<String, AccountSnapshot> accounts = new ConcurrentHashMap<>();

    public InMemoryAccountReadAdapter() {
        seed();
    }

    @Override
    public List<AccountSnapshot> findByPsuId(String psuId) {
        return accounts.values().stream()
                .filter(account -> account.psuId().equals(psuId))
                .sorted(java.util.Comparator.comparing(AccountSnapshot::accountId))
                .toList();
    }

    @Override
    public Optional<AccountSnapshot> findById(String accountId) {
        return Optional.ofNullable(accounts.get(accountId));
    }

    private void seed() {
        accounts.put("ACC-001", new AccountSnapshot(
                "ACC-001",
                "PSU-001",
                "AE210001000000123456789",
                "AED",
                "Current",
                "Enabled",
                "IBAN",
                "Retail AED"
        ));
        accounts.put("ACC-002", new AccountSnapshot(
                "ACC-002",
                "PSU-001",
                "AE430001000000000000999",
                "USD",
                "Current",
                "Enabled",
                "IBAN",
                "Corporate USD"
        ));
        accounts.put("ACC-003", new AccountSnapshot(
                "ACC-003",
                "PSU-002",
                "AE550001000000000003333",
                "AED",
                "Savings",
                "Enabled",
                "IBAN",
                "Other User Account"
        ));
    }
}
