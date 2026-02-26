package com.enterprise.openfinance.dynamiconboarding.infrastructure.persistence;

import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingIdempotencyRecord;
import com.enterprise.openfinance.dynamiconboarding.domain.port.out.OnboardingIdempotencyPort;
import com.enterprise.openfinance.dynamiconboarding.infrastructure.config.DynamicOnboardingCacheProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryOnboardingIdempotencyAdapter implements OnboardingIdempotencyPort {

    private final ConcurrentHashMap<String, OnboardingIdempotencyRecord> records = new ConcurrentHashMap<>();
    private final int maxEntries;

    @Autowired
    public InMemoryOnboardingIdempotencyAdapter(DynamicOnboardingCacheProperties properties) {
        this(properties.getMaxEntries());
    }

    InMemoryOnboardingIdempotencyAdapter(int maxEntries) {
        this.maxEntries = Math.max(1, maxEntries);
    }

    @Override
    public Optional<OnboardingIdempotencyRecord> find(String key, String tppId, Instant now) {
        String composite = compositeKey(key, tppId);
        OnboardingIdempotencyRecord record = records.get(composite);
        if (record == null || !record.isActiveAt(now)) {
            records.remove(composite);
            return Optional.empty();
        }
        return Optional.of(record);
    }

    @Override
    public void save(OnboardingIdempotencyRecord record) {
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
