package com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence;

import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporateAccountSnapshot;
import com.enterprise.openfinance.businessfinancialdata.domain.port.out.CorporateAccountReadPort;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryCorporateAccountReadAdapter implements CorporateAccountReadPort {

    private final Map<String, CorporateAccountSnapshot> accountsById = new ConcurrentHashMap<>();

    public InMemoryCorporateAccountReadAdapter() {
        seed();
    }

    @Override
    public java.util.List<CorporateAccountSnapshot> findByCorporateId(String corporateId) {
        return accountsById.values().stream()
                .filter(account -> account.corporateId().equals(corporateId))
                .sorted(java.util.Comparator.comparing(CorporateAccountSnapshot::accountId))
                .toList();
    }

    @Override
    public Optional<CorporateAccountSnapshot> findById(String accountId) {
        return Optional.ofNullable(accountsById.get(accountId));
    }

    private void seed() {
        accountsById.put("ACC-M-001", new CorporateAccountSnapshot(
                "ACC-M-001",
                "CORP-001",
                null,
                "AE210001000000123456789",
                "AED",
                "Enabled",
                "Current",
                false
        ));

        accountsById.put("ACC-V-101", new CorporateAccountSnapshot(
                "ACC-V-101",
                "CORP-001",
                "ACC-M-001",
                "AE430001000000000000101",
                "AED",
                "Enabled",
                "Virtual",
                true
        ));

        accountsById.put("ACC-V-102", new CorporateAccountSnapshot(
                "ACC-V-102",
                "CORP-001",
                "ACC-M-001",
                "AE430001000000000000102",
                "AED",
                "Enabled",
                "Virtual",
                true
        ));

        accountsById.put("ACC-M-002", new CorporateAccountSnapshot(
                "ACC-M-002",
                "CORP-001",
                null,
                "AE210001000000123456790",
                "USD",
                "Enabled",
                "Current",
                false
        ));

        accountsById.put("ACC-M-999", new CorporateAccountSnapshot(
                "ACC-M-999",
                "CORP-999",
                null,
                "AE210001000000999999999",
                "AED",
                "Enabled",
                "Current",
                false
        ));
    }
}
