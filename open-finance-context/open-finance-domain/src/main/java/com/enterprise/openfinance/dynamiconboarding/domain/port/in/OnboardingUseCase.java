package com.enterprise.openfinance.dynamiconboarding.domain.port.in;

import com.enterprise.openfinance.dynamiconboarding.domain.command.CreateOnboardingAccountCommand;
import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingAccountItemResult;
import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingAccountResult;
import com.enterprise.openfinance.dynamiconboarding.domain.query.GetOnboardingAccountQuery;

import java.util.Optional;

public interface OnboardingUseCase {

    OnboardingAccountResult createAccount(CreateOnboardingAccountCommand command);

    Optional<OnboardingAccountItemResult> getAccount(GetOnboardingAccountQuery query);
}
