package com.enterprise.openfinance.uc06.domain.port.out;

import com.enterprise.openfinance.uc06.domain.model.PaymentTransaction;

public interface PaymentEventPort {
    void publishSubmitted(PaymentTransaction transaction);
}
