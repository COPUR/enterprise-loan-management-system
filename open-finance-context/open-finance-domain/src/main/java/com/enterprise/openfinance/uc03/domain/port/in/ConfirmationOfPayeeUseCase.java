package com.enterprise.openfinance.uc03.domain.port.in;

import com.enterprise.openfinance.uc03.domain.model.ConfirmationRequest;
import com.enterprise.openfinance.uc03.domain.model.ConfirmationResult;

public interface ConfirmationOfPayeeUseCase {

    ConfirmationResult confirm(ConfirmationRequest request);
}
