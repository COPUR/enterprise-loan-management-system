package com.enterprise.openfinance.uc06.infrastructure.persistence;

import com.enterprise.openfinance.uc06.domain.model.PaymentTransaction;
import com.enterprise.openfinance.uc06.domain.port.out.PaymentTransactionPort;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPaymentTransactionAdapter implements PaymentTransactionPort {

    private final Map<String, PaymentTransaction> data = new ConcurrentHashMap<>();

    @Override
    public PaymentTransaction save(PaymentTransaction transaction) {
        data.put(transaction.paymentId(), transaction);
        return transaction;
    }

    @Override
    public Optional<PaymentTransaction> findByPaymentId(String paymentId) {
        return Optional.ofNullable(data.get(paymentId));
    }
}
