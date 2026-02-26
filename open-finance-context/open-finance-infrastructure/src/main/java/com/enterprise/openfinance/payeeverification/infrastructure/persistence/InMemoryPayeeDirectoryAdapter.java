package com.enterprise.openfinance.payeeverification.infrastructure.persistence;

import com.enterprise.openfinance.payeeverification.domain.model.AccountStatus;
import com.enterprise.openfinance.payeeverification.domain.model.DirectoryEntry;
import com.enterprise.openfinance.payeeverification.domain.port.out.PayeeDirectoryPort;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPayeeDirectoryAdapter implements PayeeDirectoryPort {

    private final Map<String, DirectoryEntry> entries = new ConcurrentHashMap<>();

    public InMemoryPayeeDirectoryAdapter() {
        seedDefaults();
    }

    @Override
    public Optional<DirectoryEntry> findBySchemeAndIdentification(String schemeName, String identification) {
        return Optional.ofNullable(entries.get(key(schemeName, identification)));
    }

    private void seedDefaults() {
        entries.put(
                key("IBAN", "GB82WEST12345698765432"),
                new DirectoryEntry("IBAN", "GB82WEST12345698765432", "Al Tareq Trading LLC", AccountStatus.ACTIVE)
        );
        entries.put(
                key("IBAN", "DE89370400440532013000"),
                new DirectoryEntry("IBAN", "DE89370400440532013000", "Closed Legacy Account", AccountStatus.CLOSED)
        );
        entries.put(
                key("BBAN", "COP-CACHE-001"),
                new DirectoryEntry("BBAN", "COP-CACHE-001", "Cache Test Beneficiary", AccountStatus.ACTIVE)
        );
    }

    private static String key(String schemeName, String identification) {
        return schemeName.trim().toUpperCase() + ":" + identification.trim().replace(" ", "").toUpperCase();
    }
}
