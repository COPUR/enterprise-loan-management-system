package com.enterprise.openfinance.consent.infrastructure.persistence;

import com.enterprise.openfinance.consent.domain.model.Consent;
import com.enterprise.openfinance.consent.domain.port.out.ConsentStore;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryConsentStore implements ConsentStore {

    private final ConcurrentHashMap<String, Consent> store = new ConcurrentHashMap<>();

    @Override
    public Consent save(Consent consent) {
        store.put(consent.getConsentId(), consent);
        return consent;
    }

    @Override
    public Optional<Consent> findById(String consentId) {
        return Optional.ofNullable(store.get(consentId));
    }

    @Override
    public List<Consent> findByCustomerId(String customerId) {
        List<Consent> result = new ArrayList<>();
        for (Consent consent : store.values()) {
            if (customerId.equals(consent.getCustomerId())) {
                result.add(consent);
            }
        }
        return result;
    }
}
