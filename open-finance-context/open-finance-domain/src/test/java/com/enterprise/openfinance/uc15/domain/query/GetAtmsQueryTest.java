package com.enterprise.openfinance.uc15.domain.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetAtmsQueryTest {

    @Test
    void shouldNormalizeLocationFilters() {
        GetAtmsQuery query = new GetAtmsQuery("  ix-uc15-1 ", 25.2048, 55.2708, 10.0);

        assertThat(query.interactionId()).isEqualTo("ix-uc15-1");
        assertThat(query.normalizedLatitude()).contains(25.2048);
        assertThat(query.normalizedLongitude()).contains(55.2708);
        assertThat(query.normalizedRadiusKm()).contains(10.0);
        assertThat(query.cacheKeySuffix()).contains("lat=25.2048");
    }

    @Test
    void shouldHandleUnfilteredQuery() {
        GetAtmsQuery query = new GetAtmsQuery("ix-uc15-2", null, null, null);

        assertThat(query.normalizedLatitude()).isEmpty();
        assertThat(query.normalizedLongitude()).isEmpty();
        assertThat(query.normalizedRadiusKm()).isEmpty();
        assertThat(query.cacheKeySuffix()).isEqualTo("ALL");
    }

    @Test
    void shouldRejectInvalidInput() {
        assertThatThrownBy(() -> new GetAtmsQuery(" ", null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("interactionId");

        assertThatThrownBy(() -> new GetAtmsQuery("ix", 25.0, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("longitude");

        assertThatThrownBy(() -> new GetAtmsQuery("ix", -95.0, 55.0, 10.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("latitude");

        assertThatThrownBy(() -> new GetAtmsQuery("ix", 25.0, 200.0, 10.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("longitude");

        assertThatThrownBy(() -> new GetAtmsQuery("ix", 25.0, 55.0, -1.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("radius");
    }
}
