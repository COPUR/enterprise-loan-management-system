package com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.port.in;

import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.command.UpsertInternalSystemSecretCommand;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalSystemSecretView;

import java.util.Optional;

public interface InternalSystemSecretUseCase {

    InternalSystemSecretView upsert(UpsertInternalSystemSecretCommand command);

    Optional<InternalSystemSecretView> getMetadata(String secretKey);
}
