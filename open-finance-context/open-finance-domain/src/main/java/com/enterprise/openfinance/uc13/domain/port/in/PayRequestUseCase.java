package com.enterprise.openfinance.uc13.domain.port.in;

import com.enterprise.openfinance.uc13.domain.command.CreatePayRequestCommand;
import com.enterprise.openfinance.uc13.domain.model.PayRequestResult;
import com.enterprise.openfinance.uc13.domain.query.GetPayRequestStatusQuery;

public interface PayRequestUseCase {

    PayRequestResult createPayRequest(CreatePayRequestCommand command);

    PayRequestResult getPayRequestStatus(GetPayRequestStatusQuery query);

    PayRequestResult acceptPayRequest(String consentId, String tppId, String paymentId, String interactionId);

    PayRequestResult rejectPayRequest(String consentId, String tppId, String interactionId);
}
