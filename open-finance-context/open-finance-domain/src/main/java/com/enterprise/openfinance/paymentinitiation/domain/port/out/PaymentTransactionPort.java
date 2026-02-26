package com.enterprise.openfinance.paymentinitiation.domain.port.out;

import com.enterprise.openfinance.paymentinitiation.domain.model.PaymentTransaction;

import java.util.Optional;

public interface PaymentTransactionPort {
    PaymentTransaction save(PaymentTransaction transaction);

    Optional<PaymentTransaction> findByPaymentId(String paymentId);
}
