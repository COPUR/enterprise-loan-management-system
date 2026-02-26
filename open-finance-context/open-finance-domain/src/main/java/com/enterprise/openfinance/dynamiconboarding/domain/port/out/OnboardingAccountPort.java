package com.enterprise.openfinance.dynamiconboarding.domain.port.out;

import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingAccount;

import java.util.Optional;

public interface OnboardingAccountPort {

    OnboardingAccount save(OnboardingAccount account);

    Optional<OnboardingAccount> findById(String accountId);
}
