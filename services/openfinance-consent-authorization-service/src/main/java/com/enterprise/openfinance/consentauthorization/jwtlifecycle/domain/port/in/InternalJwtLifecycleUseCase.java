package com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.port.in;

import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.command.InternalAuthenticateCommand;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalTokenIssueResult;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalTokenPrincipal;

public interface InternalJwtLifecycleUseCase {

    InternalTokenIssueResult authenticate(InternalAuthenticateCommand command);

    InternalTokenPrincipal validateBearerToken(String authorizationHeader);

    void logout(String authorizationHeader);
}

