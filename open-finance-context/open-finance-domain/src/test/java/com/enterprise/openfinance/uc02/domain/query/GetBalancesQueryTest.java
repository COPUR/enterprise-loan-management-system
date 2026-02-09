package com.enterprise.openfinance.uc02.domain.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetBalancesQueryTest {

    @Test
    void shouldCreateQuery() {
        GetBalancesQuery query = new GetBalancesQuery("CONS-1", "TPP-1", "ACC-1", "ix-1");

        assertThat(query.accountId()).isEqualTo("ACC-1");
    }

    @Test
    void shouldRejectInvalidQuery() {
        assertThatThrownBy(() -> new GetBalancesQuery("CONS-1", "", "ACC-1", "ix-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tppId");

        assertThatThrownBy(() -> new GetBalancesQuery("", "TPP-1", "ACC-1", "ix-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consentId");

        assertThatThrownBy(() -> new GetBalancesQuery("CONS-1", "TPP-1", "", "ix-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountId");

        assertThatThrownBy(() -> new GetBalancesQuery("CONS-1", "TPP-1", "ACC-1", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("interactionId");
    }
}
