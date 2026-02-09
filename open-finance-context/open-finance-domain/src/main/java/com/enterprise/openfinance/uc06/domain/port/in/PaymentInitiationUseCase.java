package com.enterprise.openfinance.uc06.domain.port.in;

import com.enterprise.openfinance.uc06.domain.command.SubmitPaymentCommand;
import com.enterprise.openfinance.uc06.domain.model.PaymentResult;
import com.enterprise.openfinance.uc06.domain.model.PaymentTransaction;

import java.util.Optional;

public interface PaymentInitiationUseCase {
    PaymentResult submitPayment(SubmitPaymentCommand command);

    Optional<PaymentTransaction> getPayment(String paymentId);
}
