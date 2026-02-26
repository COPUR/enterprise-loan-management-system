package com.enterprise.openfinance.paymentinitiation.domain.port.in;

import com.enterprise.openfinance.paymentinitiation.domain.command.SubmitPaymentCommand;
import com.enterprise.openfinance.paymentinitiation.domain.model.PaymentResult;
import com.enterprise.openfinance.paymentinitiation.domain.model.PaymentTransaction;

import java.util.Optional;

public interface PaymentInitiationUseCase {
    PaymentResult submitPayment(SubmitPaymentCommand command);

    Optional<PaymentTransaction> getPayment(String paymentId);
}
