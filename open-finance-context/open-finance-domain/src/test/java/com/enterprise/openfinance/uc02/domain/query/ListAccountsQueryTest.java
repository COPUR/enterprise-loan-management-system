package com.enterprise.openfinance.uc02.domain.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListAccountsQueryTest {

    @Test
    void shouldCreateQuery() {
        ListAccountsQuery query = new ListAccountsQuery("CONS-1", "TPP-1", "ix-1");

        assertThat(query.consentId()).isEqualTo("CONS-1");
    }

    @Test
    void shouldRejectInvalidQuery() {
        assertThatThrownBy(() -> new ListAccountsQuery("", "TPP-1", "ix-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consentId");

        assertThatThrownBy(() -> new ListAccountsQuery("CONS-1", "", "ix-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tppId");

        assertThatThrownBy(() -> new ListAccountsQuery("CONS-1", "TPP-1", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("interactionId");
    }
}
