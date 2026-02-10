package com.enterprise.openfinance.consentauthorization.application;

import com.enterprise.openfinance.consentauthorization.domain.command.CreateConsentCommand;
import com.enterprise.openfinance.consentauthorization.domain.model.Consent;
import com.enterprise.openfinance.consentauthorization.domain.model.ConsentStatus;
import com.enterprise.openfinance.consentauthorization.domain.port.in.ConsentManagementUseCase;
import com.enterprise.openfinance.consentauthorization.domain.port.out.ConsentStore;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ConsentManagementService implements ConsentManagementUseCase {

    private final ConsentStore consentStore;
    private final Clock clock;

    public ConsentManagementService(ConsentStore consentStore, Clock clock) {
        this.consentStore = consentStore;
        this.clock = clock;
    }

    @Override
    public Consent createConsent(CreateConsentCommand command) {
        Consent consent = Consent.create(command, clock);
        return consentStore.save(consent);
    }

    @Override
    public Optional<Consent> getConsent(String consentId) {
        return consentStore.findById(consentId).map(this::refreshExpiryState);
    }

    @Override
    public List<Consent> listConsentsByCustomer(String customerId) {
        return consentStore.findByCustomerId(customerId).stream()
                .map(this::refreshExpiryState)
                .toList();
    }

    @Override
    public Optional<Consent> authorizeConsent(String consentId) {
        return consentStore.findById(consentId)
                .map(consent -> {
                    try {
                        consent.authorize(Instant.now(clock));
                        return consentStore.save(consent);
                    } catch (RuntimeException exception) {
                        // Persist state transitions (for example AUTHORIZED -> EXPIRED) even when authorization fails.
                        consentStore.save(consent);
                        throw exception;
                    }
                });
    }

    @Override
    public Optional<Consent> revokeConsent(String consentId, String reason) {
        return consentStore.findById(consentId)
                .map(consent -> {
                    consent.revoke(reason, Instant.now(clock));
                    consentStore.save(consent);
                    return consent;
                });
    }

    @Override
    public boolean hasActiveConsentForScopes(String consentId, Set<String> requiredScopes) {
        if (consentId == null || consentId.isBlank()) {
            return false;
        }
        return consentStore.findById(consentId)
                .map(this::refreshExpiryState)
                .filter(consent -> consent.getStatus() == ConsentStatus.AUTHORIZED)
                .map(consent -> consent.coversScopes(requiredScopes))
                .orElse(false);
    }

    private Consent refreshExpiryState(Consent consent) {
        ConsentStatus previousStatus = consent.getStatus();
        consent.isActive(Instant.now(clock));
        if (previousStatus != consent.getStatus()) {
            consentStore.save(consent);
        }
        return consent;
    }
}
