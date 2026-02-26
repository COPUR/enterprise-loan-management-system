package com.enterprise.openfinance.payeeverification.domain.port.in;

import com.enterprise.openfinance.payeeverification.domain.model.ConfirmationRequest;
import com.enterprise.openfinance.payeeverification.domain.model.ConfirmationResult;

public interface ConfirmationOfPayeeUseCase {

    ConfirmationResult confirm(ConfirmationRequest request);
}
