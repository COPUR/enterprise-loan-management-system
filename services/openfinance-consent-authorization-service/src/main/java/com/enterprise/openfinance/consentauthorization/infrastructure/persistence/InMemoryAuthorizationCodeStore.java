package com.enterprise.openfinance.consentauthorization.infrastructure.persistence;

import com.enterprise.openfinance.consentauthorization.domain.model.AuthorizationCodeGrant;
import com.enterprise.openfinance.consentauthorization.domain.port.out.AuthorizationCodeStore;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryAuthorizationCodeStore implements AuthorizationCodeStore {

    private final ConcurrentHashMap<String, AuthorizationCodeGrant> grants = new ConcurrentHashMap<>();

    @Override
    public AuthorizationCodeGrant save(AuthorizationCodeGrant grant) {
        grants.put(grant.getCode(), grant);
        return grant;
    }

    @Override
    public Optional<AuthorizationCodeGrant> findByCode(String code) {
        return Optional.ofNullable(grants.get(code));
    }
}

