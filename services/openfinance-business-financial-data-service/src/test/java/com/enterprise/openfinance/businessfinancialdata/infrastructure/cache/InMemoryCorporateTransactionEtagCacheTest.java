package com.enterprise.openfinance.businessfinancialdata.infrastructure.cache;

import com.enterprise.openfinance.businessfinancialdata.infrastructure.config.CorporateCacheProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryCorporateTransactionEtagCacheTest {

    @Test
    void shouldExpireEtagEntries() {
        CorporateCacheProperties properties = new CorporateCacheProperties();
        properties.setEtagTtl(Duration.ofSeconds(10));

        MutableClock clock = new MutableClock(Instant.parse("2026-02-10T10:00:00Z"));
        InMemoryCorporateTransactionEtagCache cache = new InMemoryCorporateTransactionEtagCache(properties, clock);
        cache.put("signature-1", "\"etag-1\"");
        assertThat(cache.get("signature-1")).contains("\"etag-1\"");

        clock.setNow(Instant.parse("2026-02-10T10:00:20Z"));
        assertThat(cache.get("signature-1")).isEmpty();
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant initialNow) {
            this.now = initialNow;
        }

        private void setNow(Instant nextNow) {
            this.now = nextNow;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
