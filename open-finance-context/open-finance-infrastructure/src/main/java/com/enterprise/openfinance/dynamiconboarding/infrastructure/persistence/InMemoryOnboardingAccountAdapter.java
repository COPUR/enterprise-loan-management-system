package com.enterprise.openfinance.dynamiconboarding.infrastructure.persistence;

import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingAccount;
import com.enterprise.openfinance.dynamiconboarding.domain.port.out.OnboardingAccountPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryOnboardingAccountAdapter implements OnboardingAccountPort {

    private final ConcurrentHashMap<String, OnboardingAccount> accounts = new ConcurrentHashMap<>();

    @Override
    public OnboardingAccount save(OnboardingAccount account) {
        accounts.put(account.accountId(), account);
        return account;
    }

    @Override
    public Optional<OnboardingAccount> findById(String accountId) {
        return Optional.ofNullable(accounts.get(accountId));
    }
}
