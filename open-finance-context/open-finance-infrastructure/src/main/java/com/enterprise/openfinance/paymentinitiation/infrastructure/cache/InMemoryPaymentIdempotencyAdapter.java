package com.enterprise.openfinance.paymentinitiation.infrastructure.cache;

import com.enterprise.openfinance.paymentinitiation.domain.model.IdempotencyRecord;
import com.enterprise.openfinance.paymentinitiation.domain.port.out.PaymentIdempotencyPort;
import com.enterprise.openfinance.paymentinitiation.infrastructure.config.PaymentInitiationIdempotencyProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryPaymentIdempotencyAdapter implements PaymentIdempotencyPort {

    private final Map<String, IdempotencyRecord> data = new ConcurrentHashMap<>();
    private final int maxEntries;

    public InMemoryPaymentIdempotencyAdapter(PaymentInitiationIdempotencyProperties properties) {
        this.maxEntries = properties.getMaxEntries();
    }

    @Override
    public Optional<IdempotencyRecord> find(String idempotencyKey, String tppId, Instant now) {
        String key = key(idempotencyKey, tppId);
        IdempotencyRecord record = data.get(key);
        if (record == null) {
            return Optional.empty();
        }
        if (record.isExpired(now)) {
            data.remove(key);
            return Optional.empty();
        }
        return Optional.of(record);
    }

    @Override
    public void save(IdempotencyRecord record) {
        if (data.size() >= maxEntries) {
            evictOne();
        }
        data.put(key(record.idempotencyKey(), record.tppId()), record);
    }

    private void evictOne() {
        String candidate = data.keySet().stream().findFirst().orElse(null);
        if (candidate != null) {
            data.remove(candidate);
        }
    }

    private static String key(String idempotencyKey, String tppId) {
        return idempotencyKey + ":" + tppId;
    }
}
