package com.enterprise.openfinance.consentauthorization.domain.port.in;

import com.enterprise.openfinance.consentauthorization.domain.command.CreateConsentCommand;
import com.enterprise.openfinance.consentauthorization.domain.model.Consent;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ConsentManagementUseCase {

    Consent createConsent(CreateConsentCommand command);

    Optional<Consent> getConsent(String consentId);

    List<Consent> listConsentsByCustomer(String customerId);

    Optional<Consent> authorizeConsent(String consentId);

    Optional<Consent> revokeConsent(String consentId, String reason);

    boolean hasActiveConsentForScopes(String consentId, Set<String> requiredScopes);
}
