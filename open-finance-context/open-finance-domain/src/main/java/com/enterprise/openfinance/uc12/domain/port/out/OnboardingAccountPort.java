package com.enterprise.openfinance.uc12.domain.port.out;

import com.enterprise.openfinance.uc12.domain.model.OnboardingAccount;

import java.util.Optional;

public interface OnboardingAccountPort {

    OnboardingAccount save(OnboardingAccount account);

    Optional<OnboardingAccount> findById(String accountId);
}
