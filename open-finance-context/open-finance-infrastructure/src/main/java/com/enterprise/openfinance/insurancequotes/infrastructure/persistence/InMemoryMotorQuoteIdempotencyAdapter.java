package com.enterprise.openfinance.insurancequotes.infrastructure.persistence;

import com.enterprise.openfinance.insurancequotes.domain.model.MotorQuoteIdempotencyRecord;
import com.enterprise.openfinance.insurancequotes.domain.port.out.MotorQuoteIdempotencyPort;
import com.enterprise.openfinance.insurancequotes.infrastructure.config.InsuranceQuoteCacheProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryMotorQuoteIdempotencyAdapter implements MotorQuoteIdempotencyPort {

    private final ConcurrentHashMap<String, MotorQuoteIdempotencyRecord> records = new ConcurrentHashMap<>();
    private final int maxEntries;

    @Autowired
    public InMemoryMotorQuoteIdempotencyAdapter(InsuranceQuoteCacheProperties properties) {
        this(properties.getMaxEntries());
    }

    InMemoryMotorQuoteIdempotencyAdapter(int maxEntries) {
        this.maxEntries = Math.max(1, maxEntries);
    }

    @Override
    public Optional<MotorQuoteIdempotencyRecord> find(String key, String tppId, Instant now) {
        String composite = compositeKey(key, tppId);
        MotorQuoteIdempotencyRecord record = records.get(composite);
        if (record == null || !record.isActiveAt(now)) {
            records.remove(composite);
            return Optional.empty();
        }
        return Optional.of(record);
    }

    @Override
    public void save(MotorQuoteIdempotencyRecord record) {
        String composite = compositeKey(record.idempotencyKey(), record.tppId());
        if (records.size() >= maxEntries && !records.containsKey(composite)) {
            String oldest = records.keys().nextElement();
            records.remove(oldest);
        }
        records.put(composite, record);
    }

    private static String compositeKey(String key, String tppId) {
        return key + ':' + tppId;
    }
}
