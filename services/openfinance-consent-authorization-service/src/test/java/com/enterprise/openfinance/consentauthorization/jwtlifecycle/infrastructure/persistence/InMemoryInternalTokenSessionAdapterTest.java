package com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.persistence;

import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalTokenSession;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryInternalTokenSessionAdapterTest {

    @Test
    void shouldSaveDeactivateAndCheckActivity() {
        InMemoryInternalTokenSessionAdapter adapter = new InMemoryInternalTokenSessionAdapter();
        Instant now = Instant.parse("2026-02-25T00:00:00Z");
        InternalTokenSession first = InternalTokenSession.active("jti-1", "svc", now, now.plusSeconds(300));
        InternalTokenSession second = InternalTokenSession.active("jti-2", "svc", now, now.plusSeconds(300));

        adapter.save(first);
        adapter.save(second);

        assertThat(adapter.isActive("jti-1", now.plusSeconds(1))).isTrue();
        assertThat(adapter.isActive("jti-2", now.plusSeconds(1))).isTrue();

        adapter.deactivate("jti-1", now.plusSeconds(2));
        assertThat(adapter.isActive("jti-1", now.plusSeconds(3))).isFalse();
        assertThat(adapter.isActive("jti-2", now.plusSeconds(3))).isTrue();

        adapter.deactivateAllForSubject("svc", now.plusSeconds(4));
        assertThat(adapter.isActive("jti-2", now.plusSeconds(5))).isFalse();
    }
}
