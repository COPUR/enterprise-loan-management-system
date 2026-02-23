package com.enterprise.openfinance.consentauthorization.domain.port.out;

import com.enterprise.openfinance.consentauthorization.domain.model.AuthorizationCodeGrant;

import java.util.Optional;

public interface AuthorizationCodeStore {

    AuthorizationCodeGrant save(AuthorizationCodeGrant grant);

    Optional<AuthorizationCodeGrant> findByCode(String code);
}

