package com.enterprise.openfinance.uc04.infrastructure.persistence;

import com.enterprise.openfinance.uc04.domain.model.PartyMetadata;
import com.enterprise.openfinance.uc04.domain.port.out.PartyMetadataReadPort;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPartyMetadataReadAdapter implements PartyMetadataReadPort {

    private final Map<String, List<PartyMetadata>> partiesByAccount = new ConcurrentHashMap<>();

    public InMemoryPartyMetadataReadAdapter() {
        seed();
    }

    @Override
    public List<PartyMetadata> findByAccountId(String accountId) {
        return partiesByAccount.getOrDefault(accountId, List.of());
    }

    private void seed() {
        partiesByAccount.put("ACC-001", List.of(
                new PartyMetadata("ACC-001", "Al Tareq Trading LLC", "VERIFIED", Instant.parse("2018-05-01T00:00:00Z"))
        ));

        partiesByAccount.put("ACC-002", List.of(
                new PartyMetadata("ACC-002", "Al Tareq Treasury Unit", "VERIFIED", Instant.parse("2019-02-10T00:00:00Z"))
        ));
    }
}
