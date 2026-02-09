package com.enterprise.openfinance.uc06.domain.port.out;

import com.enterprise.openfinance.uc06.domain.model.PaymentTransaction;

import java.util.Optional;

public interface PaymentTransactionPort {
    PaymentTransaction save(PaymentTransaction transaction);

    Optional<PaymentTransaction> findByPaymentId(String paymentId);
}
