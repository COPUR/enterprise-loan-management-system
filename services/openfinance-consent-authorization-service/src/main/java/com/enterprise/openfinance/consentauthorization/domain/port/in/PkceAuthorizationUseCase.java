package com.enterprise.openfinance.consentauthorization.domain.port.in;

import com.enterprise.openfinance.consentauthorization.domain.command.AuthorizeWithPkceCommand;
import com.enterprise.openfinance.consentauthorization.domain.command.ExchangeAuthorizationCodeCommand;
import com.enterprise.openfinance.consentauthorization.domain.model.AuthorizationRedirect;
import com.enterprise.openfinance.consentauthorization.domain.model.TokenResult;

public interface PkceAuthorizationUseCase {

    AuthorizationRedirect authorize(AuthorizeWithPkceCommand command);

    TokenResult exchange(ExchangeAuthorizationCodeCommand command);
}

