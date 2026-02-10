package com.enterprise.openfinance.uc13.domain.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetPayRequestStatusQueryTest {

    @Test
    void shouldNormalizeFields() {
        GetPayRequestStatusQuery query = new GetPayRequestStatusQuery(" CONS-001 ", " TPP-001 ", "ix-uc13-2");

        assertThat(query.consentId()).isEqualTo("CONS-001");
        assertThat(query.tppId()).isEqualTo("TPP-001");
    }

    @Test
    void shouldRejectInvalidFields() {
        assertThatThrownBy(() -> new GetPayRequestStatusQuery(" ", "TPP-001", "ix"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consentId");

        assertThatThrownBy(() -> new GetPayRequestStatusQuery("CONS", " ", "ix"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tppId");

        assertThatThrownBy(() -> new GetPayRequestStatusQuery("CONS", "TPP", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("interactionId");
    }
}
