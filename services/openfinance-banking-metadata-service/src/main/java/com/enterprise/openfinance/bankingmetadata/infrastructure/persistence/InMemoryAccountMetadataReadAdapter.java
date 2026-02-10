package com.enterprise.openfinance.bankingmetadata.infrastructure.persistence;

import com.enterprise.openfinance.bankingmetadata.domain.model.AccountSchemeMetadata;
import com.enterprise.openfinance.bankingmetadata.domain.port.out.AccountMetadataReadPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(prefix = "openfinance.bankingmetadata.persistence", name = "mode", havingValue = "inmemory")
public class InMemoryAccountMetadataReadAdapter implements AccountMetadataReadPort {

    private final Map<String, AccountSchemeMetadata> metadataByAccount = new ConcurrentHashMap<>();

    public InMemoryAccountMetadataReadAdapter() {
        seed();
    }

    @Override
    public Optional<AccountSchemeMetadata> findByAccountId(String accountId) {
        return Optional.ofNullable(metadataByAccount.get(accountId));
    }

    private void seed() {
        metadataByAccount.put("ACC-001", new AccountSchemeMetadata(
                "ACC-001",
                "IBAN",
                "MOBILE:+971500000001"
        ));

        metadataByAccount.put("ACC-002", new AccountSchemeMetadata(
                "ACC-002",
                "IBAN",
                null
        ));
    }
}
