package com.enterprise.openfinance.paymentinitiation.domain.port.out;

import com.enterprise.openfinance.paymentinitiation.domain.model.PaymentTransaction;

public interface PaymentEventPort {
    void publishSubmitted(PaymentTransaction transaction);
}
