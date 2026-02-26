package com.enterprise.openfinance.paymentinitiation.infrastructure.event;

import com.enterprise.openfinance.paymentinitiation.domain.model.PaymentTransaction;
import com.enterprise.openfinance.paymentinitiation.domain.port.out.PaymentEventPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NoOpPaymentEventAdapter implements PaymentEventPort {

    private static final Logger LOG = LoggerFactory.getLogger(NoOpPaymentEventAdapter.class);

    @Override
    public void publishSubmitted(PaymentTransaction transaction) {
        LOG.debug("Published payment event paymentId={} status={}", transaction.paymentId(), transaction.status());
    }
}
