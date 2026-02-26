package com.enterprise.openfinance.fxservices.infrastructure.persistence;

import com.enterprise.openfinance.fxservices.domain.model.FxIdempotencyRecord;
import com.enterprise.openfinance.fxservices.domain.port.out.FxIdempotencyPort;
import com.enterprise.openfinance.fxservices.infrastructure.config.FxServicesCacheProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryFxIdempotencyAdapter implements FxIdempotencyPort {

    private final ConcurrentHashMap<String, FxIdempotencyRecord> records = new ConcurrentHashMap<>();
    private final int maxEntries;

    @Autowired
    public InMemoryFxIdempotencyAdapter(FxServicesCacheProperties properties) {
        this(properties.getMaxEntries());
    }

    InMemoryFxIdempotencyAdapter(int maxEntries) {
        this.maxEntries = Math.max(1, maxEntries);
    }

    @Override
    public Optional<FxIdempotencyRecord> find(String key, String tppId, Instant now) {
        String composite = compositeKey(key, tppId);
        FxIdempotencyRecord record = records.get(composite);
        if (record == null || !record.isActiveAt(now)) {
            records.remove(composite);
            return Optional.empty();
        }
        return Optional.of(record);
    }

    @Override
    public void save(FxIdempotencyRecord record) {
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
