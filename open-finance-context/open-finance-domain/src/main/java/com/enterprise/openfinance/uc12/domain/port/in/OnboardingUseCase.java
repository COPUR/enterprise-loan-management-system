package com.enterprise.openfinance.uc12.domain.port.in;

import com.enterprise.openfinance.uc12.domain.command.CreateOnboardingAccountCommand;
import com.enterprise.openfinance.uc12.domain.model.OnboardingAccountItemResult;
import com.enterprise.openfinance.uc12.domain.model.OnboardingAccountResult;
import com.enterprise.openfinance.uc12.domain.query.GetOnboardingAccountQuery;

import java.util.Optional;

public interface OnboardingUseCase {

    OnboardingAccountResult createAccount(CreateOnboardingAccountCommand command);

    Optional<OnboardingAccountItemResult> getAccount(GetOnboardingAccountQuery query);
}
